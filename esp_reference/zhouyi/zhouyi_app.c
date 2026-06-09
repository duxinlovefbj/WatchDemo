/**
 * @file zhouyi_app.c
 * @brief UI 状态机总控 + 旋钮回调注册
 */

#include <string.h>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "esp_log.h"
#include "lvgl.h"
#include "iot_knob.h"
#include "esp_lv_adapter.h"
#include "driver/gpio.h"

#include "zhouyi_app.h"
#include "ui_taiji.h"
#include "ui_casting.h"
#include "ui_result.h"
#include "data/zhouyi_calc.h"
#include "drv2605l.h"
#include "pinconfig.h"
#include "esp_timer.h"
#include "app_manager.h"

#define TAG "UI_MAIN"

#ifndef SCREEN_SIZE
#define SCREEN_SIZE 360
#endif

/* ========== 状态 ========== */

static zhouyi_app_state_t s_ui_state = ZHOUYI_STATE_WELCOME;
static lv_timer_t *s_poll_timer = NULL;

/* 本模块的根容器，将其隔离在全局环境之外 */
static lv_obj_t *s_app_container = NULL;

/* 后台旋钮轮询任务的运行标志位，方便退出时结束任务 */
static volatile bool s_app_running = false;

static void zhouyi_set_state_from_lvgl(zhouyi_app_state_t state);

extern const app_desc_t main_menu_app;

static void exit_to_main_menu_async(void *param)
{
    app_manager_switch_to(&main_menu_app);
}

void zhouyi_app_exit(void)
{
    lv_async_call(exit_to_main_menu_async, NULL);
}

/* ========== 旋钮事件 ========== */

static volatile uint32_t s_knob_activity_ticks = 0;

/* ========== 纯软件旋钮扫描任务 (起点终点防抖版) ========== */
static void software_knob_task(void *arg)
{
    gpio_config_t io_conf = {
        .intr_type = GPIO_INTR_DISABLE, 
        .pin_bit_mask = (1ULL << GPIO_KNOB_A) | (1ULL << GPIO_KNOB_B),
        .mode = GPIO_MODE_INPUT,
        .pull_up_en = GPIO_PULLUP_ENABLE,
        .pull_down_en = GPIO_PULLDOWN_DISABLE,
    };
    gpio_config(&io_conf);

    int is_turning = 0; // 状态标志：0=静止，1=正在转动过程中
    int first_a = 1, first_b = 1; // 记录首先落下的针脚电平，用于判断方向
    int combo_count = 0;          // 连续同向转动格数
    int last_dir = 0;             // 上一次转动的方向
    uint32_t last_step_time = 0;  // 上一次转动落位的时间戳

    while (s_app_running) {
        uint8_t a = gpio_get_level(GPIO_KNOB_A);
        uint8_t b = gpio_get_level(GPIO_KNOB_B);

        if (is_turning == 0) {
            // 【检测起步】只要离开了 (1,1) 静止态，说明手开始扭了
            if (a == 0 || b == 0) {
                is_turning = 1;
                first_a = a;
                first_b = b;
            }
        } 
        else {
            // 【等待落位】无视中间的各种杂波，死等它重新卡回 (1,1) 的物理段落
            if (a == 1 && b == 1) {
                // 完美转完了一格！判断方向
                int dir = 0;
                if (first_a == 0 && first_b == 1) dir = -1;
                else if (first_a == 1 && first_b == 0) dir = 1;

                if (dir != 0) {
                    uint32_t now = (uint32_t)(esp_timer_get_time() / 1000); // 毫秒
                    
                    // 若停顿超过 300ms (匹配起卦超时) 或 方向发生改变，则连击中断
                    if (now - last_step_time > 300 || dir != last_dir) {
                        combo_count = 1;
                    } else {
                        combo_count++;
                    }
                    last_dir = dir;
                    last_step_time = now;

                    // 核心拦截：连续同向满足 5 格，才放行给上层业务逻辑
                    if (combo_count >= 5) {
                        __atomic_fetch_add(&s_knob_activity_ticks, 1, __ATOMIC_RELAXED);
                    }
                }

                is_turning = 0;
                
                // 【核心防抖】落位后强行屏蔽 20ms，吃掉机械弹片归位时的所有余波
                vTaskDelay(pdMS_TO_TICKS(20)); 
            }
        }

        // 把轮询速度提升到 1ms (1000Hz)，保证丝滑跟手
        vTaskDelay(pdMS_TO_TICKS(1));
    }

    /* 收到退出信号，自行终结任务，不留残影 */
    vTaskDelete(NULL);
}

/* ========== 触摸回调（全屏触摸事件） ========== */

static void screen_touch_cb(lv_event_t *e)
{
    static bool s_zhouyi_ignore_next_click = false;
    lv_event_code_t code = lv_event_get_code(e);

    if (code == LV_EVENT_LONG_PRESSED) {
        s_zhouyi_ignore_next_click = true;
        zhouyi_app_exit();
    } else if (code == LV_EVENT_CLICKED) {
        if (s_zhouyi_ignore_next_click) {
            s_zhouyi_ignore_next_click = false;
            return;
        }
        
        if (s_ui_state == ZHOUYI_STATE_WELCOME) {
            zhouyi_set_state_from_lvgl(ZHOUYI_STATE_CASTING);
        }
    }
}

/* ========== LVGL 轮询定时器（运行在 LVGL 任务，完全安全） ========== */
static void poll_timer_cb(lv_timer_t *timer)
{
    // 原子操作提取并清零活动计数
    uint32_t total = __atomic_exchange_n(&s_knob_activity_ticks, 0, __ATOMIC_RELAXED);

    if (total > 0) {
        if (s_ui_state == ZHOUYI_STATE_WELCOME) {
            ESP_LOGI(TAG, "Welcome knob rotated -> enter casting");
            zhouyi_set_state_from_lvgl(ZHOUYI_STATE_CASTING);
        } else if (s_ui_state == ZHOUYI_STATE_CASTING) {
            ui_casting_on_knob(1);

        // } else if (s_ui_state == ZHOUYI_STATE_RESULT) {
        //     ESP_LOGI(TAG, "Result knob rotated -> restart casting");
        //     zhouyi_set_state_from_lvgl(ZHOUYI_STATE_CASTING);
        //     if (s_ui_state == ZHOUYI_STATE_CASTING) {
        //         ui_casting_on_knob(1);
        //         drv2605l_trigger(HAPTIC_TICK);
        //     }
        // }
        } else if (s_ui_state == ZHOUYI_STATE_RESULT) {
            // 已取消直接旋转旋钮重新起卦的功能，此处忽略结果页的旋钮动作
            ESP_LOGI(TAG, "Result knob rotated -> ignored");
        }
    }

    /* 检查六爻是否完成 */
    static int delay_count = 0;
    if (s_ui_state == ZHOUYI_STATE_CASTING && ui_casting_get_yao_count() >= 6) {
        delay_count++;
        if (delay_count > 30) {
            delay_count = 0;
            zhouyi_set_state_from_lvgl(ZHOUYI_STATE_RESULT);
        }
    } else {
        delay_count = 0;
    }
}

/* ========== 状态切换 ========== */

static void zhouyi_set_state_core(zhouyi_app_state_t state, bool need_lock)
{
    if (state == s_ui_state) return;

    ESP_LOGI(TAG, "UI state: %d -> %d", s_ui_state, state);

    zhouyi_app_state_t prev_state = s_ui_state;
    s_ui_state = state;

    if (need_lock) {
        if (esp_lv_adapter_lock(portMAX_DELAY) != ESP_OK) {
            ESP_LOGE(TAG, "Failed to lock LVGL");
            s_ui_state = prev_state;
            return;
        }
    }

    switch (prev_state) {
        case ZHOUYI_STATE_WELCOME:
            ui_taiji_destroy();
            break;

        case ZHOUYI_STATE_CASTING:
            ui_casting_destroy();
            break;

        case ZHOUYI_STATE_RESULT:
            ui_result_destroy();
            break;
    }

    switch (state) {
        case ZHOUYI_STATE_WELCOME:
            ui_taiji_create(s_app_container);
            break;

        case ZHOUYI_STATE_CASTING:
            ui_casting_create(s_app_container);
            drv2605l_trigger(HAPTIC_DOUBLE_CLICK);
            break;

        case ZHOUYI_STATE_RESULT: {
            const int *yao_results = ui_casting_get_results();

            int yao_array[6];
            for (int i = 0; i < 6; i++) {
                yao_array[i] = yao_results[i];
            }

            zhouyi_result_t result;
            zhouyi_calculate(yao_array, &result);

            ui_result_create(s_app_container, &result);
            drv2605l_trigger(HAPTIC_STRONG);
            break;
        }
    }

    if (need_lock) {
        esp_lv_adapter_unlock();
    }
}

void zhouyi_app_set_state(zhouyi_app_state_t state)
{
    zhouyi_set_state_core(state, true);
}

static void zhouyi_set_state_from_lvgl(zhouyi_app_state_t state)
{
    zhouyi_set_state_core(state, false);
}

zhouyi_app_state_t zhouyi_app_get_state(void)
{
    return s_ui_state;
}

/* ========== 初始化 ========== */

void zhouyi_app_init(lv_obj_t *parent, i2c_master_bus_handle_t i2c_bus)
{
    if (s_app_container != NULL) {
        ESP_LOGW(TAG, "Zhouyi App already running!");
        return;
    }

    /* 初始化震动马达 */
    drv2605l_init(i2c_bus);

    // ================== 终极纯软件旋钮初始化 ==================
    // 创建后台扫描任务，优先级为 5
    s_app_running = true;
    xTaskCreate(software_knob_task, "knob_task", 2048, NULL, 5, NULL);
    ESP_LOGI(TAG, "Software knob scanner task started");
    // ================================================================
    // ================================================================
    /* 创建初始页面 */
    if (esp_lv_adapter_lock(-1) != ESP_OK) {
        ESP_LOGE(TAG, "Failed to lock LVGL");
        return;
    }

    /* 创建模块根容器，挂载到传入的父级对象上 */
    s_app_container = lv_obj_create(parent);
    lv_obj_remove_style_all(s_app_container);
    lv_obj_set_size(s_app_container, SCREEN_SIZE, SCREEN_SIZE);
    lv_obj_center(s_app_container);
    lv_obj_set_style_bg_color(s_app_container, lv_color_hex(0x1A1A2E), 0);
    lv_obj_set_style_bg_opa(s_app_container, LV_OPA_COVER, 0);
    lv_obj_add_flag(s_app_container, LV_OBJ_FLAG_CLICKABLE);
    lv_obj_add_event_cb(s_app_container, screen_touch_cb, LV_EVENT_ALL, NULL);

    /* 创建欢迎页 */
    ui_taiji_create(s_app_container);
    s_ui_state = ZHOUYI_STATE_WELCOME;

    /* 创建轮询定时器 (50ms) */
    s_poll_timer = lv_timer_create(poll_timer_cb, 50, NULL);

    esp_lv_adapter_unlock();

    ESP_LOGI(TAG, "UI initialized");
}

void zhouyi_app_deinit(void)
{
    s_app_running = false; // 发送信号停止 FreeRTOS 旋钮轮询任务

    if (esp_lv_adapter_lock(-1) == ESP_OK) {
        /* 触发当前页面的销毁，清理 PSRAM 和内部定时器 */
        switch (s_ui_state) {
            case ZHOUYI_STATE_WELCOME: ui_taiji_destroy(); break;
            case ZHOUYI_STATE_CASTING: ui_casting_destroy(); break;
            case ZHOUYI_STATE_RESULT:  ui_result_destroy(); break;
        }
        /* 清理 UI 轮询器与总容器 */
        if (s_poll_timer) {
            lv_timer_del(s_poll_timer);
            s_poll_timer = NULL;
        }
        if (s_app_container) {
            lv_obj_del(s_app_container);
            s_app_container = NULL;
        }
        esp_lv_adapter_unlock();
    }
    ESP_LOGI(TAG, "UI deinitialized");
}

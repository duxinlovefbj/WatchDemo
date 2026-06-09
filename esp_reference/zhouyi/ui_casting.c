/**
 * @file ui_casting.c
 * @brief 掷爻交互界面
 *
 * 旋钮交互流程:
 * 1. 用户旋转旋钮 → 累计旋转值
 * 2. 停止旋转 1 秒后 → 以累计值为种子生成一爻
 * 3. 震动反馈 + 在屏幕上显示爻线
 * 4. 6 爻完成后自动跳转结果页
 */

#include <stdlib.h>
#include <string.h>
#include <math.h>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/timers.h"
#include "esp_log.h"
#include "lvgl.h"
#include "esp_lv_adapter.h"
#include "ui_casting.h"
#include "zhouyi_app.h"
#include "data/zhouyi_calc.h"
#include "drv2605l.h"
#include "esp_timer.h"

#define TAG "UI_CAST"

#define SCREEN_SIZE 360
#define MAX_YAO     6

/* 颜色 */
#define COLOR_BG        lv_color_hex(0x1A1A2E)
#define COLOR_ACCENT    lv_color_hex(0xC9A96E)
#define COLOR_TEXT      lv_color_hex(0xE8D5B7)
#define COLOR_DIM       lv_color_hex(0x666680)
#define COLOR_YANG_LINE lv_color_hex(0xF5E6CA)
#define COLOR_YIN_LINE  lv_color_hex(0x8B7355)
#define COLOR_GREEN     lv_color_hex(0x4CAF50)
#define COLOR_RED       lv_color_hex(0xC84B31)

/* 状态 */
static lv_obj_t *s_cast_page = NULL;
static lv_obj_t *s_title_label = NULL;
static lv_obj_t *s_prompt_label = NULL;
static lv_obj_t *s_yao_lines[MAX_YAO] = {NULL};
static lv_obj_t *s_yao_labels[MAX_YAO] = {NULL};

/* 前向声明 */
static void draw_yao_line(int index, int yao_value);

static int s_yao_results[MAX_YAO];
static int s_yao_count = 0;
static int s_knob_active = 0;
/* 替换原来的 s_settle_timer */
static lv_timer_t *s_debounce_timer = NULL;
static uint32_t s_last_knob_time = 0;   // 记录最后一次旋转的时间戳

static lv_obj_t *s_progress_arcs[MAX_YAO] = {NULL};
static lv_obj_t *s_coin_arcs[3] = {NULL};

static void create_progress_ring(lv_obj_t *parent)
{
    for (int i = 0; i < MAX_YAO; i++) {
        lv_obj_t *arc = lv_arc_create(parent);
        lv_obj_set_size(arc, 330, 330); // 对齐 ui_result: 半径165 -> 330
        lv_obj_center(arc);

        /* 从顶部开始，每爻约 60 度，留 4 度间隔 */
        int start = i * 60 + 3;
        int end   = i * 60 + 57;

        lv_arc_set_rotation(arc, 270);
        lv_arc_set_bg_angles(arc, start, end);
        lv_arc_set_angles(arc, start, end);

        lv_obj_remove_style(arc, NULL, LV_PART_KNOB);
        lv_obj_clear_flag(arc, LV_OBJ_FLAG_CLICKABLE);

        lv_obj_set_style_arc_width(arc, 6, LV_PART_MAIN); // 对齐 ui_result 外圈厚度 6
        lv_obj_set_style_arc_color(arc, COLOR_DIM, LV_PART_MAIN);
        lv_obj_set_style_arc_opa(arc, LV_OPA_TRANSP, LV_PART_INDICATOR);

        s_progress_arcs[i] = arc;
    }
}

static void update_progress_ring(void)
{
    for (int i = 0; i < MAX_YAO; i++) {
        if (s_progress_arcs[i]) {
            lv_obj_set_style_arc_color(
                s_progress_arcs[i],
                (i < s_yao_count) ? COLOR_ACCENT : COLOR_DIM,
                LV_PART_MAIN
            );
        }
    }
}

static void create_coin_ring(lv_obj_t *parent)
{
    for (int i = 0; i < 3; i++) {
        lv_obj_t *arc = lv_arc_create(parent);
        lv_obj_set_size(arc, 310, 310); // 对齐 ui_result: 半径155 -> 310
        lv_obj_center(arc);

        /* 分为 3 段，每段 120 度，各留 4 度间隙 */
        int start = i * 120 + 2;
        int end   = i * 120 + 118;

        lv_arc_set_rotation(arc, 270);
        lv_arc_set_bg_angles(arc, start, end);
        lv_arc_set_angles(arc, start, end);

        lv_obj_remove_style(arc, NULL, LV_PART_KNOB);
        lv_obj_clear_flag(arc, LV_OBJ_FLAG_CLICKABLE);

        /* 弧线宽度参考内圈标记 */
        lv_obj_set_style_arc_width(arc, 3, LV_PART_MAIN); // 对齐 ui_result 内圈标记厚度 3
        lv_obj_set_style_arc_color(arc, COLOR_DIM, LV_PART_MAIN);
        lv_obj_set_style_arc_opa(arc, LV_OPA_TRANSP, LV_PART_INDICATOR);

        s_coin_arcs[i] = arc;
    }
}

/* ========== 爻线绘制 ========== */

static void draw_yao_line(int index, int yao_value)
{
    if (index >= MAX_YAO || s_cast_page == NULL) return;

    int line_w = 120;
    int line_h = 6;
    int gap = 12;
    int x_center = SCREEN_SIZE / 2;

    int yao_is_yang = is_yang(yao_value);

    /* 用颜色严格区分四象 */
    lv_color_t line_color;
    if (yao_value == 7) {
        line_color = COLOR_YANG_LINE;     // 少阳：原淡金
    } else if (yao_value == 8) {
        line_color = COLOR_YIN_LINE;      // 少阴：原深褐
    } else if (yao_value == 9) {
        line_color = COLOR_ACCENT;        // 老阳：强调色（变爻）
    } else {
        line_color = COLOR_RED;           // 老阴：红色（变爻）
    }

    if (yao_is_yang) {
        lv_obj_t *line = lv_obj_create(s_cast_page);
        lv_obj_remove_style_all(line);
        lv_obj_set_size(line, line_w, line_h);
        // 初始Y位置设为0，后续由动态居中接管
        lv_obj_set_pos(line, x_center - line_w / 2, 0);
        lv_obj_set_style_bg_color(line, line_color, 0);
        lv_obj_set_style_bg_opa(line, LV_OPA_COVER, 0);
        lv_obj_set_style_radius(line, 2, 0);
        s_yao_lines[index] = line;
    } else {
        int seg_w = (line_w - gap) / 2;

        lv_obj_t *container = lv_obj_create(s_cast_page);
        lv_obj_remove_style_all(container);
        lv_obj_set_size(container, line_w, line_h);
        lv_obj_set_pos(container, x_center - line_w / 2, 0);
        lv_obj_clear_flag(container, LV_OBJ_FLAG_SCROLLABLE);

        lv_obj_t *left = lv_obj_create(container);
        lv_obj_remove_style_all(left);
        lv_obj_set_size(left, seg_w, line_h);
        lv_obj_set_pos(left, 0, 0);
        lv_obj_set_style_bg_color(left, line_color, 0);
        lv_obj_set_style_bg_opa(left, LV_OPA_COVER, 0);
        lv_obj_set_style_radius(left, 2, 0);

        lv_obj_t *right = lv_obj_create(container);
        lv_obj_remove_style_all(right);
        lv_obj_set_size(right, seg_w, line_h);
        lv_obj_set_pos(right, seg_w + gap, 0);
        lv_obj_set_style_bg_color(right, line_color, 0);
        lv_obj_set_style_bg_opa(right, LV_OPA_COVER, 0);
        lv_obj_set_style_radius(right, 2, 0);

        s_yao_lines[index] = container;
    }
    // 已彻底剔除变爻字符和英文解释符的逻辑
}

/* 新增：负责每次起卦后将当前所有的爻线动态居中对齐 */
static void update_yao_positions(void) {
    int line_spacing = 30;
    int line_h = 6;
    
    // 动态计算当前整体高度
    int total_h = (s_yao_count - 1) * line_spacing;
    // 初爻(index 0)的Y坐标 = 屏幕中点(180) + 整体高度的一半
    int base_y = (SCREEN_SIZE / 2) + (total_h / 2); 

    for (int i = 0; i < s_yao_count; i++) {
        if (s_yao_lines[i]) {
            int y = base_y - i * line_spacing;
            lv_obj_set_y(s_yao_lines[i], y - line_h / 2);
        }
    }
}

/* ========== 定时器回调：旋转停止后确定一爻 ========== */

/* 新的防抖与触发判定逻辑（运行在 LVGL 主线程，绝对安全） */
static void debounce_timer_cb(lv_timer_t *timer)
{
    if (!s_knob_active || s_yao_count >= MAX_YAO) return;

    uint32_t now = (uint32_t)(esp_timer_get_time() / 1000);
    
    /* 在旋转过程中，让硬币弧线随机闪烁，模拟掷硬币翻转效果 */
    if (s_knob_active) {
        for (int i = 0; i < 3; i++) {
            if (s_coin_arcs[i]) {
                lv_color_t color = (rand() % 2) ? COLOR_YANG_LINE : COLOR_YIN_LINE;
                lv_obj_set_style_arc_color(s_coin_arcs[i], color, LV_PART_MAIN);
            }
        }
    }

    if (now - s_last_knob_time >= 300) {
        s_knob_active = 0;
        ui_casting_commit_seed();
    }
}

/* ========== 公开 API ========== */

lv_obj_t *ui_casting_create(lv_obj_t *parent)
{
    s_yao_count = 0;
    s_knob_active = 0;
    memset(s_yao_results, 0, sizeof(s_yao_results));
    memset(s_yao_lines, 0, sizeof(s_yao_lines));
    memset(s_yao_labels, 0, sizeof(s_yao_labels));
    memset(s_progress_arcs, 0, sizeof(s_progress_arcs));
    memset(s_coin_arcs, 0, sizeof(s_coin_arcs));

    /* 页面容器 */
    s_cast_page = lv_obj_create(parent);
    lv_obj_remove_style_all(s_cast_page);
    lv_obj_set_size(s_cast_page, SCREEN_SIZE, SCREEN_SIZE);
    lv_obj_set_style_bg_color(s_cast_page, COLOR_BG, 0);
    lv_obj_set_style_bg_opa(s_cast_page, LV_OPA_COVER, 0);
    lv_obj_set_style_radius(s_cast_page, LV_RADIUS_CIRCLE, 0);
    lv_obj_clear_flag(s_cast_page, LV_OBJ_FLAG_SCROLLABLE);
    lv_obj_center(s_cast_page);

    /* 先创建外圈进度环 */
    create_progress_ring(s_cast_page);
    update_progress_ring();

    /* 创建内圈硬币环 */
    create_coin_ring(s_cast_page);

    /* 创建 LVGL 内部定时器，每 50ms 检查一次是否停止了旋转 */
    s_debounce_timer = lv_timer_create(debounce_timer_cb, 50, NULL);

    ESP_LOGI(TAG, "ui_casting page created successfully.");

    return s_cast_page;
}

/* ui_casting.c - ui_casting_on_knob 诊断版本 */
void ui_casting_on_knob(int delta)
{
    if (s_yao_count >= MAX_YAO) {
        ESP_LOGW(TAG, "[DEBUG] Knob ignored: already at MAX_YAO");
        return;
    }

    // 删除原有的：s_accum_rotation += abs(delta);
    s_last_knob_time = (uint32_t)(esp_timer_get_time() / 1000);
    s_knob_active = 1;
    
    ESP_LOGI(TAG, "[DEBUG] Knob rotated, knob_active: %d", s_knob_active);
}


void ui_casting_destroy(void)
{
    /* ====== 删除旧的 xTimerDelete，替换为以下代码 ====== */
    if (s_debounce_timer) {
        lv_timer_del(s_debounce_timer);
        s_debounce_timer = NULL;
    }
    
    if (s_cast_page) {
        lv_obj_del(s_cast_page);
        s_cast_page = NULL;
        s_title_label = NULL;
        s_prompt_label = NULL;
    }
    ESP_LOGI(TAG, "ui_casting page destroyed.");
}

/* ========== 获取已完成的六爻数据（被 ui_main 调用） ========== */

int ui_casting_get_yao_count(void)
{
    return s_yao_count;
}

const int *ui_casting_get_results(void)
{
    return s_yao_results;
}

void ui_casting_commit_seed(void)
{
    if (s_yao_count >= MAX_YAO || s_cast_page == NULL) return;

    int index = s_yao_count;
    
    // 【修改处】直接使用系统高精度运行时间(微秒)作为纯粹的随机种子
    int32_t final_seed = (int32_t)esp_timer_get_time();
    int yao = generate_single_yao(final_seed);

    s_yao_results[index] = yao;

    ESP_LOGI(TAG, "Yao %d: value=%d (seed=%ld)", index + 1, yao, (long)final_seed);

    drv2605l_trigger(HAPTIC_CLICK);

    draw_yao_line(index, yao);

    /* 抽爻完成，将硬币定格为对应的阴阳面 */
    int coins[3] = {0, 0, 0}; // 0 为阴，1 为阳
    if (yao == 6)      { coins[0]=0; coins[1]=0; coins[2]=0; } // 老阴：三个阴
    else if (yao == 7) { coins[0]=1; coins[1]=0; coins[2]=0; } // 少阳：一阳两阴
    else if (yao == 8) { coins[0]=1; coins[1]=1; coins[2]=0; } // 少阴：两阳一阴
    else if (yao == 9) { coins[0]=1; coins[1]=1; coins[2]=1; } // 老阳：三个阳

    for (int i = 0; i < 3; i++) {
        if (s_coin_arcs[i]) {
            lv_color_t color = coins[i] ? COLOR_YANG_LINE : COLOR_YIN_LINE;
            lv_obj_set_style_arc_color(s_coin_arcs[i], color, LV_PART_MAIN);
        }
    }

    s_yao_count++;
    // 删除原有的：s_accum_rotation = 0;
    s_knob_active = 0;

    // 调用居中计算逻辑
    update_yao_positions();
    update_progress_ring();

    if (s_yao_count >= MAX_YAO) {
        /* Debug 输出：六爻全部完成 */
        ESP_LOGI(TAG, "=== [DEBUG] All %d Yaos completed successfully! ===", MAX_YAO);
        drv2605l_trigger(HAPTIC_STRONG);
    }
}
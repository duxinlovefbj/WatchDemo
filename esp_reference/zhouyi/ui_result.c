/**
 * @file ui_result.c
 * @brief 结果展示界面 (极坐标环形重构版)
 *
 * 显示：
 * - 外圈：本卦（左半）/ 之卦（右半） 12等分爻弧
 * - 内圈：世应标记弧及文字
 * - 中圈：六神、六亲地支呈放射状对齐爻位
 * - 中央：卦名(20号大字)、六宫
 * - 顶部/底部：农历信息、卦辞
 */

#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include "lvgl.h"
#include "ui_result.h"
#include "data/zhouyi_calc.h"
#include "data/zhouyi_data.h"
#include "data/liuqin_calc.h"
#include "data/lunar_lite.h"
#include "zhouyi_app.h"

#define SCREEN_SIZE 360
#define COLOR_BG        lv_color_hex(0x1A1A2E)
#define COLOR_ACCENT    lv_color_hex(0xC9A96E) // 老阳(9) / 应
#define COLOR_TEXT      lv_color_hex(0xE8D5B7)
#define COLOR_DIM       lv_color_hex(0x666680)
#define COLOR_RED       lv_color_hex(0xC84B31) // 老阴(6) / 世
#define COLOR_YANG_LINE lv_color_hex(0xF5E6CA) // 少阳(7)
#define COLOR_YIN_LINE  lv_color_hex(0x8B7355) // 少阴(8)

/* 双击检测相关定义 */
#define DOUBLE_CLICK_TIME_MS 300

/* 外部字体声明 */
LV_FONT_DECLARE(lv_font_cn_14);
LV_FONT_DECLARE(lv_font_cn_20);

static bool valid_gua_idx(int idx);

/* 页面状态结构体 */
static struct {
    lv_obj_t *page;
    bool is_ben_active; 
    bool show_text_mode;      // 【新增】当前是否显示文本(农历和卦辞)面板
    bool touch_pressed;
    lv_point_t touch_start;
    zhouyi_result_t data_store;
    const zhouyi_result_t *data;
    lv_obj_t *yao_labels[6];
    lv_obj_t *title_label;
    lv_obj_t *palace_label;
    lv_obj_t *ben_arcs[6][2]; 
    lv_obj_t *zhi_arcs[6][2];
    lv_obj_t *sy_markers[2];  
    lv_obj_t *zhi_sy_markers[2];
    lv_obj_t *lunar_label;    // 【新增】农历标签
    lv_obj_t *brief_label;    // 【新增】卦辞标签
    bool ready;
    /* 双击检测相关变量 */
    uint32_t last_click_time;
    lv_timer_t *double_click_timer;
} s_ui;

static bool valid_gua_idx(int idx) {
    return (idx >= 0 && idx < 64);  // 假设有64卦，根据实际情况调整
}

/* ========== 辅助：绘制纯净弧线，复刻 ui_casting 线条样式 ========== */
static void draw_arc_segment(lv_obj_t *parent, lv_obj_t *objs[2],
                             int radius, int start, int end,
                             lv_color_t color, int is_yang) {
    if (is_yang) {
        objs[0] = lv_arc_create(parent);
        lv_obj_remove_style_all(objs[0]);

        lv_obj_clear_flag(objs[0], LV_OBJ_FLAG_CLICKABLE);
        lv_obj_clear_flag(objs[0], LV_OBJ_FLAG_SCROLLABLE);

        lv_obj_set_size(objs[0], radius * 2, radius * 2);
        lv_obj_center(objs[0]);
        lv_arc_set_bg_angles(objs[0], start, end);
        lv_obj_set_style_arc_width(objs[0], 6, LV_PART_MAIN);
        lv_obj_set_style_arc_color(objs[0], color, LV_PART_MAIN);
        lv_obj_set_style_arc_rounded(objs[0], false, LV_PART_MAIN);
        objs[1] = NULL;
    } else {
        int mid = start + (end - start) / 2;
        int gap_half = 1;

        for (int i = 0; i < 2; i++) {
            objs[i] = lv_arc_create(parent);
            lv_obj_remove_style_all(objs[i]);

            lv_obj_clear_flag(objs[i], LV_OBJ_FLAG_CLICKABLE);
            lv_obj_clear_flag(objs[i], LV_OBJ_FLAG_SCROLLABLE);

            lv_obj_set_size(objs[i], radius * 2, radius * 2);
            lv_obj_center(objs[i]);

            int s = (i == 0) ? start : mid + gap_half;
            int e = (i == 0) ? mid - gap_half : end;

            lv_arc_set_bg_angles(objs[i], s, e);
            lv_obj_set_style_arc_width(objs[i], 6, LV_PART_MAIN);
            lv_obj_set_style_arc_color(objs[i], color, LV_PART_MAIN);
            lv_obj_set_style_arc_rounded(objs[i], false, LV_PART_MAIN);
        }
    }
}

/* 重置双击标志的定时器回调 */
static void reset_double_click_flag(lv_timer_t *timer) {
    s_ui.last_click_time = 0;
    s_ui.double_click_timer = NULL;
    lv_timer_del(timer);
}

/* ========== 刷新中间文字列表与透明度 ========== */
static void refresh_center_info() {
    if (!s_ui.ready || s_ui.page == NULL || s_ui.data == NULL) {
        return;
    }

    if (s_ui.title_label == NULL ||
        s_ui.palace_label == NULL ||
        s_ui.lunar_label == NULL ||
        s_ui.brief_label == NULL) {
        return;
    }

    for (int i = 0; i < 6; i++) {
        if (s_ui.yao_labels[i] == NULL) {
            return;
        }
    }

    hexagram_details_t ben_details, zhi_details;
    calculate_hexagram_details(s_ui.data->ben_gua, -1, &ben_details);
    calculate_hexagram_details(s_ui.data->zhi_gua, (int)ben_details.palace_element, &zhi_details);

    lunar_info_t lunar;
    lunar_get_current(&lunar);
    const char *six_gods[6];
    get_six_gods_list(lunar.day_gan, six_gods);

    if (s_ui.is_ben_active) {
        if (valid_gua_idx(s_ui.data->ben_gua_idx)) {
            lv_label_set_text(s_ui.title_label, gua_data[s_ui.data->ben_gua_idx].name);
        } else {
            lv_label_set_text(s_ui.title_label, "未知卦");
        }

        lv_label_set_text(s_ui.palace_label, ben_details.palace_name);
    } else {
        if (valid_gua_idx(s_ui.data->zhi_gua_idx)) {
            lv_label_set_text(s_ui.title_label, gua_data[s_ui.data->zhi_gua_idx].name);
        } else {
            lv_label_set_text(s_ui.title_label, "无变卦");
        }

        lv_label_set_text(s_ui.palace_label, "变卦");
    }

    // 2. 更新六爻详情列表
    for (int i = 0; i < 6; i++) {
        int idx = 5 - i; // 从上往下显示 (上爻到初爻)
        char buf[64];
        const yao_detail_t *y = s_ui.is_ben_active ? &ben_details.yao[idx] : &zhi_details.yao[idx];
        snprintf(buf, sizeof(buf), "%s  %s  %s%s", six_gods[idx], y->liuqin, y->stem, y->branch);
        lv_label_set_text(s_ui.yao_labels[i], buf);
    }

    // 3. 视觉反馈：非选中侧亮度暗淡
    lv_opa_t ben_opa = s_ui.is_ben_active ? LV_OPA_COVER : LV_OPA_30;
    lv_opa_t zhi_opa = s_ui.is_ben_active ? LV_OPA_30 : LV_OPA_COVER;
    for (int i = 0; i < 6; i++) {
        for(int j = 0; j < 2; j++) {
            if(s_ui.ben_arcs[i][j]) lv_obj_set_style_arc_opa(s_ui.ben_arcs[i][j], ben_opa, LV_PART_MAIN);
            if(s_ui.zhi_arcs[i][j]) lv_obj_set_style_arc_opa(s_ui.zhi_arcs[i][j], zhi_opa, LV_PART_MAIN);
        }
    }
    
    // 4. 世应标记仅在本卦激活时显示
    if (s_ui.sy_markers[0]) lv_obj_set_style_arc_opa(s_ui.sy_markers[0], ben_opa, LV_PART_MAIN);
    if (s_ui.sy_markers[1]) lv_obj_set_style_arc_opa(s_ui.sy_markers[1], ben_opa, LV_PART_MAIN);

    // 【新增】控制右侧之卦的世应标记
    if (s_ui.zhi_sy_markers[0]) lv_obj_set_style_arc_opa(s_ui.zhi_sy_markers[0], zhi_opa, LV_PART_MAIN);
    if (s_ui.zhi_sy_markers[1]) lv_obj_set_style_arc_opa(s_ui.zhi_sy_markers[1], zhi_opa, LV_PART_MAIN);

    // --- 【在 refresh_center_info 末尾添加以下代码】 ---
    
    // 动态更新卦辞
    if (s_ui.is_ben_active) {
        lv_label_set_text(s_ui.brief_label, gua_data[s_ui.data->ben_gua_idx].brief);
    } else {
        if (s_ui.data->zhi_gua_idx >= 0) {
            lv_label_set_text(s_ui.brief_label, gua_data[s_ui.data->zhi_gua_idx].brief);
        } else {
            lv_label_set_text(s_ui.brief_label, "无动爻，卦象不变");
        }
    }

    // 控制中间内容的显示与隐藏
    if (s_ui.show_text_mode) {
        // 文本模式：显示农历和卦辞，隐藏六爻标签
        lv_obj_clear_flag(s_ui.lunar_label, LV_OBJ_FLAG_HIDDEN);
        lv_obj_clear_flag(s_ui.brief_label, LV_OBJ_FLAG_HIDDEN);
        for (int i = 0; i < 6; i++) {
            lv_obj_add_flag(s_ui.yao_labels[i], LV_OBJ_FLAG_HIDDEN);
        }
    } else {
        // 爻位模式：隐藏农历和卦辞，显示六爻标签
        lv_obj_add_flag(s_ui.lunar_label, LV_OBJ_FLAG_HIDDEN);
        lv_obj_add_flag(s_ui.brief_label, LV_OBJ_FLAG_HIDDEN);
        for (int i = 0; i < 6; i++) {
            lv_obj_clear_flag(s_ui.yao_labels[i], LV_OBJ_FLAG_HIDDEN);
        }
    }
}

static void refresh_center_info_async(void *param) {
    LV_UNUSED(param);

    if (!s_ui.ready || s_ui.page == NULL || s_ui.data == NULL) {
        return;
    }

    refresh_center_info();
}

/* ========== 滑动手势与点击拦截回调 ========== */
static void page_event_cb(lv_event_t *e) {
    if (!s_ui.ready || s_ui.page == NULL) {
        return;
    }

    lv_event_code_t code = lv_event_get_code(e);
    lv_indev_t *indev = lv_indev_get_act();

    // 自定义双击检测
    if (code == LV_EVENT_SHORT_CLICKED) {
        uint32_t current_time = lv_tick_get();
        
        if (s_ui.last_click_time == 0) {
            // 第一次点击
            s_ui.last_click_time = current_time;
            // 创建定时器，300ms后重置标志
            if (s_ui.double_click_timer) {
                lv_timer_del(s_ui.double_click_timer);
            }
            s_ui.double_click_timer = lv_timer_create(reset_double_click_flag, DOUBLE_CLICK_TIME_MS, NULL);
            return;
        } else {
            // 第二次点击，检查时间差
            if ((current_time - s_ui.last_click_time) <= DOUBLE_CLICK_TIME_MS) {
                // 双击事件：返回太极界面以便重新抽签
                if (s_ui.double_click_timer) {
                    lv_timer_del(s_ui.double_click_timer);
                    s_ui.double_click_timer = NULL;
                }
                s_ui.last_click_time = 0;
            zhouyi_app_set_state(ZHOUYI_STATE_WELCOME);
                return;
            } else {
                // 超过时间，重置
                s_ui.last_click_time = 0;
            }
        }
        return;
    }
    
    if (code == LV_EVENT_LONG_PRESSED) {
        // 长按：预留
        // 长按：退出到主界面
        zhouyi_app_exit();
        return;
    }

    if (!indev) return;

    if (code == LV_EVENT_PRESSED) {
        lv_indev_get_point(indev, &s_ui.touch_start);
        s_ui.touch_pressed = true;
        return;
    }

    if (code == LV_EVENT_RELEASED || code == LV_EVENT_PRESS_LOST) {
        if (!s_ui.touch_pressed) return;
        s_ui.touch_pressed = false;

        lv_point_t touch_end;
        lv_indev_get_point(indev, &touch_end);

        int dx = touch_end.x - s_ui.touch_start.x;
        int dy = touch_end.y - s_ui.touch_start.y;

        int adx = abs(dx);
        int ady = abs(dy);

        if (adx < 30 && ady < 30) {
            return;
        }

        if (adx > ady) {
            // 左右滑动：切换六爻 / 文本
            s_ui.show_text_mode = !s_ui.show_text_mode;
        } else {
            // 上下滑动：切换本卦 / 之卦
            if (s_ui.data && s_ui.data->zhi_gua_idx >= 0) {
                s_ui.is_ben_active = !s_ui.is_ben_active;
            } else {
                s_ui.is_ben_active = true;
                s_ui.show_text_mode = true;
            }
        }

        lv_async_call(refresh_center_info_async, NULL);
    }
}

lv_obj_t *ui_result_create(lv_obj_t *parent, const zhouyi_result_t *result) {
    if (result == NULL) {
        return NULL;
    }

    memset(&s_ui, 0, sizeof(s_ui));

    s_ui.data_store = *result;       // 关键：把结果复制到当前页面内部
    s_ui.data = &s_ui.data_store;

    s_ui.is_ben_active = true;
    s_ui.show_text_mode = false;
    s_ui.touch_pressed = false;
    s_ui.ready = false;
    s_ui.last_click_time = 0;
    s_ui.double_click_timer = NULL;

    s_ui.page = lv_obj_create(parent);
    lv_obj_remove_style_all(s_ui.page);
    lv_obj_set_size(s_ui.page, SCREEN_SIZE, SCREEN_SIZE);
    lv_obj_set_style_bg_color(s_ui.page, COLOR_BG, 0);
    lv_obj_set_style_bg_opa(s_ui.page, LV_OPA_COVER, 0);

    // 1. 严格映射左右弧线 (颜色逻辑 100% 还原)
    for (int i = 0; i < 6; i++) {
        lv_color_t ben_color, zhi_color;
        int ben_y = result->ben_gua[i];
        int zhi_y = result->zhi_gua[i];

        // 还原 ui_casting 着色逻辑
        if (ben_y != zhi_y) {
            ben_color = ben_y ? COLOR_ACCENT : COLOR_RED;       // 老阳 / 老阴
            zhi_color = ben_y ? COLOR_YIN_LINE : COLOR_YANG_LINE;
        } else {
            ben_color = ben_y ? COLOR_YANG_LINE : COLOR_YIN_LINE; // 少阳 / 少阴
            zhi_color = ben_color;
        }

        // 【左半圆：本卦】从底向上 (90度起) - 严格占据左半边 100° 到 260°
        int b_start = 100 + i * 28;
        int b_end = b_start + 20;
        draw_arc_segment(s_ui.page, s_ui.ben_arcs[i], 165, b_start, b_end, ben_color, ben_y);

        // 【右半圆：之卦】严格镜像对照，从底向上占据 80° 到 280°
        int z_start = (60 - i * 28 + 360) % 360; 
        int z_end = z_start + 20;
        draw_arc_segment(s_ui.page, s_ui.zhi_arcs[i], 165, z_start, z_end, zhi_color, zhi_y);
    }

    // 2. 左侧内圈增加世应贴靠标记
    hexagram_details_t det;
    calculate_hexagram_details(result->ben_gua, -1, &det);
    for(int i = 0; i < 6; i++) {
        if(det.yao[i].is_shi || det.yao[i].is_ying) {
            int arc_idx = det.yao[i].is_shi ? 0 : 1;
            int b_start = 100 + i * 28;
            int b_end = b_start + 20;
            s_ui.sy_markers[arc_idx] = lv_arc_create(s_ui.page);
            lv_obj_remove_style_all(s_ui.sy_markers[arc_idx]); // 剥离干扰

            lv_obj_clear_flag(s_ui.sy_markers[arc_idx], LV_OBJ_FLAG_CLICKABLE);
            lv_obj_clear_flag(s_ui.sy_markers[arc_idx], LV_OBJ_FLAG_SCROLLABLE);
            
            lv_obj_set_size(s_ui.sy_markers[arc_idx], 310, 310); // 半径155，紧贴外环165
            lv_obj_center(s_ui.sy_markers[arc_idx]);
            lv_arc_set_bg_angles(s_ui.sy_markers[arc_idx], b_start, b_end);
            lv_obj_set_style_arc_width(s_ui.sy_markers[arc_idx], 3, LV_PART_MAIN); // 极细的内部标记线
            lv_obj_set_style_arc_color(s_ui.sy_markers[arc_idx], det.yao[i].is_shi ? COLOR_RED : COLOR_ACCENT, LV_PART_MAIN);
            lv_obj_set_style_arc_rounded(s_ui.sy_markers[arc_idx], false, LV_PART_MAIN);
        }
    }

    // 【新增】2.5 右侧内圈增加之卦世应贴靠标记
    hexagram_details_t zhi_det;
    // 注意：之卦的排盘必须严格借用本卦的宫位五行属性 (det.palace_element)
    // calculate_hexagram_details(result->zhi_gua, (int)det.palace_element, &zhi_det);
    calculate_hexagram_details(result->zhi_gua, -1, &zhi_det);
    for(int i = 0; i < 6; i++) {
        if(zhi_det.yao[i].is_shi || zhi_det.yao[i].is_ying) {
            int arc_idx = zhi_det.yao[i].is_shi ? 0 : 1;
            
            // 严格采用与外侧之卦完全一致的逆时针镜像角度计算
            int z_start = (60 - i * 28 + 360) % 360; 
            int z_end = z_start + 20;
            
            s_ui.zhi_sy_markers[arc_idx] = lv_arc_create(s_ui.page);
            lv_obj_remove_style_all(s_ui.zhi_sy_markers[arc_idx]);

            lv_obj_clear_flag(s_ui.zhi_sy_markers[arc_idx], LV_OBJ_FLAG_CLICKABLE);
            lv_obj_clear_flag(s_ui.zhi_sy_markers[arc_idx], LV_OBJ_FLAG_SCROLLABLE);
            
            lv_obj_set_size(s_ui.zhi_sy_markers[arc_idx], 310, 310); // 半径155，紧贴右侧外环165
            lv_obj_center(s_ui.zhi_sy_markers[arc_idx]);
            lv_arc_set_bg_angles(s_ui.zhi_sy_markers[arc_idx], z_start, z_end);
            lv_obj_set_style_arc_width(s_ui.zhi_sy_markers[arc_idx], 3, LV_PART_MAIN);
            // 颜色维持传统：世为红，应为金
            lv_obj_set_style_arc_color(s_ui.zhi_sy_markers[arc_idx], zhi_det.yao[i].is_shi ? COLOR_RED : COLOR_ACCENT, LV_PART_MAIN);
            lv_obj_set_style_arc_rounded(s_ui.zhi_sy_markers[arc_idx], false, LV_PART_MAIN);
        }
    }

    // 3. 中间与顶部详情布局
    lunar_info_t lunar;
    lunar_get_current(&lunar);
    char lunar_buf[64];
    snprintf(lunar_buf, sizeof(lunar_buf), "%s %s %s %s时", 
             lunar.year_gan_zhi, lunar.month_gan_zhi, lunar.day_gan_zhi, lunar.hour_gan_zhi);
    s_ui.lunar_label = lv_label_create(s_ui.page);
    lv_label_set_text(s_ui.lunar_label, lunar_buf);
    lv_obj_set_style_text_font(s_ui.lunar_label, &lv_font_cn_14, 0);
    lv_obj_set_style_text_color(s_ui.lunar_label, COLOR_TEXT, 0); // 提亮为暖白色
    lv_obj_align(s_ui.lunar_label, LV_ALIGN_CENTER, 0, -35);      // 放在屏幕正中间偏上一点

    s_ui.title_label = lv_label_create(s_ui.page);
    lv_obj_set_style_text_font(s_ui.title_label, &lv_font_cn_20, 0);
    lv_obj_set_style_text_color(s_ui.title_label, COLOR_ACCENT, 0);
    lv_obj_align(s_ui.title_label, LV_ALIGN_TOP_MID, 0, 45);

    s_ui.palace_label = lv_label_create(s_ui.page);
    lv_obj_set_style_text_font(s_ui.palace_label, &lv_font_cn_14, 0);
    lv_obj_set_style_text_color(s_ui.palace_label, COLOR_RED, 0);
    lv_obj_align(s_ui.palace_label, LV_ALIGN_TOP_MID, 0, 70);

    for (int i = 0; i < 6; i++) {
        s_ui.yao_labels[i] = lv_label_create(s_ui.page);
        lv_obj_set_style_text_font(s_ui.yao_labels[i], &lv_font_cn_14, 0);
        lv_obj_set_style_text_color(s_ui.yao_labels[i], COLOR_TEXT, 0);
        lv_obj_align(s_ui.yao_labels[i], LV_ALIGN_CENTER, 0, -55 + i * 22);
    }

    // 4. 卦辞创建
    s_ui.brief_label = lv_label_create(s_ui.page);
    lv_label_set_text(s_ui.brief_label, "");
    lv_obj_set_style_text_font(s_ui.brief_label, &lv_font_cn_14, 0);
    lv_obj_set_style_text_color(s_ui.brief_label, COLOR_TEXT, 0);
    lv_obj_set_width(s_ui.brief_label, 220);
    lv_label_set_long_mode(s_ui.brief_label, LV_LABEL_LONG_WRAP);
    lv_obj_set_style_text_align(s_ui.brief_label, LV_TEXT_ALIGN_CENTER, 0);
    lv_obj_align(s_ui.brief_label, LV_ALIGN_CENTER, 0, 30);

    // 5. 最后创建透明触摸层
    lv_obj_t *gesture_area = lv_obj_create(s_ui.page);
    lv_obj_remove_style_all(gesture_area);
    lv_obj_set_size(gesture_area, SCREEN_SIZE, SCREEN_SIZE);
    lv_obj_set_pos(gesture_area, 0, 0);

    lv_obj_add_flag(gesture_area, LV_OBJ_FLAG_CLICKABLE);
    lv_obj_clear_flag(gesture_area, LV_OBJ_FLAG_SCROLLABLE);

    // 注册所有事件到一个回调函数中
    lv_obj_add_event_cb(gesture_area, page_event_cb, LV_EVENT_ALL, NULL);

    lv_obj_move_foreground(gesture_area);

    s_ui.ready = true;
    refresh_center_info();

    return s_ui.page;
}

void ui_result_destroy(void) {
    s_ui.ready = false;

    // 清理定时器
    if (s_ui.double_click_timer) {
        lv_timer_del(s_ui.double_click_timer);
        s_ui.double_click_timer = NULL;
    }

    if (s_ui.page) {
        lv_obj_del(s_ui.page);
    }

    memset(&s_ui, 0, sizeof(s_ui));
}
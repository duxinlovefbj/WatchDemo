/**
 * @file ui_taiji.c
 * @brief 太极图 + 八卦外圈绘制
 *
 * 在 360x360 圆形屏幕上绘制：
 * - 外圈：八卦符号（用线条绘制三爻卦）
 * - 中央：太极阴阳鱼图案
 * - 底部：提示文字
 */

#include <math.h>
#include <string.h>
#include "lvgl.h"
#include "esp_heap_caps.h"
#include "esp_log.h"
#include "ui_taiji.h"

#define TAG "UI_TAIJI"

#define SCREEN_SIZE     360
#define CENTER_X        (SCREEN_SIZE / 2)
#define CENTER_Y        (SCREEN_SIZE / 2)
#define PI              3.14159265f

/* 太极图颜色 */
#define COLOR_BG        lv_color_hex(0x1A1A2E)  /* 深蓝黑底 */
#define COLOR_YANG      lv_color_hex(0xF5E6CA)  /* 淡金色（阳面） */
#define COLOR_YIN       lv_color_hex(0x2D2D44)  /* 深紫灰（阴面） */
#define COLOR_ACCENT    lv_color_hex(0xC9A96E)  /* 金色强调 */
#define COLOR_RED       lv_color_hex(0xC84B31)  /* 中国红 */
#define COLOR_TEXT      lv_color_hex(0xE8D5B7)  /* 暖白文字 */
#define COLOR_DIM       lv_color_hex(0x666680)  /* 暗淡文字 */

/* 八卦数据 */
static const int8_t BAGUA_TRIGRAMS[8][3] = {
    {1,1,1}, /* 乾 ☰ */
    {0,1,1}, /* 兑 ☱ */
    {1,0,1}, /* 离 ☲ */
    {0,0,1}, /* 震 ☳ */
    {1,1,0}, /* 巽 ☴ */
    {0,1,0}, /* 坎 ☵ */
    {1,0,0}, /* 艮 ☶ */
    {0,0,0}, /* 坤 ☷ */
};

/* 先天八卦方位：乾南坤北，从正上方(北)开始顺时针 */
/* 顺序: 坤(北0°),震(东北45°),坎(东90°),兑(东南135°),乾(南180°),巽(西南225°),离(西270°),艮(西北315°) */
static const int BAGUA_ORDER[] = { 7, 3, 5, 1, 0, 4, 2, 6 };

static lv_obj_t *s_taiji_page = NULL;
static lv_obj_t *s_taiji_canvas = NULL;      // 现仅用于静止的八卦外圈背景
static lv_obj_t *s_center_canvas = NULL;     // 【新增】用于中心太极独立旋转的画布
static int32_t   s_current_angle = 0;
static lv_color_t *s_canvas_buf = NULL;
static lv_color_t *s_center_buf = NULL;      // 【新增】太极图显存

static lv_timer_t *s_anim_timer = NULL;      // 【新增】待机动画定时器
static int s_anim_step = 0;                  // 【新增】动画步数记录
static int s_lit_bagua = -1;                 // 【新增】当前点亮的八卦索引

/* ========== 八卦三爻绘制 ========== */

/**
 * @brief 在 canvas 上绘制一个三爻卦符号（用于八卦外圈）
 * @param canvas   LVGL canvas
 * @param cx, cy   中心坐标
 * @param angle    旋转角度(弧度)
 * @param trigram  三爻数组 [底,中,上]
 * @param size     符号大小
 * @param color    颜色
 */
static void draw_trigram_at(lv_obj_t *canvas, int cx, int cy,
                            float angle, const int8_t trigram[3], int size, lv_color_t color)
{
    lv_draw_line_dsc_t line_dsc;
    lv_draw_line_dsc_init(&line_dsc);
    line_dsc.color = color;
    line_dsc.width = 2;
    line_dsc.round_start = 1;
    line_dsc.round_end = 1;

    int line_w = size;
    int gap = size / 3;
    int half_gap = size / 6;

    /* 注意：三爻从底到顶是 [0][1][2]，绘制时从下到上 */
    for (int row = 0; row < 3; row++) {
        int offset_y = (1 - row) * gap; /* row0 最下面 */

        /* 旋转坐标 */
        float cos_a = cosf(angle);
        float sin_a = sinf(angle);

        if (trigram[row] == 1) {
            /* 阳爻：一条连续线 */
            float x1 = -line_w / 2;
            float y1 = offset_y;
            float x2 = line_w / 2;
            float y2 = offset_y;

            lv_point_t pts[2];
            pts[0].x = cx + (int)(x1 * cos_a - y1 * sin_a);
            pts[0].y = cy + (int)(x1 * sin_a + y1 * cos_a);
            pts[1].x = cx + (int)(x2 * cos_a - y2 * sin_a);
            pts[1].y = cy + (int)(x2 * sin_a + y2 * cos_a);
            lv_canvas_draw_line(canvas, pts, 2, &line_dsc);
        } else {
            /* 阴爻：两段断线 */
            float x1a = -line_w / 2;
            float x1b = -half_gap;
            float x2a = half_gap;
            float x2b = line_w / 2;
            float y = offset_y;

            lv_point_t pts1[2];
            pts1[0].x = cx + (int)(x1a * cos_a - y * sin_a);
            pts1[0].y = cy + (int)(x1a * sin_a + y * cos_a);
            pts1[1].x = cx + (int)(x1b * cos_a - y * sin_a);
            pts1[1].y = cy + (int)(x1b * sin_a + y * cos_a);
            lv_canvas_draw_line(canvas, pts1, 2, &line_dsc);

            lv_point_t pts2[2];
            pts2[0].x = cx + (int)(x2a * cos_a - y * sin_a);
            pts2[0].y = cy + (int)(x2a * sin_a + y * cos_a);
            pts2[1].x = cx + (int)(x2b * cos_a - y * sin_a);
            pts2[1].y = cy + (int)(x2b * sin_a + y * cos_a);
            lv_canvas_draw_line(canvas, pts2, 2, &line_dsc);
        }
    }
}

/* ========== 太极阴阳鱼绘制 ========== */

static void draw_taiji(lv_obj_t *canvas, int radius)
{
    /* 用逐像素方式绘制阴阳太极图 */
    lv_img_dsc_t *dsc = lv_canvas_get_img(canvas);
    lv_color_t *cbuf = (lv_color_t *)dsc->data;

    // 改为相对小画布自身的中心坐标
    int w = radius * 2;
    int cx = radius;
    int cy = radius;

    for (int py = 0; py < w; py++) {
        for (int px = 0; px < w; px++) {
            if (px < 0 || px >= w || py < 0 || py >= w) continue;

            float dx = px - cx;
            float dy = py - cy;
            float dist = sqrtf(dx * dx + dy * dy);

            /* 稍微缩小太极本体绘制范围，使其能够被外侧圆环完全包裹 */
            if (dist > radius - 2.0f) continue;

            /* 标准太极图算法 */
            float half_r = radius / 2.0f;

            /* 上下半圆的小圆心 */
            float top_cy = -half_r;
            float bot_cy = half_r;

            /* 到上下小圆心的距离 */
            float dist_top = sqrtf(dx * dx + (dy - top_cy) * (dy - top_cy));
            float dist_bot = sqrtf(dx * dx + (dy - bot_cy) * (dy - bot_cy));

            int is_yang;

            if (dist_top <= half_r) {
                /* 在上半小圆内 → 阳 */
                is_yang = 1;
            } else if (dist_bot <= half_r) {
                /* 在下半小圆内 → 阴 */
                is_yang = 0;
            } else {
                /* 大圆左半为阳，右半为阴 */
                is_yang = (dx < 0) ? 1 : 0;
            }

            /* 鱼眼：上半小圆中心附近的小圆点 → 阴点 */
            float eye_r = radius / 8.0f;
            float dist_top_eye = sqrtf(dx * dx + (dy + half_r) * (dy + half_r));
            float dist_bot_eye = sqrtf(dx * dx + (dy - half_r) * (dy - half_r));

            if (dist_top_eye <= eye_r) {
                is_yang = 0; /* 阳中阴点 */
            } else if (dist_bot_eye <= eye_r) {
                is_yang = 1; /* 阴中阳点 */
            }

            /* 设置像素颜色 */
            lv_color_t color = is_yang ? COLOR_YANG : COLOR_YIN;
            cbuf[py * w + px] = color;
        }
    }

    /* 太极外圈金色圆环 */
    lv_draw_arc_dsc_t arc_dsc;
    lv_draw_arc_dsc_init(&arc_dsc);
    arc_dsc.color = COLOR_ACCENT;
    arc_dsc.width = 3;
    lv_canvas_draw_arc(canvas, cx, cy, radius - 1, 0, 360, &arc_dsc); // 外圈稍微加粗，盖住本体边缘且防裁剪
}

/* ==================== 新增 anim_timer_cb ==================== */
static void anim_timer_cb(lv_timer_t *timer)
{
    s_anim_step++;

    // 1. 太极缓慢旋转：只旋转中心的画布，外圈八卦不动 (50ms 转 1度 = 10 个 0.1度)
    if (s_center_canvas) {
        lv_img_set_angle(s_center_canvas, (s_anim_step * 10) % 3600);
    }

    // 2. 八卦依次白光亮起 (每 10 次定时器，即 500ms 切换下一个)
    if (s_anim_step % 10 == 0) {
        int next_lit = (s_anim_step / 10) % 8;
        int bagua_radius = 145;
        int trigram_size = 20;

        // 恢复上一个为原始的金色
        if (s_lit_bagua >= 0 && s_lit_bagua != next_lit) {
            float angle_deg = s_lit_bagua * 45.0f - 90.0f;
            float angle_rad = angle_deg * PI / 180.0f;
            int bx = CENTER_X + (int)(bagua_radius * cosf(angle_rad));
            int by = CENTER_Y + (int)(bagua_radius * sinf(angle_rad));
            draw_trigram_at(s_taiji_canvas, bx, by, angle_rad + PI / 2.0f, BAGUA_TRIGRAMS[BAGUA_ORDER[s_lit_bagua]], trigram_size, COLOR_ACCENT);
        }

        // 绘制当前的八卦为白色
        float angle_deg = next_lit * 45.0f - 90.0f;
        float angle_rad = angle_deg * PI / 180.0f;
        int bx = CENTER_X + (int)(bagua_radius * cosf(angle_rad));
        int by = CENTER_Y + (int)(bagua_radius * sinf(angle_rad));
        draw_trigram_at(s_taiji_canvas, bx, by, angle_rad + PI / 2.0f, BAGUA_TRIGRAMS[BAGUA_ORDER[next_lit]], trigram_size, lv_color_hex(0xFFFFFF));

        s_lit_bagua = next_lit;
        lv_obj_invalidate(s_taiji_canvas);
    }
}

/* ========== 创建太极页面 ========== */

lv_obj_t *ui_taiji_create(lv_obj_t *parent)
{
    /* 页面容器 */
    s_taiji_page = lv_obj_create(parent);
    lv_obj_remove_style_all(s_taiji_page);
    lv_obj_set_size(s_taiji_page, SCREEN_SIZE, SCREEN_SIZE);
    lv_obj_set_style_bg_color(s_taiji_page, COLOR_BG, 0);
    lv_obj_set_style_bg_opa(s_taiji_page, LV_OPA_COVER, 0);
    lv_obj_set_style_radius(s_taiji_page, LV_RADIUS_CIRCLE, 0);
    lv_obj_clear_flag(s_taiji_page, LV_OBJ_FLAG_SCROLLABLE);
    lv_obj_center(s_taiji_page);

    /* Canvas 用于太极图和八卦绘制（分配在 PSRAM，约 260KB） */
    if (s_canvas_buf == NULL) {
        s_canvas_buf = (lv_color_t *)heap_caps_malloc(
            SCREEN_SIZE * SCREEN_SIZE * sizeof(lv_color_t), MALLOC_CAP_SPIRAM);
    }
    if (s_canvas_buf == NULL) {
        /* PSRAM 分配失败的 fallback：用小 canvas 或直接返回 */
        return s_taiji_page;
    }
    s_taiji_canvas = lv_canvas_create(s_taiji_page);
    lv_canvas_set_buffer(s_taiji_canvas, s_canvas_buf, SCREEN_SIZE, SCREEN_SIZE, LV_IMG_CF_TRUE_COLOR);
    lv_obj_center(s_taiji_canvas);

    /* 先填充背景 */
    lv_canvas_fill_bg(s_taiji_canvas, COLOR_BG, LV_OPA_COVER);

    /* 绘制八卦外圈 (增加默认颜色传参) */
    int bagua_radius = 145; /* 八卦符号所在圆的半径 */
    int trigram_size = 20;  /* 每个三爻符号的大小 */
    for (int i = 0; i < 8; i++) {
        float angle_deg = i * 45.0f - 90.0f; /* 从正上方开始 */
        float angle_rad = angle_deg * PI / 180.0f;

        int bx = CENTER_X + (int)(bagua_radius * cosf(angle_rad));
        int by = CENTER_Y + (int)(bagua_radius * sinf(angle_rad));

        /* 三爻符号朝外旋转 */
        draw_trigram_at(s_taiji_canvas, bx, by,
                        angle_rad + PI / 2.0f,
                        BAGUA_TRIGRAMS[BAGUA_ORDER[i]],
                        trigram_size, COLOR_ACCENT);
    }

    /* 绘制外圈装饰线 */
    lv_draw_arc_dsc_t arc_dsc;
    lv_draw_arc_dsc_init(&arc_dsc);
    arc_dsc.color = COLOR_ACCENT;
    arc_dsc.width = 1;
    lv_canvas_draw_arc(s_taiji_canvas, CENTER_X, CENTER_Y, 165, 0, 360, &arc_dsc);
    lv_canvas_draw_arc(s_taiji_canvas, CENTER_X, CENTER_Y, 125, 0, 360, &arc_dsc);

    /* 【新增】单独创建中心太极画布 */
    if (s_center_buf == NULL) {
        s_center_buf = (lv_color_t *)heap_caps_malloc(174 * 174 * sizeof(lv_color_t), MALLOC_CAP_SPIRAM);
    }
    
    if (s_center_buf) {
        s_center_canvas = lv_canvas_create(s_taiji_page);
        lv_canvas_set_buffer(s_center_canvas, s_center_buf, 174, 174, LV_IMG_CF_TRUE_COLOR);
        lv_canvas_fill_bg(s_center_canvas, COLOR_BG, LV_OPA_COVER);
        lv_obj_center(s_center_canvas);
        draw_taiji(s_center_canvas, 87); // 半径设为 87，总宽 174
    }

    /* 【新增】启动待机动画 */
    s_anim_step = 0;
    s_lit_bagua = -1;
    s_anim_timer = lv_timer_create(anim_timer_cb, 50, NULL);

    return s_taiji_page;
}

void ui_taiji_set_angle(int32_t angle)
{
    ESP_LOGI(TAG, "Setting taiji angle to: %ld (0.1 deg)", angle);
    s_current_angle = angle;
    // 强制只旋转中心画布，不旋转外围八卦
    if (s_center_canvas) {
        lv_img_set_angle(s_center_canvas, angle);
    }
}

void ui_taiji_destroy(void)
{
    if (s_anim_timer) {
        lv_timer_del(s_anim_timer);
        s_anim_timer = NULL;
    }
    if (s_taiji_page) {
        lv_obj_del(s_taiji_page);
        s_taiji_page = NULL;
        s_taiji_canvas = NULL;
        s_center_canvas = NULL;
    }
    if (s_canvas_buf) {
        heap_caps_free(s_canvas_buf);
        s_canvas_buf = NULL;
    }
    if (s_center_buf) {
        heap_caps_free(s_center_buf);
        s_center_buf = NULL;
    }
}
/**
 * @file liuqin_calc.c
 * @brief 六亲六神纳甲世应计算（移植自 liuqin.js）
 */

#include <string.h>
#include <stdio.h>
#include "liuqin_calc.h"

/* ========== 八卦表 (移植自 TRIGRAMS) ========== */

typedef struct {
    int8_t       trigram[3];  // 三爻组合
    const char  *name;        // 卦名
    element_t    element;     // 五行
    const char  *stems[2];    // 纳甲天干 [下卦用, 上卦用]
    const char  *branches[6]; // 纳甲地支 [初..上]
} trigram_info_t;

static const trigram_info_t TRIGRAMS[] = {
    { {1,1,1}, "乾", ELEM_JIN,  {"甲","壬"}, {"子","寅","辰","午","申","戌"} },
    { {1,1,0}, "兑", ELEM_JIN,  {"丁","丁"}, {"巳","卯","丑","亥","酉","未"} },
    { {1,0,1}, "离", ELEM_HUO,  {"己","己"}, {"卯","丑","亥","酉","未","巳"} },
    { {1,0,0}, "震", ELEM_MU,   {"庚","庚"}, {"子","寅","辰","午","申","戌"} },
    { {0,1,1}, "巽", ELEM_MU,   {"辛","辛"}, {"丑","亥","酉","未","巳","卯"} },
    { {0,1,0}, "坎", ELEM_SHUI, {"戊","戊"}, {"寅","辰","午","申","戌","子"} },
    { {0,0,1}, "艮", ELEM_TU,   {"丙","丙"}, {"辰","午","申","戌","子","寅"} },
    { {0,0,0}, "坤", ELEM_TU,   {"乙","癸"}, {"未","巳","卯","丑","亥","酉"} },
};

#define TRIGRAM_COUNT 8

/* ========== 地支五行 (移植自 BRANCH_ELEMENTS) ========== */

typedef struct {
    const char *branch;
    element_t   element;
} branch_element_t;

static const branch_element_t BRANCH_ELEMENTS[] = {
    {"子", ELEM_SHUI}, {"丑", ELEM_TU},   {"寅", ELEM_MU},   {"卯", ELEM_MU},
    {"辰", ELEM_TU},   {"巳", ELEM_HUO},  {"午", ELEM_HUO},  {"未", ELEM_TU},
    {"申", ELEM_JIN},  {"酉", ELEM_JIN},  {"戌", ELEM_TU},   {"亥", ELEM_SHUI},
};

/* ========== 五行名称 ========== */

static const char *ELEMENT_NAMES[] = { "金", "水", "木", "火", "土" };

const char *element_name(element_t e)
{
    if (e >= 0 && e <= 4) return ELEMENT_NAMES[e];
    return "?";
}

/* ========== 六神 ========== */

static const char *SIX_GODS[] = { "青龙", "朱雀", "勾陈", "腾蛇", "白虎", "玄武" };

/* ========== 辅助函数 ========== */

static const trigram_info_t *find_trigram(const int8_t tri[3])
{
    for (int i = 0; i < TRIGRAM_COUNT; i++) {
        if (TRIGRAMS[i].trigram[0] == tri[0] &&
            TRIGRAMS[i].trigram[1] == tri[1] &&
            TRIGRAMS[i].trigram[2] == tri[2]) {
            return &TRIGRAMS[i];
        }
    }
    return &TRIGRAMS[0]; /* fallback */
}

static element_t branch_to_element(const char *branch)
{
    for (int i = 0; i < 12; i++) {
        if (strcmp(BRANCH_ELEMENTS[i].branch, branch) == 0) {
            return BRANCH_ELEMENTS[i].element;
        }
    }
    return ELEM_TU; /* fallback */
}

/* ========== 宫位判断 (移植自 getPalaceInfo) ========== */

static void get_palace_info(const int8_t lower[3], const int8_t upper[3],
                            const int8_t **palace_tri, int *shi, int *ying)
{
    int m0 = (lower[0] == upper[0]) ? 0 : 1;
    int m1 = (lower[1] == upper[1]) ? 0 : 1;
    int m2 = (lower[2] == upper[2]) ? 0 : 1;
    int diffs = m0 + m1 * 2 + m2 * 4;

    /* 默认：纯卦 */
    *palace_tri = upper;
    *shi = 5;
    *ying = 2;

    switch (diffs) {
        case 0: *palace_tri = upper; *shi = 5; *ying = 2; break; // 纯卦
        case 1: *palace_tri = upper; *shi = 0; *ying = 3; break; // 一世
        case 3: *palace_tri = upper; *shi = 1; *ying = 4; break; // 二世
        case 7: *palace_tri = upper; *shi = 2; *ying = 5; break; // 三世
        case 6: *palace_tri = lower; *shi = 3; *ying = 0; break; // 四世
        case 4: *palace_tri = lower; *shi = 4; *ying = 1; break; // 五世
        case 5: *palace_tri = lower; *shi = 3; *ying = 0; break; // 游魂
        case 2: *palace_tri = lower; *shi = 2; *ying = 5; break; // 归魂
    }
}

/* ========== 六亲计算 (移植自 getLiuQin) ========== */

static const char *get_liu_qin(element_t palace_e, element_t yao_e)
{
    if (palace_e == yao_e) return "兄弟";
    int p = (int)palace_e;
    int y = (int)yao_e;
    if (y == (p + 4) % 5) return "父母";
    if (y == (p + 1) % 5) return "子孙";
    if (y == (p + 3) % 5) return "官鬼";
    if (y == (p + 2) % 5) return "妻财";
    return "未知";
}

/* ========== 六爻详情计算 ========== */

void calculate_hexagram_details(const int8_t six_yao_array[6],
                                int override_palace_element,
                                hexagram_details_t *out)
{
    memset(out, 0, sizeof(hexagram_details_t));

    int8_t lower[3] = { six_yao_array[0], six_yao_array[1], six_yao_array[2] };
    int8_t upper[3] = { six_yao_array[3], six_yao_array[4], six_yao_array[5] };

    const int8_t *palace_tri_ptr;
    int shi, ying;
    get_palace_info(lower, upper, &palace_tri_ptr, &shi, &ying);

    /* 找到宫位八卦 */
    int8_t palace_tri[3] = { palace_tri_ptr[0], palace_tri_ptr[1], palace_tri_ptr[2] };
    const trigram_info_t *palace = find_trigram(palace_tri);

    /* 静态缓冲区存宫名 */
    static char palace_name_buf[16];
    snprintf(palace_name_buf, sizeof(palace_name_buf), "%s宫", palace->name);
    out->palace_name = palace_name_buf;
    out->palace_element = palace->element;

    element_t base_element = (override_palace_element >= 0)
                             ? (element_t)override_palace_element
                             : palace->element;

    const trigram_info_t *lower_tri = find_trigram(lower);
    const trigram_info_t *upper_tri = find_trigram(upper);

    for (int i = 0; i < 6; i++) {
        const char *branch;
        const char *stem;

        if (i < 3) {
            branch = lower_tri->branches[i];
            stem   = lower_tri->stems[0];
        } else {
            branch = upper_tri->branches[i];
            stem   = upper_tri->stems[1];
        }

        element_t elem = branch_to_element(branch);

        out->yao[i].stem    = stem;
        out->yao[i].branch  = branch;
        out->yao[i].element = element_name(elem);
        out->yao[i].liuqin  = get_liu_qin(base_element, elem);
        out->yao[i].is_shi  = (override_palace_element < 0) ? (i == shi) : 0;
        out->yao[i].is_ying = (override_palace_element < 0) ? (i == ying) : 0;
    }
}

/* ========== 六神列表 (移植自 getSixGodsList) ========== */

void get_six_gods_list(const char *day_gan, const char *out[6])
{
    int start_idx = 0;

    if (strcmp(day_gan, "甲") == 0 || strcmp(day_gan, "乙") == 0) start_idx = 0;
    else if (strcmp(day_gan, "丙") == 0 || strcmp(day_gan, "丁") == 0) start_idx = 1;
    else if (strcmp(day_gan, "戊") == 0) start_idx = 2;
    else if (strcmp(day_gan, "己") == 0) start_idx = 3;
    else if (strcmp(day_gan, "庚") == 0 || strcmp(day_gan, "辛") == 0) start_idx = 4;
    else if (strcmp(day_gan, "壬") == 0 || strcmp(day_gan, "癸") == 0) start_idx = 5;

    for (int i = 0; i < 6; i++) {
        out[i] = SIX_GODS[(start_idx + i) % 6];
    }
}

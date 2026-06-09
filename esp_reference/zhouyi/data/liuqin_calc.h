/**
 * @file liuqin_calc.h
 * @brief 六亲六神纳甲世应计算（移植自 liuqin.js）
 */

#ifndef LIUQIN_CALC_H
#define LIUQIN_CALC_H

#include <stdint.h>

/* 五行 */
typedef enum {
    ELEM_JIN = 0,   // 金
    ELEM_SHUI,       // 水
    ELEM_MU,         // 木
    ELEM_HUO,        // 火
    ELEM_TU           // 土
} element_t;

/* 单爻详情 */
typedef struct {
    const char *stem;       // 天干
    const char *branch;     // 地支
    const char *element;    // 五行（文字）
    const char *liuqin;     // 六亲
    int         is_shi;     // 是否为世
    int         is_ying;    // 是否为应
} yao_detail_t;

/* 六爻完整详情 */
typedef struct {
    const char  *palace_name;       // 宫名（如 "乾宫"）
    element_t    palace_element;    // 宫位五行
    yao_detail_t yao[6];           // 6爻详情 [初爻..上爻]
} hexagram_details_t;

/**
 * @brief 计算六爻详情（六亲、纳甲、世应）
 * @param six_yao_array   阴阳数组 [6], 1=阳, 0=阴
 * @param override_palace 如计算变卦六亲，传入本卦宫位五行；本卦传 -1
 * @param out             输出结果
 */
void calculate_hexagram_details(const int8_t six_yao_array[6],
                                int override_palace_element,
                                hexagram_details_t *out);

/**
 * @brief 根据日干获取六神列表
 * @param day_gan 日天干（UTF-8 字符串如 "甲"）
 * @param out     输出六神名称数组 [6]
 */
void get_six_gods_list(const char *day_gan, const char *out[6]);

/**
 * @brief 获取五行名称
 */
const char *element_name(element_t e);

#endif /* LIUQIN_CALC_H */

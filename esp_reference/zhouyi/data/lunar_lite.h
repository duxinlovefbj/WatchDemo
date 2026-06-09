/**
 * @file lunar_lite.h
 * @brief 精简农历模块 — 公历转天干地支（查表法）
 *
 * 仅实现日干查询，供六神计算使用。
 * 时间通过 NVS 持久化存储，WiFi SNTP 仅在需要时同步。
 */

#ifndef LUNAR_LITE_H
#define LUNAR_LITE_H

#include <stdint.h>
#include <time.h>

/**
 * @brief 天干地支日期信息
 */
typedef struct {
    const char *year_gan_zhi;   // 年干支
    const char *month_gan_zhi;  // 月干支
    const char *day_gan_zhi;    // 日干支
    const char *hour_gan_zhi;   // 【新增】时干支 (如 "庚子")
    const char *day_gan;        // 日天干
    const char *sheng_xiao;     // 生肖
} lunar_info_t;

/**
 * @brief 根据公历日期计算天干地支
 * @param year  公历年 (2020-2040)
 * @param month 公历月 (1-12)
 * @param day   公历日 (1-31)
 * @param out   输出结果
 */
void lunar_calc_gan_zhi(int year, int month, int day, int hour, lunar_info_t *out);

/**
 * @brief 获取当前系统时间的天干地支
 */
void lunar_get_current(lunar_info_t *out);

#endif /* LUNAR_LITE_H */

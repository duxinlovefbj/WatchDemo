/**
 * @file lunar_lite.c
 * @brief 精简农历：公历日期 → 天干地支（算法计算，非查表）
 *
 * 天干地支年月日的计算使用经典公式法：
 * - 日干支：以已知基准日推算
 * - 年干支：(year - 4) % 60
 * - 月干支：以年干推月干 + 月地支固定
 */

#include <string.h>
#include <time.h>
#include "lunar_lite.h"

/* ========== 天干地支表 ========== */

static const char *TIAN_GAN[] = {
    "甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"
};

static const char *DI_ZHI[] = {
    "子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"
};

static const char *SHENG_XIAO[] = {
    "鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪"
};

/* ========== 干支组合缓冲区 ========== */

static char year_gz_buf[8];
static char month_gz_buf[8];
static char day_gz_buf[8];
static char hour_gz_buf[8]; // 【新增】时干支缓冲区

static void make_gan_zhi_str(char *buf, int gan_idx, int zhi_idx)
{
    /* 拷贝天干（UTF-8 中文字符通常 3 字节） */
    const char *g = TIAN_GAN[gan_idx % 10];
    const char *z = DI_ZHI[zhi_idx % 12];

    int gi = 0;
    while (g[gi]) { buf[gi] = g[gi]; gi++; }
    int zi = 0;
    while (z[zi]) { buf[gi + zi] = z[zi]; zi++; }
    buf[gi + zi] = '\0';
}

/* ========== 日干支计算 ========== */

/**
 * 基准日：2000年1月1日 = 庚辰年 丙子月 甲子日
 * 日干支序数 = 0 (甲子)
 * Julian Day Number for 2000-01-01 = 2451545
 */
#define BASE_YEAR  2000
#define BASE_MONTH 1
#define BASE_DAY   1
#define BASE_DAY_GZ_INDEX  0  /* 2000-01-01 → 甲子日 (index 0 in 60-cycle) */
/* 实际: 2000-01-01 是 甲戌日。修正: 甲戌 = 干0(甲) + 支10(戌) => 60甲子第10位 => index=10 */
/* 验证: 甲子=0, 乙丑=1, ..., 甲戌=10 */
#define BASE_DAY_GZ_OFFSET  54

/* ========== 【新增】时干支计算 ========== */
static void calc_hour_gan_zhi(int day_gan_idx, int hour, int *gan, int *zhi)
{
    /* 地支：23:00-00:59为子(0)，01:00-02:59为丑(1)... */
    *zhi = ((hour + 1) % 24) / 2;

    /* 天干：五鼠遁日诀
     * 甲己还加甲，乙庚丙作初，丙辛从戊起，丁壬庚子居，戊癸何方发，壬子是真途
     */
    int gan_start = (day_gan_idx % 5) * 2;
    *gan = (gan_start + *zhi) % 10;
}

static int days_from_base(int year, int month, int day)
{
    /* 计算从 2000-01-01 到目标日期的天数差 */
    struct tm base = {0};
    base.tm_year = BASE_YEAR - 1900;
    base.tm_mon  = BASE_MONTH - 1;
    base.tm_mday = BASE_DAY;
    base.tm_hour = 12;

    struct tm target = {0};
    target.tm_year = year - 1900;
    target.tm_mon  = month - 1;
    target.tm_mday = day;
    target.tm_hour = 12;

    time_t t_base = mktime(&base);
    time_t t_target = mktime(&target);

    return (int)((t_target - t_base) / 86400);
}

/* ========== 年干支 ========== */

static void calc_year_gan_zhi(int year, int *gan, int *zhi)
{
    /* 公历年份转天干地支：
     * 天干 = (year - 4) % 10
     * 地支 = (year - 4) % 12
     */
    int offset = year - 4;
    *gan = ((offset % 10) + 10) % 10;
    *zhi = ((offset % 12) + 12) % 12;
}

/* ========== 月干支 ========== */

/*
 * 月地支固定：正月=寅(2)，二月=卯(3)，……，十二月=丑(1)
 * 以公历月份近似：
 *   公历2月~3月初 ≈ 正月(寅)
 *   简化：month_zhi = (month + 1) % 12  (大致对应)
 *
 * 月天干由年干推算（五虎遁年起月诀）：
 *   甲己之年丙作首（正月起丙寅）
 *   乙庚之年戊为头（正月起戊寅）
 *   丙辛之年寻庚上（正月起庚寅）
 *   丁壬壬寅顺水流（正月起壬寅）
 *   戊癸之年何方发（正月起甲寅）
 */
static void calc_month_gan_zhi(int year, int month, int *gan, int *zhi)
{
    /* 月地支：以节气为准的简化版 */
    /* 公历月份近似映射到农历月份 */
    int lunar_month_approx;
    if (month >= 2) {
        lunar_month_approx = month - 1; /* 2月≈正月, 3月≈二月, ... */
    } else {
        lunar_month_approx = 12; /* 1月≈上年十二月 */
    }

    *zhi = (lunar_month_approx + 1) % 12; /* 正月=寅(2) */

    /* 年干推月干 */
    int year_gan;
    int year_zhi_dummy;
    calc_year_gan_zhi(year, &year_gan, &year_zhi_dummy);

    int month_gan_start;
    switch (year_gan % 5) {
        case 0: month_gan_start = 2; break; /* 甲己 → 丙(2) */
        case 1: month_gan_start = 4; break; /* 乙庚 → 戊(4) */
        case 2: month_gan_start = 6; break; /* 丙辛 → 庚(6) */
        case 3: month_gan_start = 8; break; /* 丁壬 → 壬(8) */
        case 4: month_gan_start = 0; break; /* 戊癸 → 甲(0) */
        default: month_gan_start = 0; break;
    }

    *gan = (month_gan_start + lunar_month_approx - 1) % 10;
}

/* ========== 主计算函数 ========== */

// 【修改】增加 hour 参数
void lunar_calc_gan_zhi(int year, int month, int day, int hour, lunar_info_t *out)
{
    /* 年干支 */
    int y_gan, y_zhi;
    calc_year_gan_zhi(year, &y_gan, &y_zhi);
    make_gan_zhi_str(year_gz_buf, y_gan, y_zhi);
    out->year_gan_zhi = year_gz_buf;
    out->sheng_xiao = SHENG_XIAO[y_zhi];

    /* 月干支 */
    int m_gan, m_zhi;
    calc_month_gan_zhi(year, month, &m_gan, &m_zhi);
    make_gan_zhi_str(month_gz_buf, m_gan, m_zhi);
    out->month_gan_zhi = month_gz_buf;

    /* 日干支 */
    int days = days_from_base(year, month, day);
    int day_gz_idx = ((days + BASE_DAY_GZ_OFFSET) % 60 + 60) % 60;
    int d_gan = day_gz_idx % 10;
    int d_zhi = day_gz_idx % 12;
    make_gan_zhi_str(day_gz_buf, d_gan, d_zhi);
    out->day_gan_zhi = day_gz_buf;
    out->day_gan = TIAN_GAN[d_gan];

    /* 【新增】时干支 */
    int h_gan, h_zhi;
    calc_hour_gan_zhi(d_gan, hour, &h_gan, &h_zhi);
    make_gan_zhi_str(hour_gz_buf, h_gan, h_zhi);
    out->hour_gan_zhi = hour_gz_buf;
}

void lunar_get_current(lunar_info_t *out)
{
    time_t now;
    time(&now);
    struct tm *tm_info = localtime(&now);

    int year  = tm_info->tm_year + 1900;
    int month = tm_info->tm_mon + 1;
    int day   = tm_info->tm_mday;
    int hour  = tm_info->tm_hour; // 【新增】获取当前小时

    lunar_calc_gan_zhi(year, month, day, hour, out);
}

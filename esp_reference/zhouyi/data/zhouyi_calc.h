/**
 * @file zhouyi_calc.h
 * @brief 周易起卦计算（移植自 zhouyi.js）
 */

#ifndef ZHOUYI_CALC_H
#define ZHOUYI_CALC_H

#include <stdint.h>

/**
 * @brief 起卦结果
 */
typedef struct {
    int8_t  ben_gua[6];      // 本卦阴阳数组
    int8_t  zhi_gua[6];      // 之卦阴阳数组
    int     ben_gua_idx;     // 本卦在 gua_data 中的索引
    int     zhi_gua_idx;     // 之卦在 gua_data 中的索引
    int     sum_bian;        // 变爻数量
    int     bian_index[6];   // 变爻位置
    int     bian_count;      // 变爻数量（与 sum_bian 相同）
} zhouyi_result_t;

/**
 * @brief SFC32 伪随机数生成器状态
 */
typedef struct {
    uint32_t a, b, c, d;
} sfc32_state_t;

/**
 * @brief 初始化 SFC32 PRNG
 */
void sfc32_init(sfc32_state_t *state, uint32_t seed);

/**
 * @brief 生成一个 [0, 1) 范围的浮点随机数
 */
float sfc32_next(sfc32_state_t *state);

/**
 * @brief 根据旋钮角度种子生成一爻结果
 * @param seed 旋钮累计旋转值
 * @return 爻值: 6(老阴), 7(少阳), 8(少阴), 9(老阳)
 */
int generate_single_yao(int32_t seed);

/**
 * @brief 根据六爻数组计算卦象
 * @param six_yao 六个爻值 (6/7/8/9) 的数组
 * @param result  输出结果
 */
void zhouyi_calculate(const int six_yao[6], zhouyi_result_t *result);

/**
 * @brief 判断爻值是阴还是阳
 * @return 1=阳, 0=阴
 */
int is_yang(int yao);

#endif /* ZHOUYI_CALC_H */

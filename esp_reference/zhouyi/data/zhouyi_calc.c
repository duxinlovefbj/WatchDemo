/**
 * @file zhouyi_calc.c
 * @brief 周易起卦计算（移植自 zhouyi.js）
 */

#include <stdlib.h>
#include <math.h>
#include <string.h>
#include "zhouyi_calc.h"
#include "zhouyi_data.h"

/* ========== SFC32 PRNG (同 app.js 中的 sfc32) ========== */

void sfc32_init(sfc32_state_t *state, uint32_t seed)
{
    state->a = 0x9E3779B9;
    state->b = 0x243F6A88;
    state->c = 0xB7E15162;
    state->d = seed;
    /* 预热 */
    for (int i = 0; i < 12; i++) {
        sfc32_next(state);
    }
}

float sfc32_next(sfc32_state_t *state)
{
    uint32_t a = state->a;
    uint32_t b = state->b;
    uint32_t c = state->c;
    uint32_t d = state->d;

    uint32_t t = (a + b);
    a = b ^ (b >> 9);
    b = c + (c << 3);
    c = (c << 21) | (c >> 11);
    d = d + 1;
    t = t + d;
    c = c + t;

    state->a = a;
    state->b = b;
    state->c = c;
    state->d = d;

    return (float)(t) / 4294967296.0f;
}

/* ========== 爻值生成 ========== */

int generate_single_yao(int32_t seed)
{
    uint32_t s = (uint32_t)abs(seed);
    if (s == 0) s = 12345; /* 防止零种子 */
    sfc32_state_t rng;
    sfc32_init(&rng, s);

    int sum = 0;
    for (int j = 0; j < 3; j++) {
        sum += (sfc32_next(&rng) < 0.5f) ? 3 : 2;
    }
    return sum; /* 6, 7, 8, 或 9 */
}

/* ========== 阴阳判断 ========== */

int is_yang(int yao)
{
    return (yao % 2 != 0) ? 1 : 0;
}

/* ========== 起卦计算 (移植自 zhouyi.js 的 zhouYi 函数) ========== */

void zhouyi_calculate(const int six_yao[6], zhouyi_result_t *result)
{
    memset(result, 0, sizeof(zhouyi_result_t));

    int sum_bian = 0;
    int bian_idx = 0;

    for (int i = 0; i < 6; i++) {
        result->ben_gua[i] = is_yang(six_yao[i]);

        if (six_yao[i] == 6 || six_yao[i] == 9) {
            /* 变爻：老阴变阳，老阳变阴 */
            sum_bian++;
            result->zhi_gua[i] = is_yang(six_yao[i]) ? 0 : 1;
            result->bian_index[bian_idx++] = i;
        } else {
            /* 不变爻 */
            result->zhi_gua[i] = is_yang(six_yao[i]);
        }
    }

    result->sum_bian = sum_bian;
    result->bian_count = bian_idx;

    /* 查找卦名索引 */
    result->ben_gua_idx = gua_find_index(result->ben_gua);
    result->zhi_gua_idx = gua_find_index(result->zhi_gua);
}

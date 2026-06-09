/**
 * @file ui_casting.h
 * @brief 掷爻交互界面
 */

#ifndef UI_CASTING_H
#define UI_CASTING_H

#include "lvgl.h"
#include <stdint.h>

/**
 * @brief 创建掷爻界面
 * @param parent 父对象
 * @return 掷爻页面容器
 */
lv_obj_t *ui_casting_create(lv_obj_t *parent);

/**
 * @brief 处理旋钮旋转事件
 * @param delta 旋转增量（正=顺时针，负=逆时针）
 */
void ui_casting_on_knob(int delta);

/**
 * @brief 删除掷爻页面
 */
void ui_casting_destroy(void);

/**
 * @brief 获取已完成的爻数
 */
int ui_casting_get_yao_count(void);

/**
 * @brief 获取六爻结果数组
 */
const int *ui_casting_get_results(void);

#endif /* UI_CASTING_H */

void ui_casting_commit_seed(void);
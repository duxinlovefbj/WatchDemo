/**
 * @file ui_result.h
 * @brief 结果展示界面
 */

#ifndef UI_RESULT_H
#define UI_RESULT_H

#include "lvgl.h"
#include "data/zhouyi_calc.h"

/**
 * @brief 创建结果展示页
 * @param parent 父对象
 * @param result 起卦结果
 * @return 结果页面容器
 */
lv_obj_t *ui_result_create(lv_obj_t *parent, const zhouyi_result_t *result);

/**
 * @brief 删除结果页面
 */
void ui_result_destroy(void);

#endif /* UI_RESULT_H */

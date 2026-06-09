/**
 * @file ui_taiji.h
 * @brief 太极图 + 八卦外圈绘制
 */

#ifndef UI_TAIJI_H
#define UI_TAIJI_H

#include "lvgl.h"

/**
 * @brief 创建太极图欢迎页面
 * @param parent 父对象
 * @return 太极页面容器
 */
lv_obj_t *ui_taiji_create(lv_obj_t *parent);

/**
 * @brief 设置太极图旋转角度
 * @param angle 角度 (0.1度为单位, 如 900 = 90度)
 */
void ui_taiji_set_angle(int32_t angle);

/**
 * @brief 删除太极页面
 */
void ui_taiji_destroy(void);

#endif /* UI_TAIJI_H */

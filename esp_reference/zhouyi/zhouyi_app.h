/**
 * @file houyi_app.h (建议重命名为 zhouyi_app.h)
 * @brief 六爻模块状态机总控
 */

#ifndef ZHOUYI_APP_H
#define ZHOUYI_APP_H

#include "driver/i2c_master.h"

/* 模块内部状态 */
typedef enum {
    ZHOUYI_STATE_WELCOME,   // 欢迎/太极图页
    ZHOUYI_STATE_CASTING,   // 掷爻中
    ZHOUYI_STATE_RESULT,    // 结果展示
} zhouyi_app_state_t;

/**
 * @brief 初始化并启动六爻模块
 * @param parent 外部主程序传入的容器(通常是当前界面的 lv_obj)
 * @param i2c_bus I2C 总线句柄（用于初始化 DRV2605L）
 */
void zhouyi_app_init(lv_obj_t *parent, i2c_master_bus_handle_t i2c_bus);

/**
 * @brief 销毁本模块，释放所有资源，用于退出到主菜单
 */
void zhouyi_app_deinit(void);

/**
 * @brief 切换六爻模块内部状态
 */
void zhouyi_app_set_state(zhouyi_app_state_t state);

/**
 * @brief 获取当前六爻模块内部状态
 */
zhouyi_app_state_t zhouyi_app_get_state(void);

/**
 * @brief 长按退出应用回调
 */
void zhouyi_app_exit(void);

#endif /* ZHOUYI_APP_H */

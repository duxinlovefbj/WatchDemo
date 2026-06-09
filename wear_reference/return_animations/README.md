# Oppo Watch X2 健康应用 - 返回与退出动画组件说明

本文件夹整理了从 Oppo Watch X2 自带健康应用（解包项目）中搜寻到的所有与“返回”（返回上一页、退出 Activity/Fragment/Dialog）以及“返回按钮”相关的动画定义及关键 UI 资源。

---

## 📂 整理文件清单及功能说明

| 文件名 | 原始路径 | 类型 | 功能与动画细节说明 |
| :--- | :--- | :--- | :--- |
| [**hey_activity_anim.xml**](file:///l:/apk/health/res/return_animations/hey_activity_anim.xml) | `res/anim/hey_activity_anim.xml` | 动画 (Animation) | **Activity 退出/返回动画**。采用平移动画，在 `276ms` 内将页面从当前位置（`fromXDelta="0.0%p"`）向右平移出屏幕（`toXDelta="100.0%p"`），是经典的右滑返回/退出动画。 |
| [**fragment_close_enter.xml**](file:///l:/apk/health/res/return_animations/fragment_close_enter.xml) | `res/animator/fragment_close_enter.xml` | 属性动画 (Animator) | **Fragment 关闭时，下方被覆盖的 Fragment 的进入动画**。在 `300ms` 内，X和Y轴缩放从 `1.1` 变回 `1.0`（恢复正常大小），同时在延迟 `66ms` 后执行 `50ms` 的渐显（Alpha 从 `0` 到 `1`）。 |
| [**fragment_close_exit.xml**](file:///l:/apk/health/res/return_animations/fragment_close_exit.xml) | `res/animator/fragment_close_exit.xml` | 属性动画 (Animator) | **Fragment 关闭时，当前 Fragment 的退出动画**。在 `300ms` 内，X和Y轴缩放从 `1.0` 缩小到 `0.9`，同时在延迟 `66ms` 后执行 `50ms` 的渐隐（Alpha 从 `1` 到 `0`）。 |
| [**fragment_fade_exit.xml**](file:///l:/apk/health/res/return_animations/fragment_fade_exit.xml) | `res/animator/fragment_fade_exit.xml` | 属性动画 (Animator) | **Fragment 退出淡出动画**。在 `150ms` 内，将 Alpha 通道从 `1.0` 渐变至 `0.0`，实现单纯的渐隐退出。 |
| [**hey_dialog_slide_off.xml**](file:///l:/apk/health/res/return_animations/hey_dialog_slide_off.xml) | `res/anim/hey_dialog_slide_off.xml` | 动画 (Animation) | **对话框 (Dialog) 的退出/关闭动画**。在 `267ms` 内将 Alpha 从 `1.0` 渐变到 `0.0` 实现淡出关闭。 |
| [**hey_back.xml**](file:///l:/apk/health/res/return_animations/hey_back.xml) | `res/drawable/hey_back.xml` | 矢量图 (Drawable) | **返回按钮的 SVG/Vector 矢量图标**。白色箭头（`#ffffff`），尺寸为 `18.0dp x 18.0dp`，在健康应用的返回标题栏中广泛使用。 |

---

## 🔍 相关配置与样式解析 (styles.xml)

在应用的 `res/values/styles.xml` 中，有以下关键样式定义了这些返回与退出动画的使用方式：

### 1. 窗口退出动画样式 (`animation_translucent_translate`)
该样式继承自系统半透明窗口样式，在窗口退出时指定使用 `hey_activity_anim.xml`：
```xml
<style name="animation_translucent_translate" parent="@android:style/Animation.Translucent">
    <item name="android:windowExitAnimation">@anim/hey_activity_anim</item>
</style>
```

### 2. 对话框动画样式 (`DialogAnimations`)
在对话框关闭时指定使用 `hey_dialog_slide_off.xml`（淡出）：
```xml
<style name="DialogAnimations">
    <item name="android:windowEnterAnimation">@anim/hey_dialog_slide_in</item>
    <item name="android:windowExitAnimation">@anim/hey_dialog_slide_off</item>
</style>
```

### 3. 系统滑动手势返回配置 (`windowSwipeToDismiss`)
作为 Wear OS (Oppo Watch X2 基于 Android / ColorOS Watch) 设备上的应用，大多数主题（如 `HeytapTheme`、`HeytapCompatTheme`）默认启用了系统级右滑手势返回：
```xml
<style name="HeytapTheme" parent="@android:style/Theme.DeviceDefault.NoActionBar.Fullscreen">
    <!-- 启用右滑退出当前界面（系统级手势返回） -->
    <item name="android:windowSwipeToDismiss">true</item>
</style>
```
部分不支持滑动返回的界面（如运动进行中、倒计时等）会显式将其关闭：
```xml
<style name="HeyNoSwipeStyle" parent="@style/HeytapTheme">
    <item name="android:windowSwipeToDismiss">false</item>
</style>
```

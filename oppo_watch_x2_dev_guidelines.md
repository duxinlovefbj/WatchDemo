# OPPO Watch X2 自用应用开发规范

> 适用场景：OPPO Watch X2 / 圆形智能手表 / 自用安装 APK / VS Code + Android SDK 命令行开发  
> 当前目标：优先保证应用能启动、能调试、能适配圆屏；表冠、传感器、复杂动效后续再逐步加入。

---

## 1. 基本设备信息

| 项目 | 参数 |
|---|---|
| 设备 | OPPO Watch X2 |
| 型号 | OWW251 |
| 屏幕 | 466 × 466 |
| 屏幕形态 | 1:1 圆形屏幕 |
| 内存 | 2GB |
| 存储 | 32GB |
| 芯片 | 高通骁龙 W5 + BES2800BP |
| 系统 | ColorOS Watch / Android 系统 |
| 网络 | 蓝牙网络、eSIM、WLAN |
| 传感器 | 加速度、陀螺仪、地磁、气压、心率、环境光、ECG、血氧、腕温等 |

---

## 2. 开发环境规范

推荐使用：

```text
VS Code
JDK 17
Android SDK Command-line Tools
Gradle / Gradle Wrapper
adb
```

不强制安装 Android Studio。

推荐环境变量：

```powershell
ANDROID_HOME=D:\Android\Sdk
ANDROID_SDK_ROOT=D:\Android\Sdk
PATH += D:\Android\Sdk\cmdline-tools\latest\bin
PATH += D:\Android\Sdk\platform-tools
```

基础检查命令：

```powershell
java -version
javac -version
sdkmanager --version
adb version
gradle -v
```

---

## 3. 项目配置规范

### 3.1 SDK 版本建议

如果按 OPPO 圆表文档中 Android R / Android 11 理解，可使用：

```gradle
compileSdk 35
minSdk 30
targetSdk 35
```

其中：

```text
minSdk 30 = Android 11
compileSdk 35 = 使用较新的编译 API
targetSdk 35 = 面向新系统行为
```

如果出现兼容性问题，可先尝试降低 `targetSdk`，例如：

```gradle
targetSdk 30
```

但一般建议先保留较新的 `compileSdk`。

---

## 4. 应用设计规范

### 4.1 只考虑圆表

当前应用仅面向 OPPO Watch X2 自用，不考虑方表和官方商店上架。

资源目录可以优先使用：

```text
layout-round/
drawable-round-xhdpi/
values-round/
```

如果项目较小，也可以暂时只使用默认目录：

```text
layout/
drawable/
values/
```

但布局设计必须按圆屏思路处理。

---

### 4.2 圆屏布局安全区

圆形屏幕的边缘极易裁剪布局内容。为了保障内容显示完整性，必须遵守以下安全区和边距设计：

*   **列表页顶/底留白**：
    *   列表上方随列表一起滚动的标题或首个条目必须留有足够的顶部内边距，推荐 `paddingTop="36dp"`。
    *   列表最底部的条目必须有底部留白，以防止被圆形屏幕的弧度裁切，推荐在容器上设置 `paddingBottom="60dp"`。
*   **水平外平移边距**：
    *   对于卡片条目，在 `drawable`（如 `hey_base_item_bg.xml`）中应使用 `layer-list` 增加水平外缩进边距：**左右为 `12dp`**，**上下为 `2dp`**。这样可以确保最顶部/最底部的胶囊卡片缩小时不会贴边被切掉圆角。
*   **卡片内边距**：
    *   卡片内部内容（图标、文字）与卡片边缘之间应留有足够的内边距，推荐 **左右内边距 `28dp`**。

---

## 5. UI 与视觉规范

### 5.1 字体和文字

手表屏幕较小，文字必须保持精炼且易读。

*   **列表分类/说明标题**：字号 `13sp`，粗体，颜色推荐 `#888888`（中灰色）。
*   **卡片主要内容文字**：字号 `16sp`，颜色 `#FFFFFF`，必须设置单行并采用尾部截断：
    ```xml
    android:singleLine="true"
    android:ellipsize="end"
    ```
*   **图标字符**：如果使用文字字符作为卡片左侧小图标，字号推荐 `12sp`。

### 5.2 胶囊卡片（Item Card）规范

应用内的列表项或按钮推荐采用“胶囊卡片（Capsule Card）”的圆角矩形设计，确保在圆形屏幕中获得良好的视觉和触控体验。

*   **高度**：推荐固定高度 `64dp`（如 `item_tarot_array.xml`）或通过上下内边距自适应，但不应小于 `48dp`。
*   **背景圆角半径**：`30dp`（呈现完美的扁平胶囊形状）。
*   **背景填充色**：`#202124`（半透明质感的深灰色）。
*   **背景资源定义示例 (`hey_base_item_bg.xml`)**：
    ```xml
    <?xml version="1.0" encoding="utf-8"?>
    <layer-list xmlns:android="http://schemas.android.com/apk/res/android">
        <item 
            android:left="12dp" 
            android:top="2dp" 
            android:right="12dp" 
            android:bottom="2dp">
            <shape android:shape="rectangle">
                <corners android:radius="30dp" />
                <solid android:color="#202124" />
            </shape>
        </item>
    </layer-list>
    ```
*   **左侧小图标**：
    *   圆形小背景容器：`30dp * 30dp`，通常使用带有一定透明度的白色背景（如 `@drawable/circle_white_opacity`）。
    *   与右侧文字的间距：右外边距 `10dp`。

### 5.3 颜色系统

手表在明亮户外及暗光环境下的使用场景多变，需采用高对比度、低亮度的黑色暗系色调。

*   **全局底色**：纯黑 `#000000`。
*   **卡片背景色**：深灰 `#202124`。
*   **主要文本色**：纯白 `#FFFFFF`。
*   **辅助/说明文本色**：中灰 `#888888`。

---

## 6. 兼容性与防闪退规范

OPPO Watch X2 虽然是 Android 系统，但手表系统可能对部分手机 App 默认能力支持不完整。

为减少闪退，建议遵守：

### 6.1 Activity 尽量简单

初始测试 App 不要一开始加入：

```text
复杂主题
AppCompat
Material Components
第三方库
网络请求
传感器
后台服务
权限申请
```

建议先使用原生：

```java
android.app.Activity
TextView
LinearLayout
FrameLayout
```

### 6.2 主题尽量使用系统基础主题

优先使用：

```xml
<style name="AppTheme" parent="android:style/Theme.DeviceDefault.NoActionBar">
    <item name="android:windowNoTitle">true</item>
    <item name="android:windowFullscreen">true</item>
    <item name="android:windowBackground">#000000</item>
</style>
```

### 6.3 Manifest 保持最小化

最小 Manifest 示例：

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:theme="@style/AppTheme"
        android:label="WatchDemo"
        android:allowBackup="false"
        android:supportsRtl="true">

        <activity
            android:name=".MainActivity"
            android:exported="true">

            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>

        </activity>

    </application>

</manifest>
```

---

## 7. 调试规范

### 7.1 adb 连接

无线调试连接后，先检查：

```powershell
adb devices
```

看到设备状态为 `device` 才能安装 and 调试。

### 7.2 安装 APK

```powershell
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

如果安装失败，可先卸载：

```powershell
adb uninstall com.example.watchdemo
adb install .\app\build\outputs\apk\debug\app-debug.apk
```

---

## 8. 闪退排查规范

如果应用安装成功但打开闪退，第一步必须抓日志。

### 8.1 清空日志

```powershell
adb logcat -c
```

### 8.2 启动应用后抓 crash

```powershell
adb logcat -d | findstr /i "FATAL EXCEPTION AndroidRuntime watchdemo"
```

如果包名不是 `watchdemo`，替换为实际包名关键字。

### 8.3 实时查看日志

```powershell
adb logcat | findstr /i "AndroidRuntime FATAL Exception"
```

### 8.4 常见闪退原因

| 问题 | 可能原因 |
|---|---|
| 打开即闪退 | 主题不兼容、Activity 配置错误、缺少资源 |
| 安装成功但找不到图标 | Manifest 没有 MAIN / LAUNCHER |
| 安装失败 | 版本号降级、签名冲突、minSdk 高于设备系统 |
| UI 显示不完整 | 没有考虑圆屏安全区 |
| 运行时权限异常 | 过早调用传感器、网络、定位等能力 |

---

## 9. 版本迭代规范

建议每次只改一个方向，方便排错。

推荐顺序：

```text
1. 最小 Activity 启动成功
2. 黑底白字显示成功
3. 增加圆屏布局
4. 增加按钮交互
5. 增加页面切换
6. 增加动画
7. 增加表冠测试
8. 增加传感器或网络能力
```

不要一开始同时加入 UI、动画、表冠、传感器和第三方库。

---

## 10. 表冠开发与交互动效规范

本项目已实现完善的物理表冠旋转接收、防抖分发和边缘缩放动效。

### 10.1 表冠旋转事件捕获与防抖分发

*   **捕获机制**：`MainActivity` 覆写 `onGenericMotionEvent(MotionEvent event)`，捕获 `ACTION_SCROLL` 事件下的 `AXIS_SCROLL` 或 `AXIS_VSCROLL` 轴浮点旋转值。
*   **防抖算法**：通过自定义 `WatchCrownHandler` 进行两重防抖：
    *   **累加阈值**：旋转位移累加至 `threshold = 0.5f` 触发一次步进。
    *   **时间冷却**：两次步进触发之间的冷却时间 `cooldownMs = 120ms`，防止马达和滚动响应过频。
*   **分发**：触发步进后回调 `onStepClockwise()` 或 `onStepCounterClockwise()`，由控制器转发至当前 View 的 `onCrownScroll(boolean clockwise)`。

### 10.2 列表步进滚动与震动反馈 (Haptics)

*   **步进滑动**：接收到 `onCrownScroll(boolean clockwise)` 信号后，列表以卡片间距（如 `64dp`）进行 `smoothScrollBy`。
*   **刻度震动**：在每次滑动的瞬间提供微震动反馈，调用 `vibrateCustom(VibrationEffect.EFFECT_TICK)` 以产生真实表冠旋转的段落机械刻度感。

```java
public void onCrownScroll(boolean clockwise) {
    ScrollView scrollView = findViewById(R.id.scroll_view);
    if (scrollView != null) {
        int dy = (int) (64 * activity.density); // 滚动距离为卡片高度 + 间距 (64dp)
        if (!clockwise) {
            dy = -dy;
        }
        scrollView.smoothScrollBy(0, dy);
        activity.vibrateCustom(android.os.VibrationEffect.EFFECT_TICK);
    }
}
```

### 10.3 滚边弯曲与缩放动画 (Circular Edge Transition)

当卡片列表滚动时，需要对进入屏幕顶部和底部圆弧收窄区域的卡片进行“缩小、叠放、渐隐”动效，以 1:1 还原 Wear OS 原生系统的边缘动效。

*   **活跃区域**：顶部 `64dp`，底部 `parentHeight - 64dp`。
*   **极值区域**：顶部极值 `-20dp`，底部极值 `parentHeight + 20dp`。
*   **变换计算**：
    *   `scale = ratio * 0.15f + 0.85f` (Scale 范围从 `1.0` 到 `0.85`)
    *   `alpha = ratio * 0.3f + 0.7f` (Alpha 范围从 `1.0` 到 `0.7`)
    *   `translationY`：引入向下（顶部）或向上（底部）的堆叠偏移：`translationY = ((1 - ratio) * childHeight * 0.15f) / 2`。
*   **监听时机**：在 `ScrollView` 的 `setOnScrollChangeListener` 以及容器的 `addOnLayoutChangeListener` 中实时触发重算。

### 10.4 按压微动画 (Press Micro-Animation)

列表项被手指按下与松开时必须伴随缩放与透明度的渐变动画：

*   **按下态 (ACTION_DOWN)**：时长 `200ms`，缩放至 `0.94f`，透明度渐变至 `0.8f`。
*   **恢复态 (ACTION_UP/ACTION_CANCEL)**：时长 `250ms`，恢复至 `1.0f`。
*   **注意事项**：在卡片 Touch 监听中处理此动画时，必须返回 `false`，以允许 onClick 事件正确分发。

```java
card.setOnTouchListener(new OnTouchListener() {
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                v.animate().scaleX(0.94f).scaleY(0.94f).alpha(0.8f).setDuration(200).start();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                v.animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(250).start();
                break;
        }
        return false; // 必须返回 false
    }
});
```

### 10.5 OPPO 专属线性马达震动方案 (LinearmotorVibrator)

为了在 ColorOS Watch 系统中获得 native 级别的清脆机械表冠刻度感（而不是标准的略带粘滞感的软件振动），必须使用系统隐藏的 `"linearmotor"` 服务。

*   **反射获取服务**：
    由于编译期没有 OPPO SDK 依赖，需要通过 Java 反射获取 `android.os.linearmotorvibrator.LinearmotorVibrator` 实例及相关的 `WaveformEffect`：
    ```java
    Object lmService = getSystemService("linearmotor");
    Class<?> vibratorClass = Class.forName("android.os.linearmotorvibrator.LinearmotorVibrator");
    Class<?> effectClass = Class.forName("android.os.linearmotorvibrator.WaveformEffect");
    Class<?> builderClass = Class.forName("android.os.linearmotorvibrator.WaveformEffect$Builder");
    ```
*   **机械表冠刻度映射**：
    native 表冠旋转段落感（Detent Tick）的波形 ID 是 `302`（强度 `2`，不循环 `false`）。在触发震动时，应在后台工作线程构建该波形并调用 `vibrate()`：
    ```java
    Object builder = builderClass.getConstructor().newInstance();
    builderClass.getMethod("setEffectType", int.class).invoke(builder, 302);
    builderClass.getMethod("setEffectStrength", int.class).invoke(builder, 2);
    builderClass.getMethod("setEffectLoop", boolean.class).invoke(builder, false);
    Object effect = builderClass.getMethod("build").invoke(builder);
    vibratorClass.getMethod("vibrate", effectClass).invoke(lmService, effect);
    ```
*   **防抖与去重设计**：
    *   **异步线程分发**：所有马达震动应分发到专属的 `HandlerThread`（例如命名为 `"crown_vibrate"`），避免主线程绘制掉帧。
    *   **队列防积压**：在高频表冠旋转（冷却时间缩短至 `20ms` 级别时），执行 `post()` 之前必须调用 `removeCallbacks(reusableRunnable)`，清除历史积压未执行的震动请求，确保无延迟响应。
    *   **优雅降级**：反射调用全部包裹在 `try-catch` 中，一旦失效自动降级到系统标准的 `vibrator.vibrate(VibrationEffect.EFFECT_TICK)`。

### 10.6 环形轨道滚动绝对坐标系与对齐吸附 (Snapping) 规范

对于像“卡牌环形切换”等首尾相连的列表，在旋转表冠交互设计中，应遵循以下两点核心数学模型：

*   **绝对坐标系统（无断裂感动画）**：
    *   **物理层不做归一化**：物理状态量（如 `cardProgress` 与 `targetProgress`）应该被设计为**绝对连续的浮点数坐标**，允许随着旋转无限累加或累减。
    *   **渲染层按需归一化**：在卡槽位移计算和绘制方法的入口处（例如 `updateCardPositions()`），通过 `(progress % S + S) % S` 单向转化为 `[0, S)` 进行渲染。
    *   **设计收益**：这样可以彻底避免跨越 `S/2` 环形边界时，插值器或计算公式计算出错误方向或极大突变距离，导致动画突发高速乱滚。
*   **取整截断限幅（精准吸附对齐）**：
    *   **常规限幅的弊端**：若高频旋转时采用常规浮点数限幅（如 `targetProgress = Math.min(targetProgress, cardProgress + 2.5f)`），在用户停止旋转时，目标位置极易停在非整数（如 `.5`）上，导致列表无法对齐正中心。
    *   **方向敏感型取整**：在限幅时根据旋转方向采用 `Math.floor` 或 `Math.ceil` 强行将限幅边界转换为最靠近的绝对整数：
        *   **顺时针（向右）**：`targetProgress = Math.floor(cardProgress + maxAhead)`
        *   **逆时针（向左）**：`targetProgress = Math.ceil(cardProgress - maxAhead)`
    *   **设计收益**：即使超频限幅发生，目标点依然永远被锁定在离物理进度最近的合法整数上，动画减速停止后，卡片一定能完美、平滑地对齐到中间指示器。

---

## 11. 自用应用开发原则

因为应用不准备上架官方商店，所以可以简化：

```text
不必适配方表
不必处理大量机型
不必做复杂权限说明
不必完全遵守商店审核规范
```

但仍应保证：

```text
不闪退
可卸载
不后台乱跑
不高频耗电
不长时间占用传感器
不在圆屏边缘放关键内容
```

---

## 12. 当前推荐的最小开发路线

```text
阶段 1：原生 Android 跑通 Hello Watch
阶段 2：抓取闪退日志，修复兼容性问题
阶段 3：做 466×466 圆屏静态 UI
阶段 4：加入基础点击交互与按压微动画
阶段 5：加入表冠滑动、防抖和震动反馈，以及边缘弯曲缩放特效
阶段 6：进一步扩展传感器或网络能力
```

---

## 13. 项目备注

当前项目应优先追求：

```text
简单
稳定
可调试
可逐步扩展
```

不要过早追求完整工程化和复杂架构。

手表开发的第一原则是：

```text
先跑起来，再变好看；先稳定，再加功能。
```

---

## 14. 六爻部分开发设计与算法移植规范

六爻模块移植自 `esp_reference/zhouyi` 中的 C 语言实现，包含了起卦随机算法、纳甲排盘引擎、精简农历干支及极坐标对称双半圆 UI 交互系统。

### 14.1 SFC32 随机数生成器与三钱法模拟

为保证起卦的纯正与传统，必须恢复“三钱法”（Binomial Distribution，抛掷三枚硬币算得单爻）的概率分布，而不是简单的等概率随机。

*   **传统三钱法概率分布**：
    *   **老阴 (6)**：3枚反面（阴），概率 $1/8 = 12.5\%$，变爻（阴变阳，▅▅ ▅▅ ➔ ▅▅▅▅▅）
    *   **少阳 (7)**：1枚正面（阳）2枚反面，概率 $3/8 = 37.5\%$，不变爻（阳，▅▅▅▅▅）
    *   **少阴 (8)**：2枚正面1枚反面，概率 $3/8 = 37.5\%$，不变爻（阴，▅▅ ▅▅）
    *   **老阳 (9)**：3枚正面（阳），概率 $1/8 = 12.5\%$，变爻（阳变阴，▅▅▅▅▅ ➔ ▅▅ ▅▅）

*   **SFC32 (Simple Fast Chaotic 32-bit PRNG) 算法及三钱法 Java 移植实现**：
    ```java
    public static class SFC32 {
        private int a, b, c, d;

        public SFC32(int seed) {
            a = 0x9E3779B9;
            b = 0x243F6A88;
            c = 0xB7E15162;
            d = seed == 0 ? 12345 : Math.abs(seed);
            // 预热 12 次
            for (int i = 0; i < 12; i++) {
                next();
            }
        }

        public float next() {
            int t = a + b;
            a = b ^ (b >>> 9);
            b = c + (c << 3);
            c = (c << 21) | (c >>> 11);
            d = d + 1;
            t = t + d;
            c = c + t;
            // 转换为 [0, 1) 之间的无符号浮点数
            return (float) ((t & 0xFFFFFFFFL) / 4294967296.0);
        }
    }

    public static int generateSingleYao(int seed) {
        SFC32 rng = new SFC32(seed);
        int sum = 0;
        for (int i = 0; i < 3; i++) {
            // 每枚硬币：正面(3，概率0.5)，反面(2，概率0.5)
            sum += (rng.next() < 0.5f) ? 3 : 2;
        }
        return sum; // 产生 6, 7, 8, 9
    }
    ```

### 14.2 纳甲排盘引擎 (Najia Engine)

相比于硬编码 64 卦的本卦与变卦详情，排盘引擎可动态计算出任何一卦的世应、纳甲干支与六亲。

*   **基础八卦定义与纳甲参数表**：
    ```java
    public static class Trigram {
        public int[] lines;      // 三爻组合 [下爻..上爻], 1=阳, 0=阴
        public String name;       // 卦名
        public int element;       // 五行索引 (0:金, 1:水, 2:木, 3:火, 4:土)
        public String[] stems;    // 纳甲天干 [下卦用, 上卦用]
        public String[] branches; // 纳甲地支 [下爻..上爻]

        public Trigram(int[] lines, String name, int element, String[] stems, String[] branches) {
            this.lines = lines;
            this.name = name;
            this.element = element;
            this.stems = stems;
            this.branches = branches;
        }
    }

    public static final Trigram[] TRIGRAMS = {
        new Trigram(new int[]{1, 1, 1}, "乾", 0, new String[]{"甲", "壬"}, new String[]{"子", "寅", "辰", "午", "申", "戌"}),
        new Trigram(new int[]{1, 1, 0}, "兑", 0, new String[]{"丁", "丁"}, new String[]{"巳", "卯", "丑", "亥", "酉", "未"}),
        new Trigram(new int[]{1, 0, 1}, "离", 3, new String[]{"己", "己"}, new String[]{"卯", "丑", "亥", "酉", "未", "巳"}),
        new Trigram(new int[]{1, 0, 0}, "震", 2, new String[]{"庚", "庚"}, new String[]{"子", "寅", "辰", "午", "申", "戌"}),
        new Trigram(new int[]{0, 1, 1}, "巽", 2, new String[]{"辛", "辛"}, new String[]{"丑", "亥", "酉", "未", "巳", "卯"}),
        new Trigram(new int[]{0, 1, 0}, "坎", 1, new String[]{"戊", "戊"}, new String[]{"寅", "辰", "午", "申", "戌", "子"}),
        new Trigram(new int[]{0, 0, 1}, "艮", 4, new String[]{"丙", "丙"}, new String[]{"辰", "午", "申", "戌", "子", "寅"}),
        new Trigram(new int[]{0, 0, 0}, "坤", 4, new String[]{"乙", "癸"}, new String[]{"未", "巳", "卯", "丑", "亥", "酉"})
    };
    ```

*   **世应位置与卦宫推算算法**：
    对比本卦的上卦和下卦在三爻上的异同点：
    ```java
    public static class PalaceInfo {
        public Trigram palaceTrigram; // 卦宫八卦
        public int shi;               // 世爻索引 (0-5)
        public int ying;              // 应爻索引 (0-5)
    }

    public static PalaceInfo getPalaceInfo(int[] lowerLines, int[] upperLines) {
        int m0 = (lowerLines[0] == upperLines[0]) ? 0 : 1;
        int m1 = (lowerLines[1] == upperLines[1]) ? 0 : 1;
        int m2 = (lowerLines[2] == upperLines[2]) ? 0 : 1;
        int diffs = m0 + m1 * 2 + m2 * 4;

        int shi = 5;
        int ying = 2;
        int[] palaceTriLines = upperLines; // 默认

        switch (diffs) {
            case 0: palaceTriLines = upperLines; shi = 5; ying = 2; break; // 纯卦 (世在上爻，应在三爻)
            case 1: palaceTriLines = upperLines; shi = 0; ying = 3; break; // 一世
            case 3: palaceTriLines = upperLines; shi = 1; ying = 4; break; // 二世
            case 7: palaceTriLines = upperLines; shi = 2; ying = 5; break; // 三世
            case 6: palaceTriLines = lowerLines; shi = 3; ying = 0; break; // 四世
            case 4: palaceTriLines = lowerLines; shi = 4; ying = 1; break; // 五世
            case 5: palaceTriLines = lowerLines; shi = 3; ying = 0; break; // 游魂
            case 2: palaceTriLines = lowerLines; shi = 2; ying = 5; break; // 归魂
        }

        PalaceInfo info = new PalaceInfo();
        info.palaceTrigram = findTrigram(palaceTriLines);
        info.shi = shi;
        info.ying = ying;
        return info;
    }
    ```

*   **地支五行与六亲推导算法**：
    六亲（父母、子孙、官鬼、妻财、兄弟）根据**卦宫五行**与**爻支五行**的生克关系决定：
    *   **同我者为兄弟**，**生我者为父母**，**我生者为子孙**，**克我者为官鬼**，**我克者为妻财**。
    ```java
    public static int getBranchElement(String branch) {
        switch (branch) {
            case "申": case "酉": return 0; // 金
            case "子": case "亥": return 1; // 水
            case "寅": case "卯": return 2; // 木
            case "巳": case "午": return 3; // 火
            default: return 4;             // 土 (丑辰未戌)
        }
    }

    public static String getLiuQin(int palaceElement, int yaoElement) {
        if (palaceElement == yaoElement) return "兄弟";
        if (yaoElement == (palaceElement + 4) % 5) return "父母"; // 土生金，水生木...
        if (yaoElement == (palaceElement + 1) % 5) return "子孙"; // 金生水，木生火...
        if (yaoElement == (palaceElement + 3) % 5) return "官鬼"; // 火克金，土克水...
        if (yaoElement == (palaceElement + 2) % 5) return "妻财"; // 金克木，水克火...
        return "未知";
    }
    ```

### 14.3 精简农历干支与六神系统

六神（青龙、朱雀、勾陈、腾蛇、白虎、玄武）排盘依赖于**起卦日的日天干**，其顺序从初爻向上一一对应。

*   **日干起六神算法**：
    ```java
    public static String[] getSixGodsList(String dayGan) {
        String[] allGods = {"青龙", "朱雀", "勾陈", "腾蛇", "白虎", "玄武"};
        int startIdx = 0;
        switch (dayGan) {
            case "甲": case "乙": startIdx = 0; break; // 青龙起初爻
            case "丙": case "丁": startIdx = 1; break; // 朱雀起初爻
            case "戊":           startIdx = 2; break; // 勾陈起初爻
            case "己":           startIdx = 3; break; // 腾蛇起初爻
            case "庚": case "辛": startIdx = 4; break; // 白虎起初爻
            case "壬": case "癸": startIdx = 5; break; // 玄武起初爻
        }
        String[] list = new String[6];
        for (int i = 0; i < 6; i++) {
            list[i] = allGods[(startIdx + i) % 6];
        }
        return list; // 返回 [初爻六神..上爻六神]
    }
    ```

*   **精简干支计算逻辑**：
    以公历 `2000年1月1日`（甲戌日，干支索引 54）为基准天数差推算每日天干地支，并在应用中以系统当前时间动态得出日干支与时干支，以供起盘。
    ```java
    // 详细的天数差与时辰地支分配参照 C 实现：
    // 时辰地支：((hour + 1) % 24) / 2
    // 时天干（五鼠遁日）：( (dayGanIdx % 5) * 2 + hourZhiIdx ) % 10
    ```

### 14.4 极坐标对称双半圆 UI 布局与手势设计

六爻的界面设计必须严格适应圆形手表屏幕的安全区，通过精美的 Canvas 极坐标对称环形结构展示本卦与变卦。

*   **掷爻页 (Casting UI)**：
    *   **外圈进度环**：使用 Canvas 绘制 6 等分圆弧进度条（半径 `165dp`，厚度 `6dp`），每等分弧长 `54°`，留有 `6°` 的间隔。当前已掷出的爻数点亮金色（`#C9A96E`），未掷出的显示暗灰（`#666680`）。
    *   **内圈硬币翻滚环**：半径 `155dp` 处绘制 3 等分（各占约 `116°`）的细弧（宽度 `3dp`）。旋转表冠时，此 3 段细弧在阳（`#F5E6CA`）与阴（`#8B7355`）色间快速闪烁，模拟硬币抛掷。停止旋转 300ms 后，根据 SFC32 生成的值锁定各自的阴阳面。

*   **结果页 (Result UI)**：
    *   **极坐标对称半圆弧**：
      - **左半圆 (本卦)**：半径 `165dp`。6 个爻自底向上排列，分别位于 $100^\circ + i \times 28^\circ$。
        - 阳爻：在该区间绘制一个单段圆弧。
        - 阴爻：在该区间内，从中间断开，分成两小段圆弧绘制。
        - 少阳/少阴/老阳/老阴分别严格根据其原值渲染对应的配色（少阳：淡金；少阴：深褐；老阳：亮金变爻；老阴：中国红变爻）。
      - **右半圆 (之卦/变卦)**：半径 `165dp`。对称镜像排列，对应 $360^\circ - (100^\circ + i \times 28^\circ)$ 区域。之卦中的爻根据变爻状态（老阳变阴，老阴变阳，其余不变）绘制，并使用半透明暗色。
    *   **世应标记弧 (Shi/Ying Marker)**：
      - 在对应世爻和应爻的位置，在半径为 `155dp`（紧贴本卦和之卦外圈）绘制一条宽度为 `3dp` 的细指示弧线，世爻显示红色（`#C84B31`），应爻显示金色（`#C9A96E`）。
    *   **手势控制与面板模式**：
      - **左右滑动**：切换屏幕中央内容的显示模式（“六爻详情列表”与“农历干支/卦辞面板”）。
      - **上下滑动 / 旋转表冠**：切换选中侧（本卦与变卦切换，未选中侧透明度降为 30%）。
      - **双击屏幕**：返回起卦欢迎页，重新开始。

# Wear OS 圆形表盘列表与交互开发规范

本规范详述了圆形表盘（Round Watch）智能手表应用中核心交互组件的设计与实现细节。包含 **UI 规范、按压动画、表冠旋转响应、防抖算法与马达震动反馈**。开发者可根据此规范，在任何 Android Wear OS 项目中独立实现相同品质的交互效果。

---

## 一、 UI 布局规范 (UI Layout Specification)

对于圆形表盘，列表单项（Item）通常采用“胶囊卡片（Capsule Card）”的圆角矩形设计，并伴随特定的内外边距，以保证在圆角弧度较大的边缘不会被裁剪。

### 1. 胶囊卡片背景 (`hey_base_item_bg.xml`)
卡片由 LayerDrawable 构成，包含外边距与圆角背景：
* **外平移边距**：
  * 水平外边距（左右）：`12.0dp` (防止最顶部/最底部项贴边被切圆角)
  * 垂直外边距（上下）：`2.0dp`
* **背景圆角半径**：`30.0dp` (完全胶囊状)
* **背景填充色**：`#202124` (深灰色透明质感)

```xml
<!-- res/drawable/item_capsule_bg.xml -->
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item 
        android:left="12.0dp" 
        android:top="2.0dp" 
        android:right="12.0dp" 
        android:bottom="2.0dp">
        <shape android:shape="rectangle">
            <corners android:radius="30.0dp" />
            <solid android:color="#202124" />
        </shape>
    </item>
</layer-list>
```

### 2. 列表单项布局 (无箭头仅左侧图标)
用于如“自由训练”、“登山”等运动列表项，高度建议由内边距决定。
* **卡片内边距（Padding）**：
  * 左内边距：`28.0dp`
  * 右内边距：`28.0dp`
  * 上下内边距：`20.0dp` (保证手指易于点击的触控区域)
* **左侧圆形图标**：`30.0dp * 30.0dp`，右边距 `10.0dp`。
* **右侧文本**：字体大小 `16.0sp`，垂直外边距 `20.0dp`，单行并使用 `ellipsize="end"`。

```xml
<!-- res/layout/item_only_left_image.xml -->
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:gravity="center_vertical"
    android:clickable="true"
    android:focusable="true"
    android:background="@drawable/item_capsule_bg"
    android:paddingLeft="28.0dp"
    android:paddingTop="20.0dp"
    android:paddingRight="28.0dp"
    android:paddingBottom="20.0dp">

    <!-- 圆角防锯齿ImageView (通常可使用自定义ImageView或加上ClipToOutline) -->
    <ImageView
        android:id="@+id/iv_left_icon"
        android:layout_width="30.0dp"
        android:layout_height="30.0dp"
        android:layout_marginRight="10.0dp"
        android:scaleType="centerCrop" />

    <TextView
        android:id="@+id/tv_content"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1.0"
        android:ellipsize="end"
        android:singleLine="true"
        android:textColor="#FFFFFF"
        android:textSize="16.0sp" />
</LinearLayout>
```

---

## 二、 按压微动画 (Press Micro-Animation)

为了提供细腻的触觉回馈，列表项被手指按下和松开时需伴随**缩放与淡入淡出**的联合动画。

### 1. 动画参数 specification
* **按下态 (Down)**：
  * 时长：`200ms`
  * 缩放 (`ScaleX` & `ScaleY`)：自 `1.0f` 缩放到 `0.94f`
  * 透明度 (`Alpha`)：自 `1.0f` 渐变到 `0.8f`
* **恢复态 (Up / Cancel)**：
  * 时长：`250ms`
  * 缩放：从当前缩放比恢复到 `1.0f`
  * 透明度：从当前透明度恢复到 `1.0f`

### 2. 代码实现 (Kotlin)
可在自定义 View 的 `onTouchEvent` 中或通过 `OnTouchListener` 实现：

```kotlin
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.MotionEvent
import android.view.View

fun bindPressEffect(view: View) {
    val animatorSet = AnimatorSet()
    
    view.setOnTouchListener { v, event ->
        if (!v.isClickable || !v.isEnabled) return@setOnTouchListener false
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (animatorSet.isRunning) animatorSet.cancel()
                val scaleX = ObjectAnimator.ofFloat(v, "scaleX", 1.0f, 0.94f)
                val scaleY = ObjectAnimator.ofFloat(v, "scaleY", 1.0f, 0.94f)
                val alpha = ObjectAnimator.ofFloat(v, "alpha", 1.0f, 0.8f)
                
                animatorSet.playTogether(scaleX, scaleY, alpha)
                animatorSet.duration = 200
                animatorSet.start()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (animatorSet.isRunning) animatorSet.cancel()
                val scaleX = ObjectAnimator.ofFloat(v, "scaleX", v.scaleX, 1.0f)
                val scaleY = ObjectAnimator.ofFloat(v, "scaleY", v.scaleY, 1.0f)
                val alpha = ObjectAnimator.ofFloat(v, "alpha", v.alpha, 1.0f)
                
                animatorSet.playTogether(scaleX, scaleY, alpha)
                animatorSet.duration = 250
                animatorSet.start()
            }
        }
        false // 必须返回 false，确保 onClick 事件可以正常触发
    }
}
```

---

## 三、 表冠旋转与弯曲滑动 (Rotary Input & Curved Scroll)

在圆形屏幕上，表冠是主要的导航输入设备。滑动列表时，需要将表冠的硬件旋转信号映射为列表的位移，并渲染具有圆弧轨迹的动态效果。

### 1. 表冠旋转事件捕获 (Rotary Input)
系统会将表冠的机械转动上报为 `MotionEvent.ACTION_SCROLL`。

```kotlin
override fun onGenericMotionEvent(event: MotionEvent): Boolean {
    if (event.action == MotionEvent.ACTION_SCROLL) {
        // AXIS_SCROLL 表示表冠在垂直方向上的滚动量（正值为往上，负值为往下）
        val scrollFactor = event.getAxisValue(MotionEvent.AXIS_SCROLL)
        if (scrollFactor != 0f) {
            // 根据阻尼系数将转动转换为列表的滚动距离
            val scrollPixels = (-scrollFactor * mScrollSpeedFactor).toInt()
            scrollBy(0, scrollPixels)
            return true
        }
    }
    return super.onGenericMotionEvent(event)
}
```

> [!NOTE]
> 如果直接使用 Android 官方提供的 `WearableRecyclerView`，只需调用 `requestFocus()` 聚焦，其底层已原生支持上述表冠事件流的接收与惯性滚动。

### 2. 表盘弯曲列表动画 (Curved Transition)
在 `RecyclerView` 滚动时，通过自定义 `LayoutManager` 的回调算法动态操纵每个 Item 的 Scale（缩放）和 TranslationX（平移），营造出绕表盘边缘弯曲划过的 3D 弧线感。

```kotlin
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.wear.widget.WearableLinearLayoutManager

class CurvedLayoutCallback : WearableLinearLayoutManager.LayoutCallback() {

    // 最大缩放比例偏离（中心最大，边缘最小缩放 0.8f）
    private val maxScaleDown = 0.2f
    // 最大X轴平移量（弯曲弧度深度，一般为 15dp - 30dp）
    private val maxTranslationX = 24f 

    override fun onLayoutChildren(child: View, parent: RecyclerView) {
        val parentHeight = parent.height
        // 1. 计算 Item 几何中心在列表高度中的比例位置 (0.0f 到 1.0f)
        val childCenterY = (child.top + child.bottom) / 2f
        val fraction = childCenterY / parentHeight

        // 2. 计算偏离屏幕中心 (0.5f) 的绝对距离
        val offset = Math.abs(0.5f - fraction) // 范围在 0.0f 到 0.5f

        // 3. 动态缩放计算
        // 当 offset 为 0.0f (居中) 时，scale = 1.0f
        // 当 offset 为 0.5f (边缘) 时，scale = 0.9f 左右
        val scale = 1f - offset * maxScaleDown
        child.scaleX = scale
        child.scaleY = scale

        // 4. 动态 X 轴位移计算 (正弦弯曲圆弧效果)
        // 使用正弦函数在 0.5 偏离处达到最大横向位移，向屏幕内部弯曲
        val translationX = -Math.sin(offset * Math.PI) * maxTranslationX
        child.translationX = translationX.toFloat()
    }
}
```

---

## 四、 表冠防抖与触觉马达震动 (Debounce & Haptics)

转动表冠时需要产生像机械表一样的段落“刻度感”触感（Haptic Tick），但由于传感器极高频的上报，必须经过**高频限流防抖**，并在独立的异步线程完成马达的触发。

### 1. 马达异步线程设计 (HandlerThread)
为防止马达震动调用阻塞主 UI 线程造成滚动掉帧，使用单独的 `HandlerThread` 处理震动队列：

```kotlin
class RotaryHapticHelper(context: Context) {
    private var vibrator: Vibrator? = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private var hapticThread: HandlerThread = HandlerThread("rotary_haptic_thread").apply { start() }
    private var hapticHandler: Handler = Handler(hapticThread.looper)

    // 执行一次轻度震动
    fun triggerTick() {
        hapticHandler.post {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 采用最轻快的 Click 效果
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(10) // 10ms 极短微颤 fallback
            }
        }
    }

    fun release() {
        hapticThread.quitSafely()
    }
}
```

### 2. 旋转事件限流防抖与“刻度触感”算法 (Debouncing & Tick generation)
表冠旋转时，上报的 `scrollFactor` 通常是连续的浮点微小数值。我们采用**累加物理距离**和**时间戳截断**的双重防抖法，来产生物理齿轮般的阻尼触觉。

```kotlin
class RotaryDebouncer(private val hapticHelper: RotaryHapticHelper) {
    private var accumulatedScroll = 0f
    private var lastHapticTime = 0L

    // 每次表冠旋转一个刻度段的阈值像素（可根据屏幕分辨率微调，一般为 12 - 20px）
    private val TICK_THRESHOLD_PX = 15f
    // 两次震动之间的最小安全冷却间隔，防止马达过载成为长震
    private val MIN_HAPTIC_INTERVAL_MS = 60L

    fun onCrownScrolled(dy: Float) {
        // 1. 累加本次表冠位移
        accumulatedScroll += Math.abs(dy)

        // 2. 位移达到一个“刻度”且满足时间冷却
        if (accumulatedScroll >= TICK_THRESHOLD_PX) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastHapticTime >= MIN_HAPTIC_INTERVAL_MS) {
                // 3. 触发物理马达震动
                hapticHelper.triggerTick()
                lastHapticTime = currentTime
            }
            // 4. 清空已消费的位移，重置计数器
            accumulatedScroll = 0f
        }
    }
}
```

在自定义列表或 ScrollView 的滚动监听中，只需在滑动的 `onScroll` 回调里调用 `rotaryDebouncer.onCrownScrolled(dy)` 即可。
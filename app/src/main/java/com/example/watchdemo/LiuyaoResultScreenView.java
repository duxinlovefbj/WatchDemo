package com.example.watchdemo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.View;
import android.view.ViewGroup;

public class LiuyaoResultScreenView extends View {
    private final MainActivity activity;

    // 卦象盘动画相关变量
    private float outerRotation = 0f;
    private float innerRotation = 0f;
    private float targetOuterRotation = 0f;
    private float targetInnerRotation = 0f;
    private boolean initializedPage1 = false;

    public LiuyaoResultScreenView(MainActivity activity) {
        super(activity);
        this.activity = activity;
        setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    public void triggerRotationAnimation() {
        calculateTargetAngles();
        invalidate();
    }

    private float shortestAngleDiff(float from, float to) {
        float diff = (to - from) % 360f;
        if (diff > 180f) {
            diff -= 360f;
        } else if (diff < -180f) {
            diff += 360f;
        }
        return diff;
    }

    private float getTrigramStaticAngle(int index) {
        switch (index) {
            case 7: return 0f;    // 坤
            case 3: return 45f;   // 震
            case 5: return 90f;   // 坎
            case 1: return 135f;  // 兑
            case 0: return 180f;  // 乾
            case 4: return 225f;  // 巽
            case 2: return 270f;  // 离
            case 6: return 315f;  // 艮
            default: return 0f;
        }
    }

    private void calculateTargetAngles() {
        LiuyaoCalculator.Result res = LiuyaoCalculator.calculate(activity.liuyaoLineResults);
        int[] lines = activity.liuyaoResultShowChanged ? res.zhiGua : res.benGua;
        
        int[] lower = { lines[0], lines[1], lines[2] };
        int[] upper = { lines[3], lines[4], lines[5] };
        
        int lowerTriIdx = -1;
        int upperTriIdx = -1;
        for (int i = 0; i < LiuyaoCalculator.TRIGRAMS.length; i++) {
            int[] t = LiuyaoCalculator.TRIGRAMS[i].trigram;
            if (t[0] == lower[0] && t[1] == lower[1] && t[2] == lower[2]) {
                lowerTriIdx = i;
            }
            if (t[0] == upper[0] && t[1] == upper[1] && t[2] == upper[2]) {
                upperTriIdx = i;
            }
        }
        
        if (upperTriIdx != -1) {
            targetOuterRotation = - getTrigramStaticAngle(upperTriIdx);
        }
        if (lowerTriIdx != -1) {
            targetInnerRotation = - getTrigramStaticAngle(lowerTriIdx);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        // 1. 全局背景：纯黑
        canvas.drawColor(Color.BLACK);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // 2. 根据子页面状态渲染不同内容
        if (activity.liuyaoResultSubPage == 0) {
            initializedPage1 = false; // 退出页面时重置动画标记，下次切回可再次出发转盘动效
            drawMainResultPage(canvas, w, h, paint);
        } else if (activity.liuyaoResultSubPage == 1) {
            drawDiscPlaceholderPage(canvas, w, h, paint);
        } else if (activity.liuyaoResultSubPage == 2) {
            initializedPage1 = false; // 退出页面时重置动画标记
            drawYaoPlaceholderPage(canvas, w, h, paint);
        }
    }

    /**
     * 绘制 Page 0：本卦 / 变卦之卦主页面
     */
    private void drawMainResultPage(Canvas canvas, int w, int h, Paint paint) {
        float cx = w / 2f;
        float density = activity.density;

        // 计算起卦结果
        LiuyaoCalculator.Result res = LiuyaoCalculator.calculate(activity.liuyaoLineResults);
        if (res.benGuaIdx < 0) return;

        // 判断当前显示本卦还是变卦
        boolean showChanged = activity.liuyaoResultShowChanged;
        int activeGuaIdx = showChanged ? res.zhiGuaIdx : res.benGuaIdx;
        if (activeGuaIdx < 0 || activeGuaIdx >= LiuyaoCalculator.GUA_DATA.length) return;

        LiuyaoCalculator.GuaEntry entry = LiuyaoCalculator.GUA_DATA[activeGuaIdx];
        String titleText = entry.name + (showChanged ? " (变卦)" : " (本卦)");
        String briefText = entry.brief;

        // 1. 绘制 6 爻卦象 (稍靠屏幕上方)
        float cy = h * 0.38f;
        float lineW = 60 * density;
        float thickness = 7 * density;
        float gap = 8 * density;
        float yinGap = 14 * density;
        float totalH = 5 * (thickness + gap) + thickness;

        for (int i = 0; i < 6; i++) {
            float lineY = cy + (totalH / 2f) - i * (thickness + gap) - (thickness / 2f);
            int val = activity.liuyaoLineResults[i];

            boolean isYang;
            int color;

            if (!showChanged) {
                // 显示本卦
                isYang = (val == 7 || val == 9);
                // 颜色：少阳(7)淡金，少阴(8)深褐，老阳(9)亮金变爻，老阴(6)红色变爻
                if (val == 7) {
                    color = Color.parseColor("#F5E6CA");
                } else if (val == 8) {
                    color = Color.parseColor("#8B7355");
                } else if (val == 9) {
                    color = Color.parseColor("#C9A96E");
                } else {
                    color = Color.parseColor("#C84B31");
                }
            } else {
                // 显示变卦 (变爻已转换，均为静爻)
                if (val == 6 || val == 9) {
                    // 6老阴变少阳(阳)，9老阳变少阴(阴)
                    isYang = (val == 6);
                } else {
                    isYang = (val == 7);
                }
                color = Color.parseColor(isYang ? "#F5E6CA" : "#8B7355");
            }

            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL);

            if (isYang) {
                // 阳爻：一条整圆角矩形/矩形线
                canvas.drawRect(cx - lineW / 2f, lineY - thickness / 2f, cx + lineW / 2f, lineY + thickness / 2f, paint);
            } else {
                // 阴爻：中间断开的两条线
                canvas.drawRect(cx - lineW / 2f, lineY - thickness / 2f, cx - yinGap / 2f, lineY + thickness / 2f, paint);
                canvas.drawRect(cx + yinGap / 2f, lineY - thickness / 2f, cx + lineW / 2f, lineY + thickness / 2f, paint);
            }
        }

        // 2. 绘制卦名
        paint.setTextSize(16 * density);
        paint.setColor(Color.parseColor("#C9A96E"));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setStyle(Paint.Style.FILL);
        paint.setFakeBoldText(true);
        canvas.drawText(titleText, cx, h * 0.68f, paint);

        // 3. 绘制卦辞 (使用 StaticLayout 自动换行)
        TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(12 * density);
        textPaint.setColor(Color.parseColor("#CCCCCC"));
        textPaint.setTextAlign(Paint.Align.LEFT);

        int layoutWidth = (int) (w * 0.66f);
        StaticLayout staticLayout;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            staticLayout = StaticLayout.Builder.obtain(briefText, 0, briefText.length(), textPaint, layoutWidth)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setLineSpacing(2f, 1.1f)
                    .build();
        } else {
            staticLayout = new StaticLayout(briefText, textPaint, layoutWidth,
                    Layout.Alignment.ALIGN_CENTER, 1.1f, 2f, false);
        }

        canvas.save();
        canvas.translate(cx - layoutWidth / 2f, h * 0.71f);
        staticLayout.draw(canvas);
        canvas.restore();

        // 4. 绘制上下翻页指示标
        paint.setFakeBoldText(false);
        paint.setTextSize(10 * density);
        paint.setColor(Color.parseColor("#555555"));
        canvas.drawText("▲ 卦象盘", cx, 32 * density, paint);
        canvas.drawText("▼ 爻辞详情", cx, h - 20 * density, paint);

        // 5. 绘制贴合边缘的弧线文字
        drawCurvedTimeAndShichen(canvas, w, h, paint);
    }

    /**
     * 绘制 Page 1：同心双层罗盘卦象盘页面
     */
    private void drawDiscPlaceholderPage(Canvas canvas, int w, int h, Paint paint) {
        float cx = w / 2f;
        float cy = h / 2f;
        float R_max = w / 2f;
        float density = activity.density;

        if (!initializedPage1) {
            calculateTargetAngles();
            outerRotation = targetOuterRotation - 180f;
            innerRotation = targetInnerRotation + 180f;
            initializedPage1 = true;
        }

        // 1. 物理旋转插值更新 (极简惯性动画，60帧丝滑)
        float outerDiff = shortestAngleDiff(outerRotation, targetOuterRotation);
        float innerDiff = shortestAngleDiff(innerRotation, targetInnerRotation);
        boolean animating = false;
        if (Math.abs(outerDiff) > 0.1f) {
            outerRotation += outerDiff * 0.12f;
            animating = true;
        } else {
            outerRotation = targetOuterRotation;
        }
        if (Math.abs(innerDiff) > 0.1f) {
            innerRotation += innerDiff * 0.12f;
            animating = true;
        } else {
            innerRotation = targetInnerRotation;
        }
        if (animating) {
            postInvalidateOnAnimation();
        }

        // 2. 绘制圆盘分割线与边界圆 (细致半透明金边，圆环更压缩)
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f * density);
        paint.setColor(Color.parseColor("#15FFFFFF"));
        canvas.drawCircle(cx, cy, R_max * 0.89f, paint);
        canvas.drawCircle(cx, cy, R_max * 0.72f, paint);
        canvas.drawCircle(cx, cy, R_max * 0.55f, paint);

        // 3. 计算当前的起卦排盘信息
        LiuyaoCalculator.Result res = LiuyaoCalculator.calculate(activity.liuyaoLineResults);
        LiuyaoCalculator.LunarInfo lunar = LiuyaoCalculator.getLunarInfo(activity.appStartTime);
        LiuyaoCalculator.HexagramDetails benDetails = LiuyaoCalculator.calculateDetails(activity.liuyaoLineResults, -1, lunar.dayGan);
        LiuyaoCalculator.HexagramDetails activeDetails = activity.liuyaoResultShowChanged ?
                LiuyaoCalculator.calculateDetails(res.zhiGua, benDetails.palaceElement, lunar.dayGan) : benDetails;

        // 获取当前的上卦 and 下卦索引，用于高亮
        int[] activeLines = activity.liuyaoResultShowChanged ? res.zhiGua : res.benGua;
        int activeLowerIdx = -1;
        int activeUpperIdx = -1;
        for (int i = 0; i < LiuyaoCalculator.TRIGRAMS.length; i++) {
            int[] t = LiuyaoCalculator.TRIGRAMS[i].trigram;
            if (t[0] == activeLines[0] && t[1] == activeLines[1] && t[2] == activeLines[2]) activeLowerIdx = i;
            if (t[0] == activeLines[3] && t[1] == activeLines[4] && t[2] == activeLines[5]) activeUpperIdx = i;
        }

        // 4. 循环绘制内外圆盘上的八卦 (更紧凑，字号更小)
        for (int i = 0; i < 8; i++) {
            LiuyaoCalculator.TrigramInfo ti = LiuyaoCalculator.TRIGRAMS[i];
            float angle = getTrigramStaticAngle(i);

            // --- 绘制外盘 (上卦) ---
            canvas.save();
            canvas.rotate(angle + outerRotation, cx, cy);
            boolean isOuterActive = (i == activeUpperIdx);
            
            // 绘制卦名 (放置在 0.735R 处，缩短与弧线间距，且不挨着 0.72R 内边界线)
            paint.setStyle(Paint.Style.FILL);
            paint.setFakeBoldText(isOuterActive);
            paint.setTextSize((isOuterActive ? 7.5f : 5.5f) * density);
            paint.setColor(Color.parseColor(isOuterActive ? "#C9A96E" : "#44FFFFFF"));
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(ti.name, cx, cy - R_max * 0.735f, paint);

            // 绘制该扇区的分界线 (在扇区右侧 22.5 度处，使各卦象之间有竖线分隔)
            canvas.save();
            canvas.rotate(22.5f, cx, cy);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1f * density);
            paint.setColor(Color.parseColor("#15FFFFFF"));
            canvas.drawLine(cx, cy - R_max * 0.89f, cx, cy - R_max * 0.72f, paint);
            canvas.restore();

            // 绘制卦象的3条弧线 (外侧，压缩间距与粗细，且未激活状态提高亮度)
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth((isOuterActive ? 2.0f : 1.0f) * density);
            paint.setColor(Color.parseColor(isOuterActive ? "#F5E6CA" : "#33FFFFFF"));
            float sweepAngle = 20f;
            float startAngle = -90f - sweepAngle / 2f;
            for (int line = 0; line < 3; line++) {
                float r = R_max * (0.81f + line * 0.025f);
                RectF rect = new RectF(cx - r, cy - r, cx + r, cy + r);
                boolean isYang = (ti.trigram[line] == 1);
                if (isYang) {
                    canvas.drawArc(rect, startAngle, sweepAngle, false, paint);
                } else {
                    float partSweep = (sweepAngle - 4f) / 2f;
                    canvas.drawArc(rect, startAngle, partSweep, false, paint);
                    canvas.drawArc(rect, startAngle + partSweep + 4f, partSweep, false, paint);
                }
            }
            canvas.restore();

            // --- 绘制内盘 (下卦) ---
            canvas.save();
            canvas.rotate(angle + innerRotation, cx, cy);
            boolean isInnerActive = (i == activeLowerIdx);

            // 绘制卦名 (放置在 0.57R 处，缩短与弧线间距，且不挨着 0.55R 内边界线)
            paint.setStyle(Paint.Style.FILL);
            paint.setFakeBoldText(isInnerActive);
            paint.setTextSize((isInnerActive ? 7.0f : 5.0f) * density);
            paint.setColor(Color.parseColor(isInnerActive ? "#C9A96E" : "#44FFFFFF"));
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(ti.name, cx, cy - R_max * 0.57f, paint);

            // 绘制该扇区的分界线 (在扇区右侧 22.5 度处，使各卦象之间有竖线分隔)
            canvas.save();
            canvas.rotate(22.5f, cx, cy);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1f * density);
            paint.setColor(Color.parseColor("#15FFFFFF"));
            canvas.drawLine(cx, cy - R_max * 0.72f, cx, cy - R_max * 0.55f, paint);
            canvas.restore();

            // 绘制卦象的3条弧线 (外侧，压缩间距与粗细，且未激活状态提高亮度)
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth((isInnerActive ? 1.8f : 0.9f) * density);
            paint.setColor(Color.parseColor(isInnerActive ? "#F5E6CA" : "#33FFFFFF"));
            for (int line = 0; line < 3; line++) {
                float r = R_max * (0.64f + line * 0.025f);
                RectF rect = new RectF(cx - r, cy - r, cx + r, cy + r);
                boolean isYang = (ti.trigram[line] == 1);
                if (isYang) {
                    canvas.drawArc(rect, startAngle, sweepAngle, false, paint);
                } else {
                    float partSweep = (sweepAngle - 4f) / 2f;
                    canvas.drawArc(rect, startAngle, partSweep, false, paint);
                    canvas.drawArc(rect, startAngle + partSweep + 4f, partSweep, false, paint);
                }
            }
            canvas.restore();
        }

        // 5. 绘制中心用神排盘文本 (自上而下对齐，字号适中，紧凑排布)
        paint.setStyle(Paint.Style.FILL);
        paint.setFakeBoldText(false);
        paint.setTextSize(10 * density);
        
        float startY = cy - 32.5f * density;
        float lineGap = 13f * density;

        for (int r = 0; r < 6; r++) {
            int idx = 5 - r; // 从上往下显示 (上爻到初爻)
            LiuyaoCalculator.YaoDetail yd = activeDetails.yao[idx];
            float yLine = startY + r * lineGap;

            // A. 绘制六神 (左侧对齐)
            paint.setColor(Color.parseColor("#888888"));
            paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(yd.god, cx - 42 * density, yLine + 3.5f * density, paint);

            // B. 绘制六亲干支五行 (中部对齐)
            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.LEFT);
            String yaoText = yd.liuqin + yd.stem + yd.branch + yd.element;
            canvas.drawText(yaoText, cx - 13 * density, yLine + 3.5f * density, paint);

            // C. 绘制世应标记侧边竖线 (不带文字说明以保持界面简洁)
            if (yd.isShi || yd.isYing) {
                paint.setStyle(Paint.Style.FILL);
                if (yd.isShi) {
                    paint.setColor(Color.parseColor("#C9A96E")); // 金色世爻
                } else {
                    paint.setColor(Color.parseColor("#C0C0C0")); // 银色应爻
                }
                // 绘制金银竖线，位置稍微向左靠近文字一些
                canvas.drawRect(cx + 38 * density, yLine - 4 * density, cx + 40 * density, yLine + 6 * density, paint);
            }
        }
    }


    /**
     * 绘制 Page 2：爻辞详情页面 (表冠旋转切换，被选中爻线有白色横线贯穿)
     */
    private void drawYaoPlaceholderPage(Canvas canvas, int w, int h, Paint paint) {
        float cx = w / 2f;
        float cy = h * 0.38f;
        float density = activity.density;

        // 1. 获取当前显示卦象的爻线数据 (本卦或变卦)
        LiuyaoCalculator.Result res = LiuyaoCalculator.calculate(activity.liuyaoLineResults);
        if (res.benGuaIdx < 0) return;

        boolean showChanged = activity.liuyaoResultShowChanged;
        int[] lines = showChanged ? res.zhiGua : res.benGua;

        // 2. 绘制 6 爻卦象与选中爻贯穿横线
        float lineW = 60 * density;
        float thickness = 7 * density;
        float gap = 8 * density;
        float yinGap = 14 * density;
        float totalH = 5 * (thickness + gap) + thickness;

        for (int i = 0; i < 6; i++) {
            float lineY = cy + (totalH / 2f) - i * (thickness + gap) - (thickness / 2f);
            
            // 获取当前爻的颜色配置 (与主页一致)
            boolean isYang;
            int color;
            if (!showChanged) {
                int val = activity.liuyaoLineResults[i];
                isYang = (val == 7 || val == 9);
                if (val == 7) {
                    color = Color.parseColor("#F5E6CA");
                } else if (val == 8) {
                    color = Color.parseColor("#8B7355");
                } else if (val == 9) {
                    color = Color.parseColor("#C9A96E");
                } else {
                    color = Color.parseColor("#C84B31");
                }
            } else {
                int val = activity.liuyaoLineResults[i];
                if (val == 6 || val == 9) {
                    isYang = (val == 6);
                } else {
                    isYang = (val == 7);
                }
                color = Color.parseColor(isYang ? "#F5E6CA" : "#8B7355");
            }

            // 绘制当前爻线 (正常色)
            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL);
            if (isYang) {
                canvas.drawRect(cx - lineW / 2f, lineY - thickness / 2f, cx + lineW / 2f, lineY + thickness / 2f, paint);
            } else {
                canvas.drawRect(cx - lineW / 2f, lineY - thickness / 2f, cx - yinGap / 2f, lineY + thickness / 2f, paint);
                canvas.drawRect(cx + yinGap / 2f, lineY - thickness / 2f, cx + lineW / 2f, lineY + thickness / 2f, paint);
            }

            // 如果是当前选中的爻，在爻线右侧绘制红色三角指示
            if (i == activity.selectedHexagramLineIndex) {
                float leftTipX = cx + lineW / 2f + 8 * density;
                float triWidth = 8 * density;
                float triHeight = 8 * density;
                Path path = new Path();
                path.moveTo(leftTipX, lineY);
                path.lineTo(leftTipX + triWidth, lineY - triHeight / 2f);
                path.lineTo(leftTipX + triWidth, lineY + triHeight / 2f);
                path.close();
                paint.setColor(Color.parseColor("#C84B31"));
                paint.setStyle(Paint.Style.FILL);
                canvas.drawPath(path, paint);
            }
        }

        // 3. 绘制爻辞文字说明
        int selIdx = activity.selectedHexagramLineIndex;
        if (selIdx < 0 || selIdx >= 6) selIdx = 0;

        int activeGuaIdx = activity.liuyaoResultShowChanged ? res.zhiGuaIdx : res.benGuaIdx;
        String text = LiuyaoYaoCiData.getYaoText(activeGuaIdx, selIdx);

        String title = "";
        String detail = text;
        int colonIdx = text.indexOf("：");
        if (colonIdx >= 0) {
            title = text.substring(0, colonIdx);
            detail = text.substring(colonIdx + 1);
        }

        // A. 绘制爻位标题 (如：初九)
        paint.setStyle(Paint.Style.FILL);
        paint.setFakeBoldText(true);
        paint.setColor(Color.parseColor("#C9A96E"));
        paint.setTextSize(14 * density);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(title, cx, cy + totalH / 2f + 25 * density, paint);

        // B. 绘制爻辞正文 (如：潜龙勿用。支持 StaticLayout 换行)
        TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(12 * density);
        textPaint.setColor(Color.parseColor("#CCCCCC"));
        textPaint.setTextAlign(Paint.Align.LEFT);

        int layoutWidth = (int) (w * 0.66f);
        StaticLayout staticLayout;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            staticLayout = StaticLayout.Builder.obtain(detail, 0, detail.length(), textPaint, layoutWidth)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setLineSpacing(2f, 1.1f)
                    .build();
        } else {
            staticLayout = new StaticLayout(detail, textPaint, layoutWidth,
                    Layout.Alignment.ALIGN_CENTER, 1.1f, 2f, false);
        }

        canvas.save();
        canvas.translate(cx - layoutWidth / 2f, cy + totalH / 2f + 37 * density);
        staticLayout.draw(canvas);
        canvas.restore();

        // 绘制贴合边缘的弧线文字
        drawCurvedTimeAndShichen(canvas, w, h, paint);
    }

    private void drawCurvedTimeAndShichen(Canvas canvas, int w, int h, Paint paint) {
        float cx = w / 2f;
        float cy = h / 2f;
        float density = activity.density;

        float pathR = w / 2f - 18 * density;
        RectF oval = new RectF(cx - pathR, cy - pathR, cx + pathR, cy + pathR);

        float sweepAngle = 80f;
        float pathLength = (float) (2 * Math.PI * pathR * (sweepAngle / 360f));

        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(10.5f * density);
        paint.setFakeBoldText(false);

        // Traditional colors
        int colorYear = Color.parseColor("#4E8E76");  // Bamboo Green
        int colorMonth = Color.parseColor("#D14949"); // Vermillion Red
        int colorDay = Color.parseColor("#C9A96E");   // Gold
        int colorHour = Color.parseColor("#5C80BC");  // Indigo Blue

        // 1. Left Side: Solar Calendar (Clockwise from 140 to 220)
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(activity.appStartTime);
        String sYear = cal.get(java.util.Calendar.YEAR) + "年";
        String sMonth = String.format(java.util.Locale.getDefault(), "%02d月", cal.get(java.util.Calendar.MONTH) + 1);
        String sDay = String.format(java.util.Locale.getDefault(), "%02d日", cal.get(java.util.Calendar.DAY_OF_MONTH));
        String sHour = String.format(java.util.Locale.getDefault(), "%02d:%02d", cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE));

        Path leftPath = new Path();
        leftPath.addArc(oval, 140f, sweepAngle);
        drawColoredArcText(canvas, leftPath, pathLength, sYear, colorYear, sMonth, colorMonth, sDay, colorDay, sHour, colorHour, paint);

        // 2. Right Side: Lunar Calendar / Shichen (Clockwise from -40 to 40)
        LiuyaoCalculator.LunarInfo lunar = LiuyaoCalculator.getLunarInfo(activity.appStartTime);
        String lYear = lunar.yearGanZhi + "年";
        String lMonth = lunar.monthGanZhi + "月";
        String lDay = lunar.dayGanZhi + "日";
        String lHour = lunar.hourGanZhi + "时";

        Path rightPath = new Path();
        rightPath.addArc(oval, -40f, sweepAngle);
        drawColoredArcText(canvas, rightPath, pathLength, lYear, colorYear, lMonth, colorMonth, lDay, colorDay, lHour, colorHour, paint);
    }

    private void drawColoredArcText(Canvas canvas, Path path, float pathLength, 
                                    String yearStr, int yearColor,
                                    String monthStr, int monthColor,
                                    String dayStr, int dayColor,
                                    String hourStr, int hourColor,
                                    Paint paint) {
        paint.setTextAlign(Paint.Align.LEFT);
        float wYear = paint.measureText(yearStr);
        float wMonth = paint.measureText(monthStr);
        float wDay = paint.measureText(dayStr);
        float wHour = paint.measureText(hourStr);
        float wSpace = paint.measureText(" "); // Single space

        float totalW = wYear + wSpace + wMonth + wSpace + wDay + wSpace + wHour;
        float startOffset = (pathLength - totalW) / 2f;

        // Draw Year
        paint.setColor(yearColor);
        canvas.drawTextOnPath(yearStr, path, startOffset, 0, paint);

        // Draw Month
        paint.setColor(monthColor);
        canvas.drawTextOnPath(monthStr, path, startOffset + wYear + wSpace, 0, paint);

        // Draw Day
        paint.setColor(dayColor);
        canvas.drawTextOnPath(dayStr, path, startOffset + wYear + wSpace + wMonth + wSpace, 0, paint);

        // Draw Hour
        paint.setColor(hourColor);
        canvas.drawTextOnPath(hourStr, path, startOffset + wYear + wSpace + wMonth + wSpace + wDay + wSpace, 0, paint);
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent event) {
        if (activity.gestureDetector != null) {
            activity.gestureDetector.onTouchEvent(this, event);
            return true;
        }
        return super.dispatchTouchEvent(event);
    }
}

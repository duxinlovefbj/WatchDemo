package com.example.watchdemo;

/** Pure geometry for positioning Tarot cards inside the circular watch safe area. */
final class TarotSpreadLayoutCalculator {
    private static final float CARD_WIDTH = 46f;
    private static final float CARD_HEIGHT = 80f;

    private TarotSpreadLayoutCalculator() {}

    static float[] cardBounds(int spread, int index, int drawnCount,
                              int screenWidth, int screenHeight, float density) {
        if (drawnCount <= 0) {
            return new float[]{screenWidth / 2f, screenHeight / 2f,
                    CARD_WIDTH * density, CARD_HEIGHT * density};
        }

        float fit = minimumNonOverlapScale(spread, drawnCount);
        float[] current = rawCoordinates(spread, index, drawnCount);
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        for (int i = 0; i < drawnCount; i++) {
            float[] point = rawCoordinates(spread, i, drawnCount);
            float width = virtualWidth(spread, i) * fit;
            float height = virtualHeight(spread, i) * fit;
            minX = Math.min(minX, point[0] - width / 2f);
            maxX = Math.max(maxX, point[0] + width / 2f);
            minY = Math.min(minY, point[1] - height / 2f);
            maxY = Math.max(maxY, point[1] + height / 2f);
        }

        float centerX = (minX + maxX) / 2f;
        float centerY = (minY + maxY) / 2f;
        float spreadWidth = maxX - minX;
        float spreadHeight = maxY - minY;
        float scale = Math.min(screenWidth * 0.65f / spreadWidth,
                screenHeight * 0.65f / spreadHeight);
        scale = Math.min(scale, 1.15f * density);

        float x = screenWidth / 2f + (current[0] - centerX) * scale;
        float y = screenHeight / 2f + (current[1] - centerY) * scale + 12f * density;
        return new float[]{x, y, CARD_WIDTH * scale * fit, CARD_HEIGHT * scale * fit};
    }

    private static float minimumNonOverlapScale(int spread, int count) {
        float fit = 1f;
        for (int a = 0; a < count; a++) {
            for (int b = a + 1; b < count; b++) {
                if (spread == 4 && a == 0 && b == 1) continue;
                float[] first = rawCoordinates(spread, a, count);
                float[] second = rawCoordinates(spread, b, count);
                float limitX = (virtualWidth(spread, a) + virtualWidth(spread, b)) / 2f;
                float limitY = (virtualHeight(spread, a) + virtualHeight(spread, b)) / 2f;
                float pairFit = Math.max(Math.abs(first[0] - second[0]) / limitX,
                        Math.abs(first[1] - second[1]) / limitY);
                fit = Math.min(fit, pairFit);
            }
        }
        return fit;
    }

    private static float virtualWidth(int spread, int index) {
        return spread == 4 && index == 1 ? CARD_HEIGHT : CARD_WIDTH;
    }

    private static float virtualHeight(int spread, int index) {
        return spread == 4 && index == 1 ? CARD_WIDTH : CARD_HEIGHT;
    }

    private static float[] rawCoordinates(int spread, int index, int count) {
        switch (spread) {
            case 1:
                return point((index - 1) * 52f, 0f);
            case 2:
                if (index == 0) return point(0f, 0f);
                double angle = (index - 1) * Math.PI / 3f - Math.PI / 2f;
                return point((float) Math.cos(angle) * 50f, (float) Math.sin(angle) * 50f);
            case 3:
                return point((index - 2) * 44f, 0f);
            case 4:
                return celticCross(index);
            case 5:
                return fourElements(index);
            case 6:
                return choice(index);
            case 7:
                return pyramid(index);
            case 8:
                return point(0f, (3 - index) * 24f);
            case 9:
                return directQuestion(index);
            case 10:
                return guidingStar(index);
            case 11:
                return finance(index);
            case 12:
                return relationship(index);
            default:
                return grid(index, count);
        }
    }

    private static float[] celticCross(int i) {
        float dx = 48f, dy = 58f, staffX = 104f;
        switch (i) {
            case 0: case 1: return point(0f, 0f);
            case 2: return point(0f, dy);
            case 3: return point(-dx, 0f);
            case 4: return point(0f, -dy);
            case 5: return point(dx, 0f);
            case 6: return point(staffX, 1.5f * dy);
            case 7: return point(staffX, 0.5f * dy);
            case 8: return point(staffX, -0.5f * dy);
            case 9: return point(staffX, -1.5f * dy);
            default: return point(0f, 0f);
        }
    }

    private static float[] fourElements(int i) {
        float dx = 32f, dy = 36f;
        float[][] points = {{-dx, dy}, {dx, -dy}, {dx, dy}, {-dx, -dy}};
        return indexed(points, i);
    }

    private static float[] choice(int i) {
        float dx = 38f, dy = 46f;
        float[][] points = {{0f, dy}, {-dx, 0f}, {dx, 0f}, {-dx, -dy}, {dx, -dy}};
        return indexed(points, i);
    }

    private static float[] pyramid(int i) {
        float dx = 36f, dy = 32f;
        float[][] points = {{-dx, dy}, {0f, dy}, {dx, dy}, {0f, 0f}, {0f, -2f * dy},
                {-1.5f * dx, 0f}, {-0.7f * dx, -dy}, {1.5f * dx, 0f}, {0.7f * dx, -dy}};
        return indexed(points, i);
    }

    private static float[] directQuestion(int i) {
        float dx = 42f, dy = 46f;
        float[][] points = {{0f, -dy}, {-dx, 0f}, {0f, dy}, {dx, 0f}};
        return indexed(points, i);
    }

    private static float[] guidingStar(int i) {
        float dx = 38f, dy = 52f;
        float[][] points = {{0f, 0f}, {-dx, -dy / 2f}, {-dx, dy / 2f},
                {dx, -dy / 2f}, {dx, dy / 2f}, {0f, dy}, {0f, -dy}};
        return indexed(points, i);
    }

    private static float[] finance(int i) {
        float dx = 42f, dy = 46f;
        float[][] points = {{0f, dy}, {0f, 0f}, {dx, 0f}, {-dx, 0f}, {0f, -dy}};
        return indexed(points, i);
    }

    private static float[] relationship(int i) {
        float dx = 42f, dy = 46f;
        float[][] points = {{0f, dy}, {0f, -dy}, {-dx, 0f}, {0f, 0f}, {dx, 0f}, {14f, 12f}};
        return indexed(points, i);
    }

    private static float[] grid(int index, int count) {
        int columns = Math.min(count, 3);
        int rows = (count + 2) / 3;
        float width = columns * CARD_WIDTH + (columns - 1) * 12f;
        float height = rows * CARD_HEIGHT + (rows - 1) * 12f;
        int column = index % 3;
        int row = index / 3;
        return point(column * 58f - width / 2f + CARD_WIDTH / 2f,
                row * 92f - height / 2f + CARD_HEIGHT / 2f);
    }

    private static float[] indexed(float[][] points, int index) {
        return index >= 0 && index < points.length ? points[index] : point(0f, 0f);
    }

    private static float[] point(float x, float y) {
        return new float[]{x, y};
    }
}

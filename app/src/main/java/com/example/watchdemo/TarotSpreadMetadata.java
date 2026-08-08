package com.example.watchdemo;

/** Labels associated with each position in a Tarot spread. */
final class TarotSpreadMetadata {
    private static final String[][] POSITION_LABELS = {
            {},
            {"过去", "现在", "未来"},
            {"核心问题", "过去原因", "当前状态", "未来趋势", "环境影响", "内心期望", "解决建议"},
            {"遥远的过去", "近期过去", "现在", "近期未来", "遥远未来"},
            {"现状", "阻碍", "潜意识", "过去", "意识", "未来", "自我", "环境", "希望或恐惧", "最终结果"},
            {"火：热情行动", "水：直觉情感", "风：理性沟通", "土：实际稳定"},
            {"起点：当前处境", "A优：正面影响", "B优：正面影响", "A劣：负面影响", "B劣：负面影响"},
            {"基础左：起点", "基础中：动力", "基础右：环境", "实现：行动方式", "成就：最终结果",
                    "过程：发展挑战", "上左：关键因素", "过程：发展机会", "上右：整合元素"},
            {"海底轮：安全", "脐轮：创造", "太阳轮：力量", "心轮：同理", "喉轮：表达", "眉心轮：智慧", "顶轮：觉知"},
            {"原因：问题起源", "现状：实际情况", "影响：关键力量", "解决：突破建议"},
            {"核心：灵魂使命", "过去：需要放下", "资源：内在天赋", "未来：需要迎接",
                    "机会：外在助力", "行动：落方向", "指引：高我讯息"},
            {"财务：金钱现况", "收入：流入管道", "支出：金钱流出", "障碍：风险注意", "建议：财务健康"},
            {"自我：真实感受", "对方：想法感受", "优势：正面特质", "挑战：面对问题", "氛围：互动模式", "建议：关系改善"}
    };

    private TarotSpreadMetadata() {}

    static String positionLabel(int spreadIndex, int cardIndex) {
        if (spreadIndex >= 0 && spreadIndex < POSITION_LABELS.length) {
            String[] labels = POSITION_LABELS[spreadIndex];
            if (cardIndex >= 0 && cardIndex < labels.length) return labels[cardIndex];
        }
        return "第 " + (cardIndex + 1) + " 张牌";
    }
}

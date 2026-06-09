package com.example.watchdemo;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class LiuyaoCalculator {

    // ========== 64卦数据类 ==========
    public static class GuaEntry {
        public final String name;
        public final int[] yinYang; // 6 elements, index 0 (初爻) to index 5 (上爻), 1 = Yang, 0 = Yin
        public final String brief;

        public GuaEntry(String name, int[] yinYang, String brief) {
            this.name = name;
            this.yinYang = yinYang;
            this.brief = brief;
        }
    }

    public static final GuaEntry[] GUA_DATA = new GuaEntry[] {
        new GuaEntry("乾为天", new int[]{1,1,1,1,1,1}, "乾。元亨、利贞"),
        new GuaEntry("坤为地", new int[]{0,0,0,0,0,0}, "坤。元亨，利牝马之贞"),
        new GuaEntry("水雷屯", new int[]{1,0,0,0,1,0}, "屯。元亨，利贞，勿用有攸往，利建侯"),
        new GuaEntry("山水蒙", new int[]{0,1,0,0,0,1}, "蒙。亨，匪我求童蒙，童蒙求我"),
        new GuaEntry("水天需", new int[]{1,1,1,0,1,0}, "需。有孚，光亨，贞吉，利涉大川"),
        new GuaEntry("天水讼", new int[]{0,1,0,1,1,1}, "讼。有孚，窒惕，中吉，终凶"),
        new GuaEntry("地水师", new int[]{0,1,0,0,0,0}, "师。贞，丈人吉，无咎"),
        new GuaEntry("水地比", new int[]{0,0,0,0,1,0}, "比。吉，原筮，元永贞，无咎"),
        new GuaEntry("风天小畜", new int[]{1,1,1,0,1,1}, "小畜。亨，密云不雨，自我西郊"),
        new GuaEntry("天泽履", new int[]{1,1,0,1,1,1}, "履虎尾，不咥人，亨"),
        new GuaEntry("地天泰", new int[]{1,1,1,0,0,0}, "泰。小往大来，吉，亨"),
        new GuaEntry("天地否", new int[]{0,0,0,1,1,1}, "否之匪人，不利君子贞"),
        new GuaEntry("天火同人", new int[]{1,0,1,1,1,1}, "同人于野，亨，利涉大川"),
        new GuaEntry("火天大有", new int[]{1,1,1,1,0,1}, "大有。元亨"),
        new GuaEntry("地山谦", new int[]{0,0,1,0,0,0}, "谦。亨，君子有终"),
        new GuaEntry("雷地豫", new int[]{0,0,0,1,0,0}, "豫。利建侯，行师"),
        new GuaEntry("泽雷随", new int[]{1,0,0,1,1,0}, "随。元亨，利贞，无咎"),
        new GuaEntry("山风蛊", new int[]{0,1,1,0,0,1}, "蛊。元亨，利涉大川"),
        new GuaEntry("地泽临", new int[]{1,1,0,0,0,0}, "临。元亨，利贞"),
        new GuaEntry("风地观", new int[]{0,0,0,0,1,1}, "观。盥而不荐，有孚颙若"),
        new GuaEntry("火雷噬嗑", new int[]{1,0,0,1,0,1}, "噬嗑。亨，利用狱"),
        new GuaEntry("山火贲", new int[]{1,0,1,0,0,1}, "贲。亨，小利有攸往"),
        new GuaEntry("山地剥", new int[]{0,0,0,0,0,1}, "剥。不利有攸往"),
        new GuaEntry("地雷复", new int[]{1,0,0,0,0,0}, "复。亨，出入无疾，朋来无咎"),
        new GuaEntry("天雷无妄", new int[]{1,0,0,1,1,1}, "无妄.元亨，利贞"),
        new GuaEntry("山天大畜", new int[]{1,1,1,0,0,1}, "大畜。利贞，不家食吉"),
        new GuaEntry("山雷颐", new int[]{1,0,0,0,0,1}, "颐。贞吉，观颐，自求口实"),
        new GuaEntry("泽风大过", new int[]{0,1,1,1,1,0}, "大过。栋桡，利有攸往，亨"),
        new GuaEntry("坎为水", new int[]{0,1,0,0,1,0}, "习坎。有孚，维心亨，行有尚"),
        new GuaEntry("离为火", new int[]{1,0,1,1,0,1}, "离。利贞，亨，畜牝牛吉"),
        new GuaEntry("泽山咸", new int[]{0,0,1,1,1,0}, "咸。亨，利贞，取女吉"),
        new GuaEntry("雷风恒", new int[]{0,1,1,1,0,0}, "恒。亨，无咎，利贞"),
        new GuaEntry("天山遁", new int[]{0,0,1,1,1,1}, "遁。亨，小利贞"),
        new GuaEntry("雷天大壮", new int[]{1,1,1,1,0,0}, "大壮。利贞"),
        new GuaEntry("火地晋", new int[]{0,0,0,1,0,1}, "晋。康侯用锡马蕃庶"),
        new GuaEntry("地火明夷", new int[]{1,0,1,0,0,0}, "明夷。利艰贞"),
        new GuaEntry("风火家人", new int[]{1,0,1,0,1,1}, "家人.利女贞"),
        new GuaEntry("火泽睽", new int[]{1,1,0,1,0,1}, "睽。小事吉"),
        new GuaEntry("水山蹇", new int[]{0,0,1,0,1,0}, "蹇。利西南，不利东北"),
        new GuaEntry("雷水解", new int[]{0,1,0,1,0,0}, "解.利西南，无所往"),
        new GuaEntry("山泽损", new int[]{1,1,0,0,0,1}, "损。有孚，元吉，无咎"),
        new GuaEntry("风雷益", new int[]{1,0,0,0,1,1}, "益。利有攸往，利涉大川"),
        new GuaEntry("泽天夬", new int[]{1,1,1,1,1,0}, "夬。扬于王庭"),
        new GuaEntry("天风姤", new int[]{0,1,1,1,1,1}, "姤。女壮，勿用取女"),
        new GuaEntry("泽地萃", new int[]{0,0,0,1,1,0}, "萃。亨，王假有庙"),
        new GuaEntry("地风升", new int[]{0,1,1,0,0,0}, "升。元亨，用见大人"),
        new GuaEntry("泽水困", new int[]{0,1,0,1,1,0}, "困。亨，贞大人吉"),
        new GuaEntry("水风井", new int[]{0,1,1,0,1,0}, "井.改邑不改井"),
        new GuaEntry("泽火革", new int[]{1,0,1,1,1,0}, "革。己日乃孚，元亨利贞"),
        new GuaEntry("火风鼎", new int[]{0,1,1,1,0,1}, "鼎。元吉，亨"),
        new GuaEntry("震为雷", new int[]{1,0,0,1,0,0}, "震。亨，震来虩虩"),
        new GuaEntry("艮为山", new int[]{0,0,1,0,0,1}, "艮其背，不获其身"),
        new GuaEntry("风山渐", new int[]{0,0,1,0,1,1}, "渐。女归吉，利贞"),
        new GuaEntry("雷泽归妹", new int[]{1,1,0,1,0,0}, "归妹。征凶，无攸利"),
        new GuaEntry("雷火丰", new int[]{1,0,1,1,0,0}, "丰。亨，王假之"),
        new GuaEntry("火山旅", new int[]{0,0,1,1,0,1}, "旅。小亨，旅贞吉"),
        new GuaEntry("巽为风", new int[]{0,1,1,0,1,1}, "巽。小亨，利有攸往"),
        new GuaEntry("兑为泽", new int[]{1,1,0,1,1,0}, "兑。亨，利贞"),
        new GuaEntry("风水涣", new int[]{0,1,0,0,1,1}, "涣。亨，王假有庙"),
        new GuaEntry("水泽节", new int[]{1,1,0,0,1,0}, "节。亨，苦节不可贞"),
        new GuaEntry("风泽中孚", new int[]{1,1,0,0,1,1}, "中孚.豚鱼吉，利涉大川"),
        new GuaEntry("雷山小过", new int[]{0,0,1,1,0,0}, "小过。亨，利贞"),
        new GuaEntry("水火既济", new int[]{1,0,1,0,1,0}, "既济。亨小，利贞"),
        new GuaEntry("火水未济", new int[]{0,1,0,1,0,1}, "未济。亨，小狐汔济")
    };

    public static int findIndex(int[] yinYang) {
        if (yinYang == null || yinYang.length != 6) return -1;
        for (int i = 0; i < GUA_DATA.length; i++) {
            boolean match = true;
            for (int j = 0; j < 6; j++) {
                if (GUA_DATA[i].yinYang[j] != yinYang[j]) {
                    match = false;
                    break;
                }
            }
            if (match) return i;
        }
        return -1;
    }

    public static class Result {
        public int[] benGua = new int[6];
        public int[] zhiGua = new int[6];
        public int benGuaIdx;
        public int zhiGuaIdx;
        public int sumBian;
    }

    public static Result calculate(int[] sixYao) {
        Result result = new Result();
        int sumBian = 0;
        for (int i = 0; i < 6; i++) {
            int val = sixYao[i];
            int isYangBen = (val % 2 != 0) ? 1 : 0;
            result.benGua[i] = isYangBen;
            if (val == 6 || val == 9) {
                sumBian++;
                result.zhiGua[i] = (isYangBen == 1) ? 0 : 1;
            } else {
                result.zhiGua[i] = isYangBen;
            }
        }
        result.sumBian = sumBian;
        result.benGuaIdx = findIndex(result.benGua);
        result.zhiGuaIdx = findIndex(result.zhiGua);
        return result;
    }

    // ========== 农历及天干地支推算模块 ==========
    public static class LunarInfo {
        public String yearGanZhi;
        public String monthGanZhi;
        public String dayGanZhi;
        public String hourGanZhi;
        public String dayGan;
        public String shengXiao;
    }

    private static final String[] TIAN_GAN = {"甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"};
    private static final String[] DI_ZHI = {"子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"};
    private static final String[] SHENG_XIAO = {"鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪"};

    public static int getDaysFromBase(int year, int month, int day) {
        LocalDate baseDate = LocalDate.of(2000, 1, 1);
        LocalDate targetDate = LocalDate.of(year, month, day);
        return (int) ChronoUnit.DAYS.between(baseDate, targetDate);
    }

    public static LunarInfo getLunarInfo(long timeMs) {
        java.time.Instant instant = java.time.Instant.ofEpochMilli(timeMs);
        java.time.LocalDateTime dt = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
        int year = dt.getYear();
        int month = dt.getMonthValue();
        int day = dt.getDayOfMonth();
        int hour = dt.getHour();

        LunarInfo info = new LunarInfo();

        // 1. 年干支 & 生肖
        int offset = year - 4;
        int yGan = ((offset % 10) + 10) % 10;
        int yZhi = ((offset % 12) + 12) % 12;
        info.yearGanZhi = TIAN_GAN[yGan] + DI_ZHI[yZhi];
        info.shengXiao = SHENG_XIAO[yZhi];

        // 2. 月干支
        int lunarMonthApprox = (month >= 2) ? month - 1 : 12;
        int mZhi = (lunarMonthApprox + 1) % 12; // 正月为寅 (index 2)
        int monthGanStart = 0;
        switch (yGan % 5) {
            case 0: monthGanStart = 2; break; // 甲己 -> 丙(2)
            case 1: monthGanStart = 4; break; // 乙庚 -> 戊(4)
            case 2: monthGanStart = 6; break; // 丙辛 -> 庚(6)
            case 3: monthGanStart = 8; break; // 丁壬 -> 壬(8)
            case 4: monthGanStart = 0; break; // 戊癸 -> 甲(0)
        }
        int mGan = (monthGanStart + lunarMonthApprox - 1) % 10;
        info.monthGanZhi = TIAN_GAN[mGan] + DI_ZHI[mZhi];

        // 3. 日干支
        int days = getDaysFromBase(year, month, day);
        int dayGzIdx = ((days + 54) % 60 + 60) % 60; // 2000-01-01 为甲戌日 (index 10 / offset 54)
        int dGan = dayGzIdx % 10;
        int dZhi = dayGzIdx % 12;
        info.dayGanZhi = TIAN_GAN[dGan] + DI_ZHI[dZhi];
        info.dayGan = TIAN_GAN[dGan];

        // 4. 时干支
        int hZhi = ((hour + 1) % 24) / 2;
        int ganStart = (dGan % 5) * 2;
        int hGan = (ganStart + hZhi) % 10;
        info.hourGanZhi = TIAN_GAN[hGan] + DI_ZHI[hZhi];

        return info;
    }

    // ========== 纳甲与六亲六神推算模块 ==========
    public static class TrigramInfo {
        public final int[] trigram;  // {1, 1, 1}
        public final String name;     // "乾"
        public final int element;     // 0=金, 1=水, 2=木, 3=火, 4=土
        public final String[] stems;  // 天干 {"甲","壬"}
        public final String[] branches; // 纳甲地支 {"子","寅","辰","午","申","戌"}

        public TrigramInfo(int[] trigram, String name, int element, String[] stems, String[] branches) {
            this.trigram = trigram;
            this.name = name;
            this.element = element;
            this.stems = stems;
            this.branches = branches;
        }
    }

    public static final TrigramInfo[] TRIGRAMS = new TrigramInfo[] {
        new TrigramInfo(new int[]{1,1,1}, "乾", 0, new String[]{"甲","壬"}, new String[]{"子","寅","辰","午","申","戌"}),
        new TrigramInfo(new int[]{1,1,0}, "兑", 0, new String[]{"丁","丁"}, new String[]{"巳","卯","丑","亥","酉","未"}),
        new TrigramInfo(new int[]{1,0,1}, "离", 3, new String[]{"己","己"}, new String[]{"卯","丑","亥","酉","未","巳"}),
        new TrigramInfo(new int[]{1,0,0}, "震", 2, new String[]{"庚","庚"}, new String[]{"子","寅","辰","午","申","戌"}),
        new TrigramInfo(new int[]{0,1,1}, "巽", 2, new String[]{"辛","辛"}, new String[]{"丑","亥","酉","未","巳","卯"}),
        new TrigramInfo(new int[]{0,1,0}, "坎", 1, new String[]{"戊","戊"}, new String[]{"寅","辰","午","申","戌","子"}),
        new TrigramInfo(new int[]{0,0,1}, "艮", 4, new String[]{"丙","丙"}, new String[]{"辰","午","申","戌","子","寅"}),
        new TrigramInfo(new int[]{0,0,0}, "坤", 4, new String[]{"乙","癸"}, new String[]{"未","巳","卯","丑","亥","酉"})
    };

    private static final String[] ELEMENT_NAMES = { "金", "水", "木", "火", "土" };

    public static TrigramInfo findTrigram(int[] tri) {
        for (TrigramInfo ti : TRIGRAMS) {
            if (ti.trigram[0] == tri[0] && ti.trigram[1] == tri[1] && ti.trigram[2] == tri[2]) {
                return ti;
            }
        }
        return TRIGRAMS[0];
    }

    public static int getBranchElement(String branch) {
        switch (branch) {
            case "申": case "酉": return 0; // 金
            case "子": case "亥": return 1; // 水
            case "寅": case "卯": return 2; // 木
            case "巳": case "午": return 3; // 火
            default: return 4;             // 土 (丑辰未戌)
        }
    }

    public static class PalaceResult {
        public int[] palaceTri;
        public int shi;
        public int ying;
    }

    public static PalaceResult getPalaceInfo(int[] lower, int[] upper) {
        int m0 = (lower[0] == upper[0]) ? 0 : 1;
        int m1 = (lower[1] == upper[1]) ? 0 : 1;
        int m2 = (lower[2] == upper[2]) ? 0 : 1;
        int diffs = m0 + m1 * 2 + m2 * 4;

        PalaceResult res = new PalaceResult();
        res.palaceTri = upper;
        res.shi = 5;
        res.ying = 2;

        switch (diffs) {
            case 0: res.palaceTri = upper; res.shi = 5; res.ying = 2; break; // 纯卦
            case 1: res.palaceTri = upper; res.shi = 0; res.ying = 3; break; // 一世
            case 3: res.palaceTri = upper; res.shi = 1; res.ying = 4; break; // 二世
            case 7: res.palaceTri = upper; res.shi = 2; res.ying = 5; break; // 三世
            case 6: res.palaceTri = lower; res.shi = 3; res.ying = 0; break; // 四世
            case 4: res.palaceTri = lower; res.shi = 4; res.ying = 1; break; // 五世
            case 5: res.palaceTri = lower; res.shi = 3; res.ying = 0; break; // 游魂
            case 2: res.palaceTri = lower; res.shi = 2; res.ying = 5; break; // 归魂
        }
        return res;
    }

    public static String getLiuQin(int palaceElement, int yaoElement) {
        if (palaceElement == yaoElement) return "兄弟";
        if (yaoElement == (palaceElement + 4) % 5) return "父母";
        if (yaoElement == (palaceElement + 1) % 5) return "子孙";
        if (yaoElement == (palaceElement + 3) % 5) return "官鬼";
        if (yaoElement == (palaceElement + 2) % 5) return "妻财";
        return "未知";
    }

    public static String[] getSixGodsList(String dayGan) {
        String[] SIX_GODS = new String[] { "青龙", "朱雀", "勾陈", "腾蛇", "白虎", "玄武" };
        int startIdx = 0;
        if (dayGan.equals("甲") || dayGan.equals("乙")) startIdx = 0;
        else if (dayGan.equals("丙") || dayGan.equals("丁")) startIdx = 1;
        else if (dayGan.equals("戊")) startIdx = 2;
        else if (dayGan.equals("己")) startIdx = 3;
        else if (dayGan.equals("庚") || dayGan.equals("辛")) startIdx = 4;
        else if (dayGan.equals("壬") || dayGan.equals("癸")) startIdx = 5;

        String[] result = new String[6];
        for (int i = 0; i < 6; i++) {
            result[i] = SIX_GODS[(startIdx + i) % 6];
        }
        return result;
    }

    public static class YaoDetail {
        public String stem;
        public String branch;
        public String element;
        public String liuqin;
        public boolean isShi;
        public boolean isYing;
        public String god;
    }

    public static class HexagramDetails {
        public String palaceName;
        public int palaceElement;
        public YaoDetail[] yao = new YaoDetail[6];
    }

    public static HexagramDetails calculateDetails(int[] sixYaoArray, int overridePalaceElement, String dayGan) {
        int[] lower = { sixYaoArray[0], sixYaoArray[1], sixYaoArray[2] };
        int[] upper = { sixYaoArray[3], sixYaoArray[4], sixYaoArray[5] };

        // 爻值转换：奇数=阳(1)，偶数=阴(0)
        int[] lowerYinYang = { lower[0] % 2 != 0 ? 1 : 0, lower[1] % 2 != 0 ? 1 : 0, lower[2] % 2 != 0 ? 1 : 0 };
        int[] upperYinYang = { upper[0] % 2 != 0 ? 1 : 0, upper[1] % 2 != 0 ? 1 : 0, upper[2] % 2 != 0 ? 1 : 0 };

        PalaceResult pr = getPalaceInfo(lowerYinYang, upperYinYang);
        TrigramInfo palaceTri = findTrigram(pr.palaceTri);

        HexagramDetails details = new HexagramDetails();
        details.palaceName = palaceTri.name + "宫";
        details.palaceElement = palaceTri.element;

        int baseElement = (overridePalaceElement >= 0) ? overridePalaceElement : palaceTri.element;

        TrigramInfo lowerTri = findTrigram(lowerYinYang);
        TrigramInfo upperTri = findTrigram(upperYinYang);

        String[] sixGods = getSixGodsList(dayGan);

        for (int i = 0; i < 6; i++) {
            String branch;
            String stem;

            if (i < 3) {
                branch = lowerTri.branches[i];
                stem   = lowerTri.stems[0];
            } else {
                branch = upperTri.branches[i];
                stem   = upperTri.stems[1];
            }

            int elemIdx = getBranchElement(branch);
            YaoDetail yd = new YaoDetail();
            yd.stem = stem;
            yd.branch = branch;
            yd.element = ELEMENT_NAMES[elemIdx];
            yd.liuqin = getLiuQin(baseElement, elemIdx);
            yd.isShi = (i == pr.shi);
            yd.isYing = (i == pr.ying);
            yd.god = sixGods[i];

            details.yao[i] = yd;
        }

        return details;
    }
}

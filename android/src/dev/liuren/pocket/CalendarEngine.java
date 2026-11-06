package dev.liuren.pocket;

import com.nlf.calendar.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

/** All input is civil time in UTC+8; no device timezone or daylight-saving dependency. */
public final class CalendarEngine {
    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    public static final String GAN = "甲乙丙丁戊己庚辛壬癸", ZHI = "子丑寅卯辰巳午未申酉戌亥";
    public static final String[] GENERALS = {"神后", "大吉", "功曹", "太冲", "天罡", "太乙", "胜光", "小吉", "传送", "从魁", "河魁", "登明"};
    public static final String[] QI = {"冬至", "大寒", "雨水", "春分", "谷雨", "小满", "夏至", "大暑", "处暑", "秋分", "霜降", "小雪"};
    public static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static LocalDateTime now() { return LocalDateTime.now(ZONE).withNano(0); }
    public static final class Info {
        public LocalDateTime time;
        public String year, month, day, hour, stem, branch, general, generalBranch, xun, empty;
        public String previousTerm, nextTerm, fromQi, nextQi, rules;
        public int generalIndex;
        public String text() {
            return "六壬·时令\n" + time.format(DISPLAY) + " 北京时间\n" + year + "年 " + month + "月 " + day + "日 " + hour + "时\n日干 " + stem + " · 日支 " + branch + "\n月将 " + general + "（" + generalBranch + "）\n旬首 " + xun + " · 旬空 " + empty + "\n" + rules;
        }
    }
    public static Info calculate(LocalDateTime t, boolean ziStart, int manualGeneral) {
        if(t.getYear()<1901 || t.getYear()>2099) throw new IllegalArgumentException("请选择 1901—2099 年的日期");
        Lunar l = Solar.fromYmdHms(t.getYear(), t.getMonthValue(), t.getDayOfMonth(), t.getHour(), t.getMinute(), t.getSecond()).getLunar();
        Info r = new Info(); r.time=t;
        r.year=l.getYearInGanZhiExact(); r.month=l.getMonthInGanZhiExact();
        r.day=ziStart?l.getDayInGanZhiExact():l.getDayInGanZhiExact2();
        r.stem=r.day.substring(0,1); r.branch=r.day.substring(1);
        int stem=GAN.indexOf(r.stem), hourBranch=((t.getHour()+1)/2)%12;
        // Five-rat rule uses the selected day stem for BOTH day-rollover conventions.
        r.hour=""+GAN.charAt(((stem%5)*2+hourBranch)%10)+ZHI.charAt(hourBranch);
        r.xun=ziStart?l.getDayXunExact():l.getDayXunExact2();
        r.empty=ziStart?l.getDayXunKongExact():l.getDayXunKongExact2();
        JieQi prev=l.getPrevQi(false), next=l.getNextQi(false);
        int qi=-1; for(int i=0;i<QI.length;i++) if(QI[i].equals(prev.getName())) qi=i;
        if(qi<0) throw new IllegalStateException("无法识别中气："+prev.getName());
        r.generalIndex=manualGeneral>=0 && manualGeneral<12?manualGeneral:Math.floorMod(1-qi,12);
        r.general=GENERALS[r.generalIndex]; r.generalBranch=""+ZHI.charAt(r.generalIndex);
        r.fromQi=term(prev); r.nextQi=term(next);
        r.previousTerm=term(l.getPrevJieQi(false)); r.nextTerm=term(l.getNextJieQi(false));
        r.rules="北京时间 UTC+8 · "+(ziStart?"23:00 换日":"00:00 换日")+" · "+(manualGeneral>=0?"月将手动指定":"中气交接时刻换将");
        return r;
    }
    private static String term(JieQi q) { return q.getName()+"  "+q.getSolar().toYmdHms().substring(0,16); }
}

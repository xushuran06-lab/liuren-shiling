import dev.liuren.pocket.CalendarEngine;
import com.nlf.calendar.*;
import java.time.*;

public class EngineTest {
    static int assertions=0;
    static void eq(Object a,Object b){assertions++;if(!a.equals(b))throw new AssertionError("expected "+b+", got "+a);}
    static CalendarEngine.Info at(String s,boolean zi){return CalendarEngine.calculate(LocalDateTime.parse(s),zi,-1);}
    public static void main(String[] args){
        // Sexagenary day anchor independent of the library (2000-01-07 = 甲子).
        CalendarEngine.Info anchor=at("2000-01-07T12:00:00",false);
        eq(anchor.day,"甲子");eq(anchor.xun,"甲子");eq(anchor.empty,"戌亥");eq(anchor.hour,"庚午");
        eq(at("2000-01-07T22:59:59",false).day,"甲子");
        CalendarEngine.Info midnight=at("2000-01-07T23:00:00",false),zi=at("2000-01-07T23:00:00",true);
        eq(midnight.day,"甲子");eq(midnight.hour,"甲子");eq(zi.day,"乙丑");eq(zi.hour,"丙子");
        eq(at("2000-01-08T00:00:00",false).day,"乙丑");eq(at("2000-01-08T00:00:00",false).hour,"丙子");
        eq(at("2000-01-07T00:59:59",false).hour,"甲子");eq(at("2000-01-07T01:00:00",false).hour,"乙丑");
        // HKO Almanac 2026: 小雪 2026-11-22 15:23 UTC+8 (published minute).
        CalendarEngine.Info before=at("2026-11-22T15:22:00",false),after=at("2026-11-22T15:24:00",false);
        eq(before.general,"太冲");eq(before.generalBranch,"卯");eq(after.general,"功曹");eq(after.generalBranch,"寅");
        eq(before.nextQi,"小雪  2026-11-22 15:23");
        // Verify each of twelve general transitions exactly at the library's second.
        String[] terms={"大寒","雨水","春分","谷雨","小满","夏至","大暑","处暑","秋分","霜降","小雪","冬至"};
        String[] expected={"神后","登明","河魁","从魁","传送","小吉","胜光","太乙","天罡","太冲","功曹","大吉"};
        Lunar mid=Solar.fromYmd(2026,7,1).getLunar();
        for(int i=0;i<terms.length;i++){
            Solar term=mid.getJieQiTable().get(terms[i]);
            LocalDateTime time=LocalDateTime.parse(term.toYmdHms().replace(' ','T'));
            eq(CalendarEngine.calculate(time,false,-1).general,expected[i]);
            if(CalendarEngine.calculate(time.minusSeconds(1),false,-1).general.equals(expected[i]))throw new AssertionError("switch too early "+terms[i]);assertions++;
        }
        // Leap day, range endpoints and all sixty-day progression / xun-empty groups.
        eq(at("2024-03-01T12:00:00",false).time.toLocalDate().toEpochDay()-at("2024-02-29T12:00:00",false).time.toLocalDate().toEpochDay(),1L);
        for(int d=0;d<60;d++){
            CalendarEngine.Info r=CalendarEngine.calculate(LocalDateTime.of(2000,1,7,12,0).plusDays(d),false,-1);
            eq(r.day,""+CalendarEngine.GAN.charAt(d%10)+CalendarEngine.ZHI.charAt(d%12));
            eq(r.empty,new String[]{"戌亥","申酉","午未","辰巳","寅卯","子丑"}[d/10]);
        }
        at("1901-01-01T00:00:00",false);at("2099-12-31T23:59:59",true);assertions+=2;
        eq(CalendarEngine.calculate(LocalDateTime.of(2026,11,6,20,30),false,11).general,"登明");
        // Year and month must switch at the exact 立春 instant, independently of day rollover.
        Solar lc=mid.getJieQiTable().get("立春");LocalDateTime spring=LocalDateTime.parse(lc.toYmdHms().replace(' ','T'));
        eq(CalendarEngine.calculate(spring.minusSeconds(1),false,-1).year,"乙巳");eq(CalendarEngine.calculate(spring,false,-1).year,"丙午");
        eq(CalendarEngine.calculate(spring.minusSeconds(1),false,-1).month,"己丑");eq(CalendarEngine.calculate(spring,false,-1).month,"庚寅");
        System.out.println("PASS: "+assertions+" assertions");
        System.out.println(CalendarEngine.calculate(LocalDateTime.of(2026,11,6,20,30),false,-1).text());
    }
}

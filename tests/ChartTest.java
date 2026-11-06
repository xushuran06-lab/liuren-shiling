import dev.liuren.pocket.*;
import java.util.*;
public class ChartTest {
    static int assertions;
    static void eq(Object a,Object b){assertions++;if(!Objects.equals(a,b))throw new AssertionError("expected "+b+", got "+a);}
    static void example(String day,String general,String hour,String expected,String method){ChartEngine.Chart c=ChartEngine.calculate(day,general,hour);eq(String.join("",c.transmissions),expected);eq(c.method.startsWith(method),true);}
    public static void main(String[] args){
        // Published worked examples, independently specified in docs/chart-rules.md.
        example("丙辰","辰","卯","亥午午","别责");
        example("辛酉","子","丑","丑酉酉","别责");
        example("甲寅","丑","辰","丑亥亥","八专");
        example("丁未","辰","丑","亥戌戌","八专");
        example("甲子","子","子","寅巳申","伏吟");
        example("丙寅","子","子","巳申寅","伏吟");
        example("戊辰","子","子","巳申寅","伏吟");
        example("庚午","子","子","申寅巳","伏吟");
        example("癸酉","子","子","丑戌未","伏吟");
        example("壬辰","子","子","亥辰戌","伏吟");
        example("壬午","子","子","亥午子","伏吟");
        example("丁卯","子","子","卯子午","伏吟");
        example("丁丑","未","丑","亥未丑","返吟");
        example("丁未","丑","未","巳丑丑","返吟");
        // 《六壬大全》卷七「课经集一」原文所列三传。
        example("甲子","子","卯","午卯子","贼克");
        example("丙戌","申","巳","申亥寅","贼克");
        example("壬辰","辰","巳","戌酉申","比用");
        example("丁卯","亥","丑","亥酉未","涉害");
        example("庚子","申","戌","午辰寅","涉害");
        example("甲午","午","辰","辰午申","涉害");
        example("壬辰","申","巳","戌丑辰","遥克");
        example("壬申","亥","申","巳申亥","遥克");
        example("戊申","辰","卯","戌酉午","昴星");
        example("丁丑","丑","辰","子辰戌","昴星");
        eq(ChartEngine.calculate("丁卯","亥","丑").explanation.contains("亥涉害5重"),true);
        Map<String,Integer> methods=new TreeMap<>();
        for(int d=0;d<60;d++)for(int rotation=0;rotation<12;rotation++){
            String day=""+CalendarEngine.GAN.charAt(d%10)+CalendarEngine.ZHI.charAt(d%12);
            ChartEngine.Chart c=ChartEngine.calculate(day,""+CalendarEngine.ZHI.charAt(rotation),"子");
            String method=c.method.split("·")[0];methods.put(method,methods.getOrDefault(method,0)+1);
            eq(new HashSet<>(Arrays.asList(c.sky)).size(),12);eq(c.upper.length,4);eq(c.transmissions.length,3);
            for(String b:c.transmissions)eq(CalendarEngine.ZHI.contains(b),true);
            // Rotating general and hour together must leave the entire chart unchanged.
            for(int h=1;h<12;h++){
                ChartEngine.Chart same=ChartEngine.calculate(day,""+CalendarEngine.ZHI.charAt((rotation+h)%12),""+CalendarEngine.ZHI.charAt(h));
                eq(Arrays.asList(c.sky),Arrays.asList(same.sky));eq(Arrays.asList(c.upper),Arrays.asList(same.upper));eq(Arrays.asList(c.transmissions),Arrays.asList(same.transmissions));eq(c.method,same.method);
            }
            if(c.method.startsWith("贼克")||c.method.startsWith("比用")||c.method.startsWith("涉害")||c.method.startsWith("遥克")){
                int initial=CalendarEngine.ZHI.indexOf(c.transmissions[0]);eq(c.transmissions[1],c.sky[initial]);eq(c.transmissions[2],c.sky[CalendarEngine.ZHI.indexOf(c.transmissions[1])]);
            }
        }
        for(String kind:new String[]{"贼克","比用","涉害","遥克","昴星","别责","八专","伏吟","返吟"})eq(methods.containsKey(kind),true);
        System.out.println("PASS ChartTest: "+assertions+" assertions over all 720 basic charts / 8640 day-general-hour combinations; "+methods);
        System.out.println(ChartEngine.calculate("甲申","卯","戌").text());
    }
}

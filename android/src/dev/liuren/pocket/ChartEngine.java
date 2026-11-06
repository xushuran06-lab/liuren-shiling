package dev.liuren.pocket;

import java.util.*;

/** Deterministic 九宗门 calculator. Rules and worked examples: docs/chart-rules.md. */
public final class ChartEngine {
    private static final String G=CalendarEngine.GAN, Z=CalendarEngine.ZHI;
    // 木火土金水 = 0..4, in generating order. Ten-stem lodging is NOT 藏干.
    private static final int[] GW={0,0,1,1,2,2,3,3,4,4};
    private static final int[] ZW={4,2,0,0,2,1,1,2,3,3,2,4};
    private static final int[] LODGE={2,4,5,7,5,7,8,10,11,1};
    private static final int[] PUNISH={3,10,5,0,4,8,6,1,2,9,7,11};
    public static final class Chart {
        public String day, general, hour, method, explanation;
        public final String[] sky=new String[12], upper=new String[4], lower=new String[4];
        public final String[] transmissions=new String[3], hiddenStems=new String[3], kin=new String[3];
        public String text(){
            StringBuilder s=new StringBuilder("\n\n自动排盘\n天地盘（地→天）：");
            for(int i=0;i<12;i++)s.append(Z.charAt(i)).append('→').append(sky[i]).append(i==11?'\n':' ');
            s.append("四课（一至四）：");for(int i=0;i<4;i++)s.append(lower[i]).append('/').append(upper[i]).append(' ');
            s.append("\n三传：").append(String.join(" → ",transmissions)).append("\n取法：").append(method).append("\n").append(explanation);
            return s.toString();
        }
    }
    private final int stem, branch, shift;
    private final int[] up=new int[4], downWX=new int[4];
    private final Chart out=new Chart();
    private final ArrayList<String> trace=new ArrayList<>();
    public static Chart calculate(String day,String general,String hour){return new ChartEngine(day,general,hour).solve();}
    private ChartEngine(String day,String general,String hour){
        if(day==null||day.length()!=2||G.indexOf(day.charAt(0))<0||Z.indexOf(day.charAt(1))<0||general==null||general.length()!=1||Z.indexOf(general)<0||hour==null||hour.length()!=1||Z.indexOf(hour)<0)throw new IllegalArgumentException("无效的排盘干支");
        stem=G.indexOf(day.charAt(0));branch=Z.indexOf(day.charAt(1));
        if(stem%2!=branch%2)throw new IllegalArgumentException("日干支阴阳不匹配");
        shift=mod(Z.indexOf(general)-Z.indexOf(hour));out.day=day;out.general=general;out.hour=hour;
        for(int i=0;i<12;i++)out.sky[i]=z(sky(i));
        up[0]=sky(LODGE[stem]);up[1]=sky(up[0]);up[2]=sky(branch);up[3]=sky(up[2]);
        out.lower[0]=G.substring(stem,stem+1);out.lower[1]=z(up[0]);out.lower[2]=z(branch);out.lower[3]=z(up[2]);
        downWX[0]=GW[stem];downWX[1]=ZW[up[0]];downWX[2]=ZW[branch];downWX[3]=ZW[up[2]];
        for(int i=0;i<4;i++)out.upper[i]=z(up[i]);
        trace.add("月将"+general+"加占时"+hour+"；日干寄"+z(LODGE[stem])+"宫。");
    }
    private static int mod(int n){return Math.floorMod(n,12);}
    private static String z(int n){return Z.substring(mod(n),mod(n)+1);}
    private int sky(int earth){return mod(earth+shift);}
    private int earth(int heavenly){return mod(heavenly-shift);}
    private static boolean overcomes(int a,int b){return (a+2)%5==b;}
    private boolean yang(){return stem%2==0;}
    private List<Integer> candidates(boolean thief){
        List<Integer> result=new ArrayList<>();Set<Integer> seen=new HashSet<>();
        for(int i=0;i<4;i++)if((thief?overcomes(downWX[i],ZW[up[i]]):overcomes(ZW[up[i]],downWX[i]))&&seen.add(up[i]))result.add(i);
        return result;
    }
    private int choose(List<Integer> items,boolean thief){
        if(items.size()==1)return up[items.get(0)];
        List<Integer> comparable=new ArrayList<>();for(int i:items)if(up[i]%2==stem%2)comparable.add(i);
        if(comparable.size()==1){out.method="比用·知一";trace.add("多课有克，取与日干阴阳相同的"+z(up[comparable.get(0)])+"。");return up[comparable.get(0)];}
        List<Integer> pool=comparable.isEmpty()?items:comparable;
        int max=-1;List<Integer> deepest=new ArrayList<>();
        for(int i:pool){
            int upper=up[i],score=0;
            // Count the intervening palaces only; the current 克 is already established.
            // 《课经·涉害》: 亥加丑 passes 辰、戊、未、己、戌 (five 克), not 丑 again.
            // Include earth branches AND lodged day-stems, using the original 克 direction.
            for(int step=1;step<mod(upper-earth(upper));step++){
                int palace=mod(earth(upper)+step),w=ZW[upper];
                if(thief?overcomes(ZW[palace],w):overcomes(w,ZW[palace]))score++;
                for(int g=0;g<10;g++)if(LODGE[g]==palace&&(thief?overcomes(GW[g],w):overcomes(w,GW[g])))score++;
            }
            trace.add(z(upper)+"涉害"+score+"重。");
            if(score>max){max=score;deepest.clear();}if(score==max)deepest.add(i);
        }
        out.method="涉害·计深";
        if(deepest.size()==1)return up[deepest.get(0)];
        for(int rank:new int[]{2,0,1}){
            List<Integer> group=new ArrayList<>();for(int i:deepest)if(earth(up[i])%3==rank)group.add(i);
            if(group.isEmpty())continue;
            if(group.size()==1){out.method="涉害·"+(rank==2?"见机":rank==0?"察微":"复等");return up[group.get(0)];}
            // Equal depth and equal seasonal rank: prefer 日课 on 阳日, 辰课 on 阴日.
            out.method="涉害·复等";
            int preferred=yang()?0:2;
            for(int i:group)if(i==preferred)return up[i];
            for(int i:group)if((i<2)==yang())return up[i];
            return up[group.get(0)];
        }
        throw new IllegalStateException("涉害候选为空");
    }
    private Chart solve(){
        List<Integer> thief=candidates(true),control=candidates(false);
        List<Integer> active=thief.isEmpty()?control:thief;
        boolean useThief=!thief.isEmpty();int first,second,third;
        if(shift==0){
            out.method="伏吟";
            first=active.isEmpty()?(yang()?up[0]:up[2]):up[active.get(0)];
            second=PUNISH[first];
            if(second==first)second=first==up[0]?up[2]:up[0];
            third=PUNISH[second];
            if(third==second||third==first)third=mod(second+6);
            trace.add("伏吟：有克取克，无克阳取干、阴取支；循刑，自刑换日辰，中传自刑或回初取冲。");
        }else if(!active.isEmpty()){
            out.method=useThief?"贼克·重审":"贼克·元首";
            trace.add(useThief?"先取下贼上，重复上神只计一次。":"无下贼上，取上克下。");
            first=choose(active,useThief);second=sky(first);third=sky(second);
            if(shift==6)out.method="返吟·"+out.method;
        }else if(shift==6){
            out.method="返吟·无亲";first=new int[]{2,11,8,5}[branch%4];second=up[2];third=up[0];
            trace.add("返吟无克：支之驿马发用，中取支上，末取干上。");
        }else if(LODGE[stem]==branch){
            out.method="八专";first=mod(yang()?up[0]+2:up[3]-2);second=up[0];third=up[0];
            trace.add("干支同宫无克，不取遥克；阳从日阳顺三，阴从辰阴逆三，中末干上。");
        }else{
            List<Integer> remote=new ArrayList<>();boolean incoming=true;
            Set<Integer> seen=new HashSet<>();
            for(int i=1;i<4;i++)if(overcomes(ZW[up[i]],GW[stem])&&seen.add(up[i]))remote.add(i);
            if(remote.isEmpty()){incoming=false;seen.clear();for(int i=1;i<4;i++)if(overcomes(GW[stem],ZW[up[i]])&&seen.add(up[i]))remote.add(i);}
            if(!remote.isEmpty()){
                out.method="遥克·"+(incoming?"蒿矢":"弹射");
                first=up[remote.get(0)];
                if(remote.size()>1){int matches=0;for(int i:remote)if(up[i]%2==stem%2){first=up[i];matches++;}if(matches!=1)throw new IllegalStateException("遥克比用无法唯一取传");}
                second=sky(first);third=sky(second);trace.add("无直接贼克，先神遥克日，再日遥克神；多候选按阴阳比用。");
            }else if(new HashSet<Integer>(Arrays.asList(up[0],up[1],up[2],up[3])).size()==4){
                out.method="昴星·"+(yang()?"虎视":"冬蛇掩目");first=yang()?sky(9):earth(9);second=yang()?up[2]:up[0];third=yang()?up[0]:up[2];
                trace.add("四课全，无克无遥：阳取酉上，阴取酉下；阳中支末干，阴中干末支。");
            }else{
                out.method="别责";first=yang()?sky(LODGE[(stem+5)%10]):mod(branch+4);second=up[0];third=up[0];
                trace.add("三课不备无克无遥：阳取合干寄宫上神；阴取支前三合本支，中末干上。");
            }
        }
        int[] values={first,second,third};int head=mod(branch-stem);
        for(int i=0;i<3;i++){
            int b=values[i],offset=mod(b-head);out.transmissions[i]=z(b);out.hiddenStems[i]=offset<10?G.substring(offset,offset+1):"空";
            int a=GW[stem],w=ZW[b];out.kin[i]=a==w?"兄弟":(a+1)%5==w?"子孙":(w+1)%5==a?"父母":overcomes(a,w)?"妻财":"官鬼";
        }
        out.explanation=String.join("\n",trace);return out;
    }
}

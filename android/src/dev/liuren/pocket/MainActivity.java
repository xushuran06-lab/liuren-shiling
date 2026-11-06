package dev.liuren.pocket;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class MainActivity extends Activity {
    private final int PAPER=0xFFF5F1E8, INK=0xFF2B302B, MUTED=0xFF62675F, RED=0xFF9E4435, CARD=0xFFFEFCF7;
    private LinearLayout page;
    private LocalDateTime chosen;
    private CalendarEngine.Info info;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private String renderedMinute="";
    private long pinRequestToken;
    private final Runnable tick=new Runnable(){public void run(){
        String minute=CalendarEngine.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        if(chosen==null && !minute.equals(renderedMinute)) { render(); LiurenWidget.updateAll(MainActivity.this); }
        handler.postDelayed(this,10000);
    }};
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if(state!=null && state.containsKey("chosen")) chosen=LocalDateTime.parse(state.getString("chosen"));
        getWindow().setStatusBarColor(PAPER); getWindow().setNavigationBarColor(PAPER);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        render();
    }
    @Override public void onSaveInstanceState(Bundle out) { if(chosen!=null) out.putString("chosen",chosen.toString()); super.onSaveInstanceState(out); }
    @Override public void onResume(){super.onResume(); handler.removeCallbacks(tick); handler.post(tick); LiurenWidget.updateAll(this);}
    @Override public void onPause(){handler.removeCallbacks(tick);super.onPause();}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable background(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String s,int size,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setLineSpacing(dp(3),1);return v;}
    private void gap(LinearLayout p,int h){View v=new View(this);p.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
    private LinearLayout column(){LinearLayout v=new LinearLayout(this);v.setOrientation(1);return v;}
    private LinearLayout row(){LinearLayout v=new LinearLayout(this);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    private LinearLayout card(){LinearLayout v=column();v.setPadding(dp(20),dp(18),dp(20),dp(18));v.setBackground(background(CARD,20));return v;}
    private TextView button(String name,boolean primary,Runnable run){
        TextView b=text(name,14,primary?CARD:RED); b.setGravity(Gravity.CENTER);b.setMinHeight(dp(48));
        b.setPadding(dp(12),dp(10),dp(12),dp(10));b.setBackground(background(primary?RED:0xFFEAE5DA,13));
        b.setClickable(true);b.setFocusable(true);b.setOnClickListener(v->run.run());return b;
    }
    private void addButton(LinearLayout r,TextView b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(dp(3),0,dp(3),0);r.addView(b,p);}
    private void pair(LinearLayout p,String label,String value){LinearLayout r=row();r.setPadding(0,dp(8),0,dp(8));TextView a=text(label,13,MUTED);r.addView(a,new LinearLayout.LayoutParams(0,-2,1));TextView b=text(value,15,INK);b.setGravity(Gravity.END);r.addView(b);p.addView(r);}
    private void render(){
        LocalDateTime t=chosen==null?CalendarEngine.now():chosen;
        try {info=CalendarEngine.calculate(t,Prefs.zi(this),Prefs.general(this));}
        catch(Exception e){new AlertDialog.Builder(this).setMessage(e.getMessage()).setPositiveButton("回到当前",(d,w)->{chosen=null;render();}).show();return;}
        renderedMinute=CalendarEngine.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(PAPER);
        page=column();page.setPadding(dp(22),dp(18),dp(22),dp(24));scroll.addView(page);setContentView(scroll);
        if(Build.VERSION.SDK_INT>=30){
            getWindow().setDecorFitsSystemWindows(false);
            scroll.setOnApplyWindowInsetsListener((v,in)->{android.graphics.Insets i=in.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(i.left,i.top,i.right,i.bottom);return in;});
            scroll.requestApplyInsets();
        }
        LinearLayout heading=row();TextView brand=text("六壬 · 时令",23,INK);brand.setTypeface(Typeface.create("serif",0));heading.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        TextView rules=button("规则",false,()->settings());heading.addView(rules);page.addView(heading);
        page.addView(text("起盘所需，一眼即见。",13,MUTED));gap(page,25);
        LinearLayout date=row();TextView state=text(chosen==null?"●  当前时刻":"○  指定时刻",12,RED);date.addView(state,new LinearLayout.LayoutParams(0,-2,1));date.addView(text("北京时间 UTC+8",12,MUTED));page.addView(date);gap(page,7);
        TextView dateValue=text(t.format(DateTimeFormatter.ofPattern("yyyy.MM.dd  HH:mm")),26,INK);dateValue.setTypeface(Typeface.create("sans-serif-light",0));page.addView(dateValue);gap(page,16);
        LinearLayout main=row();
        LinearLayout day=card();day.addView(text("日干 / 日支",12,MUTED));TextView glyph=text(info.day,43,INK);glyph.setTypeface(Typeface.create("serif",0));day.addView(glyph);day.addView(text(info.day+"日 · "+info.xun+"旬",12,MUTED));
        LinearLayout gen=card();gen.addView(text("月将"+(Prefs.general(this)>=0?" · 手动":" · 中气换将"),12,MUTED));TextView name=text(info.general,35,RED);name.setTypeface(Typeface.create("serif",0));gen.addView(name);gen.addView(text(info.generalBranch+"将",16,RED));
        LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,dp(155),1);left.rightMargin=dp(6);main.addView(day,left);LinearLayout.LayoutParams right=new LinearLayout.LayoutParams(0,dp(155),1);right.leftMargin=dp(6);main.addView(gen,right);page.addView(main);gap(page,12);
        LinearLayout detail=card();LinearLayout pillars=row();
        String[] labels={"年柱","月柱","日柱","占时"},values={info.year,info.month,info.day,info.hour};
        for(int i=0;i<4;i++){LinearLayout col=column();col.setGravity(Gravity.CENTER);col.addView(text(labels[i],12,MUTED));gap(col,8);col.addView(text(values[i],22,i==2?RED:INK));pillars.addView(col,new LinearLayout.LayoutParams(0,-2,1));}
        detail.addView(pillars);gap(detail,16);View line=new View(this);line.setBackgroundColor(0xFFE9E4D9);detail.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));gap(detail,7);
        pair(detail,"旬首",info.xun);pair(detail,"旬空",info.empty);page.addView(detail);gap(page,12);
        addChartSection();
        LinearLayout terms=card();terms.addView(text("节候与交将",16,INK));gap(terms,8);
        terms.addView(text("当前节气  "+info.previousTerm,13,MUTED));gap(terms,6);terms.addView(text("下一节气  "+info.nextTerm,13,MUTED));gap(terms,12);
        terms.addView(text("本次交将  "+info.fromQi,13,INK));gap(terms,6);terms.addView(text("下次交将  "+info.nextQi,13,INK));
        if(Prefs.general(this)>=0){gap(terms,9);terms.addView(text("月将已手动指定；上述时间仅为中气参考。",12,RED));}
        page.addView(terms);gap(page,16);
        LinearLayout actions=row();addButton(actions,button("选择日期时间",true,()->pick()));addButton(actions,button("回到当前",false,()->{chosen=null;render();}));page.addView(actions);gap(page,10);
        LinearLayout more=row();addButton(more,button("添加桌面组件",false,()->pin()));addButton(more,button("复制起盘信息",false,()->{String content=info.text();if(Prefs.autoChart(this))try{content+=ChartEngine.calculate(info.day,info.generalBranch,info.hour.substring(1)).text();}catch(RuntimeException e){content+="\n自动排盘未完成："+e.getMessage();}((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("起盘信息",content));Toast.makeText(this,"已复制",Toast.LENGTH_SHORT).show();}));page.addView(more);gap(page,20);
        page.addView(text(info.rules,12,MUTED));gap(page,6);page.addView(text("年柱立春换年 · 月柱以节换月\n时干按所选换日规则的日干起算 · 未启用真太阳时",12,MUTED));
    }
    private void addChartSection(){
        LinearLayout section=card(),header=row(),title=column();title.addView(text("自动排盘",17,INK));title.addView(text("天地盘 · 四课 · 三传",12,MUTED));header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        Switch toggle=new Switch(this);toggle.setContentDescription("开启或关闭自动排盘");toggle.setChecked(Prefs.autoChart(this));header.addView(toggle);section.addView(header);
        toggle.setOnCheckedChangeListener((v,on)->{Prefs.get(this).edit().putBoolean("autoChart",on).apply();render();});
        if(!Prefs.autoChart(this)){gap(section,12);section.addView(text("按需开启；桌面小组件始终只显示起盘信息。",12,MUTED));page.addView(section);gap(page,12);return;}
        try{
            ChartEngine.Chart chart=ChartEngine.calculate(info.day,info.generalBranch,info.hour.substring(1));
            gap(section,16);section.addView(text("天地盘",15,INK));gap(section,5);section.addView(text("每宫上行为天盘，下行为地盘；南在上。",11,MUTED));gap(section,10);
            int[][] ring={{5,6,7,8},{4,-1,-1,9},{3,-1,-1,10},{2,1,0,11}};
            for(int[] positions:ring){LinearLayout cells=row();for(int earth:positions){LinearLayout cell=column();cell.setGravity(Gravity.CENTER);cell.setPadding(0,dp(7),0,dp(7));if(earth>=0){cell.addView(text(chart.sky[earth],21,RED));cell.addView(text("地 "+CalendarEngine.ZHI.charAt(earth),11,MUTED));}else{cell.addView(text("",21,MUTED));cell.addView(text("",11,MUTED));}cells.addView(cell,new LinearLayout.LayoutParams(0,dp(65),1));}section.addView(cells);}
            gap(section,15);section.addView(text("四课",15,INK));gap(section,10);LinearLayout lessons=row();
            // Traditional reading order: first lesson at the right, fourth at the left.
            for(int k=3;k>=0;k--){LinearLayout cell=column();cell.setGravity(Gravity.CENTER);cell.addView(text(new String[]{"一课","二课","三课","四课"}[k],11,MUTED));gap(cell,6);cell.addView(text(chart.upper[k],21,RED));cell.addView(text(chart.lower[k],21,INK));lessons.addView(cell,new LinearLayout.LayoutParams(0,-2,1));}section.addView(lessons);
            gap(section,19);section.addView(text("三传 · "+chart.method,15,INK));gap(section,7);
            for(int i=0;i<3;i++)pair(section,new String[]{"初传","中传","末传"}[i],chart.kin[i]+"　"+chart.hiddenStems[i]+chart.transmissions[i]);
            gap(section,8);section.addView(button("查看取传依据",false,()->message("取传依据 · "+chart.method,chart.explanation+"\n\n本版取法：涉害计深含十干寄宫，深度相同再取孟仲季；阴别责取支前三合本支。仅排天地盘、四课、三传与遁干六亲，不含十二天将及断课。")));
        }catch(RuntimeException e){gap(section,12);section.addView(text("此课暂未完成排盘："+e.getMessage()+"。可关闭自动排盘继续查看起盘信息。",13,RED));}
        page.addView(section);gap(page,12);
    }
    private void changed(){LiurenWidget.updateAll(this);render();}
    private void settings(){
        String[] items={"换日："+(Prefs.zi(this)?"子初 23:00":"零点 00:00"),"月将："+(Prefs.general(this)<0?"按中气自动":CalendarEngine.GENERALS[Prefs.general(this)]+"（手动）"),"时间口径与桌面更新说明","关于与开源许可"};
        new AlertDialog.Builder(this).setTitle("起盘规则").setItems(items,(d,n)->{
            if(n==0)new AlertDialog.Builder(this).setTitle("换日规则").setSingleChoiceItems(new String[]{"零点换日（00:00）","子初换日（23:00）"},Prefs.zi(this)?1:0,(x,k)->{Prefs.get(this).edit().putBoolean("zi",k==1).apply();x.dismiss();changed();}).setNegativeButton("取消",null).show();
            if(n==1){String[] g=new String[13];g[0]="按中气交接时刻自动换将";for(int i=0;i<12;i++)g[i+1]=CalendarEngine.GENERALS[i]+" · "+CalendarEngine.ZHI.charAt(i);new AlertDialog.Builder(this).setTitle("月将取法").setSingleChoiceItems(g,Prefs.general(this)+1,(x,k)->{Prefs.get(this).edit().putInt("general",k-1).apply();x.dismiss();changed();}).setNegativeButton("取消",null).show();}
            if(n==2)message("时间与更新","所有输入与显示均为北京时间 UTC+8，不跟随手机时区；本版不提供真太阳时。\n\n零点换日：23点日干仍属当日；子初换日：23点采用次日日干。两者时干均按所显示日干起算。年柱按立春时刻，月柱按节令时刻。\n\n应用前台按分钟更新；桌面显示更新时间的快照，系统约每30分钟请求更新，省电模式可能延迟。临近换日、换时或交将时，请点击组件“刷新”校时。\n\n应用选定的历史时间只影响应用；组件始终使用当前时间与已保存的规则。");
            if(n==3){String license="";try(java.io.InputStream in=getAssets().open("LUNAR-LICENSE.txt")){java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] b=new byte[2048];int len;while((len=in.read(b))!=-1)out.write(b,0,len);license=out.toString("UTF-8");}catch(Exception e){license="源码目录 vendor/lunar-java/LICENSE";}message("六壬 · 时令 0.2.0","离线起盘助手 · 支持1901—2099年\n历法：6tail/lunar-java（MIT）\n首页可选开启天地盘、四课、三传；默认关闭。\n\n"+license);}
        }).setNegativeButton("关闭",null).show();
    }
    private void message(String title,String content){new AlertDialog.Builder(this).setTitle(title).setMessage(content).setPositiveButton("知道了",null).show();}
    private void pick(){
        LocalDateTime initial=chosen==null?CalendarEngine.now():chosen;
        DatePickerDialog d=new DatePickerDialog(this,(v,y,m,day)->new TimePickerDialog(this,(tv,h,min)->{chosen=LocalDateTime.of(y,m+1,day,h,min);render();},initial.getHour(),initial.getMinute(),true).show(),initial.getYear(),initial.getMonthValue()-1,initial.getDayOfMonth());
        d.getDatePicker().setMinDate(LocalDate.of(1901,1,1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
        d.getDatePicker().setMaxDate(LocalDate.of(2099,12,31).atTime(23,59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());d.show();
    }
    private void pin(){
        AppWidgetManager m=AppWidgetManager.getInstance(this);
        try {
            if(!m.isRequestPinAppWidgetSupported()){pinHelp("当前桌面不支持从应用内直接添加。");return;}
            long token=SystemClock.elapsedRealtimeNanos();
            Intent done=new Intent(this,LiurenWidget.class).setAction(LiurenWidget.PINNED).putExtra("token",token);
            PendingIntent callback=PendingIntent.getBroadcast(this,2,done,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            boolean accepted=m.requestPinAppWidget(new ComponentName(this,LiurenWidget.class),null,callback);
            if(!accepted){callback.cancel();pinHelp("当前桌面未接受添加请求。");return;}
            pinRequestToken=token;
            Toast.makeText(this,"已向桌面请求添加；若没有弹窗，可长按桌面空白处手动添加。",Toast.LENGTH_LONG).show();
            checkPinLater();
        }catch(RuntimeException e){pinHelp("系统未能打开桌面添加窗口，可改用手动添加。");}
    }
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(focused&&pinRequestToken!=0)checkPinLater();}
    private void checkPinLater(){
        final long token=pinRequestToken;
        handler.postDelayed(()->{
            if(token==0||pinRequestToken!=token||isFinishing()||isDestroyed())return;
            if(Prefs.get(this).getLong("lastPinToken",0)==token){pinRequestToken=0;return;}
            if(!hasWindowFocus())return;
            pinRequestToken=0;
            pinHelp("已提交添加请求，但尚未收到桌面的成功确认。若未出现系统弹窗，请使用下面的手动方式。");
        },1800);
    }
    private void pinHelp(String reason){
        new AlertDialog.Builder(this).setTitle("添加桌面组件").setMessage(reason+"\n\n1. 返回手机桌面，长按空白处（部分桌面需双指捏合）。\n2. 找到“小组件”“窗口小工具”或“安卓小组件”。\n3. 找到“六壬·时令”，长按并拖到有空位的桌面。\n\n如果列表里没有本应用，请提供手机品牌、型号和系统版本，以便继续排查。")
            .setPositiveButton("返回桌面",(d,w)->{try{startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(ActivityNotFoundException e){Toast.makeText(this,"请用手机主页手势返回桌面",Toast.LENGTH_LONG).show();}})
            .setNegativeButton("留在应用",null).show();
    }
}

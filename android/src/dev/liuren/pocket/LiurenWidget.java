package dev.liuren.pocket;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.widget.*;
import java.time.format.DateTimeFormatter;

public class LiurenWidget extends AppWidgetProvider {
    static final String REFRESH="dev.liuren.pocket.REFRESH";
    static final String PINNED="dev.liuren.pocket.PINNED";
    public static void updateAll(Context c){AppWidgetManager m=AppWidgetManager.getInstance(c);int[] ids=m.getAppWidgetIds(new ComponentName(c,LiurenWidget.class));for(int id:ids)update(c,m,id);}
    @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){for(int id:ids)update(c,m,id);}
    @Override public void onReceive(Context c,Intent i){super.onReceive(c,i);if(PINNED.equals(i.getAction())){Prefs.get(c).edit().putLong("lastPinToken",i.getLongExtra("token",0)).apply();updateAll(c);Toast.makeText(c,"桌面组件已添加",Toast.LENGTH_LONG).show();}if(REFRESH.equals(i.getAction())||Intent.ACTION_TIME_CHANGED.equals(i.getAction())||Intent.ACTION_TIMEZONE_CHANGED.equals(i.getAction())||Intent.ACTION_DATE_CHANGED.equals(i.getAction()))updateAll(c);}
    private static void update(Context c,AppWidgetManager m,int id){
        CalendarEngine.Info r=CalendarEngine.calculate(CalendarEngine.now(),Prefs.zi(c),Prefs.general(c));
        RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.widget);
        v.setTextViewText(R.id.widget_main,"日干 "+r.stem+"   "+r.general+"·"+r.generalBranch);
        v.setTextViewText(R.id.widget_detail,"日支 "+r.branch+" · "+r.hour+"时 · 旬空 "+r.empty);
        v.setTextViewText(R.id.widget_stamp,r.time.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))+" 北京时间快照 · "+(Prefs.zi(c)?"23点":"零点")+"换日"+(Prefs.general(c)>=0?" · 手动月将":""));
        v.setOnClickPendingIntent(R.id.widget_root,PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        v.setOnClickPendingIntent(R.id.widget_refresh,PendingIntent.getBroadcast(c,1,new Intent(c,LiurenWidget.class).setAction(REFRESH),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        m.updateAppWidget(id,v);
    }
}

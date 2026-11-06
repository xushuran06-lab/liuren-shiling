package dev.liuren.pocket;
import android.content.*;
final class Prefs {
    static android.content.SharedPreferences get(Context c) { return c.getSharedPreferences("rules",0); }
    static boolean zi(Context c) { return get(c).getBoolean("zi",false); }
    static int general(Context c) { return get(c).getInt("general",-1); }
    static boolean autoChart(Context c) { return get(c).getBoolean("autoChart",false); }
}

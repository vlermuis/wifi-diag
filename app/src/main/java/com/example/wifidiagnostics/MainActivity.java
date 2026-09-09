package com.example.wifidiagnostics;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int LOCATION_REQUEST = 42;
    private LinearLayout root, details; private TextView status; private Button testButton;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int green = Color.rgb(53, 104, 89), ink = Color.rgb(35, 46, 43), muted = Color.rgb(101, 113, 108);

    @Override public void onCreate(Bundle state) { super.onCreate(state); buildUi();
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.NEARBY_WIFI_DEVICES, Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);
        else if (android.os.Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);
        refresh(); }

    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private TextView text(String value, float size, int color) { TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setFontFeatureSettings("kern"); return t; }
    private LinearLayout row(String label, String value) { LinearLayout r = new LinearLayout(this); r.setGravity(Gravity.CENTER_VERTICAL); r.setPadding(0, dp(10), 0, dp(10));
        TextView l=text(label,14,muted), v=text(value,15,ink); v.setTypeface(Typeface.DEFAULT,Typeface.BOLD); v.setGravity(Gravity.RIGHT); r.addView(l,new LinearLayout.LayoutParams(0,-2,1)); r.addView(v,new LinearLayout.LayoutParams(dp(190),-2)); return r; }
    private TextView title(String s) { TextView t=text(s,14,green); t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); t.setPadding(0,dp(4),0,dp(3)); return t; }
    private void card(String heading) { LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(18),dp(13),dp(18),dp(8)); android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable(); bg.setColor(Color.WHITE); bg.setCornerRadius(dp(18)); c.setBackground(bg); c.setElevation(dp(2)); details.addView(c,new LinearLayout.LayoutParams(-1,-2)); ((LinearLayout.LayoutParams)c.getLayoutParams()).setMargins(0,0,0,dp(13)); c.addView(title(heading)); c.setTag(heading); }
    private void buildUi() { ScrollView scroll=new ScrollView(this); scroll.setBackgroundColor(Color.rgb(247,247,242)); root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(20),dp(25),dp(20),dp(20)); scroll.addView(root); setContentView(scroll);
        TextView h=text("Wi‑Fi Diagnostics",30,ink); h.setTypeface(Typeface.DEFAULT,Typeface.BOLD); root.addView(h); TextView sub=text("A quick look at your current connection",15,muted); sub.setPadding(0,dp(4),0,dp(20)); root.addView(sub);
        status=text("Checking connection…",16,Color.WHITE); status.setGravity(Gravity.CENTER_VERTICAL); status.setPadding(dp(18),0,dp(18),0); android.graphics.drawable.GradientDrawable sb=new android.graphics.drawable.GradientDrawable(); sb.setColor(green); sb.setCornerRadius(dp(16)); status.setBackground(sb); root.addView(status,new LinearLayout.LayoutParams(-1,dp(62))); 
        testButton=new Button(this); testButton.setText("Run internet test"); testButton.setTextColor(green); testButton.setAllCaps(false); testButton.setOnClickListener(v -> runInternetTest()); LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(50)); bp.setMargins(0,dp(14),0,dp(16)); root.addView(testButton,bp);
        details=new LinearLayout(this); details.setOrientation(LinearLayout.VERTICAL); root.addView(details); }

    private void refresh() { details.removeAllViews(); ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE); Network n=cm.getActiveNetwork(); NetworkCapabilities cap=n==null?null:cm.getNetworkCapabilities(n); boolean wifi=cap!=null && cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI); status.setText(wifi?"●  Connected to Wi‑Fi":"○  Wi‑Fi is not the active connection"); status.setBackgroundColor(wifi?green:Color.rgb(150,91,46));
        card("Connection"); LinearLayout c=(LinearLayout)details.getChildAt(0); WifiManager wm=(WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE); WifiInfo info=wm.getConnectionInfo(); String ssid=wifi?clean(info.getSSID()):"Not connected"; c.addView(row("Network",ssid)); c.addView(row("Signal",wifi?signal(info.getRssi()):"—")); c.addView(row("Link speed",wifi?info.getLinkSpeed()+" Mbps":"—")); c.addView(row("Frequency",wifi?frequency(info.getFrequency()):"—"));
        card("Network details"); c=(LinearLayout)details.getChildAt(1); String ip="—", gateway="—"; if(n!=null){ LinkProperties lp=cm.getLinkProperties(n); if(lp!=null){ if(!lp.getLinkAddresses().isEmpty()) ip=lp.getLinkAddresses().get(0).getAddress().getHostAddress(); if(lp.getRoutes()!=null) for(android.net.RouteInfo route:lp.getRoutes()) if(route.isDefaultRoute()&&route.getGateway()!=null){gateway=route.getGateway().getHostAddress();break;} }} c.addView(row("IP address",ip)); c.addView(row("Gateway",gateway)); c.addView(row("Validated internet",cap!=null&&cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)?"Yes":"Not confirmed"));
        card("Radio identity"); c=(LinearLayout)details.getChildAt(2); c.addView(row("SSID",ssid)); c.addView(row("BSSID",wifi?clean(info.getBSSID()):"—")); c.addView(row("Network type",wifi?"802.11 Wi‑Fi":"—")); }
    private String clean(String s){ if(s==null||s.equals("<unknown ssid>")||s.equals("02:00:00:00:00:00")) return "Unavailable"; return s.replace("\"",""); }
    private String signal(int r){ if(r<=-100)return "Very weak ("+r+" dBm)"; if(r<=-80)return "Weak ("+r+" dBm)"; if(r<=-65)return "Fair ("+r+" dBm)"; return "Strong ("+r+" dBm)"; }
    private String frequency(int f){ if(f<=0)return "—"; return String.format(Locale.US,"%d MHz",f); }
    private void runInternetTest(){ testButton.setEnabled(false); testButton.setText("Testing…"); status.setText("●  Testing internet reachability…"); new Thread(() -> { long start=System.currentTimeMillis(); String result; try { HttpURLConnection h=(HttpURLConnection)new URL("https://connectivitycheck.gstatic.com/generate_204").openConnection(); h.setConnectTimeout(5000); h.setReadTimeout(5000); h.setInstanceFollowRedirects(false); int code=h.getResponseCode(); result=(code==204?"Internet reachable":"Internet responded (HTTP "+code+")"); h.disconnect(); } catch(Exception e){ result="No internet response ("+e.getClass().getSimpleName()+")"; } long ms=System.currentTimeMillis()-start; String out=result+"  •  "+ms+" ms"; handler.post(() -> { testButton.setEnabled(true); testButton.setText("Run internet test"); status.setText(out); }); }).start(); }
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);refresh();}
}

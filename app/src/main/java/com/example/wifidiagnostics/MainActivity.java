package com.example.wifidiagnostics;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int LOCATION_REQUEST = 42;
    private LinearLayout root, details, scanResults; private TextView status, scanSummary; private Button testButton, scanButton;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private WifiManager wifiManager;
    private int green = Color.rgb(53, 104, 89), ink = Color.rgb(35, 46, 43), muted = Color.rgb(101, 113, 108);

    @Override public void onCreate(Bundle state) { super.onCreate(state); buildUi();
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.NEARBY_WIFI_DEVICES, Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);
        else if (android.os.Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);
        wifiManager=(WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
        IntentFilter scanFilter=new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        if(Build.VERSION.SDK_INT>=33) registerReceiver(scanReceiver,scanFilter,Context.RECEIVER_EXPORTED); else registerReceiver(scanReceiver,scanFilter);
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
        testButton=new Button(this); testButton.setText("Run internet test"); testButton.setTextColor(green); testButton.setAllCaps(false); testButton.setOnClickListener(v -> runInternetTest()); LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(50)); bp.setMargins(0,dp(14),0,dp(8)); root.addView(testButton,bp);
        scanButton=new Button(this); scanButton.setText("Scan nearby Wi‑Fi"); scanButton.setTextColor(green); scanButton.setAllCaps(false); scanButton.setOnClickListener(v -> startWifiScan()); LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(50)); sp.setMargins(0,0,0,dp(16)); root.addView(scanButton,sp);
        details=new LinearLayout(this); details.setOrientation(LinearLayout.VERTICAL); root.addView(details); }

    private void refresh() { details.removeAllViews(); ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE); Network n=cm.getActiveNetwork(); NetworkCapabilities cap=n==null?null:cm.getNetworkCapabilities(n); boolean wifi=cap!=null && cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI); status.setText(wifi?"●  Connected to Wi‑Fi":"○  Wi‑Fi is not the active connection"); status.setBackgroundColor(wifi?green:Color.rgb(150,91,46));
        card("Connection"); LinearLayout c=(LinearLayout)details.getChildAt(0); WifiManager wm=(WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE); WifiInfo info=wm.getConnectionInfo(); String ssid=wifi?clean(info.getSSID()):"Not connected"; c.addView(row("Network",ssid)); c.addView(row("Signal",wifi?signal(info.getRssi()):"—")); c.addView(row("Link speed",wifi?info.getLinkSpeed()+" Mbps":"—")); c.addView(row("Frequency",wifi?frequency(info.getFrequency()):"—"));
        card("Network details"); c=(LinearLayout)details.getChildAt(1); String ip="—", gateway="—"; if(n!=null){ LinkProperties lp=cm.getLinkProperties(n); if(lp!=null){ if(!lp.getLinkAddresses().isEmpty()) ip=lp.getLinkAddresses().get(0).getAddress().getHostAddress(); if(lp.getRoutes()!=null) for(android.net.RouteInfo route:lp.getRoutes()) if(route.isDefaultRoute()&&route.getGateway()!=null){gateway=route.getGateway().getHostAddress();break;} }} c.addView(row("IP address",ip)); c.addView(row("Gateway",gateway)); c.addView(row("Validated internet",cap!=null&&cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)?"Yes":"Not confirmed"));
        card("Radio identity"); c=(LinearLayout)details.getChildAt(2); c.addView(row("SSID",ssid)); c.addView(row("BSSID",wifi?clean(info.getBSSID()):"—")); c.addView(row("Network type",wifi?"802.11 Wi‑Fi":"—"));
        card("Nearby networks"); c=(LinearLayout)details.getChildAt(3); scanSummary=text("Tap Scan nearby Wi‑Fi to discover access points.",14,muted); scanSummary.setPadding(0,dp(4),0,dp(10)); c.addView(scanSummary); scanResults=new LinearLayout(this); scanResults.setOrientation(LinearLayout.VERTICAL); c.addView(scanResults); }

    private final BroadcastReceiver scanReceiver=new BroadcastReceiver(){ @Override public void onReceive(Context context,Intent intent){ boolean success=!intent.hasExtra(WifiManager.EXTRA_RESULTS_UPDATED)||intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED,true); showScanResults(success); } };
    private void startWifiScan(){ if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){ scanSummary.setText("Location permission is required by Android to scan Wi‑Fi."); requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_REQUEST); return; } if(!wifiManager.isWifiEnabled()){ scanSummary.setText("Wi‑Fi is turned off. Turn it on and scan again."); return; } scanButton.setEnabled(false); scanButton.setText("Scanning…"); scanSummary.setText("Collecting nearby access point details…"); if(!wifiManager.startScan()){ scanButton.setEnabled(true); scanButton.setText("Scan nearby Wi‑Fi"); scanSummary.setText("Android rejected the scan. Try again after a few seconds."); } }
    private void showScanResults(boolean success){ scanButton.setEnabled(true); scanButton.setText("Scan nearby Wi‑Fi"); if(!success){scanSummary.setText("The scan failed or was throttled by Android.");return;} List<ScanResult> results=new ArrayList<>(wifiManager.getScanResults()); Collections.sort(results, Comparator.comparingInt((ScanResult x)->x.level).reversed()); scanResults.removeAllViews(); if(results.isEmpty()){scanSummary.setText("No access points reported. Check Location and Wi‑Fi settings.");return;} scanSummary.setText(results.size()+" access point"+(results.size()==1?"":"s")+" found • strongest first"); int shown=0; for(ScanResult r:results){ if(shown++>=50)break; LinearLayout block=new LinearLayout(this); block.setOrientation(LinearLayout.VERTICAL); block.setPadding(0,dp(8),0,dp(8)); String name=clean(r.SSID); if(name.equals("Unavailable")||name.isEmpty())name="Hidden network"; TextView n=text(name,16,ink);n.setTypeface(Typeface.DEFAULT,Typeface.BOLD);block.addView(n); String line=r.BSSID+"  •  "+signal(r.level)+"  •  "+frequency(r.frequency)+" (ch "+channel(r.frequency)+")"; block.addView(text(line,13,muted)); String extra=security(r.capabilities)+"  •  "+standard(r)+"  •  "+channelWidth(r)+"  •  centers "+centerFrequencies(r); block.addView(text(extra,13,muted)); String flags="Age "+scanAge(r.timestamp)+"  •  "+(r.isPasspointNetwork()?"Passpoint":"Non‑Passpoint")+"  •  "+(r.is80211mcResponder()?"802.11mc responder":"No 802.11mc flag"); if(r.operatorFriendlyName!=null&&r.operatorFriendlyName.length()>0)flags+="  •  "+r.operatorFriendlyName; block.addView(text(flags,12,muted)); scanResults.addView(block); } }
    private String security(String caps){ if(caps==null||caps.isEmpty())return "Open / unknown security"; String s=caps.replace("["," ").replace("]","").trim(); if(s.contains("SAE")||s.contains("WPA3"))return "WPA3 / "+s; if(s.contains("WEP"))return "WEP / "+s; if(s.contains("WPA"))return "WPA / "+s; return s; }
    private String standard(ScanResult r){ if(Build.VERSION.SDK_INT<30)return "Standard unavailable"; switch(r.getWifiStandard()){case 8:return "802.11be";case 7:return "802.11ad";case 6:return "802.11ax";case 5:return "802.11ac";case 4:return "802.11n";case 1:return "Legacy 802.11";default:return "Standard unknown";} }
    private String channelWidth(ScanResult r){ if(Build.VERSION.SDK_INT<23)return "Width unavailable"; switch(r.channelWidth){case 0:return "20 MHz";case 1:return "40 MHz";case 2:return "80 MHz";case 3:return "160 MHz";case 4:return "80+80 MHz";default:return "Width unknown";} }
    private String centerFrequencies(ScanResult r){ if(r.centerFreq0<=0)return "unknown"; return r.centerFreq1>0?r.centerFreq0+"/"+r.centerFreq1+" MHz":r.centerFreq0+" MHz"; }
    private String channel(int f){ if(f>=2412&&f<=2484)return f==2484?"14":String.valueOf((f-2407)/5); if(f>=5000&&f<=5900)return String.valueOf((f-5000)/5); if(f>=5955&&f<=7115)return String.valueOf((f-5950)/5); return "?"; }
    private String scanAge(long timestamp){ if(timestamp<=0)return "age unknown"; long sec=Math.max(0,(System.nanoTime()-timestamp)/1000000000L); return sec<60?sec+"s old":(sec/60)+"m old"; }
    private String clean(String s){ if(s==null||s.equals("<unknown ssid>")||s.equals("02:00:00:00:00:00")) return "Unavailable"; return s.replace("\"",""); }
    private String signal(int r){ if(r<=-100)return "Very weak ("+r+" dBm)"; if(r<=-80)return "Weak ("+r+" dBm)"; if(r<=-65)return "Fair ("+r+" dBm)"; return "Strong ("+r+" dBm)"; }
    private String frequency(int f){ if(f<=0)return "—"; return String.format(Locale.US,"%d MHz",f); }
    private void runInternetTest(){ testButton.setEnabled(false); testButton.setText("Testing…"); status.setText("●  Testing internet reachability…"); new Thread(() -> { long start=System.currentTimeMillis(); String result; try { HttpURLConnection h=(HttpURLConnection)new URL("https://connectivitycheck.gstatic.com/generate_204").openConnection(); h.setConnectTimeout(5000); h.setReadTimeout(5000); h.setInstanceFollowRedirects(false); int code=h.getResponseCode(); result=(code==204?"Internet reachable":"Internet responded (HTTP "+code+")"); h.disconnect(); } catch(Exception e){ result="No internet response ("+e.getClass().getSimpleName()+")"; } long ms=System.currentTimeMillis()-start; String out=result+"  •  "+ms+" ms"; handler.post(() -> { testButton.setEnabled(true); testButton.setText("Run internet test"); status.setText(out); }); }).start(); }
    @Override protected void onResume(){super.onResume(); if(details!=null)refresh();}
    @Override protected void onDestroy(){unregisterReceiver(scanReceiver);super.onDestroy();}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);refresh();}
}

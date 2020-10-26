package com.huolong.hf.uu;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.MotionEvent;

import com.huolong.hf.MainActivity;

public class TriTestAgent {
    private Activity activity;
    private String url;
    private TriangleGesture triangleGesture;
    private static String SP_KEY = "TriTestAgent_SP_KEY";

    private Gesture.OnAppear onAppear = new Gesture.OnAppear() {
        @Override
        public void onAppear() {
            AlertDialog dialog = IpDialog.getDialog(activity, "保存", new IpDialog.OnGetIp() {
                @Override
                public void onGetIp(int ty,String ip, String port) {
                    boolean exit = true;
                    if(ty == IpDialog.TypeIP_PORT)
                    {
                        SharedPreferences.Editor editor = getSpEditer();
                        editor.putString("ip",ip);
                        editor.putString("port",port);
                        editor.commit();
                    }else if(ty == IpDialog.TypeCOMMAND){
                        String[] cmd = ip.split(" ");
                        if(cmd[0].equals("origin"))
                        {
                            SharedPreferences.Editor editor = getSpEditer();
                            editor.clear();
                            editor.commit();
                        }else
                        if(cmd[0].equals("url") && cmd.length >= 2)
                        {
                            SharedPreferences.Editor editor = getSpEditer();
                            editor.putString("url",cmd[1]);
                            editor.commit();
                        }else
                        if(cmd[0].equals("hide_sp"))
                        {
                            ((MainActivity)activity).hide_splash();
                            exit = false;
                        }
                    }
                    if(exit) System.exit(0);
                }
            });
            dialog.show();
        }
    };

    public SharedPreferences getSp()
    {
        return activity.getSharedPreferences( SP_KEY, Context.MODE_PRIVATE );
    }

    public SharedPreferences.Editor getSpEditer()
    {
        return activity.getSharedPreferences( SP_KEY, Context.MODE_PRIVATE ).edit();
    }

    public TriTestAgent(Activity activity, String url)
    {
        this.activity = activity;
        this.url = url;
        triangleGesture = new TriangleGesture(onAppear);
    }

    public String getUrl()
    {
        SharedPreferences mContextSp = getSp();
        String override_url = mContextSp.getString("url","");
        if(!override_url.isEmpty())
        {
            return override_url;
        }
        String ip = mContextSp.getString("ip","");
        String port = mContextSp.getString("port","");
        if(!ip.isEmpty() && !port.isEmpty())
        {
            return url_form_ip_port(ip,port);
        }
        return url;
    }

    private String url_form_ip_port(String ip, String port) {
        return "http://"+ip+":"+port+"/bin/index.js";
    }

    public void handleEvent(MotionEvent event)
    {
        triangleGesture.handleEvent(event);
    }
}

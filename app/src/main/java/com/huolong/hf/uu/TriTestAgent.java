package com.huolong.hf.uu;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.MotionEvent;

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
                    if(ty == IpDialog.TypeIP_PORT)
                    {
                        SharedPreferences mContextSp = activity.getSharedPreferences( SP_KEY, Context.MODE_PRIVATE );
                        SharedPreferences.Editor editor = mContextSp.edit();
                        editor.putString("ip",ip);
                        editor.putString("port",port);
                        editor.commit();
                    }else if(ty == IpDialog.TypeCOMMAND){
                        if(ip .equals("origin"))
                        {
                            SharedPreferences mContextSp = activity.getSharedPreferences( SP_KEY, Context.MODE_PRIVATE );
                            SharedPreferences.Editor editor = mContextSp.edit();
                            editor.clear();
                            editor.commit();
                        }
                    }
                    System.exit(0);
                }
            });
            dialog.show();
        }
    };

    public TriTestAgent(Activity activity, String url)
    {
        this.activity = activity;
        this.url = url;
        triangleGesture = new TriangleGesture(onAppear);
    }

    public String getUrl()
    {
        SharedPreferences mContextSp = activity.getSharedPreferences( SP_KEY, Context.MODE_PRIVATE );
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

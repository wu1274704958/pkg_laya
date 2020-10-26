package com.huolong.hf.uu;


import android.content.Context;
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

public class IpDialog {

    public static int TypeIP_PORT = 0x1;
    public static int TypeCOMMAND = 0x2;

    public interface OnGetIp{
        void onGetIp(int ty,String ip,String port);
    }

    public static AlertDialog getDialog(final Context context, String positove, final OnGetIp onGetIp)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("请输入ip:port");
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setVerticalGravity(LinearLayout.VERTICAL);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        final EditText editText = new EditText(context);
        editText.setGravity(Gravity.START);
        linearLayout.addView(editText,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        builder.setView(linearLayout);
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton(positove, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String s = editText.getText().toString();
                if(s.charAt(0) == '@')
                {
                    if(onGetIp != null)
                        onGetIp.onGetIp(TypeCOMMAND,s.substring(1),null);
                    return;
                }
                try {
                    String[]ip_port = s.split(":");
                    String[] _n = ip_port[0].split("\\.");
                    int port = Integer.parseInt(ip_port[1]);
                    int _0 = Integer.parseInt(_n[0]);
                    int _1 = Integer.parseInt(_n[1]);
                    int _2 = Integer.parseInt(_n[2]);
                    int _3 = Integer.parseInt(_n[3]);
                    Toast.makeText(context,_0+"."+_1+"."+_2+"."+_3+":"+port,Toast.LENGTH_LONG).show();
                    if(onGetIp != null)
                        onGetIp.onGetIp(TypeIP_PORT,ip_port[0],ip_port[1]);
                }catch (Exception e)
                {
                    Toast.makeText(context,e.getMessage(),Toast.LENGTH_LONG).show();
                }
            }
        });

        return  builder.create();
    }
}

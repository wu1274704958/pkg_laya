package com.huolong.hf;

import android.content.Context;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;

public class Logw {
    public static void e(String str)
    {
        Log.e("===",str);
        try {
            save_log("===", str);
        }catch (Exception e)
        {

        }
    }

    private static Context context = null;
    public static void init_write(Context _context)
    {
        context = _context;
    }
    private static BufferedWriter writer = null;
    public static void close_writer()
    {
        try{
            if(writer != null) writer.close();
        }catch (Exception e)
        {

        }
    }
    public static BufferedWriter getWriter(Context context) throws  IOException
    {
        if(writer == null)
        {
            File dir = context.getExternalFilesDir("logs");
            if(!dir.exists())
                dir.mkdirs();
            File log = new File(dir,"log.txt");
            if(!log.exists())
                log.createNewFile();

            return (writer = new BufferedWriter(new FileWriter(log)));
        }else{
            return  writer;
        }
    }

    public static void save_log( String tag,String body) throws IOException {

        if(context != null) {
            BufferedWriter w = getWriter(context);
            w.write("T: " + tag + " M: " + body);
            w.newLine();
            w.flush();
        }
    }
}
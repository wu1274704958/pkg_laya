package com.huolong.hf.uu;

import android.util.Log;
import android.view.MotionEvent;

import java.util.ArrayList;

public abstract class Gesture {
    public interface OnAppear{
        void onAppear();
    }
    private OnAppear onAppear;
    public Gesture(OnAppear onAppear)
    {
        this.onAppear = onAppear;
        this.points = new ArrayList<>();
    }

    public void appear(){
        if(onAppear != null)
        {
            onAppear.onAppear();
        }
    }
    protected long down_ms;
    protected ArrayList<Vec2> points;
    public void begin(Vec2 v)
    {
        down_ms = System.currentTimeMillis();
        if(!points.isEmpty())
            points.clear();
        points.add(v);
    }

    public void move(Vec2 v)
    {
        if(System.currentTimeMillis() - down_ms > 2000)
            return;
        if(points.size() != 1)
            Log.e("Gesture","call move point num should be 1!");
        points.add(v);
    }

    public void end(Vec2 v)
    {
        if(System.currentTimeMillis() - down_ms > 2000)
            return;
        points.add(v);
        onEnd();
    }

    public void handleEvent(MotionEvent event)
    {
        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                begin(new Vec2(event.getX(),event.getY()));
                break;
            case MotionEvent.ACTION_UP:
                end(new Vec2(event.getX(),event.getY()));
                break;
            case MotionEvent.ACTION_MOVE:
                move(new Vec2(event.getX(),event.getY()));
                break;
        }
    }

    public abstract void onEnd();
}

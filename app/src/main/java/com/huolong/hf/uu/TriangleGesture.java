package com.huolong.hf.uu;

import android.util.Log;

import java.util.ArrayList;

public class TriangleGesture extends Gesture {

    public TriangleGesture(OnAppear onAppear) {
        super(onAppear);
    }

    @Override
    public void onEnd() {
        if(points.size() <= 3)
            return;
        ArrayList<Vec2> vs = new ArrayList<>();
        int b = 0;
        for(int i = 1;i < points.size() - 1;++i)
        {
            Vec2 last = points.get(i).sub(points.get(i - 1));
            Vec2 curr = points.get(i + 1).sub(points.get(i));
            if( Math.abs(points.get(i).sub(points.get(b)).len()) > 100.0f && Math.abs( Vec2.angle(last.angle(curr)) ) > 30.f )
            {
                vs.add(points.get(i).sub(points.get(b)));
                b = i;
            }
        }
        if(vs.size() == 0)
            return;
        if(
                Math.abs( points.get(points.size() - 1).sub(points.get(b)).len()) > 100.0f &&
                        Math.abs( Vec2.angle( points.get(points.size() - 1).sub(points.get(b)).angle(vs.get(vs.size() - 1)) ) ) > 30.0f
        )
        {
            vs.add(points.get(points.size() - 1).sub(points.get(b)));
        }
        boolean f1 = false,f2 = false;
        if(Math.abs( points.get(0).sub(points.get(points.size() - 1)).len() ) < 50.0 )
            f1 = true;
        if(vs.size() >= 3)
        {
            double _a,_b,_c;
            Log.e("===",(_a = 180.0 - Vec2.angle(vs.get(0).angle(vs.get(1)))) + "");
            Log.e("===",(_b = 180.0 - Vec2.angle(vs.get(1).angle(vs.get(2)))) + "");
            Log.e("===",(_c = 180.0 - Vec2.angle(vs.get(2).angle(vs.get(0)))) +  "");
            if(_a + _b + _c >= 170.0 && _a + _b + _c <= 190.0)
            {
                f2 = true;
            }
        }
        if(f1 && f2)
            appear();
    }
}

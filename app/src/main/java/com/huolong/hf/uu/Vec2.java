package com.huolong.hf.uu;

public class Vec2 {
    static float angle(float rad) {
        return (float) ((rad / Math.PI) * 180.0);
    }

    float x;
    float y;
    public Vec2(float x,float y)
    {
        this.x = x;
        this.y = y;
    }

    public float len() {
        return (float) Math.sqrt(Math.pow(this.x,2.0) + Math.pow(this.y,2.0));
    }
    public void unitized(){
        float len = this.len();
        float ratio =  1.0f/len;
        this.x *= ratio;
        this.y *= ratio;
    }
    public float dot_product(Vec2 oth){
        return this.x * oth.x + this.y * oth.y;
    }
    public void multiply(float n)
    {
        this.x *= n;
        this.y *= n;
    }
    public float angle(Vec2 other)
    {
        Vec2 oth = new Vec2(other.x,other.y);
        oth.unitized();

        Vec2 sel = new Vec2(this.x,this.y);
        sel.unitized();

        return (float) Math.acos(sel.dot_product(oth));
    }
    public Vec2 Projection(Vec2 oth){
        float v_len = (float) Math.cos( this.angle(oth) ) * this.len();
        float ration = v_len / oth.len();
        Vec2 ret = new Vec2(oth.x,oth.y);
        ret.multiply(ration);
        return ret;
    }
    public Vec2 mul_k(float n){
        return new Vec2(n * this.x,n * this.y);
    }


    public Vec2 add(Vec2 rhs) {
        return new Vec2(x + rhs.x ,y + rhs.y);
    }


    public Vec2 sub(Vec2 rhs) {
        return new Vec2(x - rhs.x ,y - rhs.y);
    }

    @Override
    public String toString() {
        return "Vec2{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}

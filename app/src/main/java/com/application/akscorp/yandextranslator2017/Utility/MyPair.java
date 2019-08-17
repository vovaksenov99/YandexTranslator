package com.application.akscorp.yandextranslator2017.Utility;

/**
 * Created by vovaaksenov99 on 14.04.2016.
 */
public class MyPair<F,S>  {
    public F first;
    public S second;
    public MyPair mp(F f, S s) {
        MyPair rez = new MyPair();
        rez.second = s;
        rez.first = f;
        return rez;
    }

    public F first()
    {
        return this.first;
    }
    public S second()
    {
        return this.second;
    }
}
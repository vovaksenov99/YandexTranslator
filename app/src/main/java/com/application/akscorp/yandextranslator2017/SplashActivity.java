package com.application.akscorp.yandextranslator2017;

/**
 * Created by AksCorp on 06.04.2017.
 */


import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    Thread t;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        t = new Thread(new Runnable() {
            @Override
            public void run() {
                final Intent intent = new Intent(SplashActivity.this, StartScreen.class);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if(t!=null && !t.isInterrupted())
                            startActivity(intent);
                         finish();
                    }
                });


            }
        });
        t.start();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        t.interrupt();
        finish();
    }

}
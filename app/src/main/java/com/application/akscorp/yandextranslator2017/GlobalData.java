package com.application.akscorp.yandextranslator2017;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.application.akscorp.yandextranslator2017.DatabaseWork.DatabaseWork;

/**
 * Created by Владимир on 24.03.2017.
 * This class contains global data such as: global settings variables, pages view.
 */

public class GlobalData extends Application {

    //Main screen
    private TranslateScreen TranslateScreen;
    private HistoryAndFavouriteScreen HistoryAndFavouriteScreen;
    private SettingScreen SettingScreen;

    int LANGUAGE_CODE = 0;//Current language in app
    String LANGUAGE_START_CODE = "ru";//define start language in translate toolbar
    String LAST_LANGUAGE_TRANSLATE_CODE = "ru";
    boolean AUTOTRANSLATE_ENABLE = false;//true - translate only by button "translate"; false - translate with every text or language changes

    DatabaseWork db;//My custom database class

    Activity getActivity(Context context)
    {
        return (Activity)context;
    }

    //Get main preference from SharedPreferences
    public void StartLoadApplicationGlobalData(Context context)
    {
        SharedPreferences sPref = getActivity(context).getPreferences(MODE_PRIVATE);
        //The values description above
        LANGUAGE_CODE = sPref.getInt("LANGUAGE_CODE",0);
        AUTOTRANSLATE_ENABLE = sPref.getBoolean("AUTOTRANSLATE_ENABLE",false);
        LANGUAGE_START_CODE = sPref.getString("LANGUAGE_START_CODE","ru");
        LAST_LANGUAGE_TRANSLATE_CODE = sPref.getString("LAST_LANGUAGE_TRANSLATE_CODE","ru");

        db = new DatabaseWork(context);
    }
    //Getter and Setter for screen View
    public TranslateScreen getTranslateScreen() {
        return this.TranslateScreen;
    }
    public void setTranslateScreen(TranslateScreen TranslateScreen) {
       this.TranslateScreen = TranslateScreen;
    }
    public HistoryAndFavouriteScreen getHistoryAndFavouriteScreen() {
        return this.HistoryAndFavouriteScreen;
    }
    public void setHistoryAndFavouriteScreen(HistoryAndFavouriteScreen HistoryAndFavouriteScreen) {
        this.HistoryAndFavouriteScreen = HistoryAndFavouriteScreen;
    }
    public SettingScreen getSettingScreen() {
        return this.SettingScreen;
    }
    public void setSettingScreen(SettingScreen SettingScreen) {
        this.SettingScreen = SettingScreen;
    }
}
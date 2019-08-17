package com.application.akscorp.yandextranslator2017;

import android.app.Activity;
import android.content.Context;

/**
 * Created by Владимир on 27.03.2017.
 *
 * Language codes description:
 * 0 - Russian language
 * 1 - English language
 * 2 - Joke language :)
 */

public class LanguageWork {

    /**
     *
     * @param id language code(Describe in class header)
     * @return language abbreviated name
     */
    static String GetLanguageNameById(int id)
    {
        String rez = "en";
        switch (id)
        {
            case 0:
                rez = "ru";
                break;
            case 1:
                rez = "en";
                break;
            case 2:
                rez = "ru";
                break;
        }
        return rez;
    }


    /**
     *
     * @param context
     * @param LanguagesCode (Describe in class header)
     * @return string from strings.xml by name and language code(Describe in class header)
     */
    static String GetResourceString(Context context,String name, int LanguagesCode)
    {
        String rez = null;
        int Id;
        switch (LanguagesCode)
        {
            case 0:
                Id = context.getResources().getIdentifier(name,"string",context.getPackageName());
                rez = context.getString(Id);
                break;
            case 1:
                Id = context.getResources().getIdentifier(name+"_en","string",context.getPackageName());
                rez = context.getString(Id);
                break;
            case 2:
                Id = context.getResources().getIdentifier(name+"_co","string",context.getPackageName());
                rez = context.getString(Id);
                break;
        }
        return rez;
    }

    /**
     *  Use current application language code
     * @param context
     * @return string from strings.xml by name
     */
    static String GetResourceString(Context context,String name)
    {
        GlobalData myApp = (GlobalData)((Activity)context).getApplication();
        String rez = null;
        int Id;
        switch (myApp.LANGUAGE_CODE)
        {
            case 0:
                Id = context.getResources().getIdentifier(name,"string",context.getPackageName());
                rez = context.getString(Id);
                break;
            case 1:
                Id = context.getResources().getIdentifier(name+"_en","string",context.getPackageName());
                rez = context.getString(Id);
                break;
            case 2:
                Id = context.getResources().getIdentifier(name+"_co","string",context.getPackageName());
                rez = context.getString(Id);
                break;
        }
        return rez;
    }
}

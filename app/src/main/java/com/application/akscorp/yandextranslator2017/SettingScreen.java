package com.application.akscorp.yandextranslator2017;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.widget.Toolbar;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by Владимир on 27.03.2017.
 */

public class SettingScreen {
    Context context;
    GlobalData myApp;

    View SettingPage;
    Switch AutoTranslate;
    //ChooseLanguageApp - show choose dialog with available app UI languages by click
    //ChooseLanguageStart - show choose dialog with available start translate languages by click
    LinearLayout ChooseLanguageApp, ChooseLanguageStart, AboutApp;

    SettingScreen(Context context) {
        this.context = context;
        StartInit();
    }

    Activity getActivity() {
        return (Activity) context;
    }

    View getView() {
        return SettingPage;
    }

    /**
     * Define text by current app language
     */
    void LanguageInit() {
        TextView m_choose_language = (TextView) SettingPage.findViewById(R.id.m_choose_language_app);
        m_choose_language.setText(LanguageWork.GetResourceString(context, "choose_language_app"));
        TextView m_autotranslate = (TextView) SettingPage.findViewById(R.id.m_autotranslate);
        m_autotranslate.setText(LanguageWork.GetResourceString(context, "auto_translate"));
        TextView m_autotranslate_description = (TextView) SettingPage.findViewById(R.id.m_autotranslate_description);
        m_autotranslate_description.setText(LanguageWork.GetResourceString(context, "auto_translate_description"));
        TextView m_about_app = (TextView) SettingPage.findViewById(R.id.m_about_app);
        m_about_app.setText(LanguageWork.GetResourceString(context, "about_app"));

        TextView m_choose_language_start = (TextView) SettingPage.findViewById(R.id.m_choose_language_start);
        m_choose_language_start.setText(LanguageWork.GetResourceString(context, "choose_language_start"));

        Toolbar toolbar = (Toolbar) SettingPage.findViewById(R.id.setting_toolbar);
        toolbar.setTitle(LanguageWork.GetResourceString(context, "setting"));

    }

    void StartInit() {
        final SharedPreferences sPref = getActivity().getPreferences(MODE_PRIVATE);
        myApp = (GlobalData) getActivity().getApplication();
        SettingPage = View.inflate(getActivity(), R.layout.setting_page, null);
        AutoTranslate = (Switch) SettingPage.findViewById(R.id.auto_translate_switch);
        ChooseLanguageApp = (LinearLayout) SettingPage.findViewById(R.id.choose_language_app);
        ChooseLanguageStart = (LinearLayout) SettingPage.findViewById(R.id.choose_language_start);
        AboutApp = (LinearLayout) SettingPage.findViewById(R.id.about_app);
        //Autotranslate switch initialization
        AutoTranslate.setChecked(myApp.AUTOTRANSLATE_ENABLE);
        AutoTranslate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor ed = sPref.edit();
                ed.putBoolean("AUTOTRANSLATE_ENABLE", isChecked);
                ed.commit();
                ShowMessage(LanguageWork.GetResourceString(context, "reload_application"));
            }
        });

        ChooseLanguageApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShowChooseAppLanguageDialog();
            }
        });
        ChooseLanguageStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShowChooseStartLanguageDialog();
            }
        });
        AboutApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShowAboutAppDialog();
            }
        });
        LanguageInit();
    }

    void ShowAboutAppDialog() {
        final TextView message = new TextView(context);
        message.setPadding(30, 30, 30, 5);
        message.setLinkTextColor(context.getResources().getColor(R.color.colorPrimaryDark));
        final SpannableString s =
                new SpannableString("Developer: Аксенов Владимир Алексеевич\n\nContacts:\nhttps://vk.com/vovaaks\nhttps://github.com/vovaksenov99\n\n(c)Aksenov Vladimir 2017 for Yandex");
        Linkify.addLinks(s, Linkify.WEB_URLS);
        message.setText(s);
        message.setMovementMethod(LinkMovementMethod.getInstance());

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(message);
        builder.setPositiveButton(LanguageWork.GetResourceString(context, "back"), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    /**
     * Show dialog with available languages for use in app UI interface
     */
    void ShowChooseAppLanguageDialog() {
        String[] language_list = context.getResources().getStringArray(R.array.language_list);

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.MyAlertDialogStyle);
        builder.setTitle(LanguageWork.GetResourceString(context, "choose_language_app"))
                .setCancelable(true)

                .setNeutralButton(LanguageWork.GetResourceString(context, "back"),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int id) {
                                dialog.cancel();
                            }
                        })
                .setSingleChoiceItems(language_list, myApp.LANGUAGE_CODE,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int item) {
                                final SharedPreferences sPref = getActivity().getPreferences(MODE_PRIVATE);
                                SharedPreferences.Editor ed = sPref.edit();
                                ed.putInt("LANGUAGE_CODE", item);
                                ed.commit();
                                ShowMessage(LanguageWork.GetResourceString(context, "reload_application"));

                            }
                        });
        builder.show();
    }

    /**
     * Show dialog with available languages for default translate
     */
    void ShowChooseStartLanguageDialog() {
        String[] language_list = myApp.getTranslateScreen().LangsTranslateNameSpinner;
        int pos = -1;
        for (int i = 0; i < language_list.length; i++) {
            if (myApp.getTranslateScreen().LangsCodeSpinner[i].equals(myApp.LANGUAGE_START_CODE)) {
                pos = i;
                break;
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.MyAlertDialogStyle);
        builder.setTitle(LanguageWork.GetResourceString(context, "choose_language_start"))
                .setCancelable(true)

                .setNeutralButton(LanguageWork.GetResourceString(context, "back"),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int id) {
                                dialog.cancel();
                            }
                        })
                .setSingleChoiceItems(language_list, pos,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int item) {
                                final SharedPreferences sPref = getActivity().getPreferences(MODE_PRIVATE);
                                SharedPreferences.Editor ed = sPref.edit();
                                ed.putString("LANGUAGE_START_CODE", myApp.getTranslateScreen().LangsCodeSpinner[item]);
                                ed.commit();
                                ShowMessage(LanguageWork.GetResourceString(context, "reload_application"));

                            }
                        });
        builder.show();
    }

    /**
     * SnackBar with important information about setting
     */
    void ShowMessage(String message) {
        Snackbar mSnackbar = Snackbar.make(SettingPage, message, Snackbar.LENGTH_LONG);
        View snackbarView = mSnackbar.getView();
        snackbarView.setBackgroundColor(Color.WHITE);
        mSnackbar.show();
    }
}

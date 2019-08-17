package com.application.akscorp.yandextranslator2017;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Parcelable;
import android.speech.RecognizerIntent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class StartScreen extends AppCompatActivity {

    private ViewPager viewPager;
    public static GlobalData myApp;
    private List<View> pages = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_screen);
        myApp = (GlobalData)this.getApplication();
        viewPager = (ViewPager) findViewById(R.id.main_viewpager);
        //Describe in GlobalData class
        myApp.StartLoadApplicationGlobalData(this);
        myApp.setTranslateScreen(new TranslateScreen(this));
        myApp.setHistoryAndFavouriteScreen(new HistoryAndFavouriteScreen(this));
        myApp.setSettingScreen(new SettingScreen(this));
    }

    @Override
    protected void onResume() {
        super.onResume();

        ViewPagerController();
        CategoryButtonController();
    }

    /**
     * View Pager init with translate, favourite, setting screen.
     */
    private void ViewPagerController() {


        if (pages.isEmpty()) {
            pages.add(myApp.getTranslateScreen().getView());
            pages.add(myApp.getHistoryAndFavouriteScreen().getView());
            pages.add(myApp.getSettingScreen().getView());
        }

        MainViewPagerAdapter pagerAdapter = new MainViewPagerAdapter(pages);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(0);
        SelectCurrentPage(0);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

                switch (position)
                {
                    /**
                     * 0 - Translate Screen
                     * 1 - History and favourite screen
                     * 2 - Setting screen
                     */
                    case 0:
                        //Check available languages list
                        myApp.getTranslateScreen().CheckLanguagesListCorrect();
                        //Get current selected language pair
                        final String type = myApp.getTranslateScreen().GetLanguagesType();
                        Thread t = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                //Set check status to current fraze from translate EditText
                                myApp.getTranslateScreen().FavoriteButtonInit(myApp.getTranslateScreen().editTextInputFrazeForTranslate.getText().toString(),type);
                            }
                        });
                        t.start();
                        break;
                    case 1:
                        //get text from search field for filter data by query
                        String text = myApp.getHistoryAndFavouriteScreen().searchView.getQuery().toString();
                        myApp.getHistoryAndFavouriteScreen().SearchFilterWork(text);
                        CloseErrorSnackBar();
                        break;
                    case 2:
                        CloseErrorSnackBar();
                        break;

                }
                SelectCurrentPage(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

    }

    /**
     * Close SnackBar with translateScreen error in another screen
     */
    private void CloseErrorSnackBar()
    {
        if(myApp.getTranslateScreen().ErrorSnackBar!=null && myApp.getTranslateScreen().ErrorSnackBar.isShown())
            myApp.getTranslateScreen().ErrorSnackBar.dismiss();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /**
     * Select current section
     * @param position
     *
     */
    private void SelectCurrentPage(int position)
    {
        final ImageButton translate = (ImageButton) findViewById(R.id.translate_section_button);
        final ImageButton favourite = (ImageButton) findViewById(R.id.favourite_section_button);
        final ImageButton setting = (ImageButton) findViewById(R.id.setting_section_button);
        switch (position)
        {
            case 0:
                translate.setImageResource(R.drawable.ic_translate_red_a700_36dp);
                favourite.setImageResource(R.drawable.ic_bookmark_black_36dp);
                setting.setImageResource(R.drawable.ic_settings_black_36dp);
                break;
            case 1:
                favourite.setImageResource(R.drawable.ic_bookmark_red_a700_36dp);
                translate.setImageResource(R.drawable.ic_translate_black_36dp);
                setting.setImageResource(R.drawable.ic_settings_black_36dp);
                break;
            case 2:
                setting.setImageResource(R.drawable.ic_settings_red_a700_36dp);
                translate.setImageResource(R.drawable.ic_translate_black_36dp);
                favourite.setImageResource(R.drawable.ic_bookmark_black_36dp);
                break;
        }
    }

    /**
     * Init button for transition between screen
     */
    private void CategoryButtonController() {
        final ImageButton translate = (ImageButton) findViewById(R.id.translate_section_button);
        final ImageButton favourite = (ImageButton) findViewById(R.id.favourite_section_button);
        final ImageButton setting = (ImageButton) findViewById(R.id.setting_section_button);

        translate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(0);
                SelectCurrentPage(0);
            }
        });
        favourite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(1);
                SelectCurrentPage(1);
            }
        });
        setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(2);
                SelectCurrentPage(2);
            }
        });
    }

    /**
     * This method get data from SpeechToText from TranslateScreen
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 200:
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        //Get data from speech to text(Translate Screen)
                        try {
                            ArrayList<String> result = data
                                    .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                            myApp.getTranslateScreen().editTextInputFrazeForTranslate.getEditText().setText(result.get(0));
                        }
                        catch (Exception e)
                        {
                            Logs.SaveLog(this,e);
                            Toast.makeText(this, LanguageWork.GetResourceString(this, "forbidden_option"), Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
                }
        }

    }

    private class MainViewPagerAdapter extends PagerAdapter {

        List<View> pages = null;

        public MainViewPagerAdapter(List<View> pages){
            this.pages = pages;
        }

        @Override
        public Object instantiateItem(View collection, int position){
            View v = pages.get(position);
            ((ViewPager) collection).addView(v, 0);
            return v;
        }

        @Override
        public void destroyItem(View collection, int position, Object view){
            ((ViewPager) collection).removeView((View) view);
        }

        @Override
        public int getCount(){
            return pages.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object){
            return view.equals(object);
        }

        @Override
        public void finishUpdate(View arg0){
        }

        @Override
        public void restoreState(Parcelable arg0, ClassLoader arg1){
        }

        @Override
        public Parcelable saveState(){
            return null;
        }

        @Override
        public void startUpdate(View arg0){
        }
    }

}

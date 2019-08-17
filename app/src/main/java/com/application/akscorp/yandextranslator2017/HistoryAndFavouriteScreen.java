package com.application.akscorp.yandextranslator2017;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.application.akscorp.yandextranslator2017.DatabaseWork.DatabaseWork;
import com.application.akscorp.yandextranslator2017.Utility.MyPair;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

/**
 * Created by Владимир on 23.03.2017.
 * This screen show Users translation history and mark favourite records
 */

public class HistoryAndFavouriteScreen{
    Context context;
    GlobalData myApp;
    DatabaseWork db;

    private View historyAndFavouriteScreen;//Screen with historyAndFavouriteScreen layout
    private Handler InitHistoryHandler;//show result of load history and favourite lists from database
    private HistoryRecyclerViewAdapter DataAdapter;//History and Favourite adapter for RecyclerView below
    private RecyclerView recyclerViewHistory,recyclerViewFavourite;//For show history and favourite lists
    private ProgressBar LoadProgressBar;
    private ImageButton ClearHistory;//Clear history list
    SearchView searchView;//Find substring in word and translate from history or favourite list

    ArrayList<ArrayList<String>> HistoryData = new ArrayList<>();//History records list
    ArrayList<ArrayList<String>> FavouriteData = new ArrayList<>();//Favourite records list

    private final int HISTORY_DATA = 0,FAVOURITE_DATA = 1;//current mode mark(HISTORY_DATA - if screen show history,FAVOURITE_DATA - if screen show favourite records
    private int CurrentPage = 0;//Current screen position(For viewPager with list history or favourite)

    HistoryAndFavouriteScreen(Context context) {
        this.context = context;
        historyAndFavouriteScreen = View.inflate(getActivity(), R.layout.favourite_and_history_page, null);
        LoadProgressBar = (ProgressBar) historyAndFavouriteScreen.findViewById(R.id.start_load);
        myApp = (GlobalData)getActivity().getApplication();
        db = myApp.db;
        StartInit();
    }

    /**
     * @return translate screen as a view
     */
    public View getView() {
        return historyAndFavouriteScreen;
    }

    /**
     * @return StartScreen Activity
     */
    private Activity getActivity() {
        return ((Activity) context);
    }

    private void StartInit()
    {
        searchView = (SearchView) historyAndFavouriteScreen.findViewById(R.id.search);
        recyclerViewHistory = new RecyclerView(context);
        //recyclerViewHistory.setAdapter(DataAdapter);
        recyclerViewFavourite = new RecyclerView(context);
        recyclerViewHistory.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));

        LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        recyclerViewHistory.setLayoutManager(layoutManager);

        recyclerViewFavourite.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));
        LinearLayoutManager layoutManager2 = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        recyclerViewFavourite.setLayoutManager(layoutManager2);

        ViewPager viewPager = (ViewPager) historyAndFavouriteScreen.findViewById(R.id.history_pager);
        List<View> viewList = new ArrayList<>();
        viewList.add(recyclerViewHistory);
        viewList.add(recyclerViewFavourite);
        viewPager.setAdapter(new HistoryAndFavouritePagerAdapter(viewList));
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {

                switch (position)
                {
                    case HISTORY_DATA:
                        //History data position. Initialization adapter with history data
                        InitAdapter(HISTORY_DATA);
                        CurrentPage = HISTORY_DATA;
                        recyclerViewHistory.setAdapter(DataAdapter);
                        break;
                    case FAVOURITE_DATA:
                        //Favourite data position. Initialization adapter with favourite data
                        InitAdapter(FAVOURITE_DATA);
                        CurrentPage = FAVOURITE_DATA;
                        recyclerViewFavourite.setAdapter(DataAdapter);
                        break;
                }
                //Filter data by text in searchview
                SearchFilterWork(searchView.getQuery().toString().toLowerCase());
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        TabLayout tabLayout = (TabLayout) historyAndFavouriteScreen.findViewById(R.id.tab);
        tabLayout.setupWithViewPager(viewPager);
        ClearHistory = (ImageButton)historyAndFavouriteScreen.findViewById(R.id.clear_history);
        ClearHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeleteHistory();
            }
        });
        InitSearchViewListener();
        HistoryAndFavouriteDataLoad();
    }

    private void InitSearchViewListener()
    {
        searchView.setOnClickListener(new android.widget.SearchView.OnClickListener()

                                    {
                                        @Override
                                        public void onClick(View v) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                                                searchView.setIconified(false);
                                            }


                                        }
                                    }

        );
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()

                                              {
                                                  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                                                  @Override
                                                  public boolean onQueryTextChange(String newText) {
                                                      String text = searchView.getQuery().toString().toLowerCase(Locale.getDefault());
                                                      SearchFilterWork(text);

                                                      return true;
                                                  }
                                                  @Override
                                                  public boolean onQueryTextSubmit(String query) {
                                                      return true;
                                                  }
                                              }

            );
        }
    }

    /**
     *
     * @param text search query
     */
    public void SearchFilterWork(String text)
    {
        switch (CurrentPage)
        {
            case HISTORY_DATA:
                searchView.setQueryHint(LanguageWork.GetResourceString(context,"find_in_history"));
                DataAdapter.filter(text,HistoryData);
                break;
            case FAVOURITE_DATA:
                searchView.setQueryHint(LanguageWork.GetResourceString(context,"find_in_favourite"));
                DataAdapter.filter(text,FavouriteData);
                break;
        }
    }

    /**
     * Selete all history and favourite data from local database
     */
    private void DeleteHistory()
    {
        String db_name = "Cache";
        String TableName = "History";
        db.DeleteDatabaseRecord(db_name,TableName,"");
        FavouriteData.clear();
        HistoryData.clear();

        myApp.getTranslateScreen().editTextInputFrazeForTranslate.getEditText().setText("");
        myApp.getTranslateScreen().FrazeTranslate = new YandexDictionaryWork.WordDescription[0];
        myApp.getTranslateScreen().TranslateAdapter.notifyDataSetChanged();

        myApp.getTranslateScreen().SetFavouriteCheck(false);

        InitAdapter(HISTORY_DATA);
        recyclerViewHistory.setAdapter(DataAdapter);
        InitAdapter(FAVOURITE_DATA);
        recyclerViewFavourite.setAdapter(DataAdapter);
    }

    /**
     * Load history and favourite lists from database and show history list on screen
     */
    private void HistoryAndFavouriteDataLoad() {
        LoadProgressBar.setVisibility(View.VISIBLE);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                String db_name = "Cache";
                String TableName = "History";
                FavouriteData.clear();
                HistoryData = db.GetRecordFromDatabase(db_name, TableName, "", -1, " CreateTime DESC");
                for(int i =0;i<HistoryData.size();i++)
                    if(HistoryData.get(i).get(5).equals("1"))//Get record with positive favourite flag only
                        FavouriteData.add(HistoryData.get(i));
                InitHistoryHandler.sendEmptyMessage(1);
            }
        });
        InitHistoryHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        LoadProgressBar.setVisibility(View.INVISIBLE);
                        InitAdapter(HISTORY_DATA);
                        recyclerViewHistory.setAdapter(DataAdapter);
                        break;
                }
                return false;
            }
        });
        t.start();
    }

    /**
     *
     * @param type it is mode which we want to show
     * This method Initializes adapter for history and favourite data
     */
    private void InitAdapter(int type)
    {
        switch (type)
        {
            case HISTORY_DATA:
                DataAdapter = new HistoryRecyclerViewAdapter(context,HistoryData);
                break;
            case FAVOURITE_DATA:
                DataAdapter = new HistoryRecyclerViewAdapter(context,FavouriteData);
                break;
        }

    }

    /**
     * ViewPager adapter with favourite and history page
     */
    private class HistoryAndFavouritePagerAdapter extends PagerAdapter {

        List<View> pages = null;
        String[] title = {LanguageWork.GetResourceString(context,"history"),
                LanguageWork.GetResourceString(context,"favourite")};

        public HistoryAndFavouritePagerAdapter(List<View> pages) {
            this.pages = pages;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return title[position];
        }

        @Override
        public Object instantiateItem(View collection, int position) {
            View v = pages.get(position);
            ((ViewPager) collection).addView(v, 0);
            return v;
        }

        @Override
        public void destroyItem(View collection, int position, Object view) {
            ((ViewPager) collection).removeView((View) view);
        }

        @Override
        public int getCount() {
            return pages.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view.equals(object);
        }

        @Override
        public void finishUpdate(View arg0) {
        }

        @Override
        public void restoreState(Parcelable arg0, ClassLoader arg1) {
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void startUpdate(View arg0) {
        }
    }

    /**
     * Adapter for history and favourite RecyclerView
     */
    private class HistoryRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        Context context;
        ArrayList<ArrayList<String>> LocalData = new ArrayList<>();

        HistoryRecyclerViewAdapter(Context context,ArrayList<ArrayList<String>> localData) {
            LocalData.addAll(localData);
            this.context = context;
        }

        class ViewHolder_ extends RecyclerView.ViewHolder {
            boolean isFavourite = false;
            TextView Translate, InputFraze, TypeTranslate;
            ImageButton FavouriteFlag;
            RelativeLayout relativeLayout;

            ViewHolder_(View v) {
                super(v);
                relativeLayout = (RelativeLayout) v;
                Translate = (TextView) relativeLayout.findViewById(R.id.Translate_history);
                InputFraze = (TextView) relativeLayout.findViewById(R.id.input_history);
                TypeTranslate = (TextView) relativeLayout.findViewById(R.id.type_translate);
                FavouriteFlag = (ImageButton) relativeLayout.findViewById(R.id.favourite_button);
            }

        }


        @Override
        public int getItemCount() {
            return LocalData.size();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            RelativeLayout v = (RelativeLayout) inflater.inflate(R.layout.history_recyclerview_element, null);
            return new ViewHolder_(v);
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
            final ViewHolder_ viewHolderFirst = (ViewHolder_) holder;
            viewHolderFirst.Translate.setText(LocalData.get(position).get(1));
            viewHolderFirst.InputFraze.setText(LocalData.get(position).get(2));
            viewHolderFirst.TypeTranslate.setText(LocalData.get(position).get(3).toUpperCase());
            //if current record favourite
            if (LocalData.get(position).get(5).equals("0")) {
                viewHolderFirst.FavouriteFlag.setImageResource(R.drawable.ic_bookmark_black_36dp);
                viewHolderFirst.isFavourite = false;
            } else {
                viewHolderFirst.isFavourite = true;
                viewHolderFirst.FavouriteFlag.setImageResource(R.drawable.ic_bookmark_red_a700_36dp);
            }
            //On click favourite flag button
            viewHolderFirst.FavouriteFlag.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!viewHolderFirst.isFavourite) {
                        viewHolderFirst.FavouriteFlag.setImageResource(R.drawable.ic_bookmark_red_a700_36dp);
                        viewHolderFirst.isFavourite = true;
                    } else {
                        viewHolderFirst.FavouriteFlag.setImageResource(R.drawable.ic_bookmark_black_36dp);
                        viewHolderFirst.isFavourite = false;
                    }
                    //Update record in database
                    UpdateTranslateFavouriteStatus(LocalData.get(position), viewHolderFirst.isFavourite);
                    switch (CurrentPage)
                    {
                        case HISTORY_DATA:
                            filter(searchView.getQuery().toString(),HistoryData);
                            break;
                        case FAVOURITE_DATA:
                            filter(searchView.getQuery().toString(),FavouriteData);
                            break;
                    }
                }
            });

        }

        /**
         *
         * @param data record information [ID,Translate,InputFraze,Translate languages pair, Favourite flag]
         * @param check set favourite status
         */
        void UpdateTranslateFavouriteStatus(ArrayList<String> data, boolean check) {
            String db_name = "Cache";
            String TableName = "History";
            ArrayList<MyPair<String, String>> cc = new ArrayList<>();

            String fraze = data.get(2).replace("'","''");
            String Type = data.get(3);
            if(check) {
                cc.add(new MyPair<String, String>().mp("Favourite", "1"));
                data.set(5,"1");
            }
            else {

                cc.add(new MyPair<String, String>().mp("Favourite", "0"));
            }
            db.UpdateDatabaseRecord(db_name, TableName, cc, new String[]{"text"}, "Input = '" + fraze + "' AND Type = '"+Type+"'");
            //Get new history and favourite data with updated favourite status
            HistoryData = db.GetRecordFromDatabase(db_name,TableName,"",-1,"CreateTime DESC");
            FavouriteData = db.GetRecordFromDatabase(db_name,TableName,"Favourite = '1'",-1,"CreateTime DESC");
        }

        /**
         *
         * @param QueryText
         * @param NonFilterData array list without query mask
         */
        public void filter(String QueryText,ArrayList<ArrayList<String>> NonFilterData) {
            QueryText = QueryText.toLowerCase(Locale.getDefault());

            LocalData.clear();
            if (QueryText.length() == 0) {
                LocalData.addAll(NonFilterData);
            } else {
                for (int i = 0; i < NonFilterData.size(); i++) {
                    if (NonFilterData.get(i).get(1).toLowerCase(Locale.getDefault()).contains(QueryText) ||
                            NonFilterData.get(i).get(2).toLowerCase(Locale.getDefault()).contains(QueryText)) {
                        LocalData.add( NonFilterData.get(i));
                    }
                }
            }
            notifyDataSetChanged();
        }
    }
}

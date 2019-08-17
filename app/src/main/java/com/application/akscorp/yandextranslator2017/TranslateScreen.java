package com.application.akscorp.yandextranslator2017;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.BaseColumns;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import androidx.core.content.res.ResourcesCompat;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.application.akscorp.yandextranslator2017.DatabaseWork.DatabaseWork;
import com.application.akscorp.yandextranslator2017.Utility.MyPair;
import com.application.akscorp.yandextranslator2017.Utility.MyUtility;
import com.google.android.material.snackbar.Snackbar;

import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import static android.content.Context.MODE_PRIVATE;
import static java.lang.Math.max;

/**
 * Created by Aksenov Vladimir on 21.03.2017.
 */

public class TranslateScreen {
    private Context context;//Main context
    GlobalData myApp;

    private View TranslateScreen;//This is view with translate UI. Toolbar with spinner,edittext for translate and etc.
    EditTextWithButtons editTextInputFrazeForTranslate;//Edit text for translate
    private ProgressBar TranslateProgressBar;//show progress awhile get translate
    private Spinner From, To;//Spinner with languages
    private TextToSpeech SpeechEngine;
    ImageButton AddToFavourite, CopyButton, SpeakButton;
    Snackbar ErrorSnackBar;
    Locale[] TTSLocaleAvailable;//Locale available list for speech
    ListView TranslateList;

    private String TranslateKey = "trnsl.1.1.20170317T194851Z.7290772f805654c7.a98ea948b9c347a56f7f52241456de7cb8b4b8cc";
    private int FromLang = 0, ToLang = 0;//index language position from spinner

    private Map<String, String> LanguagesName = new HashMap<>();// Get language name by code. Exmpl: "ru"->"Russian"
    String[] LangsTranslateNameSpinner = new String[0];//Languages list name for spinner
    String[] LangsCodeSpinner = new String[0];//Languages list code for spinner(as mask)
    String[] LanguagesDictionaryAvailable;

    boolean IsTranslateEnable = true;
    boolean InFavourite = false;//show favourite status fraze from EditText

    DatabaseWork db;//Database
    YandexDictionaryWork.WordDescription[] FrazeTranslate = new YandexDictionaryWork.WordDescription[0];//fraze translate(Include detail translate if available)
    TranslateListViewAdapter TranslateAdapter;//ListView adapter with fraze translate

    TranslateScreen(Context context) {
        this.context = context;
        myApp = (GlobalData) getActivity().getApplication();
        TranslateScreen = View.inflate(context, R.layout.translate_page, null);
        TranslateProgressBar = (ProgressBar) TranslateScreen.findViewById(R.id.translate_progresss_bar);
        TranslateList = (ListView) TranslateScreen.findViewById(R.id.translate_elements_listview);
        CopyButton = (ImageButton) TranslateScreen.findViewById(R.id.copy_translate);
        SpeakButton = (ImageButton) TranslateScreen.findViewById(R.id.speech_btn);

        db = myApp.db;

        StartInit();
    }

    /**
     * @return translate screen as a view
     */
    public View getView() {
        return TranslateScreen;
    }

    /**
     * @return StartScreen Activity
     */
    private Activity getActivity() {
        return ((Activity) context);
    }

    /**
     * Ecxhange languages for translate and automatically translate text again
     */
    private void ExchangeTranslateLanguages() {
        int p = FromLang;
        FromLang = ToLang;
        ToLang = p;
        if (IsTranslateEnable && myApp.AUTOTRANSLATE_ENABLE) {
            IsTranslateEnable = false;
            From.setSelection(FromLang, true);
            To.setSelection(ToLang, true);
            TranslateWork();
        }
        if (!myApp.AUTOTRANSLATE_ENABLE) {
            From.setSelection(FromLang, true);
            To.setSelection(ToLang, true);
        }

    }

    /**
     * start broadcast speech to text. Result process in StartScreen
     */
    private void promptSpeechInput(Locale locale) {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.getLanguage());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                    LanguageWork.GetResourceString(context, "say"));
            getActivity().startActivityForResult(intent, 200);
        } catch (ActivityNotFoundException a) {
            Logs.SaveLog(context, a);
            Toast.makeText(context, LanguageWork.GetResourceString(context, "forbidden_option"), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Start load languages list for translate and init languages spinners. If the application loads the data before, then this data will be used
     */
    private void InitLanguagesTranslateList() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TranslateLanguagesListLoad translateLanguagesListLoad = new TranslateLanguagesListLoad(context);
                translateLanguagesListLoad.execute("https://translate.yandex.net/api/v1.5/tr.json/getLangs");
            }
        });

    }

    /**
     * Start component init(initialization)
     */
    private void StartInit() {
        //Init speech object
        SpeechEngine = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    SpeechEngine.setLanguage(Locale.UK);
                }
            }
        });

        ImageButton ExchangeBtn = (ImageButton) TranslateScreen.findViewById(R.id.exchange_languages);
        ExchangeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ExchangeTranslateLanguages();
            }
        });


        CopyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CopyTextToClipBoard(GetTranslate());
            }
        });
        SpeakButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (SpeechEngine.isSpeaking())
                    SpeechEngine.stop();
                else {
                    try {
                        TextToSpeech(GetTranslate(), new Locale(LangsCodeSpinner[ToLang]));
                    } catch (Exception e) {
                        Logs.SaveLog(context, e);
                        Toast.makeText(context, LanguageWork.GetResourceString(context, "forbidden_option"), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        editTextInputFrazeForTranslate = (EditTextWithButtons) TranslateScreen.findViewById(R.id.translate_input);
        AddToFavourite = (ImageButton) TranslateScreen.findViewById(R.id.add_favourite_button);
        TranslateAdapter = new TranslateListViewAdapter(context);
        TranslateList.setAdapter(TranslateAdapter);
        MyUtility.setListViewHeightBasedOnChildren(TranslateList);

        TranslateButtonController();
        InitLanguagesTranslateList();

    }

    String GetTranslate() {
        try {
            if (FrazeTranslate[0].tr == null)
                return FrazeTranslate[0].word.text;
            else
                return FrazeTranslate[0].tr[0].word.text;
        }
        catch (Exception e)
        {
            Logs.SaveLog(context,e);
            return "";
        }
    }

    /**
     * Load available locale for Speech and initialization data for spinner with languages list
     */
    private void InitAvailableLocalList() {
        Locale loc = new Locale("en");
        TTSLocaleAvailable = loc.getAvailableLocales();
        int i = 0;
        LangsTranslateNameSpinner = new String[LanguagesName.size()];
        LangsCodeSpinner = new String[LanguagesName.size()];
        for (String key : LanguagesName.keySet()) {
            LangsTranslateNameSpinner[i] = LanguagesName.get(key);
            LangsCodeSpinner[i] = key;
            if (LangsCodeSpinner[i].equals(myApp.LANGUAGE_START_CODE)) {
                FromLang = i;
            }
            if (LangsCodeSpinner[i].equals(myApp.LAST_LANGUAGE_TRANSLATE_CODE)) {
                ToLang = i;
            }
            i++;

        }
    }

    private void CopyTextToClipBoard(String text) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(text);
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
            clipboard.setPrimaryClip(clip);
        }
        Toast.makeText(context, LanguageWork.GetResourceString(context, "copy_text"), Toast.LENGTH_SHORT).show();
    }

    /**
     * If list with available language empty show error and offer to load this list
     */
    void CheckLanguagesListCorrect() {
        if (LangsTranslateNameSpinner.length == 0) {
            ShowErrorMessage(LanguageWork.GetResourceString(context, "languages_list_not_available"), LanguageWork.GetResourceString(context, "download"), new Runnable() {
                @Override
                public void run() {
                    InitLanguagesTranslateList();
                }
            });
        }
    }

    /**
     * Init toolbar with spinner. Spinner contains languages list for translate function. Contains text change listener.
     */
    private void ToolBarController() {
        CheckLanguagesListCorrect();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, LangsTranslateNameSpinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        From = (Spinner) TranslateScreen.findViewById(R.id.language_from);
        From.setAdapter(adapter);
        From.setSelection(FromLang, true);
        From.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                FromLang = position;
                if (IsTranslateEnable && myApp.AUTOTRANSLATE_ENABLE) {
                    IsTranslateEnable = false;
                    TranslateWork();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });


        ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, LangsTranslateNameSpinner);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        To = (Spinner) TranslateScreen.findViewById(R.id.language_to);
        To.setAdapter(adapter2);
        To.setSelection(ToLang, true);
        To.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                ToLang = position;

                final SharedPreferences sPref = getActivity().getPreferences(MODE_PRIVATE);
                SharedPreferences.Editor ed = sPref.edit();
                ed.putString("LAST_LANGUAGE_TRANSLATE_CODE", LangsCodeSpinner[ToLang]);
                ed.commit();

                if (IsTranslateEnable && myApp.AUTOTRANSLATE_ENABLE) {
                    IsTranslateEnable = false;
                    TranslateWork();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }

    /**
     * Create custom edittext with right button and Runnable action
     */
    private void EditTextControl() {
        editTextInputFrazeForTranslate.defineButton(
                new MyPair<Drawable, Runnable>()
                        .mp(ResourcesCompat.getDrawable(getActivity().getResources(), R.drawable.ic_volume_up_blue_grey_300_36dp, null), Speech()),
                new MyPair<Drawable, Runnable>()
                        .mp(ResourcesCompat.getDrawable(getActivity().getResources(), R.drawable.ic_mic_blue_grey_300_36dp, null), Record()),
                new MyPair<Drawable, Runnable>()
                        .mp(ResourcesCompat.getDrawable(getActivity().getResources(), R.drawable.ic_close_blue_grey_300_36dp, null), Clear()));

        editTextInputFrazeForTranslate.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            private Timer timer = new Timer();
            private final long DELAY = 1300;

            @Override
            public void afterTextChanged(Editable s) {
                timer.cancel();
                timer = new Timer();
                timer.schedule(
                        new TimerTask() {
                            @Override
                            public void run() {
                                if (IsTranslateEnable && myApp.AUTOTRANSLATE_ENABLE) {
                                    IsTranslateEnable = false;
                                    TranslateWork();

                                }
                            }
                        },
                        DELAY);
            }
        });
    }

    /**
     * Create translate query
     */
    private void TranslateWork() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TranslateLoad tl = new TranslateLoad(context);
                tl.execute("https://translate.yandex.net/api/v1.5/tr.json/translate",
                        editTextInputFrazeForTranslate.getEditText().getText().toString());

            }
        });
    }

    /**
     * Play speech button action for editTextInputFrazeForTranslate
     *
     * @return
     */
    private Runnable Speech() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    if (SpeechEngine.isSpeaking())
                        SpeechEngine.stop();
                    else
                        TextToSpeech(editTextInputFrazeForTranslate.getEditText().getText().toString(), new Locale(LangsCodeSpinner[FromLang]));
                } catch (Exception e) {
                    Logs.SaveLog(context, e);
                    Toast.makeText(context, LanguageWork.GetResourceString(context, "forbidden_option"), Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    /**
     * clear editTextInputFrazeForTranslate action
     *
     * @return
     */
    private Runnable Clear() {
        return new Runnable() {
            @Override
            public void run() {
                editTextInputFrazeForTranslate.getEditText().setText("");
                FrazeTranslate = new YandexDictionaryWork.WordDescription[0];
                TranslateAdapter.notifyDataSetChanged();
                MyUtility.setListViewHeightBasedOnChildren(TranslateList);
                SetFavouriteCheck(false);
            }
        };
    }

    /**
     * Record speech button action for editTextInputFrazeForTranslate
     *
     * @return
     */
    private Runnable Record() {
        return new Runnable() {
            @Override
            public void run() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            promptSpeechInput(new Locale(LangsCodeSpinner[FromLang]));
                        } catch (Exception e) {
                            Logs.SaveLog(context, e);
                            Toast.makeText(context, LanguageWork.GetResourceString(context, "forbidden_option"), Toast.LENGTH_SHORT).show();
                        }

                    }
                });

            }
        };
    }

    /**
     * @param data string for say
     */
    private void TextToSpeech(String data, Locale locale) {
        try {
            //data = MyUtility.StringNormalize(data);
            String utteranceId = this.hashCode() + "";
            SpeechEngine.setLanguage(locale);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                SpeechEngine.speak(data, TextToSpeech.QUEUE_FLUSH, null, utteranceId);

            } else {
                HashMap<String, String> map = new HashMap<>();
                map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "MessageId");
                SpeechEngine.speak(data, TextToSpeech.QUEUE_FLUSH, map);
            }
        } catch (Exception e) {
            Logs.SaveLog(context, e);
            Toast.makeText(context, LanguageWork.GetResourceString(context, "forbidden_option"), Toast.LENGTH_LONG).show();
        }
    }


    private void ShowTranslateResult() {

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TranslateAdapter.notifyDataSetChanged();
                MyUtility.setListViewHeightBasedOnChildren(TranslateList);
            }
        });
    }

    /**
     * Check current fraze from EditTextInputFrazeForTranslate in favourite status
     *
     * @param fraze
     * @param type
     */
    void FavoriteButtonInit(String fraze, String type) {
        if (!fraze.equals("") && IsFrazeInFavourite(fraze, type)) {
            InFavourite = true;
            SetFavouriteCheck(true);
        } else {
            InFavourite = false;
            SetFavouriteCheck(false);
        }
    }

    /**
     * @param data current record from history
     */
    private void FavouriteButtonController(final ArrayList<String> data) {
        final String fraze = data.get(2);
        final String type = data.get(3);
        FavoriteButtonInit(fraze, type);
        AddToFavourite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = String.valueOf(editTextInputFrazeForTranslate.getEditText().getText());
                if (text.equals("")) {
                    InFavourite = false;
                    SetFavouriteCheck(false);
                    return;
                }
                if (InFavourite) {
                    UpdateTranslateFavouriteStatus(data, false);
                    InFavourite = false;
                    SetFavouriteCheck(false);
                } else {

                    UpdateTranslateFavouriteStatus(data, true);
                    InFavourite = true;
                    SetFavouriteCheck(true);
                }
            }
        });
    }

    void SetFavouriteCheck(final boolean Check) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (Check) {
                    AddToFavourite.setImageResource(R.drawable.ic_bookmark_red_a700_36dp);
                } else {
                    AddToFavourite.setImageResource(R.drawable.ic_bookmark_black_36dp);
                }
            }
        });

    }

    /**
     * @param fraze
     * @param type  language pair format "ru-en" and etc.
     * @return
     */
    private boolean IsFrazeInFavourite(String fraze, String type) {

        String db_name = "Cache";
        String TableName = "History";
        String Condition = String.format("Input = '%1$s' AND Favourite = '%2$s' AND Type = '%3$s'", fraze.replace("'","''"), "1", type);
        ArrayList<ArrayList<String>> d = db.GetRecordFromDatabase(db_name, TableName, Condition, -1, "");
        if (d.size() == 0)
            return false;
        return true;
    }

    /**
     * @param data  Record from history
     * @param check set favourite status flag
     */
    private void UpdateTranslateFavouriteStatus(ArrayList<String> data, boolean check) {
        String db_name = "Cache";
        String TableName = "History";
        ArrayList<MyPair<String, String>> cc = new ArrayList<>();

        String fraze = data.get(2).replace("'","''");
        String Type = data.get(3);
        if (check) {
            cc.add(new MyPair<String, String>().mp("Favourite", "1"));
            data.set(5, "1");
        } else {

            cc.add(new MyPair<String, String>().mp("Favourite", "0"));

        }
        db.UpdateDatabaseRecord(db_name, TableName, cc, new String[]{"text"}, "Input = '" + fraze + "' AND Type = '" + Type + "'");
        //Update favourite and history list with changes
        myApp.getHistoryAndFavouriteScreen().HistoryData = db.GetRecordFromDatabase(db_name, TableName, "", -1, "CreateTime DESC");
        myApp.getHistoryAndFavouriteScreen().FavouriteData = db.GetRecordFromDatabase(db_name, TableName, "Favourite = '1'", -1, "CreateTime DESC");
    }

    public long GetUnixTime() {
        Calendar calendar = Calendar.getInstance();
        long now = calendar.getTimeInMillis();
        long utc = (int) (now / 1000);
        return (utc);

    }

    /**
     * @param JSONResult json translate answer from server
     * @param input      text from EditTextInputForTranslate
     * @param type       language pair form "ru-en" and etc.
     * @return record which was added in database
     * @throws Exception
     */
    private ArrayList<String> AddTranslateToHistory(String JSONResult, String input, String type) throws Exception {
        try {
            MyPair<String, String> data = new MyPair<String, String>();
            JSONObject jsonResponse = new JSONObject(JSONResult);

            String db_name = "Cache";
            String TableName = "History";
            String Create_query = "create table "
                    + TableName + " (" + BaseColumns._ID
                    + " integer primary key autoincrement, " + "Translate"
                    + " text not null, " + "Input " + "text not null, " + "Type " + "text not null, " + "CreateTime " + "text not null, " + "Favourite " + "text not null" + ");";

            ArrayList<MyPair<String, String>> cc = new ArrayList<>();

            String Translate = (String) jsonResponse.getJSONArray("text").get(0);
            String time = String.valueOf(GetUnixTime());
            cc.add(data.mp("Translate", Translate));
            cc.add(data.mp("Input", input));
            cc.add(data.mp("Type", type));

            cc.add(data.mp("CreateTime", time));
            cc.add(data.mp("Favourite", "0"));


            if (!db.isTableExists(db_name, TableName))
                db.CreateDatabase(db_name, Create_query);
            long id = 0;
            id = db.InsertDataInTable(db_name, TableName, cc);
            ArrayList<ArrayList<String>> Data = db.GetRecordFromDatabase(db_name, TableName, "_id = " + String.valueOf(id), -1, "");
            myApp.getHistoryAndFavouriteScreen().HistoryData.add(0, Data.get(0));
            return Data.get(0);
        } catch (Exception e) {
            Logs.SaveLog(context, e);
            throw e;
        }
    }

    /**
     * If AUTOTRANSLATE_ENABLE = false this button used for translate edittext text
     */
    private void TranslateButtonController() {
        Button TranslateButton = (Button) TranslateScreen.findViewById(R.id.translate_button);
        if (myApp.AUTOTRANSLATE_ENABLE) {
            MyUtility.removeView(TranslateButton);
            return;
        }

        TranslateButton.setText(LanguageWork.GetResourceString(context, "translate"));
        TranslateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (IsTranslateEnable) {
                    IsTranslateEnable = false;
                    TranslateWork();
                }
            }
        });
    }

    /**
     * @return language pair format "ru-en" and etc.
     */
    public String GetLanguagesType() {
        String ans = "";
        try {
            ans = LangsCodeSpinner[FromLang] + "-" + LangsCodeSpinner[ToLang];
        } catch (Exception e) {
            Logs.SaveLog(context, e);
        }
        return ans;
    }


    /**
     * This method use for notify user about error
     *
     * @param Message
     * @param ButtonText
     * @param action
     */
    private void ShowErrorMessage(String Message, String ButtonText, final Runnable action) {

        ErrorSnackBar = Snackbar.make(getActivity().findViewById(R.id.activity_start_screen), Message, Snackbar.LENGTH_INDEFINITE);
        ErrorSnackBar.setAction(ButtonText, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action.run();
                ErrorSnackBar.dismiss();
            }
        });

        View snackbarView = ErrorSnackBar.getView();
        snackbarView.setBackgroundColor(Color.WHITE);
        TextView textView = (TextView) snackbarView.findViewById(R.id.snackbar_text);
        textView.setMaxLines(4);
        ErrorSnackBar.show();

    }

    /**
     *
     * @param type string format ru-en and etc.
     * @return true if detail translate available for this language pair
     */
    boolean IsYandexDictionaryAvailAbleForThisLanguagePair(String type) {
        for (int i = 0; i < LanguagesDictionaryAvailable.length; i++)
            if (LanguagesDictionaryAvailable[i].equals(type))
                return true;
        return false;
    }

    /**
     * Class for get translate fraze
     */
    private class TranslateLoad extends AsyncTask<String, String, String> {
        Context context;
        String input;

        public TranslateLoad(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... params) {
            YandexDictionaryWork yandexDictionaryWork = new YandexDictionaryWork(context);
            if (params[1].isEmpty())
                return "";
            int WordsCount = MyUtility.GetWordCount(params[1]);
            String type = GetLanguagesType();
            boolean IsYandexDictionaryAvailAbleForThisLanguagePair = IsYandexDictionaryAvailAbleForThisLanguagePair(type);
            if (WordsCount == 1) {

                //get detail translate
                if(IsYandexDictionaryAvailAbleForThisLanguagePair)
                    FrazeTranslate = yandexDictionaryWork.LookUp(params[1].replace(" ", ""),type);
            }
            try {
                DefaultHttpClient hc = new DefaultHttpClient();
                ResponseHandler<String> res = new BasicResponseHandler();
                HttpPost postMethod = new HttpPost(params[0]);
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
                nameValuePairs.add(new BasicNameValuePair("key", TranslateKey));
                nameValuePairs.add(new BasicNameValuePair("text", MyUtility.convertToUTF8(params[1])));
                input = params[1];
                nameValuePairs.add(new BasicNameValuePair("lang", type));
                postMethod.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                String response = hc.execute(postMethod, res);

                ArrayList<String> data = AddTranslateToHistory(response, input, type);
                //If input is sentence or not available detail translate
                if (WordsCount > 1 || !IsYandexDictionaryAvailAbleForThisLanguagePair || FrazeTranslate.length == 0) {
                    FrazeTranslate = new YandexDictionaryWork.WordDescription[1];
                    YandexDictionaryWork.WordDescription el = new YandexDictionaryWork.WordDescription();
                    el.ElementCount = 1;
                    el.word = yandexDictionaryWork.GetWord(data.get(1));
                    FrazeTranslate[0] = el;
                }
                //check favourite status for current fraze
                FavouriteButtonController(data);
                if (ErrorSnackBar != null)
                    ErrorSnackBar.dismiss();
                return response;
            } catch (Exception e) {
                Logs.SaveLog(context, e);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!MyUtility.isOnline()) {
                            ShowErrorMessage(LanguageWork.GetResourceString(context, "internet_error"), LanguageWork.GetResourceString(context, "retry"), new Runnable() {
                                @Override
                                public void run() {
                                    TranslateWork();
                                }
                            });
                        } else
                            ShowErrorMessage(LanguageWork.GetResourceString(context, "undefined_exception"), LanguageWork.GetResourceString(context, "retry"), new Runnable() {
                                @Override
                                public void run() {
                                    TranslateWork();
                                }
                            });
                    }
                });
                return "";
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String result) {
            TranslateProgressBar.setVisibility(View.INVISIBLE);
            IsTranslateEnable = true;
            ShowTranslateResult();
            super.onPostExecute(result);
        }

        @Override
        protected void onPreExecute() {

            TranslateProgressBar.setIndeterminate(true);
            TranslateProgressBar.setVisibility(View.VISIBLE);
            super.onPreExecute();
        }
    }

    /**
     * Class for load(from SD card or API server if not exist in SD card) available languages for translate
     */
    private class TranslateLanguagesListLoad extends AsyncTask<String, String, String> {
        Context context;
        String CurrentLanguage = LanguageWork.GetLanguageNameById(myApp.LANGUAGE_CODE);

        public TranslateLanguagesListLoad(Context context) {
            this.context = context;
        }

        SharedPreferences sp;

        @Override
        protected String doInBackground(String... params) {
            try {
                //Load language list from cache
                sp = context.getSharedPreferences("Cache",
                        Context.MODE_PRIVATE);
                String LastResponseTranslateLanguage = sp.getString("AllLanguagesResponse", "");
                String LastResponseDictionaryLanguage = sp.getString("DictionaryAvailableLanguages", "");
                String Loc = sp.getString("Locale", "");

                //if cache not exist
                if (LastResponseTranslateLanguage.isEmpty() || !Loc.equals(CurrentLanguage)) {
                    LastResponseTranslateLanguage = LoadAllLanguages(params[0]);
                }
                ParseAllLang(LastResponseTranslateLanguage);
                if (LastResponseDictionaryLanguage.isEmpty() || !Loc.equals(CurrentLanguage)) {
                    LastResponseDictionaryLanguage = LoadDictionaryLanguages();
                }
                ParseDictionaryLang(LastResponseDictionaryLanguage);
                return "";

            } catch (Exception e) {
                Logs.SaveLog(context, e);
                return null;
            }
        }

        String LoadDictionaryLanguages() {
            YandexDictionaryWork yandexDictionaryWork = new YandexDictionaryWork(context);
            String rez = yandexDictionaryWork.GetSupportLangs();
            Editor e = sp.edit();
            e.putString("DictionaryAvailableLanguages", rez);
            e.putString("Locale", CurrentLanguage);
            e.commit();
            return rez;
        }

        String LoadAllLanguages(String URL) {
            try {
                DefaultHttpClient hc = new DefaultHttpClient();
                ResponseHandler<String> res = new BasicResponseHandler();
                HttpPost postMethod = new HttpPost(URL);
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
                nameValuePairs.add(new BasicNameValuePair("key", TranslateKey));
                nameValuePairs.add(new BasicNameValuePair("ui", CurrentLanguage));
                postMethod.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                String response = hc.execute(postMethod, res);
                Editor e = sp.edit();
                e.putString("AllLanguagesResponse", response);
                e.putString("Locale", CurrentLanguage);
                e.commit();
                return response;
            } catch (Exception e) {
                Logs.SaveLog(context, e);
                return "";
            }
        }

        public List<Object> toList(JSONArray array) throws JSONException {
            List<Object> list = new ArrayList<Object>();
            for (int i = 0; i < array.length(); i++) {
                Object value = array.get(i);
                if (value instanceof JSONArray) {
                    value = toList((JSONArray) value);
                } else if (value instanceof JSONObject) {
                    value = toMap((JSONObject) value);
                }
                list.add(value);
            }
            return list;
        }

        public Map<String, String> toMap(JSONObject object) throws JSONException {
            Map<String, String> map = new HashMap<String, String>();

            Iterator<String> keysItr = object.keys();
            while (keysItr.hasNext()) {
                String key = keysItr.next();
                Object value = object.get(key);

                if (value instanceof JSONArray) {
                    value = toList((JSONArray) value);
                } else if (value instanceof JSONObject) {
                    value = toMap((JSONObject) value);
                }
                map.put(key, (String) value);
            }
            return map;
        }

        /**
         * @param output Json string with languages list to Map
         */
        void ParseAllLang(String output) {

            try {
                JSONObject jsonResponse = new JSONObject(output);
                JSONObject langs = jsonResponse.getJSONObject("langs");
                LanguagesName = toMap(langs);
                InitAvailableLocalList();
            } catch (JSONException e) {
                Logs.SaveLog(context, e);
                e.printStackTrace();
            }

        }

        void ParseDictionaryLang(String output) {
            try {
                JSONArray jsonResponse = new JSONArray(output);
                LanguagesDictionaryAvailable = new String[jsonResponse.length()];
                for (int i = 0; i < jsonResponse.length(); i++) {
                    LanguagesDictionaryAvailable[i] = jsonResponse.getString(i);
                }
            } catch (JSONException e) {
                Logs.SaveLog(context, e);
                e.printStackTrace();
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            TranslateProgressBar.setIndeterminate(true);
            TranslateProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            TranslateProgressBar.setVisibility(View.INVISIBLE);
            ToolBarController();
            EditTextControl();

        }
    }

    /**
     * Detail word translate
     */
    public class TranslateListViewAdapter extends BaseAdapter {


        Map<Integer, Integer> TranslateNodes = new HashMap<>();//position where part of speech change
        ArrayList<MyPair<Integer, Integer>> CurrentNode = new ArrayList<>();//contains pair <parent,children>
        /**
         * Example tree
         *
         * Car - parent(number 0). Its <0,0> pair
         * |
         * ---автомобиль - children(number 0). Its <0,0> pair
         * |
         * ----->машина- children(number 1). Its <0,1> pair
         *
         * Cars - parent(number 1). Its <1,1> pair
         * |
         * ---автомобили - children(number 0). Its <1,0> pair
         * |
         * ----->машины- children(number 1). Its <1,1> pair
         */
        public Context context;
        int Count = 0;
        boolean[] ExampleShowStatus;//is example show in current list element

        public TranslateListViewAdapter(Context context) {
            this.context = context;

        }

        @Override
        public int getCount() {
            Count = 0;
            TranslateNodes.clear();
            CurrentNode.clear();
            if(FrazeTranslate == null)
                return 0;
            for (int i = 0; i < FrazeTranslate.length; i++) {
                TranslateNodes.put(Count, i);
                CurrentNode.add(new MyPair<Integer, Integer>().mp(i, i));
                for (int j = Count; j < Count + FrazeTranslate[i].ElementCount - 1; j++) {
                    CurrentNode.add(new MyPair<Integer, Integer>().mp(j - Count, i));
                }
                Count += FrazeTranslate[i].ElementCount;
            }
            //For not show license screen
            if(Count == 0)
                return 0;
            return Count+1;
        }

        @Override
        public void notifyDataSetChanged() {
            ExampleShowStatus = new boolean[getCount()];
            super.notifyDataSetChanged();
            MyUtility.setListViewHeightBasedOnChildren(TranslateList);


        }

        /**
         * Update ListView Height when show example button click
         */
        public void ExpandExamples() {
            super.notifyDataSetChanged();
            MyUtility.setListViewHeightBasedOnChildren(TranslateList);

        }
        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        int GetLayoutType(int pos) {
            if(pos == Count)
                return 2;//Licence element
            if (TranslateNodes.containsKey(pos))//Tranlate element. Format: translate,transcription,path of speech
                return 0;
            else
                return 1;//Translates sync element. Format: translates,genus
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View resource = null;
            switch (GetLayoutType(position)) {
                case 0:
                    resource = GetMainTranslateLayout(TranslateNodes.get(position));
                    //Copy current Text To Clip Board
                    resource.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            String text = FrazeTranslate[TranslateNodes.get(position)].word.text;
                            CopyTextToClipBoard(text);
                            return false;
                        }
                    });
                    break;
                case 1:
                    resource =  GetSubTranslateLayout(CurrentNode.get(position).first, CurrentNode.get(position).second,position);
                    //Copy current Text To Clip Board
                    resource.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            String text = FrazeTranslate[CurrentNode.get(position).second].tr[CurrentNode.get(position).first].word.text;
                            CopyTextToClipBoard(text);
                            return false;
                        }
                    });
                    break;
                case 2:
                    //Create Yandex Licence view
                    final TextView message = new TextView(context);
                    message.setPadding(5,5,5,5);
                    message.setLinkTextColor(context.getResources().getColor(R.color.colorPrimaryDark));
                    final SpannableString s =
                           new SpannableString(LanguageWork.GetResourceString(context,"API_licence"));
                    Linkify.addLinks(s, Linkify.WEB_URLS);
                    message.setText(s);
                    message.setMovementMethod(LinkMovementMethod.getInstance());
                    resource = message;
                    break;
            }

            return resource;
        }
        //Tranlate element. Format: translate,transcription,path of speech
        View GetMainTranslateLayout(int NodePosition) {
            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout tr = new LinearLayout(context);
            tr.setOrientation(LinearLayout.HORIZONTAL);
            TextView translate = new TextView(context);
            translate.setText(FrazeTranslate[NodePosition].word.text + " ");
            TextView pos = new TextView(context);
            pos.setTextColor(getActivity().getResources().getColor(R.color.Green));
            pos.setText("");
            if(FrazeTranslate[NodePosition].word.pos!=null)
                pos.setText(FrazeTranslate[NodePosition].word.pos + " ");
            TextView ts = new TextView(context);
            ts.setTextColor(getActivity().getResources().getColor(R.color.Grey));
            ts.setText("");
            if(FrazeTranslate[NodePosition].word.ts != null)
                ts.setText("[" + FrazeTranslate[NodePosition].word.ts + "] ");
            tr.addView(translate);
            tr.addView(ts);
            tr.addView(pos);
            linearLayout.addView(tr);
            linearLayout.setPadding(5, 5, 5, 5);
            return linearLayout;
        }
        //Translates sync element. Format: translates,genus, means
        View GetSubTranslateLayout(int num, int NodePosition, final int AbsolutePosition) {
            LinearLayout main = new LinearLayout(context);
            main.setOrientation(LinearLayout.VERTICAL);

            final LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setOrientation(LinearLayout.VERTICAL);

            LinearLayout tr = new LinearLayout(context);
            tr.setOrientation(LinearLayout.HORIZONTAL);

            TextView Num = new TextView(context);
            Num.setText(String.valueOf(num+1));
            Num.setTextColor(getActivity().getResources().getColor(R.color.Grey));
            Num.setPadding(0, 0, 15, 0);
            tr.addView(Num);

            TextView translate = new TextView(context);
            translate.setTextColor(getActivity().getResources().getColor(R.color.Blue));
            String syn_string = "";
            syn_string += FrazeTranslate[NodePosition].tr[num].word.text;
            if(FrazeTranslate[NodePosition].tr[num].word.gen!=null)
                syn_string += " ("+ FrazeTranslate[NodePosition].tr[num].word.gen + ")";
            if (FrazeTranslate[NodePosition].tr != null && FrazeTranslate[NodePosition].tr[num].syn != null) {
                syn_string += ", ";
                for (int i = 0; i < FrazeTranslate[NodePosition].tr[num].syn.length; i++) {
                    syn_string += FrazeTranslate[NodePosition].tr[num].syn[i].text;
                    if (FrazeTranslate[NodePosition].tr[num].syn[i].gen != null)
                        syn_string += " (" + FrazeTranslate[NodePosition].tr[num].syn[i].gen + ")";
                    if (i + 1 != FrazeTranslate[NodePosition].tr[num].syn.length)
                        syn_string += ", ";
                }
            }
            translate.setText(syn_string);

            TextView means = new TextView(context);
            String mean_string="";
            if (FrazeTranslate[NodePosition].tr != null && FrazeTranslate[NodePosition].tr[num].mean != null) {
                mean_string = "(";
                for (int i = 0; i < FrazeTranslate[NodePosition].tr[num].mean.length; i++) {
                    mean_string += FrazeTranslate[NodePosition].tr[num].mean[i].text;
                    if (i + 1 != FrazeTranslate[NodePosition].tr[num].mean.length)
                        mean_string += ", ";
                }
                mean_string += ")";
            }
            means.setText(mean_string);


            linearLayout.addView(translate);
            if(!means.getText().equals(""))
                linearLayout.addView(means);
            tr.addView(linearLayout);
            if(FrazeTranslate[NodePosition].tr[num].ex!=null)
            {

                Button ShowExample = new Button(context);
                ShowExample.setMinWidth(0);
                ShowExample.setMinHeight(0);
                ShowExample.setText(LanguageWork.GetResourceString(context,"show_examples"));
                ShowExample.setBackground(getActivity().getResources().getDrawable(R.drawable.selector));
                ShowExample.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));


                final TextView Examples = new TextView(context);
                Examples.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                String ExamplesString = "";
                for(int i=0;i<FrazeTranslate[NodePosition].tr[num].ex.length;i++)
                {
                    ExamplesString+=FrazeTranslate[NodePosition].tr[num].ex[i].text+" - "+FrazeTranslate[NodePosition].tr[num].ex[i].translate+"\n";
                }
                linearLayout.addView(ShowExample);
                Examples.setText(ExamplesString);
                if(ExampleShowStatus[AbsolutePosition])
                {
                    linearLayout.addView(Examples);

                }
                else
                {
                    try {
                        linearLayout.removeView(Examples);
                    }
                    catch (Exception e)
                    {}
                }

                ShowExample.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ExampleShowStatus[AbsolutePosition] = !ExampleShowStatus[AbsolutePosition];
                        TranslateAdapter.ExpandExamples();
                    }
                });
            }
            tr.setPadding(5, 5, 5, 5);
            return tr;
        }
    }
}

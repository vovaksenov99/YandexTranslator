package com.application.akscorp.yandextranslator2017;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.application.akscorp.yandextranslator2017.Utility.MyUtility;

import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by AksCorp on 15.04.2017.
 */

public class YandexDictionaryWork {
    Context context;
    String DictionaryKey = "dict.1.1.20170415T045540Z.ff55a9db6d2eecfd.49029a6d3c5ee74005a9fecced182c69f4b012d7";
    public GlobalData myApp;

    YandexDictionaryWork(Context context) {
        this.context = context;
        myApp = (GlobalData) getActivity().getApplication();
    }

    /**
     * @return Get Activity
     */
    private Activity getActivity() {
        return ((Activity) context);
    }

    /**
     * Get support languages for detail translate
     *
     * @return
     */
    String GetSupportLangs() {
        String Languages = "";
        String URL = "https://dictionary.yandex.net/api/v1/dicservice.json/getLangs";
        try {
            DefaultHttpClient hc = new DefaultHttpClient();
            ResponseHandler<String> res = new BasicResponseHandler();
            HttpPost postMethod = new HttpPost(URL);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
            nameValuePairs.add(new BasicNameValuePair("key", DictionaryKey));
            postMethod.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            String response = hc.execute(postMethod, res);
            Languages = response;
            return Languages;
        } catch (Exception e) {
            Logs.SaveLog(context, e);

        }
        return Languages;
    }

    /**
     * Get detail word translate. Return null if detail translate not available
     *
     * @param word
     * @param lang
     * @return
     */
    WordDescription[] LookUp(String word, String lang) {
        String URL = "https://dictionary.yandex.net/api/v1/dicservice.json/lookup";
        try {
            DefaultHttpClient hc = new DefaultHttpClient();
            ResponseHandler<String> res = new BasicResponseHandler();
            HttpPost postMethod = new HttpPost(URL);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
            nameValuePairs.add(new BasicNameValuePair("key", DictionaryKey));
            nameValuePairs.add(new BasicNameValuePair("text", MyUtility.convertToUTF8(word)));
            nameValuePairs.add(new BasicNameValuePair("ui", LanguageWork.GetLanguageNameById(myApp.LANGUAGE_CODE)));
            nameValuePairs.add(new BasicNameValuePair("lang", lang));
            postMethod.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            String response = hc.execute(postMethod, res);
            return ParseWordInformation(response);
        } catch (Exception e) {
            Logs.SaveLog(context, e);
        }
        return null;
    }

    /**
     * Convert JSONObject to instance of a Word class
     *
     * @param JsonWord
     * @return instance of a Word class
     */
    Word GetWord(JSONObject JsonWord) {
        Word word = new Word();
        try {
            if (JsonWord.has("text"))
                word.text = JsonWord.getString("text");
            if (JsonWord.has("pos"))
                word.pos = JsonWord.getString("pos");
            if (JsonWord.has("ts"))
                word.ts = JsonWord.getString("ts");
            if (JsonWord.has("gen"))
                word.gen = JsonWord.getString("gen");
        } catch (Exception e) {
            Logs.SaveLog(context, e);
        }
        return word;
    }

    Word GetWord(String translate) {
        Word word = new Word();
        word.text = translate;
        return word;
    }

    /**
     * Parse JSON data to classes array
     *
     * @param json Json string with word describe
     */
    WordDescription[] ParseWordInformation(String json) {
        try {
            JSONObject Main = new JSONObject(json);
            JSONArray Def = Main.getJSONArray("def");
            WordDescription[] words = new WordDescription[Def.length()];

            for (int i = 0; i < Def.length(); i++) {
                JSONObject current = Def.getJSONObject(i);

                WordDescription CurDescription = new WordDescription();
                CurDescription.word = GetWord(current);
                CurDescription.ElementCount++;

                JSONArray AltrenativeTranslate = current.getJSONArray("tr");
                CurDescription.tr = new TranslateList[AltrenativeTranslate.length()];
                for (int j = 0; j < AltrenativeTranslate.length(); j++) {
                    JSONObject TrElement = AltrenativeTranslate.getJSONObject(j);
                    TranslateList tr = new TranslateList();
                    tr.word = GetWord(TrElement);
                    CurDescription.ElementCount++;

                    if (TrElement.has("syn")) {
                        JSONArray syn = TrElement.getJSONArray("syn");
                        tr.syn = new Word[syn.length()];
                        for (int k = 0; k < syn.length(); k++) {
                            JSONObject msyn = syn.getJSONObject(k);
                            tr.syn[k] = GetWord(msyn);
                        }
                    }
                    if (TrElement.has("mean")) {
                        JSONArray mean = TrElement.getJSONArray("mean");
                        tr.mean = new Word[mean.length()];
                        for (int k = 0; k < mean.length(); k++) {
                            JSONObject mmean = mean.getJSONObject(k);
                            tr.mean[k] = GetWord(mmean);
                        }
                    }
                    if (TrElement.has("ex")) {
                        JSONArray ex = TrElement.getJSONArray("ex");
                        tr.ex = new UsedExample[ex.length()];
                        for (int k = 0; k < ex.length(); k++) {
                            JSONObject mex = ex.getJSONObject(k);
                            UsedExample exx = new UsedExample();
                            exx.text = mex.getString("text");
                            if (mex.has("tr") && mex.getJSONArray("tr").length() > 0)
                                exx.translate = mex.getJSONArray("tr").getJSONObject(0).getString("text");
                            tr.ex[k] = exx;
                        }
                    }

                    CurDescription.tr[j] = tr;
                }
                words[i] = CurDescription;

            }
            return words;
        } catch (Exception e) {
            Logs.SaveLog(context, e);
        }
        return null;
    }


    /**
     * Classes for JSON parsing
     */
    public class UsedExample {
        String text, translate;
    }

    public class Word {
        String text;
        String pos, ts, gen;
    }

    public class TranslateList {
        Word word;
        Word[] syn;
        Word[] mean;
        UsedExample[] ex;
    }

    public static class WordDescription {
        int ElementCount = 0;
        Word word;
        TranslateList[] tr;

    }
}

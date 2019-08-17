package com.application.akscorp.yandextranslator2017;

import android.content.Context;
import android.util.Log;

import com.application.akscorp.yandextranslator2017.Utility.MyUtility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by vovaa on 01.04.2017.
 */

public class Logs{

    public static void SaveLog(Context context,String message)
    {
        String path = MyUtility.GetApplicationDirectory(context);
        String date = MyUtility.GetCurrentDate();
        File logFile = new File(path+"/Logs.txt");
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();
            }
            catch (IOException e)
            {
                Log.i(context.getPackageName(),e.toString());
                e.printStackTrace();
            }
        }
        try
        {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(date+message);
            buf.newLine();
            buf.close();
            Log.i(context.getPackageName(),message);
        }
        catch (IOException e)
        {
            Log.i(context.getPackageName(),e.toString());
            e.printStackTrace();
        }
    }

    public static void SaveLog(Context context,Exception exception)
    {
        String path = MyUtility.GetApplicationDirectory(context);
        String date = MyUtility.GetCurrentDate();
        File logFile = new File(path+"/Logs.txt");
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();
            }
            catch (IOException e)
            {
                Log.i(context.getPackageName(),e.toString());
                e.printStackTrace();
            }
        }
        try
        {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(date+exception.toString());
            buf.newLine();
            buf.close();
            Log.i(context.getPackageName(),exception.toString());
        }
        catch (IOException e)
        {
            Log.i(context.getPackageName(),e.toString());
            e.printStackTrace();
        }

    }
}

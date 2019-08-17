package com.application.akscorp.yandextranslator2017.DatabaseWork;

/**
 * Created by vovaaksenov99 on 13.04.2015.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;


import com.application.akscorp.yandextranslator2017.Logs;
import com.application.akscorp.yandextranslator2017.Utility.ArrayListOperations;
import com.application.akscorp.yandextranslator2017.Utility.MyPair;

import java.util.ArrayList;


public class DatabaseWork<T> {

    private DatabaseHelper mDatabaseHelper;
    Context context;

    public DatabaseWork(Context context) {
        super();
        this.context = context;
    }

    /**
     * DatabaseName - имя создаваемой бд
     * <p/>
     * Query - сформированный SQL запрос на создание бд
     */
    public void CreateDatabase(String DatabaseName, String Query) {

        try {
            mDatabaseHelper.DATABASE_CREATE_SCRIPT = Query;//запрос на создание бд
            mDatabaseHelper = new DatabaseHelper(context, DatabaseName + ".db", null, 1);
        }
        catch (Exception e)
        {
            Log.i("","");
        }

    }

    public void CreateTable(String DatabaseName, String Query) {
        try {
            DatabaseHelper mDatabaseHelper = new DatabaseHelper(context, DatabaseName + ".db", null, 1);//получение расположения бд
            SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();//получение бд
            mDatabaseHelper.CreateTable(db, Query);
        }
        catch (Exception e)
        {
            Logs.SaveLog(context,e);
        }

    }

    /**
     * DatabaseName - имя бд
     * <p/>
     * TableName - имя таблицы для вставки
     * <p/>
     * RecordList - массив пар. Содержит 2 значения.
     * 1)Названия столбца
     * 2)Значение которое необходимо вставить
     * Например:
     * StringPair.mp("FirstCollum","1")
     * StringPair.mp("SecondCollum","Mordor")
     * Значения будут автоматически преобразованы в целочисленный или текстовой вид.
     */
    public long InsertDataInTable(String DatabaseName, String TableName, ArrayList<MyPair<String,String>> RecordList) {
        DatabaseHelper mDatabaseHelper = new DatabaseHelper(context, DatabaseName + ".db", null, 1);//получение расположения бд
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();//получение бд
        Cursor cursor = db.rawQuery("PRAGMA table_info(" + TableName + ")", null);//запрос на получение информации о бд
        ContentValues values = new ContentValues();//создание строки для вставки
        int typeIdx = cursor.getColumnIndexOrThrow("type");//получение столбца типа
        int pk = cursor.getColumnIndexOrThrow("pk");
        int id_current_record = 0;
        while (cursor.moveToNext()) {

            String type = cursor.getString(typeIdx).toUpperCase();//получение типа колонки в бд
            String Primary_key = cursor.getString(pk);
            if (Primary_key.equals("1"))
                continue;
            switch (type) {
                case "INTEGER":
                    try {
                        values.put(RecordList.get(id_current_record).first(), Integer.parseInt(RecordList.get(id_current_record).second()));
                    } catch (Exception e) {
                        Log.e("DatabaseWork", "InsertDataInTable " + e.toString());
                        Logs.SaveLog(context,e);
                    }
                    break;
                case "TEXT":
                    try {
                        values.put(RecordList.get(id_current_record).first(), RecordList.get(id_current_record).second());
                    } catch (Exception e) {
                        Log.e("DatabaseWork", "InsertDataInTable " + e.toString());
                        Logs.SaveLog(context,e);
                    }
                    break;
                default:
                    Log.e("DatabaseWork", "Unknown collum type");
                    break;
            }
            id_current_record++;
        }
        cursor.close();
        // Вставляем данные в таблицу
       return db.insert(TableName, null, values);

    }

    /**
     * Функия возвращает двумерный массив(Таблицу) - результат выборки по заданным пораметрам.
     * DatabaseName - имя бд
     * <p/>
     * TableName - имя таблицы для выборки
     * <p/>
     * CollumNameList - название колонки для получение данных из бд
     * <p/>
     * CollumTypes - тип i-той колонки бд
     * <p/>
     * Conditions - условие выборки данных. Аналог WHERE в SQL. (Например: FirstCollum > 0 AND SecondCollum = 'Yiff')
     * <p/>
     * LimitRecord - количество записей которое необходимо вернуть. Аналог LIMIT в SQL.
     * Если количество записей ограничивать не надо, то поле должно быть -1 или null.
     */
    public ArrayList<ArrayList<String>> GetRecordFromDatabase(String DatabaseName, String TableName, String Conditions, int LimitRecord, String OrderBy) {
        if (!isDatabaseExist(DatabaseName) || !isTableExists(DatabaseName, TableName)) {
            return new ArrayList<ArrayList<String>>();
        }
        try {
            ArrayList<ArrayList<String>> info = GetTableInfo(DatabaseName, TableName);
            String[] CollumNameList = new String[info.size()];
            for (int i = 0; i < info.size(); i++) {
                CollumNameList[i] = info.get(i).get(1);
            }

            DatabaseHelper mDatabaseHelper = new DatabaseHelper(context, DatabaseName + ".db", null, 1);//получение расположения бд
            SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
            Cursor cursor = db.query(TableName, CollumNameList,
                    Conditions, null,
                    null, null, OrderBy);

            //cursor.moveToFirst();
            ArrayList<ArrayList<String>> output = new ArrayList<>();
            int count = 0;
            while (cursor.moveToNext()) {
                output.add(new ArrayList<String>());
                for (int i = 0; i < CollumNameList.length; i++) {
                    String type = info.get(i).get(2).toUpperCase();
                    switch (type) {
                        case "INTEGER":
                            try {

                                output.get(output.size() - 1).add(Integer.toString(cursor.getInt(cursor.getColumnIndex(CollumNameList[i]))));
                            } catch (Exception e) {
                                Log.e("DatabaseWork", "GetRecordFromDatabase " + e.toString());
                                Logs.SaveLog(context, e);
                            }
                            break;
                        case "TEXT":
                            try {

                                output.get(output.size() - 1).add(cursor.getString(cursor.getColumnIndex(CollumNameList[i])));
                            } catch (Exception e) {
                                Log.e("DatabaseWork", "GetRecordFromDatabase " + e.toString());
                                Logs.SaveLog(context, e);
                            }
                            break;
                        default:
                            try {
                                output.get(output.size() - 1).add(Integer.toString(cursor.getInt(cursor.getColumnIndex(CollumNameList[i]))));
                            } catch (Exception e) {
                                Log.e("DatabaseWork", "GetRecordFromDatabase " + e.toString());
                                Logs.SaveLog(context, e);
                            }
                            break;
                    }
                }
                count++;
                if (count == LimitRecord)
                    break;
            }
            cursor.close();
            return output;
        }
        catch (Exception e)
        {
            Log.e("DatabaseWork",e.toString());
            return new ArrayList<ArrayList<String>>();
        }
    }

    /**
     * Функия возвращает true при наличии таблицы с такими параметрами, и false в противном случае.
     * DatabaseName - имя бд
     * <p/>
     * TableName - имя таблицы для выборки
     * <p/>
     */
    public boolean isTableExists(String DatabaseName, String TableName) {
        int count = 0;
        try {
            if (!isDatabaseExist(DatabaseName))
                return false;
            DatabaseHelper mDatabaseHelper = new DatabaseHelper(context, DatabaseName + ".db", null, 1);//получение расположения бд
            SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
            if (db == null || !db.isOpen()) {
                return false;
            }
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type = ? AND name = ?", new String[]{"table", TableName});
            if (!cursor.moveToFirst()) {
                return false;
            }
            count = cursor.getInt(0);
            cursor.close();
        }
        catch (Exception e)
        {
            return false;
        }
        return count > 0;
    }

    /**
     * Функия возвращает true при наличии БД с такими параметрами, и false в противном случае.
     * DatabaseName - имя бд
     * <p/>
     */
    public boolean isDatabaseExist(String DatabaseName) {
        SQLiteDatabase checkDB = null;
        try {
        String Path = String.valueOf(context.getDatabasePath(DatabaseName));

            checkDB = SQLiteDatabase.openDatabase(Path + ".db", null,
                    SQLiteDatabase.OPEN_READONLY);
            checkDB.close();
        } catch (SQLiteException e) {
            // database doesn't exist yet.
        }
        return checkDB != null;
    }

    /**
     * Процедура обновляет значения в ячейках с указаными параметрами.
     * DatabaseName - имя бд
     * <p/>
     * TableName - имя таблицы
     * <p/>
     * RecordList - массив пар. Содержит 2 значения.
     * 1)Названия столбца
     * 2)Значение которое необходимо вставить
     * Например:
     * StringPair.mp("FirstCollum","1")
     * StringPair.mp("SecondCollum","Mordor")
     * Значения будут автоматически преобразованы в целочисленный или текстовой вид.
     * CollumTypes - тип i-той колонки бд
     * <p/>
     * Conditions - условие выборки данных. Аналог WHERE в SQL. (Например: FirstCollum > 0 AND SecondCollum = 'Yiff')
     */
    public void UpdateDatabaseRecord(String DatabaseName, String TableName, ArrayList<MyPair<String,String>> RecordList, String[] CollumTypes, String Conditions) {
        DatabaseHelper mDatabaseHelper = new DatabaseHelper(context, DatabaseName + ".db", null, 1);
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        for (int i = 0; i < RecordList.size(); i++) {
            String type = CollumTypes[i].toUpperCase();
            switch (type) {
                case "INTEGER":
                    try {
                        values.put(RecordList.get(i).first(), Integer.parseInt(RecordList.get(i).second().toString()));

                    } catch (Exception e) {
                        Log.e("DatabaseWork", "GetRecordFromDatabase " + e.toString());

                    }
                    break;
                case "TEXT":
                    try {
                        values.put(RecordList.get(i).first(), RecordList.get(i).second().toString());
                    } catch (Exception e) {
                        Log.e("DatabaseWork", "GetRecordFromDatabase " + e.toString());
                    }
                    break;
                default:
                    try {
                    } catch (Exception e) {
                        Log.e("DatabaseWork", "UpdateDatabaseRecord " + e.toString());
                    }
                    break;
            }
        }
        db.update(TableName, values, Conditions, null);
    }

    /**
     * Процедура удаляет записи с указаными параметрами.
     * DatabaseName - имя бд
     * <p/>
     * TableName - имя таблицы
     * <p/>
     * Conditions - условие выборки данных. Аналог WHERE в SQL. (Например: FirstCollum > 0 AND SecondCollum = 'Yiff')
     */
    public void DeleteDatabaseRecord(String DatabaseName, String TableName, String Conditions) {
        try {
            DatabaseHelper mDatabaseHelper = new DatabaseHelper(context, DatabaseName + ".db", null, 1);
            SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
            db.delete(TableName, Conditions, null);
        }
        catch (Exception e)
        {
            Log.i("","");
        }
    }

    /**
     * Процедура возвращает true когда запись с указанными параметрами существует, и false в противном случае.
     * DatabaseName - имя бд
     * <p/>
     * TableName - имя таблицы
     * <p/>
     * CollumNameList - название i-той колонки бд
     * <p/>
     * CollumTypes - тип i-той колонки бд
     * <p/>
     * Conditions - условие выборки данных. Аналог WHERE в SQL. (Например: FirstCollum > 0 AND SecondCollum = 'Yiff')
     */
    public boolean isExistRecord(String DatabaseName, String TableName, String[] CollumNameList,
                                 String[] CollumTypes, String Conditions) {

        DatabaseHelper mDatabaseHelper = new DatabaseHelper(context, DatabaseName + ".db", null, 1);
            SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
            if (GetRecordFromDatabase(DatabaseName, TableName, Conditions, -1, null).size() == 0) {
                return false;
            }
        return true;
    }

    /**
     * Процедура возвращает количество записей в бд.
     * DatabaseName - имя бд
     * <p/>
     * TableName - имя таблицы
     * <p/>
     */
    public long GetRecordCount(String DatabaseName, String TableName) {
        DatabaseHelper mDatabaseHelper = new DatabaseHelper(context, DatabaseName + ".db", null, 1);//получение расположения бд
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        Cursor cursor = db.query(TableName, null,
                null, null,
                null, null, null);
        int RecordCount = cursor.getCount();
        return RecordCount;
    }

    /**
     * Процедура определяет мерность массива и записывает массив в бд.
     * DatabaseName - имя бд
     * <p/>
     * TableName - имя таблицы
     * <p/>
     * add_list - массив с записываемым содержимым
     */
    public void ArrayListRecordToDatabase(String DatabaseName, String TableName, ArrayList<ArrayList<T>> add_list) {
        ArrayListOperations arr_op = new ArrayListOperations();
        if (arr_op.getListType(add_list) != -1 && arr_op.getListType(add_list) == 1) {
            ArrayListRecordToDatabaseTwoDimensions(DatabaseName, TableName, add_list);
        }

        if (arr_op.getListType(add_list) != -1 && arr_op.getListType(add_list) == 0) {
            ArrayList<T> list = new ArrayList<>();
            for (int i = 0; i < add_list.get(0).size(); i++)
                list.add(add_list.get(0).get(i));
            ArrayListRecordToDatabaseOneDimensions(DatabaseName, TableName, list);

        }
    }

    /**
     * Процедура записывает двумерный массив в бд.
     * DatabaseName - имя бд
     * <p/>
     * TableName - имя таблицы
     * <p/>
     * add_list - массив с записываемым содержимым
     */
    public void ArrayListRecordToDatabaseTwoDimensions(String DatabaseName, String TableName, ArrayList<ArrayList<T>> add_list) {
        if (!isDatabaseExist(DatabaseName) || !isTableExists(DatabaseName, TableName) || add_list == null) {
            return;
        }
        DatabaseHelper mDatabaseHelper = new DatabaseHelper(context, DatabaseName + ".db", null, 1);//получение расположения бд
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        if (db == null || !db.isOpen()) {
            return;
        }
        Cursor cursor = db.rawQuery("select * from " + TableName, null);
        String[] columnNames = cursor.getColumnNames();


        if (columnNames != null && add_list != null &&
                columnNames.length - 1 != add_list.get(0).size())
            return;//too much column or to less column count

        for (int i = 0; i < add_list.size(); i++) {
            ArrayList<MyPair<String,String>> list = new ArrayList<>();
            MyPair<String,String> p = new MyPair<String,String>();
            for (int j = 0; j < add_list.get(i).size(); j++)
                list.add(p.mp(columnNames[j + 1], (String) add_list.get(i).get(j)));
            InsertDataInTable(DatabaseName, TableName, list);
        }
    }

    /**
     * Процедура записывает одномерный массив в бд.
     * DatabaseName - имя бд
     * <p/>
     * TableName - имя таблицы
     * <p/>
     * add_list - массив с записываемым содержимым
     */
    public void ArrayListRecordToDatabaseOneDimensions(String DatabaseName, String TableName, ArrayList<T> add_list) {
        if (!isDatabaseExist(DatabaseName) || !isTableExists(DatabaseName, TableName) || add_list == null) {
            return;
        }
        DatabaseHelper mDatabaseHelper = new DatabaseHelper(context, DatabaseName + ".db", null, 1);//получение расположения бд
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        if (db == null || !db.isOpen()) {
            return;
        }
        Cursor cursor = db.rawQuery("select * from " + TableName, null);
        String[] columnNames = cursor.getColumnNames();


        if (columnNames.length - 1 != add_list.size())
            return;//too much column or to less column count
        ArrayList<MyPair<String,String>> list = new ArrayList<>();
        MyPair<String,String> p = new MyPair<String,String>();
        for (int j = 0; j < add_list.size(); j++)
            list.add(p.mp(columnNames[j + 1], (String) add_list.get(j)));
        InsertDataInTable(DatabaseName, TableName, list);

    }

    /**
     * Процедура выдает информацию о таблице в БД.
     * DatabaseName - имя бд
     * <p/>
     * TableName - имя таблицы
     * <p/>
     * Пример формата:
     * cid|name                 |type    |notnull |dflt_value |pk
     * 0  |id_fields_starring   |INTEGER |0       |           |1
     * 1  |fields_descriptor_id |INTEGER |1       |           |0
     * 2  |starring_id          |INTEGER |1       |           |0
     * 3  |form_mandatory       |INTEGER |1       |1          |0
     * 4  |form_visible         |INTEGER |1       |1          |0
     */
    public ArrayList<ArrayList<String>> GetTableInfo(String DatabaseName, String TableName) {
        if (!isDatabaseExist(DatabaseName) || !isTableExists(DatabaseName, TableName)) {
            return null;
        }
        ArrayList<ArrayList<String>> table_info = new ArrayList<ArrayList<String>>();
        DatabaseHelper mDatabaseHelper = new DatabaseHelper(context, DatabaseName + ".db", null, 1);//получение расположения бд
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();

        Cursor cursor = db.rawQuery("PRAGMA table_info(" + TableName + ")", null);//запрос на получение информации о бд

        int _cid = cursor.getColumnIndexOrThrow("cid");
        int _name = cursor.getColumnIndexOrThrow("name");
        int _typeIdx = cursor.getColumnIndexOrThrow("type");
        int _notnull = cursor.getColumnIndexOrThrow("notnull");
        int _dflt_value = cursor.getColumnIndexOrThrow("dflt_value");
        int _pk = cursor.getColumnIndexOrThrow("pk");

        while (cursor.moveToNext()) {

            String cid = cursor.getString(_cid);//получение типа колонки в бд
            String name = cursor.getString(_name);
            String typeIdx = cursor.getString(_typeIdx);//получение типа колонки в бд
            String notnull = cursor.getString(_notnull);
            String dflt_value = cursor.getString(_dflt_value);//получение типа колонки в бд
            String pk = cursor.getString(_pk);
            table_info.add(new ArrayList<String>());
            table_info.get((int) table_info.size() - 1).add(cid);
            table_info.get((int) table_info.size() - 1).add(name);
            table_info.get((int) table_info.size() - 1).add(typeIdx);
            table_info.get((int) table_info.size() - 1).add(notnull);
            table_info.get((int) table_info.size() - 1).add(dflt_value);
            table_info.get((int) table_info.size() - 1).add(pk);
        }
        cursor.close();
        return table_info;
    }

    /////////////////////

    /**
     * Процедура записывает одномерный массив в бд.
     * DatabaseName - имя бд
     * <p/>
     * TableName - имя таблицы
     * <p/>
     * add_list - массив с записываемым содержимым
     */
    public void InsertIfNotExist(String DatabaseName, String TableName, ArrayList<ArrayList<T>> add_list) {
        if (!isDatabaseExist(DatabaseName) || !isTableExists(DatabaseName, TableName)) {
            return;
        }

        DatabaseHelper mDatabaseHelper = new DatabaseHelper(context, DatabaseName + ".db", null, 1);//получение расположения бд
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        ArrayList<ArrayList<String>> db_info = GetTableInfo(DatabaseName, TableName);
        String Values = "(";

        for (int i = 0; i < db_info.size(); i++) {
            if(!db_info.get(i).get(5).equals("1"))
                Values += "?" + (i + 1 == db_info.size() ? ")" : ",");
            else
                Values += (i + 1 == db_info.size() ? ")" : ",");

        }
        for (int i = 0; i < add_list.size(); i++) {

            String Conditions = "WHERE ";
            int cc = 0;
            for (int j = 0; j < db_info.size(); j++) {
                String type = db_info.get(j).get(2).toUpperCase();
                String name_collum = db_info.get(j).get(1);
                if (!db_info.get(j).get(5).equals("1")) {
                    switch (type) {
                        case "INTEGER":
                            Conditions += name_collum + "=" + (String) add_list.get(i).get(j - cc) + (j + 1 == db_info.size() ? "" : " AND ");
                            break;
                        case "TEXT":
                            Conditions += name_collum + "=" + "'" + (String) add_list.get(i).get(j - cc) + "'" + (j + 1 == db_info.size() ? "" : " AND ");
                            break;
                        case "BLOB":
                            Conditions += name_collum + "=" + "'" + (String) add_list.get(i).get(j - cc) + "'" + (j + 1 == db_info.size() ? "" : " AND ");//!!!
                            break;
                        case "REAL":
                            Conditions += name_collum + "=" + (String) add_list.get(i).get(j - cc) + (j + 1 == db_info.size() ? "" : " AND ");
                            break;
                    }
                } else
                    cc++;
            }



            String countQuery = "SELECT  * FROM " + TableName + " " + Conditions;
            Cursor cursor = db.rawQuery(countQuery, null);
            int cnt = cursor.getCount();
            cursor.close();
            if (cnt == 0) {


                ArrayList<MyPair<String,String>> list = new ArrayList<>();
                MyPair<String,String> p = new MyPair<String,String>();
                int cc1 = 0;
                for (int j = 0; j < db_info.size(); j++) {
                    String name = db_info.get(j).get(1);
                    if(!db_info.get(j).get(5).equals("1")) {
                        list.add(p.mp(name, String.valueOf(add_list.get(i).get(cc1))));
                        cc1++;
                    }
                }
                InsertDataInTable(DatabaseName, TableName, list);
            }

        }
    }
}
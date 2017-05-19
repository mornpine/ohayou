package com.ohayou.japanese.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.SparseArray;

import com.ohayou.japanese.model.Textbook;

/**
 * Created by Oxygen on 15/8/16.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "ohayou.db";
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_TEXTBOOK = "textbook";
    private static final String TABLE_USER = "user";
    private static final String TABLE_BUY_INFO = "buyinfo";

    private SparseArray<Textbook> mTextbooks = new SparseArray<>();
    private String mUsername;
    private String mPassword;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        loadTextbooks();
        loadUser();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_TEXTBOOK + "(" +
                "tid INTEGER PRIMARY KEY ON CONFLICT REPLACE," +
                "cid INTEGER DEFAULT 0," +
                "name TEXT," +
                "level TEXT," +
                "questions TEXT," +
                "status TEXT," +
                "passed INTEGER DEFAULT 0," +
                "failed INTEGER DEFAULT 0," +
                "complete INTEGER DEFAULT 0" +
                ");");

        db.execSQL("CREATE TABLE " + TABLE_USER + "(" +
                "uid INTEGER PRIMARY KEY," +
                "name TEXT," +
                "password TEXT" +
                ");");

        db.execSQL("CREATE TABLE " + TABLE_BUY_INFO + "(" +
                "uid INTEGER PRIMARY KEY," +
                "name TEXT," +
                "tid INTEGER," +
                "question INTEGER" +
                ");");
    }

    public void addTextbook(Textbook book) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("tid", book.tid);
        values.put("cid", book.cid);
        values.put("name", book.name);
        values.put("level", book.level);
        db.insert(TABLE_TEXTBOOK, null, values);
        mTextbooks.put(book.tid, book);
    }

    public void updateTextbook(int tid, int passed, int failed, String status) {
        Textbook book = mTextbooks.get(tid);
        if (book != null) {
            book.passed = passed;
            book.failed = failed;
            book.status = status;
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("passed", passed);
            values.put("failed", failed);
            values.put("status", status);
            db.update(TABLE_TEXTBOOK, values, "tid=" + tid, null);
        }
    }

    public void clearTextbooks() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_TEXTBOOK);
        mTextbooks.clear();
    }

    public void deleteTextbooks(Textbook book) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_TEXTBOOK, "tid=?", new String[]{String.valueOf(book.tid)});
        mTextbooks.remove(book.tid);
    }

    public void updateTextbook(int tid, int complete) {
        Textbook book = mTextbooks.get(tid);
        if (book != null) {
            book.complete = complete;
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("complete", complete);
            db.update(TABLE_TEXTBOOK, values, "tid=" + tid, null);
        }
    }

    public void updateTextbook(int tid, String questions) {
        Textbook book = mTextbooks.get(tid);
        if (book != null) {
            book.questions = questions;
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("questions", questions);
            db.update(TABLE_TEXTBOOK, values, "tid=" + tid, null);
        }
    }

    public void loadTextbooks() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_TEXTBOOK, new String[]{"tid", "cid", "name", "level", "questions", "status", "passed", "failed", "complete"}, null, null, null, null, null);
        while (cursor.moveToNext()) {
            int i = 0;
            Textbook textbook = new Textbook();
            textbook.tid = cursor.getInt(i++);
            textbook.cid = cursor.getInt(i++);
            textbook.name = cursor.getString(i++);
            textbook.level = cursor.getString(i++);
            textbook.questions = cursor.getString(i++);
            textbook.status = cursor.getString(i++);
            textbook.passed = cursor.getInt(i++);
            textbook.failed = cursor.getInt(i++);
            textbook.complete = cursor.getInt(i++);
            mTextbooks.put(textbook.tid, textbook);
        }
        cursor.close();
    }

    public void loadUser() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_USER, new String[]{"name", "password"}, null, null, null, null, null);
        if (cursor.moveToNext()) {
            int i = 0;
            mUsername = cursor.getString(i++);
            mPassword = cursor.getString(i++);
        }
        cursor.close();
    }

    public void setUser(String username, String password) {
        if (!TextUtils.equals(username, mUsername) || !TextUtils.equals(password, password)) {
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL("DELETE FROM " + TABLE_USER);
            ContentValues values = new ContentValues();
            values.put("name", username);
            values.put("password", password);
            db.insert(TABLE_USER, null, values);
            mUsername = username;
            mPassword = password;
        }
    }

    public void clearUser() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_USER);
        mPassword = null;
        mUsername = null;
    }

    public void addBuyInfo(String username, int tid, int question) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", username);
        values.put("tid", tid);
        values.put("question", question);
        db.insert(TABLE_BUY_INFO, null, values);
    }

    public boolean hasBuyInfo(String username, int tid, int question) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_BUY_INFO, new String[]{"tid", "question"}, "name=? AND tid=?", new String[]{username, String.valueOf(tid)}, null, null, null);
        while (cursor.moveToNext()) {
            int buyQuestion = cursor.getInt(1);
            if (buyQuestion == -1 || buyQuestion == question) {
                cursor.close();
                return true;
            }
        }
        cursor.close();
        return false;
    }

    public String getUsername() {
        return mUsername;
    }

    public String getPassword() {
        return mPassword;
    }

    public Textbook getTextbook(int tid) {
        return mTextbooks.get(tid);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE " + TABLE_BUY_INFO + "(" +
                    "uid INTEGER PRIMARY KEY," +
                    "name TEXT," +
                    "tid INTEGER," +
                    "question INTEGER" +
                    ");");
        }
    }
}

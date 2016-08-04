package com.watch.guoneng.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.watch.guoneng.model.WifiDevice;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


public class WifiDeviceDao {
    private DatabaseHelper dbHelper;
    private static final String TABLE_NAME = "WifiDevices";

    public WifiDeviceDao(Context context) {
        super();

        dbHelper = new DatabaseHelper(context);
    }

    public boolean isExist(WifiDevice d) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, "id = ?",
                new String[]{d.getId()}, null, null, null);
        if (cursor.getCount() > 0) {
            db.close();
            cursor.close();
            return true;
        }
        db.close();
        cursor.close();
        return false;
    }

    public int insert(WifiDevice device) {
        WifiDevice bef = queryById(device.getId());
        if (bef != null) {
            deleteById(bef.getId());
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", device.getId());
        values.put("name", device.getName());
        values.put("thumbnail", device.getThumbnail());

        int id = (int) db.insert(TABLE_NAME, null, values);
        db.close();
        Log.e("hjq", "insert id = " + id);
        return id;
    }

    public int update(WifiDevice device, ContentValues cv) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        int index = (int) db.update(TABLE_NAME, cv, "id = ?", new String[]{device.getId()});
        db.close();

        return index;
    }

    public int update(WifiDevice device) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("name", device.getName());
        values.put("address", device.getAddress());
        values.put("thumbnail", device.getThumbnail());

        int index = (int) db.update(TABLE_NAME, values, "id = ?", new String[]{device.getId()});
        db.close();
        return index;
    }

    public void deleteAll() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(TABLE_NAME, null, null);
        db.close();
    }

    public void deleteById(String address) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(TABLE_NAME, "id = ?", new String[]{address});
        db.close();
    }

    public ArrayList<WifiDevice> queryAll() {
        ArrayList<WifiDevice> list = new ArrayList<WifiDevice>(10);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db
                .query(TABLE_NAME, null, null, null, null, null, null);
        while (cursor.moveToNext()) {
            String id = cursor.getString(cursor.getColumnIndex("id"));
            String image_thumb = cursor.getString(cursor.getColumnIndex("thumbnail"));
            String name = cursor.getString(cursor.getColumnIndex("name"));

            list.add(new WifiDevice(image_thumb, name, id));
        }
        cursor.close();
        db.close();

        Log.d("hjq", "list size from dao is " + list.size());

        // sort the list by name.
        Collections.sort(list, comparator);

        return list;
    }

    public WifiDevice queryById(String id) {
        WifiDevice device = null;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, "id=?",
                new String[]{id}, null, null, null);
        while (cursor.moveToNext()) {
            String address = cursor.getString(cursor.getColumnIndex("id"));
            String image_thumb = cursor.getString(cursor.getColumnIndex("thumbnail"));
            String name = cursor.getString(cursor.getColumnIndex("name"));

            device = new WifiDevice(image_thumb, name, address);
        }
        cursor.close();
        db.close();

        return device;
    }

    public int getcount() {
        return queryAll().size();
    }

    Comparator<WifiDevice> comparator = new Comparator<WifiDevice>() {
        public int compare(WifiDevice s1, WifiDevice s2) {
            if (!s1.getName().equals(s2.getName())) {
                return s1.getName().compareTo(s2.getName());
            }

            return 0;
        }
    };
}


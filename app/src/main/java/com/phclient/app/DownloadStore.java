package com.phclient.app;

import android.content.Context;
import android.os.Environment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** 下载记录的极简本地存储（JSON 落在 SharedPreferences 里，够用且零依赖）。 */
public final class DownloadStore {

    private static final String KEY = "downloads";

    private final android.content.SharedPreferences sp;

    private DownloadStore(Context ctx) {
        sp = ctx.getSharedPreferences("phclient_downloads", Context.MODE_PRIVATE);
    }

    private static DownloadStore inst;

    public static synchronized DownloadStore get(Context ctx) {
        if (inst == null) {
            inst = new DownloadStore(ctx.getApplicationContext());
        }
        return inst;
    }

    public static class Item {
        public long id;
        public String name;
        public String url;
        public long time;

        public Item(long id, String name, String url, long time) {
            this.id = id;
            this.name = name;
            this.url = url;
            this.time = time;
        }
    }

    public List<Item> list() {
        List<Item> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(sp.getString(KEY, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(new Item(o.optLong("id"), o.optString("name"),
                        o.optString("url"), o.optLong("time")));
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    public void add(Item item) {
        List<Item> items = list();
        items.add(0, item);
        save(items);
    }

    public void remove(String name) {
        List<Item> keep = new ArrayList<>();
        for (Item i : list()) {
            if (!i.name.equals(name)) keep.add(i);
        }
        save(keep);
    }

    private void save(List<Item> items) {
        JSONArray arr = new JSONArray();
        try {
            for (Item i : items) {
                JSONObject o = new JSONObject();
                o.put("id", i.id);
                o.put("name", i.name);
                o.put("url", i.url);
                o.put("time", i.time);
                arr.put(o);
            }
        } catch (Throwable ignored) {
        }
        sp.edit().putString(KEY, arr.toString()).apply();
    }

    public static File dir() {
        return new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES), "ZHUB");
    }
}

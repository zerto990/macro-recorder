package com.macrorecorder.app;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class MacroStorageManager {
    private final Context ctx;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final SharedPreferences prefs;
    private static final String DIR = "macros";

    public MacroStorageManager(Context ctx) {
        this.ctx = ctx;
        this.prefs = ctx.getSharedPreferences("macro_prefs", Context.MODE_PRIVATE);
        new File(ctx.getFilesDir(), DIR).mkdirs();
    }

    public void save(MacroScript s) {
        try {
            FileWriter w = new FileWriter(new File(ctx.getFilesDir(), DIR + "/" + s.getId() + ".json"));
            gson.toJson(s, w); w.close();
            List<String> idx = getIndex();
            if (!idx.contains(s.getId())) { idx.add(s.getId()); saveIndex(idx); }
        } catch (Exception ignored) {}
    }

    public MacroScript load(String id) {
        try {
            File f = new File(ctx.getFilesDir(), DIR + "/" + id + ".json");
            if (!f.exists()) return null;
            FileReader r = new FileReader(f);
            MacroScript s = gson.fromJson(r, MacroScript.class); r.close(); return s;
        } catch (Exception e) { return null; }
    }

    public List<MacroScript> loadAll() {
        List<MacroScript> list = new ArrayList<>();
        for (String id : getIndex()) { MacroScript s = load(id); if (s != null) list.add(s); }
        return list;
    }

    public void delete(String id) {
        new File(ctx.getFilesDir(), DIR + "/" + id + ".json").delete();
        List<String> idx = getIndex(); idx.remove(id); saveIndex(idx);
    }

    private List<String> getIndex() {
        Type t = new TypeToken<List<String>>(){}.getType();
        List<String> l = gson.fromJson(prefs.getString("idx", "[]"), t);
        return l != null ? l : new ArrayList<>();
    }

    private void saveIndex(List<String> idx) { prefs.edit().putString("idx", gson.toJson(idx)).apply(); }
}

package com.talayar.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/** لایهٔ داده — کاملاً محلی و آفلاین (SQLite) */
public class Db extends SQLiteOpenHelper {
    private static Db inst;
    public static synchronized Db get(Context c) {
        if (inst == null) inst = new Db(c.getApplicationContext());
        return inst;
    }
    public Db(Context c) { super(c, "talayar.db", null, 1); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE settings (k TEXT PRIMARY KEY, v TEXT)");
        db.execSQL("CREATE TABLE rates (id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER, date_j TEXT, rate INTEGER)");
        db.execSQL("CREATE TABLE customers (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, phone TEXT, note TEXT, cts INTEGER)");
        db.execSQL("CREATE TABLE customer_tx (id INTEGER PRIMARY KEY AUTOINCREMENT, cid INTEGER, ts INTEGER, date_j TEXT, cash INTEGER, goldmw INTEGER, descr TEXT, iid INTEGER)");
        db.execSQL("CREATE TABLE items (id INTEGER PRIMARY KEY AUTOINCREMENT, code INTEGER, name TEXT, karat INTEGER, wmw INTEGER, wtype INTEGER, wval INTEGER, stone_mw INTEGER, stoneval INTEGER, descr TEXT, status TEXT, cts INTEGER)");
        db.execSQL("CREATE TABLE invoices (id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER, date_j TEXT, cid INTEGER, cname TEXT, rate INTEGER, goldval INTEGER, wage INTEGER, stone INTEGER, tax INTEGER, total INTEGER, pcash INTEGER, pgold_mw INTEGER, pgold_val INTEGER, pgold_karat INTEGER, debt INTEGER, note TEXT)");
        db.execSQL("CREATE TABLE invoice_lines (id INTEGER PRIMARY KEY AUTOINCREMENT, iid INTEGER, item_id INTEGER, title TEXT, karat INTEGER, wmw INTEGER, unit INTEGER, wage INTEGER, stone INTEGER, tax INTEGER, total INTEGER)");
        db.execSQL("CREATE TABLE cash_tx (id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER, date_j TEXT, kind TEXT, amount INTEGER, descr TEXT, iid INTEGER)");
        db.execSQL("CREATE TABLE gold_tx (id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER, date_j TEXT, kind TEXT, wmw INTEGER, karat INTEGER, descr TEXT, cid INTEGER)");
        db.execSQL("CREATE INDEX idx_custtx ON customer_tx(cid)");
        db.execSQL("CREATE INDEX idx_lines ON invoice_lines(iid)");
        db.execSQL("CREATE INDEX idx_inv_date ON invoices(date_j)");
        db.execSQL("CREATE INDEX idx_items_status ON items(status)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int o, int n) {}

    // ---------- settings ----------
    public String getS(String k, String def) {
        Cursor c = getReadableDatabase().rawQuery("SELECT v FROM settings WHERE k=?", new String[]{k});
        String v = def;
        if (c.moveToFirst()) v = c.getString(0);
        c.close();
        return v;
    }
    public void setS(String k, String v) {
        ContentValues cv = new ContentValues();
        cv.put("k", k); cv.put("v", v);
        getWritableDatabase().replace("settings", null, cv);
    }
    public long getL(String k, long def) {
        try { return Long.parseLong(getS(k, "" + def)); } catch (Exception e) { return def; }
    }

    // ---------- rate ----------
    public long currentRate() {
        Cursor c = getReadableDatabase().rawQuery("SELECT rate FROM rates ORDER BY ts DESC, id DESC LIMIT 1", null);
        long r = c.moveToFirst() ? c.getLong(0) : 0;
        c.close();
        return r;
    }
    public Cursor rateHistory(int limit) {
        return getReadableDatabase().rawQuery("SELECT id, ts, date_j, rate FROM rates ORDER BY ts DESC, id DESC LIMIT " + limit, null);
    }
    public volatile boolean dummy = false;

    /** مجموع ورودی/خروجی صندوق */
    public static class Pair { public long a, b; }
    public Pair cashInOut() {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(CASE WHEN kind='in' THEN amount ELSE 0 END),0), COALESCE(SUM(CASE WHEN kind='out' THEN amount ELSE 0 END),0) FROM cash_tx", null);
        Pair p = new Pair();
        if (c.moveToFirst()) { p.a = c.getLong(0); p.b = c.getLong(1); }
        c.close();
        return p;
    }
    public long cashBalance() { Pair p = cashInOut(); return p.a - p.b; }

    /** موجودی اجناس */
    public Pair itemsStock() {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*), COALESCE(SUM(wmw),0) FROM items WHERE status='stock'", null);
        Pair p = new Pair();
        if (c.moveToFirst()) { p.a = c.getLong(0); p.b = c.getLong(1); }
        c.close();
        return p;
    }

    /** موجودی طلا (آبشده/کارکرده): [0]=گرم خام، [1]=معادل ۱۸عیار */
    public long[] goldBalance() {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT kind, wmw, karat FROM gold_tx", null);
        long raw = 0, eq = 0;
        while (c.moveToNext()) {
            String kind = c.getString(0);
            long w = c.getLong(1);
            int k = c.getInt(2);
            long sign = "out".equals(kind) ? -1 : 1;
            raw += sign * w;
            eq  += sign * U.equiv750(w, k);
        }
        c.close();
        return new long[]{raw, eq};
    }

    /** بدهی مشتریان: [0]=نقد (بدهکار-بستانکار)، [1]=طلا میلی‌گرم معادل‌سازی‌شده در ورودی */
    public long[] customerDebts() {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(cash),0), COALESCE(SUM(goldmw),0) FROM customer_tx", null);
        long[] r = {0, 0};
        if (c.moveToFirst()) { r[0] = c.getLong(0); r[1] = c.getLong(1); }
        c.close();
        return r;
    }

    /** جمع بدهی/بستانکاری تفکیکی مشتریان */
    public long[] customerDebtSplit() {
        long debtC = 0, credC = 0, debtG = 0, credG = 0;
        Cursor c2 = getReadableDatabase().rawQuery("SELECT cid, SUM(cash), SUM(goldmw) FROM customer_tx GROUP BY cid", null);
        while (c2.moveToNext()) {
            long cs = c2.getLong(1), g = c2.getLong(2);
            if (cs > 0) debtC += cs; else credC += -cs;
            if (g > 0) debtG += g; else credG += -g;
        }
        c2.close();
        return new long[]{debtC, credC, debtG, credG};
    }

    /** آمار فروش برای یک پیشوند تاریخ (روز یا ماه) */
    public long[] salesStats(String likePrefix) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*), COALESCE(SUM(total),0), COALESCE(SUM(wage),0), COALESCE(SUM(tax),0), COALESCE(SUM(pcash),0), COALESCE(SUM(debt),0) " +
                "FROM invoices WHERE date_j LIKE ?", new String[]{likePrefix + "%"});
        long[] r = new long[6];
        if (c.moveToFirst()) for (int i = 0; i < 6; i++) r[i] = c.getLong(i);
        c.close();
        return r;
    }

    public static long cl(Cursor c, String col) { return c.getLong(c.getColumnIndex(col)); }
    public static int  ci(Cursor c, String col) { return c.getInt(c.getColumnIndex(col)); }
    public static String cs(Cursor c, String col) {
        String s = c.getString(c.getColumnIndex(col));
        return s == null ? "" : s;
    }

    // ---------- درج رکوردها (سازندگان کوچک) ----------
    public long ins(String table, ContentValues cv) {
        return getWritableDatabase().insert(table, null, cv);
    }
    public SQLiteDatabase w() { return getWritableDatabase(); }
    public SQLiteDatabase r() { return getReadableDatabase(); }
}

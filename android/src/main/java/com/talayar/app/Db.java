package com.talayar.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/** لایهٔ داده — کاملاً محلی و آفلاین (SQLite) — نسخهٔ ۲ (اسناد مرکزی + کد حساب + دارایی‌ها) */
public class Db extends SQLiteOpenHelper {
    private static final int VER = 3;
    private static Db inst;
    public static synchronized Db get(Context c) {
        if (inst == null) inst = new Db(c.getApplicationContext());
        return inst;
    }
    public Db(Context c) { super(c, "talayar.db", null, VER); }

    @Override public void onCreate(SQLiteDatabase db) {
        createV1(db);
        createV2(db);
    }

    private void createV1(SQLiteDatabase db) {
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

    private void createV2(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE defs (id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT, name TEXT, x1 INTEGER, x2 INTEGER, x3 TEXT, cts INTEGER)");
        db.execSQL("CREATE INDEX idx_defs_kind ON defs(kind)");
        db.execSQL("CREATE TABLE docs (id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER, date_j TEXT, descr TEXT, upd_ts INTEGER)");
        db.execSQL("CREATE INDEX idx_docs_date ON docs(date_j)");
        db.execSQL("CREATE TABLE doc_rows (id INTEGER PRIMARY KEY AUTOINCREMENT, doc_id INTEGER, seq INTEGER, txt TEXT)");
        db.execSQL("CREATE INDEX idx_drows ON doc_rows(doc_id)");
        db.execSQL("CREATE TABLE assets_ledger (id INTEGER PRIMARY KEY AUTOINCREMENT, doc_id INTEGER, ts INTEGER, date_j TEXT, scope TEXT, asset TEXT, qty REAL, karat INTEGER, cid INTEGER, descr TEXT)");
        db.execSQL("CREATE INDEX idx_asset ON assets_ledger(scope, asset)");
        db.execSQL("CREATE INDEX idx_asset_cid ON assets_ledger(cid)");
        db.execSQL("CREATE TABLE banks (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, acc_no TEXT, cts INTEGER)");
        db.execSQL("CREATE TABLE bank_tx (id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER, date_j TEXT, bank_id INTEGER, kind TEXT, amount INTEGER, descr TEXT, cid INTEGER, doc_id INTEGER)");
        db.execSQL("CREATE INDEX idx_bank ON bank_tx(bank_id)");
        db.execSQL("CREATE TABLE checks (id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER, date_j TEXT, due_j TEXT, amount INTEGER, bank_id INTEGER, cid INTEGER, cname TEXT, kind TEXT, no TEXT, status TEXT, descr TEXT, doc_id INTEGER)");
        db.execSQL("CREATE TABLE prices (id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER, key TEXT, val INTEGER)");
        db.execSQL("CREATE TABLE etiket (id INTEGER PRIMARY KEY AUTOINCREMENT, code TEXT, name TEXT, wmw INTEGER, item_id INTEGER, photo TEXT, mezane TEXT, rfid TEXT, updated_ts INTEGER, cts INTEGER)");
        // کد حساب و گروه به مشتری‌ها
        safeAlter(db, "ALTER TABLE customers ADD COLUMN code INTEGER DEFAULT 0");
        safeAlter(db, "ALTER TABLE customers ADD COLUMN grp TEXT DEFAULT ''");
        safeAlter(db, "ALTER TABLE customers ADD COLUMN address TEXT DEFAULT ''");
        // بازنویسی کد حساب برای رکوردهای بدون کد
        db.execSQL("UPDATE customers SET code=id WHERE code=0 OR code IS NULL");
        seedDefs(db);
        v3(db);
    }
    private void v3(SQLiteDatabase db) {
        safeAlter(db, "ALTER TABLE gold_tx ADD COLUMN doc_id INTEGER DEFAULT 0");
        safeAlter(db, "CREATE INDEX idx_gold_doc ON gold_tx(doc_id)");
    }
    private void safeAlter(SQLiteDatabase db, String sql) {
        try { db.execSQL(sql); } catch (Exception ignored) {}
    }

    @Override public void onUpgrade(SQLiteDatabase db, int o, int n) {
        if (o < 2) { createV2TablesIfMissing(db); seedDefs(db); }
        if (o < 3) v3(db);
    }
    private void createV2TablesIfMissing(SQLiteDatabase db) {
        try { db.execSQL("CREATE TABLE defs (id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT, name TEXT, x1 INTEGER, x2 INTEGER, x3 TEXT, cts INTEGER)"); } catch (Exception e) {}
        try { db.execSQL("CREATE INDEX idx_defs_kind ON defs(kind)"); } catch (Exception e) {}
        try { db.execSQL("CREATE TABLE docs (id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER, date_j TEXT, descr TEXT, upd_ts INTEGER)"); } catch (Exception e) {}
        try { db.execSQL("CREATE INDEX idx_docs_date ON docs(date_j)"); } catch (Exception e) {}
        try { db.execSQL("CREATE TABLE doc_rows (id INTEGER PRIMARY KEY AUTOINCREMENT, doc_id INTEGER, seq INTEGER, txt TEXT)"); } catch (Exception e) {}
        try { db.execSQL("CREATE INDEX idx_drows ON doc_rows(doc_id)"); } catch (Exception e) {}
        try { db.execSQL("CREATE TABLE assets_ledger (id INTEGER PRIMARY KEY AUTOINCREMENT, doc_id INTEGER, ts INTEGER, date_j TEXT, scope TEXT, asset TEXT, qty REAL, karat INTEGER, cid INTEGER, descr TEXT)"); } catch (Exception e) {}
        try { db.execSQL("CREATE INDEX idx_asset ON assets_ledger(scope, asset)"); } catch (Exception e) {}
        try { db.execSQL("CREATE INDEX idx_asset_cid ON assets_ledger(cid)"); } catch (Exception e) {}
        try { db.execSQL("CREATE TABLE banks (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, acc_no TEXT, cts INTEGER)"); } catch (Exception e) {}
        try { db.execSQL("CREATE TABLE bank_tx (id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER, date_j TEXT, bank_id INTEGER, kind TEXT, amount INTEGER, descr TEXT, cid INTEGER, doc_id INTEGER)"); } catch (Exception e) {}
        try { db.execSQL("CREATE INDEX idx_bank ON bank_tx(bank_id)"); } catch (Exception e) {}
        try { db.execSQL("CREATE TABLE checks (id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER, date_j TEXT, due_j TEXT, amount INTEGER, bank_id INTEGER, cid INTEGER, cname TEXT, kind TEXT, no TEXT, status TEXT, descr TEXT, doc_id INTEGER)"); } catch (Exception e) {}
        try { db.execSQL("CREATE TABLE prices (id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER, key TEXT, val INTEGER)"); } catch (Exception e) {}
        try { db.execSQL("CREATE TABLE etiket (id INTEGER PRIMARY KEY AUTOINCREMENT, code TEXT, name TEXT, wmw INTEGER, item_id INTEGER, photo TEXT, mezane TEXT, rfid TEXT, updated_ts INTEGER, cts INTEGER)"); } catch (Exception e) {}
        safeAlter(db, "ALTER TABLE customers ADD COLUMN code INTEGER DEFAULT 0");
        safeAlter(db, "ALTER TABLE customers ADD COLUMN grp TEXT DEFAULT ''");
        safeAlter(db, "ALTER TABLE customers ADD COLUMN address TEXT DEFAULT ''");
        db.execSQL("UPDATE customers SET code=id WHERE code=0 OR code IS NULL");
    }

    /** کدینگ‌های پیش‌فرض */
    private void seedDefs(SQLiteDatabase db) {
        if (countWhere("defs", null, null, null) > 0) return;
        long ts = System.currentTimeMillis();
        String[] groups = {"مشتریان", "تولیدکنندگان", "بنکداران", "همکاران", "پرسنل"};
        for (String g : groups) addDef(db, "group", g, 0, 0, "", ts);
        String[][] coins = new String[5][];
        coins[0] = new String[]{"سکه امامی", "8133", "900"};
        coins[1] = new String[]{"سکه بهار آزادی", "8133", "900"};
        coins[2] = new String[]{"نیم سکه", "4066", "900"};
        coins[3] = new String[]{"ربع سکه", "2033", "900"};
        coins[4] = new String[]{"سکه گرمی", "1010", "900"};
        for (String[] c : coins) addDef(db, "coin", c[0], Long.parseLong(c[1]), Long.parseLong(c[2]), "", ts);
        String[][] bulls = new String[3][];
        bulls[0] = new String[]{"شمش ۱۰ گرمی ۹۹۵", "10000", "995"};
        bulls[1] = new String[]{"شمش ۱۰۰ گرمی ۹۹۵", "100000", "995"};
        bulls[2] = new String[]{"شمش ۱ کیلویی ۹۹۵", "1000000", "995"};
        for (String[] c : bulls) addDef(db, "bullion", c[0], Long.parseLong(c[1]), Long.parseLong(c[2]), "", ts);
        String[] curs = {"دلار آمریکا", "یورو", "درهم امارات", "لیر ترکیه", "پوند انگلیس", "تتر USDT"};
        for (String c : curs) addDef(db, "curr", c, 0, 0, "", ts);
        String[][] silvs = new String[3][];
        silvs[0] = new String[]{"نقره ۹۹۹", "999"};
        silvs[1] = new String[]{"نقره ۹۲۵", "925"};
        silvs[2] = new String[]{"نقره ۸۴۰", "840"};
        for (String[] c : silvs) addDef(db, "silver", c[0], 0, Long.parseLong(c[1]), "", ts);
        String[] rgs = {"بدون رسوب", "ری‌گیری ۱٪", "ری‌گیری ۲٪", "ری‌گیری ۳٪", "رسوب مخصوص"};
        for (String r : rgs) addDef(db, "rizgiri", r, 0, 0, "", ts);
    }
    private void addDef(SQLiteDatabase db, String kind, String name, long x1, long x2, String x3, long ts) {
        ContentValues cv = new ContentValues();
        cv.put("kind", kind); cv.put("name", name);
        cv.put("x1", x1); cv.put("x2", x2); cv.put("x3", x3); cv.put("cts", ts);
        db.insert("defs", null, cv);
    }
    private int countWhere(String table, String where, String[] args, String x) {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + table + (where != null ? " WHERE " + where : ""), args);
        c.moveToFirst();
        int r = c.getInt(0);
        c.close();
        return r;
    }

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

    // ---------- prices (آخرین نرخ‌های دریافتی) ----------
    public long priceGet(String key) {
        Cursor c = getReadableDatabase().rawQuery("SELECT val FROM prices WHERE key=? ORDER BY ts DESC, id DESC LIMIT 1", new String[]{key});
        long v = c.moveToFirst() ? c.getLong(0) : 0;
        c.close();
        return v;
    }
    public void priceSet(String key, long val) {
        ContentValues cv = new ContentValues();
        cv.put("ts", System.currentTimeMillis()); cv.put("key", key); cv.put("val", val);
        getWritableDatabase().insert("prices", null, cv);
    }

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

    /** موجودی طلا (آبشده): [0]=گرم خام، [1]=معادل ۱۸عیار */
    public long[] goldBalance() {
        Cursor c = getReadableDatabase().rawQuery("SELECT kind, wmw, karat FROM gold_tx", null);
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

    /** بدهی مشتریان (نقدی و طلایی ۱۸معادل) */
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

    // ---------- تعاریف (defs) ----------
    public Cursor defsOf(String kind) {
        return getReadableDatabase().rawQuery("SELECT * FROM defs WHERE kind=? ORDER BY id", new String[]{kind});
    }

    // ---------- بانک ----------
    public long bankBalance(int bankId) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(CASE WHEN kind='in' THEN amount ELSE -amount END),0) FROM bank_tx WHERE bank_id=?",
                new String[]{"" + bankId});
        long r = c.moveToFirst() ? c.getLong(0) : 0;
        c.close();
        return r;
    }
    public long banksTotal() {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(CASE WHEN kind='in' THEN amount ELSE -amount END),0) FROM bank_tx", null);
        long r = c.moveToFirst() ? c.getLong(0) : 0;
        c.close();
        return r;
    }

    // ---------- موجودی دارایی generic ----------
    public double stockOf(String asset) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(qty),0) FROM assets_ledger WHERE scope='stock' AND asset=?", new String[]{asset});
        double r = c.moveToFirst() ? c.getDouble(0) : 0;
        c.close();
        return r;
    }

    public static long cl(Cursor c, String col) { return c.getLong(c.getColumnIndex(col)); }
    public static int  ci(Cursor c, String col) { return c.getInt(c.getColumnIndex(col)); }
    public static String cs(Cursor c, String col) {
        String s = c.getString(c.getColumnIndex(col));
        return s == null ? "" : s;
    }

    public long ins(String table, ContentValues cv) {
        return getWritableDatabase().insert(table, null, cv);
    }
    public SQLiteDatabase w() { return getWritableDatabase(); }
    public SQLiteDatabase r() { return getReadableDatabase(); }
}

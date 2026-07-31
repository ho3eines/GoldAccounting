package com.talayar.app;

import android.content.ContentValues;

/** موتور ثبت اسناد — اثر حسابداری هر رویداد در یک تراکنش */
public final class Post {
    private Post() {}

    public static long doc(Db db, String dateJ, String descr) {
        ContentValues cv = new ContentValues();
        long ts = System.currentTimeMillis();
        cv.put("ts", ts);
        cv.put("date_j", dateJ);
        cv.put("descr", descr);
        cv.put("upd_ts", ts);
        return db.ins("docs", cv);
    }
    public static void line(Db db, long docId, int seq, String txt) {
        ContentValues cv = new ContentValues();
        cv.put("doc_id", docId);
        cv.put("seq", seq);
        cv.put("txt", txt);
        db.ins("doc_rows", cv);
    }

    /** حرکت صندوق نقد (iid = −docId برای عدم تداخل با فاکتورها) */
    public static void cash(Db db, long docId, String dateJ, String kind, long amount, String descr) {
        ContentValues cv = new ContentValues();
        cv.put("ts", System.currentTimeMillis());
        cv.put("date_j", dateJ);
        cv.put("kind", kind);
        cv.put("amount", amount);
        cv.put("descr", descr);
        cv.put("iid", -docId);
        db.ins("cash_tx", cv);
    }

    /** حرکت آبشده (با عیار) */
    public static void gold(Db db, long docId, String dateJ, String kind, int wmg, int karat, String descr, int cid) {
        ContentValues cv = new ContentValues();
        cv.put("ts", System.currentTimeMillis());
        cv.put("date_j", dateJ);
        cv.put("kind", kind);
        cv.put("wmw", wmg);
        cv.put("karat", karat);
        cv.put("descr", descr);
        cv.put("cid", cid);
        cv.put("doc_id", docId);
        db.ins("gold_tx", cv);
    }

    /** اثر در حساب مشتری — نقدی (+ بدهی مشتری / − بستانکار) و/یا طلای ۱۸معادل */
    public static void cust(Db db, long docId, String dateJ, int cid, long cash, long goldmw, String descr) {
        if (cid <= 0) return;
        ContentValues cv = new ContentValues();
        cv.put("cid", cid);
        cv.put("ts", System.currentTimeMillis());
        cv.put("date_j", dateJ);
        cv.put("cash", cash);
        cv.put("goldmw", goldmw);
        cv.put("descr", descr);
        cv.put("iid", -docId);
        db.ins("customer_tx", cv);
    }

    /** حرکت بانک */
    public static void bank(Db db, String dateJ, int bankId, String kind, long amount, String descr, int cid, long docId) {
        if (bankId <= 0) return;
        ContentValues cv = new ContentValues();
        cv.put("ts", System.currentTimeMillis());
        cv.put("date_j", dateJ);
        cv.put("bank_id", bankId);
        cv.put("kind", kind);
        cv.put("amount", amount);
        cv.put("descr", descr);
        cv.put("cid", cid);
        cv.put("doc_id", docId);
        db.ins("bank_tx", cv);
    }

    /** چک */
    public static void check(Db db, String dateJ, String dueJ, long amount, int bankId, int cid, String cname,
                             String kind, String no, String descr, long docId) {
        ContentValues cv = new ContentValues();
        cv.put("ts", System.currentTimeMillis());
        cv.put("date_j", dateJ);
        cv.put("due_j", dueJ);
        cv.put("amount", amount);
        cv.put("bank_id", bankId);
        cv.put("cid", cid);
        cv.put("cname", cname);
        cv.put("kind", kind);
        cv.put("no", no);
        cv.put("status", "open");
        cv.put("descr", descr);
        cv.put("doc_id", docId);
        db.ins("checks", cv);
    }

    /** دارایی generic (سکه/شمش/ارز/نقره/کارساخته) — scope: stock | customer */
    public static void asset(Db db, long docId, String dateJ, String scope, String asset, double qty, int karat, int cid, String descr) {
        ContentValues cv = new ContentValues();
        cv.put("doc_id", docId);
        cv.put("ts", System.currentTimeMillis());
        cv.put("date_j", dateJ);
        cv.put("scope", scope);
        cv.put("asset", asset);
        cv.put("qty", qty);
        cv.put("karat", karat);
        cv.put("cid", cid);
        cv.put("descr", descr);
        db.ins("assets_ledger", cv);
    }

    /** نام نمایشی دارایی بر اساس کد داخلی */
    public static String assetName(Db db, String asset) {
        if (asset.startsWith("work")) return "کارساخته (وزنی)";
        String name = null;
        int id = parseId(asset);
        if (id > 0) {
            android.database.Cursor c = db.r().rawQuery("SELECT name FROM defs WHERE id=?", new String[]{"" + id});
            if (c.moveToFirst()) name = c.getString(0);
            c.close();
        }
        if (name == null) name = asset;
        return name;
    }
    static int parseId(String asset) {
        int i = asset.lastIndexOf("_d");
        if (i < 0) return 0;
        try { return Integer.parseInt(asset.substring(i + 2)); } catch (Exception e) { return 0; }
    }
    public static String assetUnit(String asset) {
        if (asset.startsWith("coin") || asset.startsWith("bull")) return "عدد";
        if (asset.startsWith("cur") || asset.startsWith("sil") || asset.startsWith("work")) return "گرم/واحد";
        return "";
    }
    public static String fmtQty(String asset, double qty) {
        if (asset.startsWith("coin") || asset.startsWith("bull")) return U.intFa(Math.round(qty)) + " عدد";
        if (asset.startsWith("work") || asset.startsWith("sil")) return U.gs((int) Math.round(qty));
        if (asset.startsWith("cur")) return U.intFa(Math.round(qty * 100) / 100);
        return U.intFa(Math.round(qty));
    }
}

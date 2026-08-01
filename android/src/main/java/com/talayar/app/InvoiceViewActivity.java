package com.talayar.app;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;

/** نمایش فاکتور + اشتراک‌گذاری + ابطال با سند معکوس */
public class InvoiceViewActivity extends A {
    private int iid;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        iid = getIntent().getIntExtra("id", 0);
        scaffold("فاکتور شماره " + U.dig(iid + ""), true);
        render();
    }

    private void render() {
        body.removeAllViews();
        Cursor c = db.r().rawQuery("SELECT * FROM invoices WHERE id=?", new String[]{"" + iid});
        if (!c.moveToFirst()) { c.close(); finish(); return; }
        final String date = Db.cs(c, "date_j");
        final String cname = Db.cs(c, "cname");
        final long rate = Db.cl(c, "rate");
        final long total = Db.cl(c, "total");
        final long pcash = Db.cl(c, "pcash");
        final long pgw = Db.cl(c, "pgold_mw");
        final long pgv = Db.cl(c, "pgold_val");
        final long pgk = Db.cl(c, "pgold_karat");
        final long debt = Db.cl(c, "debt");
        final String note = Db.cs(c, "note");
        final long cid = Db.cl(c, "cid");
        c.close();

        LinearLayout head = cardHi();
        head.addView(tv("🧾 فاکتور فروش طلا", U.GOLD, 17, true));
        String shop = db.getS("shop", "");
        if (shop.length() > 0) head.addView(tv(shop + (db.getS("shopTel", "").length() > 0 ? " • " + U.dig(db.getS("shopTel", "")) : ""), U.SUB, 12, false));
        head.addView(kv("شماره فاکتور", U.dig(iid + "")));
        head.addView(kv("تاریخ", U.dig(date)));
        head.addView(kv("مشتری", cname.length() > 0 ? cname : "عابر"));
        head.addView(kv("نرخ روز (گرم ۱۸)", U.money(rate) + " تومان"));
        body.addView(head);

        // اقلام
        Cursor lc = db.r().rawQuery("SELECT * FROM invoice_lines WHERE iid=?", new String[]{"" + iid});
        int i = 1;
        while (lc.moveToNext()) {
            LinearLayout card = card(dp(10));
            card.addView(tv(U.dig(i + "") + ". " + Db.cs(lc, "title"), U.TXT, 14, true));
            card.addView(tv(U.karatName(Db.ci(lc, "karat")) + " • " + U.mw(Db.ci(lc, "wmw")) + " گرم × " + U.money(Db.cl(lc, "unit")), U.SUB, 12, false));
            long goldv = Math.round(Db.ci(lc, "wmw") * Db.cl(lc, "unit") / 1000.0);
            card.addView(kv("طلا", U.money(goldv)));
            if (Db.cl(lc, "wage") > 0) card.addView(kv("اجرت", U.money(Db.cl(lc, "wage"))));
            if (Db.cl(lc, "stone") > 0) card.addView(kv("سنگ/مزین", U.money(Db.cl(lc, "stone"))));
            if (Db.cl(lc, "tax") > 0) card.addView(kv("مالیات", U.money(Db.cl(lc, "tax"))));
            card.addView(kv("جمع قلم", U.money(Db.cl(lc, "total")) + " تومان", U.GOLD));
            body.addView(card);
            i++;
        }
        lc.close();

        // تسویه
        LinearLayout pay = card();
        pay.addView(kv("جمع کل فاکتور", U.money(total) + " تومان", U.GOLD));
        pay.addView(kv("پرداخت نقدی", U.money(pcash) + " تومان"));
        if (pgw > 0) pay.addView(kv("طلا دریافتی", U.mw((int) pgw) + " گرم " + (pgk > 0 ? U.karatName((int) pgk) : "") + " ≈ " + U.money(pgv) + " تومان"));
        if (debt > 0) pay.addView(kv("مانده (بدهی مشتری)", U.money(debt) + " تومان", 0xFFFFCC80));
        else if (debt < 0) pay.addView(kv("اضافه‌پرداخت (بستانکاری)", U.money(-debt) + " تومان", U.OK));
        else pay.addView(kv("وضعیت", "تسویه کامل ✓", U.OK));
        if (note != null && note.length() > 0) pay.addView(kv("یادداشت", note));
        body.addView(pay);

        addBtn(body, btn("↗ اشتراک‌گذاری فاکتور (متن)", new Tap() {
            public void go() { share(iid); }
        }));
        addBtn(body, gbtn("📄 خروجی PDF فاکتور", new Tap() {
            public void go() { exportInvoicePdf(iid); }
        }));
        addBtn(body, dbtn("ابطال فاکتور (سند معکوس)", new Tap() {
            public void go() { voidInvoice(iid); }
        }));
    }

    private String buildText(int iid) {
        Cursor c = db.r().rawQuery("SELECT * FROM invoices WHERE id=?", new String[]{"" + iid});
        if (!c.moveToFirst()) { c.close(); return ""; }
        StringBuilder sb = new StringBuilder();
        String shop = db.getS("shop", "");
        sb.append("🧾 فاکتور فروش طلا");
        if (shop.length() > 0) sb.append(" — ").append(shop);
        sb.append("\nشماره: ").append(U.dig(iid + ""));
        sb.append("\nتاریخ: ").append(U.dig(Db.cs(c, "date_j")));
        String cn = Db.cs(c, "cname");
        sb.append("\nمشتری: ").append(cn.length() > 0 ? cn : "عابر");
        sb.append("\nنرخ روز: ").append(U.money(Db.cl(c, "rate"))).append(" تومان");
        sb.append("\n──────────────");
        c.close();
        Cursor lc = db.r().rawQuery("SELECT * FROM invoice_lines WHERE iid=?", new String[]{"" + iid});
        int i = 1;
        while (lc.moveToNext()) {
            long goldv = Math.round(Db.ci(lc, "wmw") * Db.cl(lc, "unit") / 1000.0);
            sb.append("\n").append(U.dig(i + "")).append(") ").append(Db.cs(lc, "title"));
            sb.append(" — ").append(U.karatName(Db.ci(lc, "karat")));
            sb.append(" ").append(U.mw(Db.ci(lc, "wmw"))).append(" گرم");
            sb.append("\n   طلا: ").append(U.money(goldv));
            if (Db.cl(lc, "wage") > 0) sb.append("  اجرت: ").append(U.money(Db.cl(lc, "wage")));
            if (Db.cl(lc, "stone") > 0) sb.append("  سنگ: ").append(U.money(Db.cl(lc, "stone")));
            if (Db.cl(lc, "tax") > 0) sb.append("  مالیات: ").append(U.money(Db.cl(lc, "tax")));
            sb.append("\n   جمع: ").append(U.money(Db.cl(lc, "total"))).append(" تومان");
            i++;
        }
        lc.close();
        Cursor c2 = db.r().rawQuery("SELECT * FROM invoices WHERE id=?", new String[]{"" + iid});
        c2.moveToFirst();
        sb.append("\n──────────────");
        sb.append("\nجمع کل: ").append(U.money(Db.cl(c2, "total"))).append(" تومان");
        sb.append("\nپرداخت نقدی: ").append(U.money(Db.cl(c2, "pcash"))).append(" تومان");
        if (Db.cl(c2, "pgold_mw") > 0)
            sb.append("\nطلا دریافتی: ").append(U.mw((int) Db.cl(c2, "pgold_mw"))).append(" گرم ≈ ").append(U.money(Db.cl(c2, "pgold_val"))).append(" تومان");
        long debt = Db.cl(c2, "debt");
        if (debt > 0) sb.append("\nمانده (بدهی): ").append(U.money(debt)).append(" تومان");
        else if (debt < 0) sb.append("\nاضافه‌پرداخت: ").append(U.money(-debt)).append(" تومان");
        else sb.append("\nتسویه کامل ✓");
        String note = Db.cs(c2, "note");
        if (note.length() > 0) sb.append("\nیادداشت: ").append(note);
        sb.append("\n— طلایار، حسابداری طلافروشی");
        c2.close();
        return sb.toString();
    }

    private void share(int iid) {
        String txt = buildText(iid);
        Intent it = new Intent(Intent.ACTION_SEND);
        it.setType("text/plain");
        it.putExtra(Intent.EXTRA_TEXT, txt);
        startActivity(Intent.createChooser(it, "اشتراک‌گذاری فاکتور"));
    }

    private void exportInvoicePdf(int iid) {
        Cursor c = db.r().rawQuery("SELECT * FROM invoices WHERE id=?", new String[]{"" + iid});
        if (!c.moveToFirst()) { c.close(); return; }
        String date = Db.cs(c, "date_j");
        String cn = Db.cs(c, "cname");
        long rate = Db.cl(c, "rate");
        long total = Db.cl(c, "total");
        long pcash = Db.cl(c, "pcash");
        long pgw = Db.cl(c, "pgold_mw");
        long pgv = Db.cl(c, "pgold_val");
        long pgk = Db.cl(c, "pgold_karat");
        long debt = Db.cl(c, "debt");
        String note = Db.cs(c, "note");
        c.close();

        ArrayList<InvoicePdf.LineInfo> pdfLines = new ArrayList<InvoicePdf.LineInfo>();
        Cursor lc = db.r().rawQuery("SELECT * FROM invoice_lines WHERE iid=?", new String[]{"" + iid});
        while (lc.moveToNext()) {
            pdfLines.add(new InvoicePdf.LineInfo(
                    Db.cs(lc, "title"),
                    Db.ci(lc, "karat"),
                    Db.ci(lc, "wmw"),
                    Db.cl(lc, "unit"),
                    Db.cl(lc, "total")
            ));
        }
        lc.close();

        InvoicePdf.generateAndShare(this, db, false, iid, date, cn, rate, total, pcash, pgw, pgv, pgk, debt, note, pdfLines);
    }

    private void voidInvoice(final int iid) {
        confirm("فاکتور شماره " + U.dig(iid + "") + " باطل شود؟\nاقلام انبار به موجودی برمی‌گردند و اسناد معکوس ثبت می‌شود.", new Tap() {
            public void go() {
                SQLiteDatabase w = db.w();
                w.beginTransaction();
                try {
                    Cursor c = w.rawQuery("SELECT * FROM invoices WHERE id=?", new String[]{"" + iid});
                    if (!c.moveToFirst()) { c.close(); w.endTransaction(); return; }
                    long pcash = Db.cl(c, "pcash");
                    long pgw = Db.cl(c, "pgold_mw");
                    long pgk = Db.cl(c, "pgold_karat");
                    long debt = Db.cl(c, "debt");
                    long cid = Db.cl(c, "cid");
                    String date = Db.cs(c, "date_j");
                    c.close();
                    // برگشت اقلام به انبار
                    Cursor lc = w.rawQuery("SELECT item_id FROM invoice_lines WHERE iid=?", new String[]{"" + iid});
                    while (lc.moveToNext()) {
                        int itemId = lc.getInt(0);
                        if (itemId > 0) {
                            android.content.ContentValues u = new android.content.ContentValues();
                            u.put("status", "stock");
                            w.update("items", u, "id=?", new String[]{"" + itemId});
                        }
                    }
                    lc.close();
                    long ts = System.currentTimeMillis();
                    String tj = Jal.today();
                    if (pcash > 0) {
                        android.content.ContentValues cx = new android.content.ContentValues();
                        cx.put("ts", ts); cx.put("date_j", tj); cx.put("kind", "out");
                        cx.put("amount", pcash); cx.put("descr", "ابطال فاکتور شماره " + U.dig(iid + "") + " (برگشت وجه نقد)"); cx.put("iid", 0);
                        w.insert("cash_tx", null, cx);
                    }
                    if (pgw > 0) {
                        android.content.ContentValues gt = new android.content.ContentValues();
                        gt.put("ts", ts); gt.put("date_j", tj); gt.put("kind", "out");
                        gt.put("wmw", pgw); gt.put("karat", pgk > 0 ? (int) pgk : 750);
                        gt.put("descr", "ابطال فاکتور " + U.dig(iid + "") + " (برگشت طلای دریافتی)"); gt.put("cid", cid);
                        w.insert("gold_tx", null, gt);
                    }
                    if (debt != 0) {
                        android.content.ContentValues ct = new android.content.ContentValues();
                        ct.put("cid", cid); ct.put("ts", ts); ct.put("date_j", tj);
                        ct.put("cash", -debt); ct.put("goldmw", 0);
                        ct.put("descr", "ابطال فاکتور شماره " + U.dig(iid + ""));
                        w.insert("customer_tx", null, ct);
                    }
                    // حذف فاکتور و اقلام
                    w.delete("invoice_lines", "iid=?", new String[]{"" + iid});
                    w.delete("invoices", "id=?", new String[]{"" + iid});
                    w.setTransactionSuccessful();
                    U.toast(InvoiceViewActivity.this, "فاکتور باطل شد");
                    finish();
                } catch (Exception e) {
                    msg("خطا", e.getMessage() == null ? "خطا" : e.getMessage());
                } finally {
                    w.endTransaction();
                }
            }
        });
    }
}

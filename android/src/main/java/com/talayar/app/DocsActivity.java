package com.talayar.app;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** لیست اسناد ثبت‌شده — ردیف شرح — استعلام — حذف */
public class DocsActivity extends A {
    private LinearLayout list;
    private EditText search;
    private int openDoc = 0;
    private int sortMode = 0; // 0=زمان ثبت 1=زمان به‌روزرسانی

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        openDoc = getIntent().getIntExtra("doc", 0);
        scaffold("اسناد", true);

        LinearLayout find = card();
        find.addView(tv("استعلام سند", U.GOLD, 14, true));
        search = in("شماره سند، تاریخ (۱۴۰۵/…)، بخشی از شرح یا نام مشتری…", false);
        find.addView(search);
        find.addView(space(4));
        find.addView(chipsRow(new String[]{"بر اساس زمان ثبت", "بر اساس زمان به‌روزرسانی"}, 0, new OnIdx() {
            public void ok(int i) { sortMode = i; refresh(); }
        }));
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { refresh(); }
            public void afterTextChanged(Editable s) {}
        });
        body.addView(find);

        list = v();
        body.addView(list);
        refresh();
    }

    @Override protected void onResume() { super.onResume(); if (list != null) refresh(); }

    private void refresh() {
        list.removeAllViews();
        String q = U.en(U.str(search)).trim();
        String order = sortMode == 0 ? "ts DESC, id DESC" : "upd_ts DESC, id DESC";
        Cursor c = db.r().rawQuery("SELECT id, ts, date_j, descr, upd_ts FROM docs ORDER BY " + order + " LIMIT 400", null);
        boolean any = false;
        while (c.moveToNext()) {
            final int id = c.getInt(0);
            String date = c.getString(2);
            String descr = c.getString(3);
            if (q.length() > 0) {
                String hay = U.en(id + " " + date + " " + (descr == null ? "" : descr) + " " + rowsText(id));
                if (!hay.contains(q)) continue;
            }
            any = true;
            LinearLayout card = card();
            LinearLayout top = h();
            top.addView(tv("سند " + U.dig(id + ""), U.GOLD, 15, true));
            top.addView(wspace(8));
            top.addView(tv(U.dig(date), U.SUB, 12, false),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            TextView del = tv(" 🗑 ", U.BAD, 14, true);
            del.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { delDoc(id); }
            });
            top.addView(del);
            card.addView(top);
            // ردیف‌های شرح
            Cursor rc = db.r().rawQuery("SELECT txt FROM doc_rows WHERE doc_id=? ORDER BY seq", new String[]{"" + id});
            boolean first = true;
            while (rc.moveToNext()) {
                if (first) card.addView(space(2));
                first = false;
                TextView t = tv("▪ " + rc.getString(0), id == openDoc ? U.GOLD : U.TXT, 13, false);
                t.setPadding(dp(4), dp(1), 0, dp(1));
                card.addView(t);
            }
            rc.close();
            if (id == openDoc) card.setBackgroundDrawable(round(0xFF1E2533, 16, 0xFF6B5518, 1));
            list.addView(card);
        }
        c.close();
        if (!any) {
            LinearLayout e = card();
            e.addView(tv("سندی یافت نشد. از «ثبت سند جدید» در داشبورد شروع کنید.", U.SUB, 13, false));
            list.addView(e);
        }
    }

    /** اتیکت‌های مرتبط با سند (خط‌های «اتیکت #<num>») را به حالت موجود برمی‌گرداند */
    private void revertEtikets(SQLiteDatabase w, String txt) {
        int p = txt.indexOf("اتیکت #");
        while (p >= 0) {
            int i = p + 7;
            while (i < txt.length() && txt.charAt(i) >= '0' && txt.charAt(i) <= '9') i++;
            if (i > p + 7) {
                android.content.ContentValues ecv = new android.content.ContentValues();
                ecv.put("status", "stock");
                ecv.put("updated_ts", System.currentTimeMillis());
                w.update("etiket", ecv, "id=?", new String[]{txt.substring(p + 7, i)});
            }
            p = txt.indexOf("اتیکت #", i);
        }
    }

    private String rowsText(int id) {
        Cursor rc = db.r().rawQuery("SELECT txt FROM doc_rows WHERE doc_id=?", new String[]{"" + id});
        StringBuilder sb = new StringBuilder();
        while (rc.moveToNext()) sb.append(rc.getString(0)).append(' ');
        rc.close();
        return sb.toString();
    }

    private void delDoc(final int id) {
        LinearLayout box = v();
        box.setPadding(dp(16), dp(14), dp(16), dp(10));
        box.addView(tv("حذف سند شماره " + U.dig(id + ""), U.GOLD, 16, true));
        TextView mm = tv("با حذف سند، همهٔ اثرات آن (تراز صندوق، بانک، آبشده، حساب مشتری، سکه/ارز/شمش و چک) برگردانده می‌شود.\nاین کار قابل بازگشت نیست.", U.TXT, 13, false);
        mm.setLineSpacing(4, 1.2f);
        box.addView(mm);
        box.addView(space(8));
        final android.app.AlertDialog d = sheet(box);
        LinearLayout br = h();
        br.setGravity(android.view.Gravity.CENTER);
        br.addView(dbtn("حذف قطعی", new Tap() { public void go() {
            d.dismiss();
            doDelete(id);
        } }), new LinearLayout.LayoutParams(dp(130), ViewGroup.LayoutParams.WRAP_CONTENT));
        br.addView(wspace(10));
        br.addView(gbtn("انصراف", new Tap() { public void go() { d.dismiss(); } }),
                new LinearLayout.LayoutParams(dp(110), ViewGroup.LayoutParams.WRAP_CONTENT));
        box.addView(br);
    }

    private void doDelete(int id) {
        SQLiteDatabase w = db.w();
        w.beginTransaction();
        try {
            // برگشت وضعیت اتیکت‌های این سند به «موجود» قبل از حذف
            revertEtikets(w, rowsText(id));
            w.delete("doc_rows", "doc_id=?", new String[]{"" + id});
            w.delete("assets_ledger", "doc_id=?", new String[]{"" + id});
            w.delete("bank_tx", "doc_id=?", new String[]{"" + id});
            w.delete("checks", "doc_id=?", new String[]{"" + id});
            w.delete("gold_tx", "doc_id=?", new String[]{"" + id});
            w.delete("customer_tx", "iid=?", new String[]{"" + (-id)});
            w.delete("cash_tx", "iid=?", new String[]{"" + (-id)});
            w.delete("docs", "id=?", new String[]{"" + id});
            w.setTransactionSuccessful();
            U.toast(this, "سند حذف شد ✓");
            refresh();
        } catch (Exception e) {
            msg("خطا", e.getMessage() == null ? "خطا" : e.getMessage());
        } finally {
            w.endTransaction();
        }
    }
}

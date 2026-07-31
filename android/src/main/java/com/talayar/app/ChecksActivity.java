package com.talayar.app;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** چک‌ها — وضعیت (باز/پاس‌شده/برگشتی/ابطال) */
public class ChecksActivity extends A {
    private LinearLayout list;
    private int filter = 0;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        scaffold("چک‌ها", true);
        LinearLayout f = card();
        f.addView(chipsRow(new String[]{"همه", "باز", "پاس‌شده", "برگشتی"}, 0, new OnIdx() {
            public void ok(int i) { filter = i; refresh(); }
        }));
        body.addView(f);
        list = v();
        body.addView(list);
        refresh();
    }

    @Override protected void onResume() { super.onResume(); if (list != null) refresh(); }

    static String stName(String s) {
        if ("open".equals(s)) return "باز";
        if ("pass".equals(s)) return "پاس‌شده";
        if ("ret".equals(s)) return "برگشتی";
        if ("void".equals(s)) return "باطل";
        return s;
    }
    static boolean stGreen(String s) { return "pass".equals(s); }

    private void refresh() {
        list.removeAllViews();
        StringBuilder sql = new StringBuilder("SELECT * FROM checks WHERE 1=1");
        if (filter == 1) sql.append(" AND status='open'");
        if (filter == 2) sql.append(" AND status='pass'");
        if (filter == 3) sql.append(" AND status='ret'");
        sql.append(" ORDER BY due_j, id DESC LIMIT 300");
        Cursor c = db.r().rawQuery(sql.toString(), null);
        boolean any = false;
        while (c.moveToNext()) {
            any = true;
            final int id = Db.ci(c, "id");
            final String kind = Db.cs(c, "kind");
            final String status = Db.cs(c, "status");
            String no = Db.cs(c, "no");
            String due = Db.cs(c, "due_j");
            final long amt = Db.cl(c, "amount");
            String cname = Db.cs(c, "cname");

            LinearLayout card = card();
            LinearLayout top = h();
            top.addView(tv(("recv".equals(kind) ? "↙ چک دریافتی" : "↗ چک پرداختنی") + " " + U.dig(no),
                    "recv".equals(kind) ? U.OK : 0xFFFFA9B1, 14, true),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            top.addView(badge(stName(status), stGreen(status) || "open".equals(status)));
            card.addView(top);
            card.addView(kv("مبلغ", U.money(amt) + " تومان"));
            card.addView(kv("سررسید", U.dig(due)));
            card.addView(kv("طرف حساب", cname));
            if ("open".equals(status)) {
                LinearLayout row = h();
                row.addView(btn("پاس شد ✓", new Tap() { public void go() { setStatus(id, kind, amt, "pass"); } }),
                        new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                row.addView(wspace(6));
                row.addView(gbtn("برگشت خورد ↩", new Tap() { public void go() { setStatus(id, kind, amt, "ret"); } }),
                        new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                row.addView(wspace(6));
                row.addView(dbtn("ابطال", new Tap() { public void go() { setStatus(id, kind, amt, "void"); } }),
                        new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                card.addView(space(6));
                card.addView(row);
            }
            list.addView(card);
        }
        c.close();
        if (!any) {
            LinearLayout e = card();
            e.addView(tv("چکی ثبت نشده — از «ثبت سند» گروه «چک» استفاده کنید.", U.SUB, 13, false));
            list.addView(e);
        }
    }

    private void setStatus(final int id, final String kind, final long amt, final String st) {
        String verb = "pass".equals(st) ? "پاس شدن" : "ret".equals(st) ? "برگشت خوردن" : "ابطال";
        confirm(verb + " این چک ثبت شود؟" + ("pass".equals(st) ? "\n(مبلغ به/از بانک منتقل می‌شود)" : ""), new Tap() {
            public void go() {
                android.content.ContentValues cv = new android.content.ContentValues();
                cv.put("status", st);
                db.w().update("checks", cv, "id=?", new String[]{"" + id});
                if ("pass".equals(st)) {
                    Cursor c = db.r().rawQuery("SELECT bank_id, cid, no FROM checks WHERE id=?", new String[]{"" + id});
                    if (c.moveToFirst()) {
                        int bankId = c.getInt(0), cid = c.getInt(1);
                        String no = c.getString(2);
                        String dateJ = Jal.today();
                        // بانک def id → موجودی بانک (در bank_tx، bank_id = defs id از انتخاب کاربر؟)
                        // در چک، bank_id همان defs id بانک است؛ پست بانکی مستقیم با همان id انجام می‌شود
                        if ("recv".equals(kind)) {
                            Post.bank(db, dateJ, bankId, "in", amt, "پاس شدن چک دریافتی " + U.dig(no), cid, 0);
                            Post.cust(db, 0, dateJ, cid, -amt, 0, "تأمین چک دریافتی " + U.dig(no) + " (پاس شد)");
                        } else {
                            Post.bank(db, dateJ, bankId, "out", amt, "پاس شدن چک پرداختنی " + U.dig(no), cid, 0);
                            Post.cust(db, 0, dateJ, cid, +amt, 0, "تأمین چک پرداختنی " + U.dig(no) + " (پاس شد)");
                        }
                    }
                    c.close();
                } else if ("ret".equals(st) || "void".equals(st)) {
                    // برگشت اثر تسویه اولیه در حساب مشتری
                    Cursor c = db.r().rawQuery("SELECT cid, no FROM checks WHERE id=?", new String[]{"" + id});
                    if (c.moveToFirst()) {
                        int cid = c.getInt(0);
                        String no = c.getString(1);
                        String dateJ = Jal.today();
                        if ("recv".equals(kind)) Post.cust(db, 0, dateJ, cid, +amt, 0, "برگشت چک دریافتی " + U.dig(no));
                        else Post.cust(db, 0, dateJ, cid, -amt, 0, "ابطال چک پرداختنی " + U.dig(no));
                    }
                    c.close();
                }
                U.toast(ChecksActivity.this, "ثبت شد ✓");
                refresh();
            }
        });
    }
}

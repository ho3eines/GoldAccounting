package com.talayar.app;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** فهرست مشتریان با مانده حساب */
public class CustomersActivity extends A {
    private LinearLayout list;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        scaffold("مشتریان", true);
        LinearLayout add = card();
        addBtn(add, btn("＋ مشتری جدید", new Tap() { public void go() { addCustomer(); } }));
        long[] sp = db.customerDebtSplit();
        add.addView(kv("مجموع بدهی نقدی مشتریان", U.money(sp[0]) + " تومان", 0xFFFFCC80));
        add.addView(kv("مجموع بستانکاری نقدی", U.money(sp[1]) + " تومان", U.OK));
        add.addView(kv("مجموع بدهی طلایی (۱۸ معادل)", U.mw((int) sp[2]) + " گرم", 0xFFFFCC80));
        body.addView(add);
        list = v();
        body.addView(list);
        refresh();
    }

    @Override protected void onResume() { super.onResume(); if (list != null) refresh(); }

    private void addCustomer() {
        final LinearLayout box = v();
        box.setPadding(dp(16), dp(14), dp(16), dp(10));
        box.addView(tv("مشتری جدید", U.GOLD, 16, true));
        box.addView(space(6));
        final android.widget.EditText e1 = in("نام و نام خانوادگی");
        final android.widget.EditText e2 = in("تلفن (اختیاری)", true);
        final android.widget.EditText e3 = in("یادداشت (اختیاری)");
        box.addView(e1); box.addView(space(6)); box.addView(e2); box.addView(space(6)); box.addView(e3);
        box.addView(space(8));
        final android.app.AlertDialog d = new android.app.AlertDialog.Builder(this).create();
        LinearLayout br = h();
        br.setGravity(android.view.Gravity.CENTER);
        br.addView(btn("ثبت", new Tap() { public void go() {
            String n = U.str(e1);
            if (n.length() == 0) { U.toast(CustomersActivity.this, "نام را بنویسید"); return; }
            android.content.ContentValues cv = new android.content.ContentValues();
            cv.put("name", n);
            cv.put("phone", U.en(U.str(e2)));
            cv.put("note", U.str(e3));
            cv.put("cts", System.currentTimeMillis());
            db.ins("customers", cv);
            d.dismiss();
            U.toast(CustomersActivity.this, "مشتری ثبت شد ✓");
            refresh();
        } }), new LinearLayout.LayoutParams(dp(130), ViewGroup.LayoutParams.WRAP_CONTENT));
        br.addView(wspace(10));
        br.addView(gbtn("انصراف", new Tap() { public void go() { d.dismiss(); } }),
                new LinearLayout.LayoutParams(dp(130), ViewGroup.LayoutParams.WRAP_CONTENT));
        box.addView(br);
        d.setView(box);
        d.show();
        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(round(0xFF151C28, 18, 0xFF2E3A4E, 1));
            d.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void refresh() {
        list.removeAllViews();
        Cursor c = db.r().rawQuery(
                "SELECT c.id, c.name, c.phone, COALESCE(s.cash,0), COALESCE(s.gold,0) FROM customers c " +
                "LEFT JOIN (SELECT cid, SUM(cash) cash, SUM(goldmw) gold FROM customer_tx GROUP BY cid) s ON s.cid=c.id " +
                "ORDER BY c.name", null);
        boolean any = false;
        while (c.moveToNext()) {
            any = true;
            final int id = c.getInt(0);
            String name = c.getString(1);
            String phone = c.getString(2);
            long cash = c.getLong(3);
            long gold = c.getLong(4);

            LinearLayout card = card();
            LinearLayout top = h();
            top.addView(tv(name, U.TXT, 15, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            if (cash > 0) top.addView(badge("بدهکار " + U.money(cash), false));
            else if (cash < 0) top.addView(badge("بستانکار " + U.money(-cash), true));
            else top.addView(badge("تسویه", true));
            card.addView(top);
            LinearLayout sub = h();
            sub.addView(tv(phone != null && phone.length() > 0 ? U.dig(phone) : "—", U.SUB, 12, false),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            if (gold != 0) {
                sub.addView(tvM(U.mw((int) Math.abs(gold)) + " گرم طلا " + (gold > 0 ? "بدهکار" : "بستانکار"),
                        gold > 0 ? 0xFFFFCC80 : U.OK, 12));
            }
            card.addView(sub);
            card.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent i = new Intent(CustomersActivity.this, CustomerViewActivity.class);
                    i.putExtra("id", id);
                    startActivity(i);
                }
            });
            list.addView(card);
        }
        c.close();
        if (!any) {
            LinearLayout e = card();
            e.addView(tv("مشتری ثبت نشده — با دکمهٔ بالا اضافه کنید.", U.SUB, 13, false));
            list.addView(e);
        }
    }
}

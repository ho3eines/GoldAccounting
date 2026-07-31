package com.talayar.app;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** فهرست فاکتورها */
public class InvoicesActivity extends A {
    private EditText search;
    private LinearLayout list;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        scaffold("فاکتورها", true);
        search = in("جستجو: شماره یا نام مشتری…", false);
        body.addView(search);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { refresh(); }
            public void afterTextChanged(Editable s) {}
        });
        LinearLayout sum = card();
        long[] today = db.salesStats(Jal.today());
        long[] month = db.salesStats(Jal.thisMonth());
        sum.addView(kv("فروش امروز", U.intFa(today[0]) + " فاکتور • " + U.money(today[1]) + " تومان", U.GOLD));
        sum.addView(kv("فروش این ماه", U.intFa(month[0]) + " فاکتور • " + U.money(month[1]) + " تومان"));
        body.addView(sum);
        list = v();
        body.addView(list);
        refresh();
    }

    @Override protected void onResume() { super.onResume(); if (list != null) refresh(); }

    private void refresh() {
        list.removeAllViews();
        String q = U.en(U.str(search)).trim();
        Cursor c = db.r().rawQuery("SELECT id, date_j, cname, total, debt FROM invoices ORDER BY id DESC LIMIT 300", null);
        boolean any = false;
        while (c.moveToNext()) {
            final int id = c.getInt(0);
            String date = c.getString(1);
            String cname = c.getString(2);
            long total = c.getLong(3);
            long debt = c.getLong(4);
            if (q.length() > 0) {
                String hay = id + " " + U.en(cname == null ? "" : cname);
                if (!hay.contains(q)) continue;
            }
            any = true;
            LinearLayout card = card(dp(10));
            LinearLayout top = h();
            top.addView(tv("فاکتور " + U.dig(id + ""), U.TXT, 14, true),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            if (debt > 0) top.addView(badge("مانده " + U.money(debt), false));
            else if (debt < 0) top.addView(badge("بستانکار", true));
            else top.addView(badge("تسویه", true));
            card.addView(top);
            LinearLayout sub = h();
            sub.addView(tv(U.dig(date) + " • " + (cname != null && cname.length() > 0 ? cname : "عابر"),
                    U.SUB, 12, false), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            sub.addView(tvM(U.money(total), U.GOLD, 14));
            card.addView(sub);
            card.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent i = new Intent(InvoicesActivity.this, InvoiceViewActivity.class);
                    i.putExtra("id", id);
                    startActivity(i);
                }
            });
            list.addView(card);
        }
        c.close();
        if (!any) {
            LinearLayout e = card();
            e.addView(tv("فاکتوری ثبت نشده است.", U.SUB, 13, false));
            list.addView(e);
        }
    }
}

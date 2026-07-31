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

/** اجناس و انبار طلا */
public class ItemsActivity extends A {
    private EditText search;
    private int filter = 0; // 0=همه 1=موجود 2=فروخته‌شده

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        scaffold("اجناس و انبار", true);
        search = in("جستجو: نام یا کد جنس…", false);
        body.addView(search);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { refresh(); }
            public void afterTextChanged(Editable s) {}
        });
        body.addView(space(4));
        body.addView(chipsRow(new String[]{"همه", "فقط موجود", "فروخته‌شده"}, 0, new OnIdx() {
            public void ok(int i) { filter = i; refresh(); }
        }));
        body.addView(space(4));
        body.addView(cardWithAdd());
        body.addView(body2());
    }

    LinearLayout itemsList;
    private LinearLayout cardWithAdd() {
        LinearLayout c = card();
        long rate = db.currentRate();
        Db.Pair st = db.itemsStock();
        c.addView(kv("تعداد اجناس موجود", U.intFa(st.a) + " قلم"));
        c.addView(kv("وزن اجناس موجود", U.mw((int) st.b) + " گرم"));
        if (rate > 0 && st.b > 0) {
            c.addView(kv("ارزش تقریبی طلای موجود", U.money(Math.round(st.b * rate * 1.0 / 1000.0)) + " تومان",
                    U.GOLD));
        }
        addBtn(c, btn("＋ افزودن جنس", new Tap() {
            public void go() {
                startActivity(new Intent(ItemsActivity.this, ItemEditActivity.class));
            }
        }));
        return c;
    }

    private LinearLayout body2() {
        itemsList = v();
        refresh();
        return itemsList;
    }

    @Override protected void onResume() { super.onResume(); if (itemsList != null) refresh(); }

    private void refresh() {
        if (itemsList == null) return;
        itemsList.removeAllViews();
        String q = U.en(U.str(search)).trim();
        StringBuilder sql = new StringBuilder("SELECT * FROM items WHERE 1=1");
        if (filter == 1) sql.append(" AND status='stock'");
        if (filter == 2) sql.append(" AND status='sold'");
        sql.append(" ORDER BY cts DESC LIMIT 500");
        Cursor c = db.r().rawQuery(sql.toString(), null);
        final long rate = db.currentRate();
        boolean any = false;
        while (c.moveToNext()) {
            String name = Db.cs(c, "name");
            int code = Db.ci(c, "code");
            if (q.length() > 0) {
                String hay = U.en(name) + " " + code;
                if (!hay.contains(q)) continue;
            }
            any = true;
            final int id = Db.ci(c, "id");
            int karat = Db.ci(c, "karat");
            int wmw = Db.ci(c, "wmw");
            int wtype = Db.ci(c, "wtype");
            int wval = Db.ci(c, "wval");
            int stoneval = Db.ci(c, "stoneval");
            String status = Db.cs(c, "status");

            LinearLayout card = card();
            LinearLayout top = h();
            TextView nm = tv(name, U.TXT, 15, true);
            top.addView(nm, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            top.addView(badge("stock".equals(status) ? "موجود" : "فروخته‌شده", "stock".equals(status)));
            card.addView(top);
            card.addView(space(2));
            LinearLayout info = h();
            info.addView(tv(U.karatName(karat) + " • کد " + U.dig(code + "") , U.SUB, 12, false),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            info.addView(tvM(U.mw(wmw) + " گرم", U.GOLD, 14));
            card.addView(info);
            String wtxt = wtype == 0 ? "اجرت " + U.pct(wval)
                    : wtype == 1 ? "اجرت " + U.money(wval) + " ت/گرم"
                    : "اجرت " + U.money(wval) + " تومان";
            LinearLayout info2 = h();
            info2.addView(tv(wtxt + (stoneval > 0 ? " • سنگ " + U.money(stoneval) : ""), U.SUB, 12, false),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            if (rate > 0) {
                long goldVal = Math.round(U.equiv750(wmw, karat) * rate / 1000.0);
                long wage = ItemEditActivity.calcWage(goldVal, wmw, wtype, wval);
                long day = goldVal + wage + stoneval;
                info2.addView(tv("≈ " + U.money(day), 0xFF8FD3A8, 13, true));
            }
            card.addView(info2);
            card.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent i = new Intent(ItemsActivity.this, ItemEditActivity.class);
                    i.putExtra("id", id);
                    startActivity(i);
                }
            });
            itemsList.addView(card);
        }
        c.close();
        if (!any) {
            LinearLayout c0 = card();
            c0.addView(tv("جنس ثبت نشده است. از دکمهٔ «افزودن جنس» شروع کنید.", U.SUB, 13, false));
            itemsList.addView(c0);
        }
    }
}

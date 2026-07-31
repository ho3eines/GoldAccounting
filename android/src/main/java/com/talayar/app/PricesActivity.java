package com.talayar.app;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** قیمت‌های بازار — دریافت مستقیم از API عمومی (بدون سرور میانی) + ویرایش دستی */
public class PricesActivity extends A {
    public static final String DEFAULT_API = "https://api.tgju.org/v1/data/sana/json";

    private LinearLayout list;
    private TextView status;
    private boolean fetching = false;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        scaffold("قیمت‌های بازار", true);

        LinearLayout head = cardHi();
        head.addView(tv("🌐 دریافت قیمت آنلاین", U.GOLD, 15, true));
        TextView hint = tv("قیمت‌ها مستقیم از گوشی شما و بدون هیچ سرور میانی از یک API عمومی خوانده می‌شوند. بدون اینترنت، همهٔ امکانات برنامه همچنان به‌صورت آفلاین کار می‌کنند و می‌توانید قیمت‌ها را دستی وارد کنید.", U.SUB, 12, false);
        hint.setLineSpacing(3, 1.2f);
        head.addView(hint);
        head.addView(space(4));
        head.addView(kv("منبع قیمت", apiUrl(), U.BLUE));
        status = tvM(lastFetchText(), U.SUB, 12);
        head.addView(status);
        addBtn(head, btn("⬇  دریافت قیمت‌ها از اینترنت", new Tap() {
            public void go() { fetch(); }
        }));
        body.addView(head);

        LinearLayout gold = card();
        long rate = db.currentRate();
        gold.addView(kv("نرخ دفتری طلای ۱۸ (نرخ روز فاکتورها)", rate > 0 ? U.moneyT(rate) : "ثبت نشده", U.GOLD));
        gold.addView(tv("با دریافت موفق قیمت، نرخ دفتری به‌صورت خودکار به‌روز می‌شود.", U.SUB, 11, false));
        body.addView(gold);

        list = v();
        body.addView(list);
        refresh();
    }

    @Override protected void onResume() { super.onResume(); if (list != null) refresh(); }

    private String apiUrl() { return db.getS("api_url", DEFAULT_API); }

    private long lastFetchTs() {
        Cursor c = db.r().rawQuery("SELECT MAX(ts) FROM prices", null);
        long r = c.moveToFirst() && !c.isNull(0) ? c.getLong(0) : 0;
        c.close();
        return r;
    }
    private long keyTs(String key) {
        Cursor c = db.r().rawQuery("SELECT ts FROM prices WHERE key=? ORDER BY ts DESC, id DESC LIMIT 1", new String[]{key});
        long r = c.moveToFirst() ? c.getLong(0) : 0;
        c.close();
        return r;
    }
    private String lastFetchText() {
        long ts = lastFetchTs();
        if (ts <= 0) return "هنوز قیمتی دریافت نشده است.";
        Jal j = Jal.of(ts);
        return "آخرین به‌روزرسانی: " + j.fa();
    }

    private void fetch() {
        if (fetching) return;
        fetching = true;
        status.setText("در حال دریافت به‌روز قیمت…");
        Net.fetchPrices(db, apiUrl(), this, new Net.Done() {
            public void ok(String err) {
                fetching = false;
                if (status != null) status.setText(lastFetchText());
                if (err != null) msg("خطا در دریافت", "اتصال برقرار نشد: " + err + "\nاینترنت را بررسی کنید یا قیمت را دستی وارد کنید.");
                else U.toast(PricesActivity.this, "قیمت‌ها به‌روز شد ✓");
                refresh();
            }
        });
    }

    private void refresh() {
        list.removeAllViews();
        for (int i = 0; i < Net.KEYS.length; i++) {
            final String key = Net.KEYS[i][0];
            String name = Net.KEYS[i][1];
            long val = db.priceGet(key);
            long ts = keyTs(key);
            LinearLayout card = card(dp(10));
            LinearLayout top = h();
            top.addView(tv(name, U.TXT, 14, true),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            String unit = "ons".equals(key) ? " دلار" : " تومان";
            top.addView(tvM(val > 0 ? U.money(val) + unit : "—", val > 0 ? U.GOLD : U.SUB, 14));
            card.addView(top);
            LinearLayout sub = h();
            sub.addView(tv(val > 0 ? "به‌روزرسانی: " + Jal.of(ts).fa() : "ثبت نشده", U.SUB, 11, false),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            TextView ed = tv(" ✎ ویرایش دستی ", U.BLUE, 12, true);
            ed.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    input("قیمت «" + Net.keyName(key) + "»", "قیمت به تومان", true, new OnText() {
                        public void ok(String s) {
                            long v2 = U.parseMoney(s);
                            if (v2 <= 0) { U.toast(PricesActivity.this, "مقدار نامعتبر"); return; }
                            db.priceSet(key, v2);
                            U.toast(PricesActivity.this, "ثبت شد ✓");
                            refresh();
                        }
                    });
                }
            });
            sub.addView(ed);
            card.addView(sub);
            list.addView(card);
        }
    }
}

package com.talayar.app;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** داشبورد اصلی «طلایار» */
public class MainActivity extends A {

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().getDecorView().setBackgroundColor(U.BG);
    }

    @Override protected void onResume() {
        super.onResume();
        build();
    }

    private void build() {
        LinearLayout root = v();
        root.setBackgroundColor(U.BG);
        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        LinearLayout col = v();
        col.setPadding(dp(14), dp(12), dp(14), dp(30));
        sv.addView(col);

        // ── هدر ──
        LinearLayout hd = h();
        LinearLayout tcol = v();
        tcol.addView(tv("طلایار", U.GOLD, 26, true));
        tcol.addView(tv("حسابداری طلافروشی", U.SUB, 13, false));
        hd.addView(tcol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        String shop = db.getS("shop", "");
        if (shop.length() > 0) {
            TextView s = tv(shop, U.TXT, 13, true);
            s.setGravity(Gravity.LEFT);
            hd.addView(s);
        }
        col.addView(hd);
        col.addView(tv(Jal.longToday(), U.SUB, 12, false));
        col.addView(space(12));

        // ── کارت نرخ امروز ──
        final long rate = db.currentRate();
        LinearLayout hero = v();
        hero.setPadding(dp(16), dp(14), dp(16), dp(14));
        GradientDrawable hg = new GradientDrawable(GradientDrawable.Orientation.BL_TR,
                new int[]{0xFF2A2110, 0xFF1A1610});
        hg.setCornerRadius(dp(18));
        hg.setStroke(dp(1), 0xFF6B5518);
        hero.setBackgroundDrawable(hg);
        hero.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { startActivity(new Intent(MainActivity.this, RateActivity.class)); }
        });
        LinearLayout hr = h();
        LinearLayout hi = v();
        hi.addView(tv("🪙", U.TXT, 30, false));
        hr.addView(hi);
        hr.addView(wspace(10));
        LinearLayout hmid = v();
        hmid.addView(tv("نرخ امروز طلا (گرم ۱۸ عیار)", 0xFFD9C68A, 13, false));
        hmid.addView(space(2));
        if (rate > 0) {
            hmid.addView(tv(U.money(rate) + " تومان", U.GOLD, 22, true));
        } else {
            hmid.addView(tv("ثبت نشده  —  برای ثبت لمس کنید", 0xFFFFB9C0, 14, true));
        }
        hr.addView(hmid, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView go = tv("❯", 0xFF8F6A16, 22, true);
        hr.addView(go);
        hero.addView(hr);
        LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hl.setMargins(0, dp(4), 0, dp(8));
        col.addView(hero, hl);

        // ── آمار کلی ──
        Db.Pair cashio = db.cashInOut();
        long cashBal = cashio.a - cashio.b;
        Db.Pair stock = db.itemsStock();
        long[] gold = db.goldBalance();
        long[] debts = db.customerDebts();
        long[] today = db.salesStats(Jal.today());

        LinearLayout grid1 = h();
        grid1.addView(statTile("💰 تراز صندوق", U.money(cashBal) + " تومان", cashBal >= 0 ? U.OK : U.BAD,
                new Tap() { public void go() { startActivity(new Intent(MainActivity.this, CashActivity.class)); } }), tileLp());
        grid1.addView(wspace(8));
        grid1.addView(statTile("📦 اجناس موجودی", U.intFa(stock.a) + " قلم • " + U.mw((int) stock.b) + " گرم", U.TXT,
                new Tap() { public void go() { startActivity(new Intent(MainActivity.this, ItemsActivity.class)); } }), tileLp());
        col.addView(grid1);

        LinearLayout grid2 = h();
        grid2.addView(statTile("⚖️ طلای آبشده", U.mw((int) gold[1]) + " گرم (۱۸ معادل)", U.TXT,
                new Tap() { public void go() { startActivity(new Intent(MainActivity.this, GoldActivity.class)); } }), tileLp());
        grid2.addView(wspace(8));
        long dc = debts[0], dg = debts[1];
        grid2.addView(statTile("👥 طلب از مشتریان",
                U.money(dc) + " تومان" + (dg != 0 ? "\n" + U.mw((int) dg) + " گرم طلا" : ""),
                dc > 0 ? 0xFFFFCC80 : U.TXT,
                new Tap() { public void go() { startActivity(new Intent(MainActivity.this, CustomersActivity.class)); } }), tileLp());
        col.addView(grid2);

        // ── فروش امروز ──
        LinearLayout todayC = card();
        todayC.addView(tv("🧾 فروش امروز", U.GOLD, 15, true));
        todayC.addView(space(4));
        LinearLayout r1 = h();
        r1.addView(tv("تعداد فاکتور", U.SUB, 12, false), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        r1.addView(tv("مبلغ کل", U.SUB, 12, false), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        r1.addView(tv("اجرت‌ها", U.SUB, 12, false), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        todayC.addView(r1);
        LinearLayout r2 = h();
        r2.addView(tvM(U.intFa(today[0]), U.TXT, 17), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        r2.addView(tvM(U.money(today[1]), U.GOLD, 17), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        r2.addView(tvM(U.money(today[2]), U.OK, 17), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        todayC.addView(r2);
        col.addView(todayC);

        // ── منو ──
        col.addView(space(6));
        menuRow(col, "📜", "ثبت سند جدید", "🗂", "اسناد",
                new Tap() { public void go() { startActivity(new Intent(MainActivity.this, DocNewActivity.class)); } },
                new Tap() { public void go() { startActivity(new Intent(MainActivity.this, DocsActivity.class)); } });
        menuRow(col, "🧾", "فاکتور جدید", "🔖", "فاکتورها",
                new Tap() { public void go() { startActivity(new Intent(MainActivity.this, InvoiceNewActivity.class)); } },
                new Tap() { public void go() { startActivity(new Intent(MainActivity.this, InvoicesActivity.class)); } });
        menuRow(col, "🌐", "قیمت‌های بازار", "⚖️", "دارایی‌ها و تراز",
                new Tap() { public void go() { startActivity(new Intent(MainActivity.this, PricesActivity.class)); } },
                new Tap() { public void go() { startActivity(new Intent(MainActivity.this, AssetsActivity.class)); } });
        menuRow(col, "🛍️", "خرید طلا", "⚖️", "طلا و آبشده",
                new Tap() { public void go() { startActivity(new Intent(MainActivity.this, BuyActivity.class)); } },
                new Tap() { public void go() { startActivity(new Intent(MainActivity.this, GoldActivity.class)); } });
        menuRow(col, "👥", "حساب‌ها / مشتریان", "📦", "اجناس و انبار",
                new Tap() { public void go() { startActivity(new Intent(MainActivity.this, CustomersActivity.class)); } },
                new Tap() { public void go() { startActivity(new Intent(MainActivity.this, ItemsActivity.class)); } });
        menuRow(col, "🏷", "اتیکت‌ها", "📄", "چک‌ها",
                new Tap() { public void go() { startActivity(new Intent(MainActivity.this, EtiketActivity.class)); } },
                new Tap() { public void go() { startActivity(new Intent(MainActivity.this, ChecksActivity.class)); } });
        menuRow(col, "📈", "نرخ طلا", "📊", "گزارش‌ها",
                new Tap() { public void go() { startActivity(new Intent(MainActivity.this, RateActivity.class)); } },
                new Tap() { public void go() { startActivity(new Intent(MainActivity.this, ReportsActivity.class)); } });
        menuRow(col, "💰", "صندوق", "🗄", "کدینگ‌ها",
                new Tap() { public void go() { startActivity(new Intent(MainActivity.this, CashActivity.class)); } },
                new Tap() { public void go() { startActivity(new Intent(MainActivity.this, DefsActivity.class)); } });
        menuRow(col, "⚙️", "تنظیمات", "ℹ️", "درباره",
                new Tap() { public void go() { startActivity(new Intent(MainActivity.this, SettingsActivity.class)); } },
                new Tap() { public void go() { startActivity(new Intent(MainActivity.this, GuideActivity.class)); } });

        TextView ver = tv("طلایار نسخهٔ ۴٫۰ • امکانات کامل آفلاین + قیمت‌خوانی آنلاین", 0xFF556275, 11, false);
        ver.setGravity(Gravity.CENTER);
        ver.setPadding(0, dp(18), 0, 0);
        col.addView(ver);

        sv.setFillViewport(false);
        setContentView(sv);
    }

    private LinearLayout.LayoutParams tileLp() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
    }

    private LinearLayout statTile(String t, String value, int vc, final Tap tap) {
        LinearLayout c = card(dp(12));
        c.addView(tv(t, U.SUB, 12, false));
        c.addView(space(3));
        TextView vv = tvM(value, vc, 15);
        c.addView(vv);
        c.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { tap.go(); } });
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) c.getLayoutParams();
        lp.setMargins(0, 0, 0, dp(8));
        return c;
    }

    private void menuRow(LinearLayout col, String ic1, String t1, String ic2, String t2, final Tap a, final Tap b) {
        LinearLayout r = h();
        LinearLayout m1 = menuTile(ic1, t1, a);
        LinearLayout m2 = menuTile(ic2, t2, b);
        r.addView(m1, tileLp());
        r.addView(wspace(8));
        r.addView(m2, tileLp());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(8));
        col.addView(r, lp);
    }

    private LinearLayout menuTile(String icon, String title, final Tap tap) {
        LinearLayout c = v();
        c.setBackgroundDrawable(cardBg());
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.setPadding(dp(8), dp(14), dp(8), dp(12));
        c.addView(tv(icon, U.TXT, 26, false));
        c.addView(space(6));
        c.addView(tv(title, U.TXT, 14, true));
        c.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { tap.go(); } });
        return c;
    }
}

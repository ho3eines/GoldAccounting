package com.talayar.app;

import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** نرخ روز طلا: ثبت، معادل‌ها، تاریخچه و نمودار */
public class RateActivity extends A {

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        scaffold("نرخ طلا", true);
        refresh();
    }

    private void refresh() {
        body.removeAllViews();
        final long rate = db.currentRate();

        LinearLayout c = card();
        c.addView(tv("نرخ فعلی (گرم طلای ۱۸ عیار)", U.SUB, 13, false));
        c.addView(space(2));
        c.addView(tv(rate > 0 ? U.money(rate) + " تومان" : "ثبت نشده", rate > 0 ? U.GOLD : U.BAD, 24, true));
        c.addView(space(8));
        addBtn(c, btn("ثبت نرخ امروز", new Tap() {
            public void go() {
                input("نرخ امروز طلا", "مبلغ هر گرم ۱۸ عیار به تومان", rate > 0 ? U.en(U.money(rate)) : "", true, new OnText() {
                    public void ok(String s) {
                        long v = U.parseMoney(s);
                        if (v <= 0) { U.toast(RateActivity.this, "مبلغ نامعتبر است"); return; }
                        android.content.ContentValues cv = new android.content.ContentValues();
                        cv.put("ts", System.currentTimeMillis());
                        cv.put("date_j", Jal.today());
                        cv.put("rate", v);
                        db.ins("rates", cv);
                        U.toast(RateActivity.this, "نرخ امروز ثبت شد ✓");
                        refresh();
                    }
                });
            }
        }));
        c.addView(space(6));
        addBtn(c, gbtn("⬇  بروزرسانی آنلاین نرخ طلا", new Tap() {
            public void go() {
                String apiUrl = db.getS("api_url", PricesActivity.DEFAULT_API);
                U.toast(RateActivity.this, "در حال دریافت آنلاین نرخ طلا…");
                Net.fetchPrices(db, apiUrl, RateActivity.this, new Net.Done() {
                    public void ok(String err) {
                        long g18 = db.priceGet("gold18");
                        if (g18 > 0) {
                            android.content.ContentValues cv = new android.content.ContentValues();
                            cv.put("ts", System.currentTimeMillis());
                            cv.put("date_j", Jal.today());
                            cv.put("rate", g18);
                            db.ins("rates", cv);
                            U.toast(RateActivity.this, "نرخ طلای ۱۸ عیار به‌روز شد: " + U.money(g18) + " تومان ✓");
                            refresh();
                        } else {
                            U.toast(RateActivity.this, "خطا در دریافت آنلاین نرخ");
                        }
                    }
                });
            }
        }));
        body.addView(c);

        // معادل‌ها
        if (rate > 0) {
            LinearLayout eq = card();
            eq.addView(tv("معادل‌های نرخی", U.GOLD, 14, true));
            eq.addView(kv("گرم ۲۴ عیار", U.money(priceFor(1000, rate)) + " تومان"));
            eq.addView(kv("گرم ۲۲ عیار", U.money(priceFor(916, rate)) + " تومان"));
            eq.addView(kv("گرم ۲۱ عیار", U.money(priceFor(875, rate)) + " تومان"));
            eq.addView(kv("گرم ۱۸ عیار", U.money(priceFor(750, rate)) + " تومان"));
            eq.addView(kv("گرم ۱۴ عیار", U.money(priceFor(585, rate)) + " تومان"));
            eq.addView(kv("مثقال ۱۷ عیار (۴٫۶۰۸ گرم)", U.money(Math.round(rate * 4.608 * 705 / 750.0)) + " تومان", U.GOLD));
            body.addView(eq);
        }

        // نمودار
        Cursor hc = db.rateHistory(30);
        final java.util.ArrayList<Long> vals = new java.util.ArrayList<Long>();
        final java.util.ArrayList<String> dts = new java.util.ArrayList<String>();
        while (hc.moveToNext()) { vals.add(hc.getLong(3)); dts.add(hc.getString(2)); }
        hc.close();
        if (vals.size() >= 2) {
            LinearLayout ch = card();
            ch.addView(tv("روند نرخ (۳۰ ثبت اخیر)", U.GOLD, 14, true));
            ch.addView(space(6));
            java.util.Collections.reverse(vals);
            java.util.Collections.reverse(dts);
            ChartView cv = new ChartView(this, vals, dts);
            ch.addView(cv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(170)));
            body.addView(ch);
        }

        // تاریخچه
        LinearLayout hh = card();
        hh.addView(tv("تاریخچهٔ نرخ", U.GOLD, 14, true));
        hh.addView(space(4));
        Cursor c2 = db.rateHistory(50);
        boolean any = false;
        while (c2.moveToNext()) {
            any = true;
            final long rid = c2.getLong(0);
            LinearLayout r = h();
            r.addView(tv(U.dig(c2.getString(2)), U.SUB, 13, false),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            r.addView(tvM(U.money(c2.getLong(3)) + " تومان", U.TXT, 14));
            TextView del = tv(" ✕ ", U.BAD, 13, true);
            del.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    confirm("این رکورد نرخ حذف شود؟", new Tap() {
                        public void go() { db.w().delete("rates", "id=?", new String[]{"" + rid}); refresh(); }
                    });
                }
            });
            r.addView(del);
            r.setPadding(0, dp(4), 0, dp(4));
            hh.addView(r);
        }
        if (!any) hh.addView(tv("هنوز نرخی ثبت نشده است.", U.SUB, 13, false));
        c2.close();
        body.addView(hh);
    }

    static long priceFor(int karat, long rate18) { return Math.round(rate18 * karat / 750.0); }

    /** نمودار سادهٔ نرخ */
    static class ChartView extends View {
        final java.util.ArrayList<Long> vals;
        final java.util.ArrayList<String> dts;
        Paint pLine = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint pFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint pDot  = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint pTxt  = new Paint(Paint.ANTI_ALIAS_FLAG);

        ChartView(android.content.Context ctx, java.util.ArrayList<Long> v, java.util.ArrayList<String> ds) {
            super(ctx);
            vals = v; dts = ds;
            pLine.setColor(U.GOLD);
            pLine.setStyle(Paint.Style.STROKE);
            pLine.setStrokeWidth(U.dp(ctx, 2.5f));
            pLine.setStrokeJoin(Paint.Join.ROUND);
            pDot.setColor(0xFFF6D988);
            pTxt.setColor(U.SUB);
            pTxt.setTextSize(U.dp(ctx, 11));
            pTxt.setTypeface(U.F);
            pTxt.setTextAlign(Paint.Align.CENTER);
        }

        @Override protected void onDraw(Canvas cv) {
            super.onDraw(cv);
            int w = getWidth(), h = getHeight();
            if (w == 0 || vals.isEmpty()) return;
            long mn = Long.MAX_VALUE, mx = Long.MIN_VALUE;
            for (int i2 = 0; i2 < vals.size(); i2++) { long v = ((Long) vals.get(i2)).longValue(); if (v < mn) mn = v; if (v > mx) mx = v; }
            if (mx == mn) { mx += 1; mn -= 1; }
            float padX = U.dp(getContext(), 8), padT = U.dp(getContext(), 14), padB = U.dp(getContext(), 22);
            float iw = w - padX * 2, ih = h - padT - padB;
            Path path = new Path();
            int n = vals.size();
            for (int i = 0; i < n; i++) {
                float x = n == 1 ? padX + iw / 2 : padX + iw * i / (n - 1);
                float y = padT + ih * (1f - (((Long) vals.get(i)).longValue() - mn) / (float) (mx - mn));
                if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
            }
            // گرادیان زیر خط
            Path area = new Path(path);
            area.lineTo(padX + iw, h - padB);
            area.lineTo(padX, h - padB);
            area.close();
            pFill.setShader(new LinearGradient(0, padT, 0, h - padB,
                    0x55C99327, 0x00C99327, Shader.TileMode.CLAMP));
            cv.drawPath(area, pFill);
            cv.drawPath(path, pLine);
            // نقاط ابتدا و انتها
            for (int i = 0; i < n; i += Math.max(1, n - 1)) {
                float x = n == 1 ? padX + iw / 2 : padX + iw * i / (n - 1);
                float y = padT + ih * (1f - (((Long) vals.get(i)).longValue() - mn) / (float) (mx - mn));
                cv.drawCircle(x, y, U.dp(getContext(), 3.5f), pDot);
            }
            cv.drawText(U.money(mx), w / 2f, padT - U.dp(getContext(), 3), pTxt);
            cv.drawText(U.money(mn), w / 2f, h - U.dp(getContext(), 6), pTxt);
        }
    }
}

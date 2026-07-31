package com.talayar.app;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/** گزارش‌های مالی */
public class ReportsActivity extends A {

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        scaffold("گزارش‌ها", true);
        long rate = db.currentRate();

        // فروش
        long[] today = db.salesStats(Jal.today());
        long[] month = db.salesStats(Jal.thisMonth());
        LinearLayout s1 = section("🧾 فروش");
        s1.addView(secHeader("امروز (" + U.dig(Jal.today()) + ")", "این ماه شمسی"));
        s1.addView(kv("تعداد فاکتور", U.intFa(today[0]) + "  |  " + U.intFa(month[0])));
        s1.addView(kv("مبلغ فروش", U.money(today[1]) + "  |  " + U.money(month[1]), U.GOLD));
        s1.addView(kv("اجرت دریافتی (سود ناچیز)", U.money(today[2]) + "  |  " + U.money(month[2]), U.OK));
        s1.addView(kv("مالیات جمع‌آوری‌شده", U.money(today[3]) + "  |  " + U.money(month[3])));
        s1.addView(kv("نقد دریافت‌شده", U.money(today[4]) + "  |  " + U.money(month[4])));
        s1.addView(kv("مانده به حساب مشتریان", U.money(today[5]) + "  |  " + U.money(month[5]), 0xFFFFCC80));
        body.addView(s1);

        // موجودی
        Db.Pair stock = db.itemsStock();
        long stockDayVal = 0, stockWage = 0;
        Cursor ic = db.r().rawQuery("SELECT karat, wmw, wtype, wval, stoneval FROM items WHERE status='stock'", null);
        while (ic.moveToNext()) {
            long gold = Math.round(U.equiv750(ic.getInt(1), ic.getInt(0)) * rate / 1000.0);
            long wage = ItemEditActivity.calcWage(gold, ic.getInt(1), ic.getInt(2), ic.getInt(3));
            stockDayVal += gold + wage + ic.getInt(4);
            stockWage += wage;
        }
        ic.close();
        long[] gold = db.goldBalance();
        LinearLayout s2 = section("📦 موجودی");
        s2.addView(kv("اجناس در انبار", U.intFa(stock.a) + " قلم • " + U.mw((int) stock.b) + " گرم"));
        if (rate > 0) s2.addView(kv("ارزش روز اجناس (با اجرت)", U.money(stockDayVal) + " تومان", U.GOLD));
        s2.addView(kv("طلای آبشده (۱۸ معادل)", U.mw((int) gold[1]) + " گرم • خام " + U.mw((int) gold[0]) + " گرم"));
        if (rate > 0) s2.addView(kv("ارزش روز آبشده", U.money(Math.round(gold[1] * rate / 1000.0)) + " تومان"));
        body.addView(s2);

        // نقد و حساب‌ها
        long[] sp = db.customerDebtSplit();
        long bal = db.cashBalance();
        LinearLayout s3 = section("💰 نقدینگی و حساب‌ها");
        s3.addView(kv("تراز صندوق", U.money(bal) + " تومان", bal >= 0 ? U.OK : U.BAD));
        s3.addView(kv("بدهی نقدی مشتریان", U.money(sp[0]) + " تومان", 0xFFFFCC80));
        s3.addView(kv("بستانکاری نقدی", U.money(sp[1]) + " تومان", U.OK));
        s3.addView(kv("بدهی طلایی مشتریان", U.mw((int) sp[2]) + " گرم", 0xFFFFCC80));
        s3.addView(kv("بستانکاری طلایی", U.mw((int) sp[3]) + " گرم"));
        body.addView(s3);

        // گردش کلی
        Cursor q = db.r().rawQuery("SELECT COUNT(*), COALESCE(SUM(total),0), COALESCE(SUM(wage),0) FROM invoices", null);
        q.moveToFirst();
        long invN = q.getLong(0), invSum = q.getLong(1), invWage = q.getLong(2);
        q.close();
        Cursor q2 = db.r().rawQuery("SELECT COALESCE(SUM(wmw),0) FROM gold_tx WHERE kind='in'", null);
        q2.moveToFirst();
        long goldIn = q2.getLong(0);
        q2.close();
        LinearLayout s4 = section("📈 گردش از ابتدا");
        s4.addView(kv("کل فاکتورها", U.intFa(invN) + " فاکتور • " + U.money(invSum) + " تومان"));
        s4.addView(kv("کل اجرت‌های دریافتی", U.money(invWage) + " تومان", U.OK));
        s4.addView(kv("کل طلای خریداری/دریافت‌شده", U.mw((int) goldIn) + " گرم"));
        body.addView(s4);
    }

    private LinearLayout section(String title) {
        LinearLayout c = card();
        c.addView(tv(title, U.GOLD, 15, true));
        c.addView(space(4));
        return c;
    }
    private View secHeader(String a, String b) {
        LinearLayout r = h();
        r.addView(tv(a, U.SUB, 12, false), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        r.addView(tv(b, U.SUB, 12, false));
        return r;
    }
}

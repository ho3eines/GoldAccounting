package com.talayar.app;

import android.database.Cursor;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/** دارایی‌ها و تراز — موجودی صندوق، بانک، آبشده (تکی بر عیار)، سکه، شمش، ارز، نقره، کارساخته، چک */
public class AssetsActivity extends A {

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        scaffold("دارایی‌ها و تراز", true);
        build();
    }

    @Override protected void onResume() {
        super.onResume();
        if (body != null) { body.removeAllViews(); build(); }
    }

    private LinearLayout sec(String title) {
        LinearLayout c = card();
        c.addView(tv(title, U.GOLD, 15, true));
        c.addView(space(2));
        return c;
    }

    private void build() {
        long cashBal = db.cashBalance();
        long banks = db.banksTotal();
        long[] gold = db.goldBalance();
        long[] debts = db.customerDebts();

        // ── مشتریان ──
        LinearLayout s0 = sec("👥 مانده حساب مشتریان");
        s0.addView(kv("طلب نقدی از مشتریان (بدهی آن‌ها)", U.moneyT(debts[0] > 0 ? debts[0] : 0), 0xFFFFCC80));
        s0.addView(kv("بستانکاری نقدی (مانده نقدی کل)", U.moneyT(debts[0]), debts[0] >= 0 ? U.TXT : U.OK));
        s0.addView(kv("مانده طلایی کل (۱۸ معادل)", U.gs((int) debts[1]), debts[1] > 0 ? 0xFFFFCC80 : U.TXT));
        body.addView(s0);

        // ── نقد و بانک ──
        LinearLayout s1 = sec("💰 نقدی و بانکی");
        s1.addView(kv("تراز صندوق", U.moneyT(cashBal), cashBal >= 0 ? U.OK : U.BAD));
        s1.addView(kv("مجموع موجودی بانک‌ها", U.moneyT(banks), banks >= 0 ? U.OK : U.BAD));
        Cursor bc = db.defsOf("bank");
        while (bc.moveToNext()) {
            int bid = Db.ci(bc, "id");
            long bal = db.bankBalance(bid);
            s1.addView(kv("   🏦 " + Db.cs(bc, "name"), U.moneyT(bal), bal >= 0 ? U.TXT : U.BAD));
        }
        bc.close();
        body.addView(s1);

        // ── آبشده و متفرقه ──
        LinearLayout s2 = sec("⚖️ آبشده و متفرقه (طلا)");
        s2.addView(kv("موجودی خام", U.gs((int) gold[0]), U.TXT));
        s2.addView(kv("معادل ۱۸ عیار", U.gs((int) gold[1]), U.GOLD));
        s2.addView(tv("تفکیک تکی بر اساس عیار:", U.SUB, 12, false));
        Cursor kc = db.r().rawQuery(
                "SELECT karat, COALESCE(SUM(CASE WHEN kind='out' THEN -wmw ELSE wmw END),0), COUNT(*) " +
                "FROM gold_tx GROUP BY karat ORDER BY karat DESC", null);
        boolean anyK = false;
        while (kc.moveToNext()) {
            int k = kc.getInt(0);
            long w = kc.getLong(1);
            int n = kc.getInt(2);
            if (w == 0) continue;
            anyK = true;
            s2.addView(kv("   " + U.karatName(k) + " (" + U.intFa(n) + " قلم)",
                    U.gs((int) w) + " ≈ " + U.gs((int) U.equiv750(w, k)) + " (۱۸ معادل)", U.TXT));
        }
        kc.close();
        if (!anyK) s2.addView(tv("گردشی ثبت نشده است.", U.SUB, 12, false));
        body.addView(s2);

        // ── سکه / شمش / ارز / نقره / کارساخته ──
        body.addView(assetSec("🪙 سکه‌ها", "coin", "coin"));
        body.addView(assetSec("🧱 شمش‌ها", "bullion", "bull"));
        body.addView(assetSec("💵 ارزها", "curr", "cur"));
        body.addView(assetSec("🥈 نقره", "silver", "sil"));

        LinearLayout s6 = sec("💍 کارساخته");
        Db.Pair stock = db.itemsStock();
        s6.addView(kv("اجناس انبار", U.intFa(stock.a) + " قلم • " + U.mwG((int) stock.b), U.TXT));
        double workMg = db.stockOf("work_mg");
        s6.addView(kv("کارساختهٔ وزنی (دفتر دارایی)", U.gs((int) Math.round(workMg)), U.TXT));
        body.addView(s6);

        // ── چک‌های باز ──
        LinearLayout s7 = sec("📄 چک‌های باز");
        Cursor cc = db.r().rawQuery(
                "SELECT kind, COUNT(*), COALESCE(SUM(amount),0) FROM checks WHERE status='open' GROUP BY kind", null);
        boolean anyCh = false;
        while (cc.moveToNext()) {
            anyCh = true;
            String kind = cc.getString(0);
            s7.addView(kv("recv".equals(kind) ? "چک‌های دریافتی باز" : "چک‌های پرداختنی باز",
                    U.intFa(cc.getInt(1)) + " فقره • " + U.moneyT(cc.getLong(2)),
                    "recv".equals(kind) ? U.OK : 0xFFFFA9B1));
        }
        cc.close();
        if (!anyCh) s7.addView(tv("چک بازی وجود ندارد ✓", U.SUB, 12, false));
        body.addView(s7);
    }

    /** بخش موجودی یک نوع دارایی از دفتر دارایی‌ها */
    private LinearLayout assetSec(String title, String defKind, String prefix) {
        LinearLayout s = sec(title);
        Cursor c = db.defsOf(defKind);
        boolean any = false;
        while (c.moveToNext()) {
            int id = Db.ci(c, "id");
            String name = Db.cs(c, "name");
            String asset = prefix + "_d" + id;
            double q = db.stockOf(asset);
            if (q == 0) continue;
            any = true;
            s.addView(kv("   " + name, Post.fmtQty(asset, q), q >= 0 ? U.TXT : U.BAD));
        }
        c.close();
        if (!any) s.addView(tv("موجودی ثبت نشده (از «ثبت سند» ورود/خرید ثبت کنید).", U.SUB, 12, false));
        return s;
    }
}

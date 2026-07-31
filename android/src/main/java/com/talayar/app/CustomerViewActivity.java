package com.talayar.app;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** پرونده مشتری: مانده، تسویه، گردش حساب */
public class CustomerViewActivity extends A {
    private int cid;
    private String name = "";

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        cid = getIntent().getIntExtra("id", 0);
        Cursor c = db.r().rawQuery("SELECT name, phone, note FROM customers WHERE id=?", new String[]{"" + cid});
        String phone = "", note = "";
        if (c.moveToFirst()) { name = c.getString(0); phone = c.getString(1); note = c.getString(2); }
        c.close();
        final String ph = phone, nt = note;
        scaffold("پرونده مشتری", true);

        LinearLayout head = card();
        head.addView(tv(name, U.GOLD, 19, true));
        if (ph.length() > 0) head.addView(tv("☎ " + U.dig(ph), U.SUB, 13, false));
        if (nt.length() > 0) head.addView(tv(nt, U.SUB, 12, false));
        body.addView(head);
        refreshBody();
        LinearLayout ops = card();
        LinearLayout row = h();
        row.addView(btn("دریافت وجه", new Tap() { public void go() { payCash(); } }),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(wspace(8));
        row.addView(btn("دریافت طلا", new Tap() { public void go() { payGold(); } }),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        ops.addView(row);
        ops.addView(space(6));
        LinearLayout row2 = h();
        row2.addView(gbtn("ویرایش نام", new Tap() {
            public void go() {
                input("ویرایش نام مشتری", "نام", name, false, new OnText() {
                    public void ok(String s) {
                        if (s.length() == 0) return;
                        android.content.ContentValues cv = new android.content.ContentValues();
                        cv.put("name", s);
                        db.w().update("customers", cv, "id=?", new String[]{"" + cid});
                        name = s;
                        U.toast(CustomerViewActivity.this, "ذخیره شد");
                        recreate();
                    }
                });
            }
        }), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row2.addView(wspace(8));
        row2.addView(dbtn("حذف مشتری", new Tap() { public void go() { delCustomer(); } }),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        ops.addView(row2);
        body.addView(ops);

        TextView lt = tv("گردش حساب", U.GOLD, 15, true);
        lt.setPadding(dp(4), dp(10), 0, dp(4));
        body.addView(lt);
        ledgerBox = v();
        body.addView(ledgerBox);
        fillLedger();
    }

    private LinearLayout balanceBox;
    private LinearLayout ledgerBox;

    private void refreshBody() {
        if (body == null) return;
    }

    private long[] sums() {
        Cursor c = db.r().rawQuery("SELECT COALESCE(SUM(cash),0), COALESCE(SUM(goldmw),0) FROM customer_tx WHERE cid=?",
                new String[]{"" + cid});
        long[] r = {0, 0};
        if (c.moveToFirst()) { r[0] = c.getLong(0); r[1] = c.getLong(1); }
        c.close();
        return r;
    }

    @Override protected void onResume() { super.onResume(); if (ledgerBox != null) { fillLedger(); fillBalance(); } }

    private void fillBalance() {
        long[] s = sums();
        // جعبه مانده (بازسازی)
        if (balanceBox != null) body.removeView(balanceBox);
        balanceBox = cardHi();
        LinearLayout row = h();
        LinearLayout c1 = v();
        c1.addView(tv("مانده نقدی", U.SUB, 12, false));
        c1.addView(tvM(s[0] > 0 ? U.money(s[0]) + " بدهکار" : s[0] < 0 ? U.money(-s[0]) + " بستانکار" : "تسویه",
                s[0] > 0 ? 0xFFFFCC80 : U.OK, 15));
        LinearLayout c2 = v();
        c2.addView(tv("مانده طلایی (۱۸ معادل)", U.SUB, 12, false));
        c2.addView(tvM(s[1] > 0 ? U.mw((int) s[1]) + " گرم بدهکار" : s[1] < 0 ? U.mw((int) -s[1]) + " بستانکار" : "—",
                s[1] > 0 ? 0xFFFFCC80 : U.TXT, 15));
        row.addView(c1, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(c2, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        balanceBox.addView(row);
        // قرار دادن بعد از کارت هدر (اندیس 1)
        body.addView(balanceBox, 1);
    }

    private void fillLedger() {
        ledgerBox.removeAllViews();
        fillBalance();
        Cursor c = db.r().rawQuery("SELECT * FROM customer_tx WHERE cid=? ORDER BY ts DESC, id DESC LIMIT 200",
                new String[]{"" + cid});
        boolean any = false;
        while (c.moveToNext()) {
            any = true;
            String date = Db.cs(c, "date_j");
            long cash = Db.cl(c, "cash");
            long gold = Db.cl(c, "goldmw");
            String desc = Db.cs(c, "descr");
            LinearLayout card = card(dp(10));
            LinearLayout top = h();
            top.addView(tv(U.dig(date), U.SUB, 12, false),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            if (cash != 0) top.addView(tvM((cash > 0 ? "+" : "") + U.money(cash), cash > 0 ? 0xFFFFCC80 : U.OK, 13));
            if (gold != 0) top.addView(wspace(6));
            if (gold != 0) top.addView(tvM((gold > 0 ? "+" : "") + U.mw((int) gold) + " گرم", gold > 0 ? 0xFFFFCC80 : U.OK, 13));
            card.addView(top);
            if (desc.length() > 0) card.addView(tv(desc, U.TXT, 13, false));
            ledgerBox.addView(card);
        }
        c.close();
        if (!any) {
            LinearLayout e = card();
            e.addView(tv("تراکنشی ثبت نشده است.", U.SUB, 13, false));
            ledgerBox.addView(e);
        }
    }

    /** دریافت وجه نقد از مشتری */
    private void payCash() {
        final long[] s = sums();
        final LinearLayout box = v();
        box.setPadding(dp(16), dp(14), dp(16), dp(10));
        box.addView(tv("دریافت وجه از " + name, U.GOLD, 16, true));
        if (s[0] > 0) box.addView(tv("مانده فعلی: " + U.money(s[0]) + " تومان بدهی", U.SUB, 12, false));
        box.addView(space(6));
        final android.widget.EditText e = in("مبلغ به تومان", true);
        final android.widget.EditText ed = in("شرح (اختیاری)");
        box.addView(e); box.addView(space(6)); box.addView(ed);
        box.addView(space(8));
        final android.app.AlertDialog d = sheet(box);
        LinearLayout br = h();
        br.setGravity(android.view.Gravity.CENTER);
        br.addView(btn("ثبت دریافت", new Tap() { public void go() {
            long amt = U.parseMoney(U.str(e));
            if (amt <= 0) { U.toast(CustomerViewActivity.this, "مبلغ نامعتبر"); return; }
            String desc = U.str(ed);
            if (desc.length() == 0) desc = "دریافت وجه از " + name;
            long ts = System.currentTimeMillis();
            android.content.ContentValues tx = new android.content.ContentValues();
            tx.put("ts", ts); tx.put("date_j", Jal.today()); tx.put("kind", "in");
            tx.put("amount", amt); tx.put("descr", desc); tx.put("iid", 0);
            db.ins("cash_tx", tx);
            android.content.ContentValues ct = new android.content.ContentValues();
            ct.put("cid", cid); ct.put("ts", ts); ct.put("date_j", Jal.today());
            ct.put("cash", -amt); ct.put("goldmw", 0); ct.put("descr", desc);
            db.ins("customer_tx", ct);
            d.dismiss();
            U.toast(CustomerViewActivity.this, "ثبت شد ✓");
            fillLedger();
        } }), new LinearLayout.LayoutParams(dp(140), ViewGroup.LayoutParams.WRAP_CONTENT));
        br.addView(wspace(10));
        br.addView(gbtn("انصراف", new Tap() { public void go() { d.dismiss(); } }),
                new LinearLayout.LayoutParams(dp(110), ViewGroup.LayoutParams.WRAP_CONTENT));
        box.addView(br);
    }

    /** دریافت طلا از مشتری (کارکرده) — معادل ۱۸عیار */
    private void payGold() {
        final long rate = db.currentRate();
        final long[] s = sums();
        final LinearLayout box = v();
        box.setPadding(dp(16), dp(14), dp(16), dp(10));
        box.addView(tv("دریافت طلا از " + name, U.GOLD, 16, true));
        box.addView(tv("طلا به موجودی آبشده اضافه و از بدهی طلایی/نقدی مشتری کم می‌شود.", U.SUB, 12, false));
        box.addView(space(6));
        final android.widget.EditText ew = in("وزن (گرم)", true);
        box.addView(label2("عیار:"));
        final int[] karat = {750};
        box.addView(chipsRow(new String[]{"۲۴","۲۲","۲۱","۱۸","۱۴","۹"}, 3, new OnIdx() {
            public void ok(int i) { karat[0] = ItemEditActivity.K_VALS[i]; }
        }));
        final android.widget.EditText ed = in("شرح (اختیاری)");
        box.addView(ew); box.addView(space(6)); box.addView(ed);
        box.addView(space(8));
        final android.app.AlertDialog d = sheet(box);
        LinearLayout br = h();
        br.setGravity(android.view.Gravity.CENTER);
        br.addView(btn("ثبت دریافت", new Tap() { public void go() {
            int w = U.parseMw(U.str(ew));
            if (w <= 0) { U.toast(CustomerViewActivity.this, "وزن نامعتبر"); return; }
            long eq = U.equiv750(w, karat[0]);
            String desc = U.str(ed);
            if (desc.length() == 0) desc = "دریافت " + U.mw(w) + " گرم طلای " + U.karatName(karat[0]) + " از " + name;
            long ts = System.currentTimeMillis();
            android.content.ContentValues gt = new android.content.ContentValues();
            gt.put("ts", ts); gt.put("date_j", Jal.today()); gt.put("kind", "in");
            gt.put("wmw", w); gt.put("karat", karat[0]); gt.put("descr", desc); gt.put("cid", cid);
            db.ins("gold_tx", gt);
            // کسر از بدهی طلایی، سپس مازاد → معادل نقدی با نرخ روز از بدهی نقدی
            long cashAdj = 0, goldAdj = -eq;
            if (s[1] <= 0 && rate > 0) {
                cashAdj = -Math.round(eq * rate / 1000.0);
                goldAdj = 0;
            } else if (eq > s[1] && s[1] > 0 && rate > 0) {
                long extra = eq - s[1];
                goldAdj = -s[1];
                cashAdj = -Math.round(extra * rate / 1000.0);
            }
            android.content.ContentValues ct = new android.content.ContentValues();
            ct.put("cid", cid); ct.put("ts", ts); ct.put("date_j", Jal.today());
            ct.put("cash", cashAdj); ct.put("goldmw", goldAdj); ct.put("descr", desc);
            db.ins("customer_tx", ct);
            d.dismiss();
            U.toast(CustomerViewActivity.this, "ثبت شد ✓");
            fillLedger();
        } }), new LinearLayout.LayoutParams(dp(140), ViewGroup.LayoutParams.WRAP_CONTENT));
        br.addView(wspace(10));
        br.addView(gbtn("انصراف", new Tap() { public void go() { d.dismiss(); } }),
                new LinearLayout.LayoutParams(dp(110), ViewGroup.LayoutParams.WRAP_CONTENT));
        box.addView(br);
    }

    private TextView label2(String s) { return label(s); }

    private void delCustomer() {
        Cursor c = db.r().rawQuery("SELECT COUNT(*) FROM customer_tx WHERE cid=?", new String[]{"" + cid});
        c.moveToFirst();
        int n = c.getInt(0);
        c.close();
        long[] s = sums();
        if (n > 0 || s[0] != 0 || s[1] != 0) {
            msg("قابل حذف نیست", "این مشتری گردش حساب دارد و برای حفظ صحت اسناد قابل حذف نیست.");
            return;
        }
        confirm("مشتری «" + name + "» حذف شود؟", new Tap() {
            public void go() {
                db.w().delete("customers", "id=?", new String[]{"" + cid});
                U.toast(CustomerViewActivity.this, "حذف شد");
                finish();
            }
        });
    }
}

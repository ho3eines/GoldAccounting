package com.talayar.app;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** خرید طلای کارکرده/دست‌دوم از مشتری یا فروشنده */
public class BuyActivity extends A {
    private int cid = 0;
    private String cname = "";
    private int karat = 750;
    private boolean payNow = true;
    private EditText eW, eRate, eNote;
    private TextView custTv, calcTv, payTv;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        scaffold("خرید طلای کارکرده", true);

        LinearLayout c = card();
        c.addView(tv("از این فرم برای خرید طلای دست‌دوم / آبشده از مشتری یا فروشنده استفاده کنید. طلا به موجودی «آبشده» اضافه می‌شود.", U.SUB, 12, false));
        body.addView(c);

        LinearLayout f = card();
        LinearLayout cr = h();
        custTv = tv("بدون مشتری (فروشندهٔ عابر)", U.TXT, 14, true);
        LinearLayout cc = v();
        cc.addView(tv("فروشنده", U.SUB, 11, false));
        cc.addView(custTv);
        cr.addView(cc, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        cr.addView(gbtn("انتخاب", new Tap() {
            public void go() {
                pickCustomer(new OnCustomer() {
                    public void ok(int id, String name) {
                        cid = id; cname = name;
                        custTv.setText(id == 0 ? "بدون مشتری (عابر)" : name);
                    }
                });
            }
        }));
        f.addView(cr);

        f.addView(label("وزن (گرم)"));
        eW = in("۰", true);
        f.addView(eW);
        f.addView(label("عیار"));
        f.addView(chipsRow(new String[]{"۲۴","۲۲","۲۱","۱۸","۱۴","۹"}, 3, new OnIdx() {
            public void ok(int i) { karat = ItemEditActivity.K_VALS[i]; fillRateDef(); }
        }));
        f.addView(label("نرخ خرید هر گرم (تومان)"));
        eRate = in("", true);
        f.addView(eRate);
        f.addView(label("یادداشت"));
        eNote = in("اختیاری");
        f.addView(eNote);
        body.addView(f);

        calc = cardHi();
        calcTv = tv("", U.TXT, 13, false);
        calcTv.setLineSpacing(6, 1.25f);
        calc.addView(calcTv);
        body.addView(calc);

        LinearLayout payC = card();
        payTv = tv("", U.TXT, 14, true);
        payC.addView(payTv);
        body.addView(payC);
        payC.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                payNow = !payNow;
                upd();
            }
        });

        addBtn(body, btn("⬇  ثبت خرید", new Tap() { public void go() { save(); } }));

        android.text.TextWatcher tw = new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { upd(); }
            public void afterTextChanged(android.text.Editable s) {}
        };
        eW.addTextChangedListener(tw);
        eRate.addTextChangedListener(tw);
        fillRateDef();
        upd();
    }

    LinearLayout calc;

    private void fillRateDef() {
        if (U.str(eRate).length() == 0) {
            long rate = db.currentRate();
            if (rate > 0) {
                long rasub = db.getL("resub", 0);
                long per = Math.round(rate * karat / 750.0 * (100 - rasub) / 100.0);
                eRate.setText(U.dig(per + ""));
            }
        }
    }

    private void upd() {
        long rate = db.currentRate();
        int w = mwOf(eW);
        long per = moneyOf(eRate);
        long total = Math.round(w * per / 1000.0);
        long eq = U.equiv750(w, karat);
        StringBuilder sb = new StringBuilder();
        sb.append("معادل ۱۸ عیار: ").append(U.mw((int) eq)).append(" گرم");
        if (rate > 0 && w > 0) {
            long dayVal = Math.round(eq * rate / 1000.0);
            sb.append("\nارزش روز (خام): ").append(U.money(dayVal)).append(" تومان");
        }
        sb.append("\nمبلغ خرید: ").append(U.money(total)).append(" تومان");
        calcTv.setText(sb.toString());
        payTv.setText((payNow ? "☑" : "☐") + " پرداخت فوری از صندوق" + (cid > 0 ? "\n" + (payNow ? "☐" : "☑") + " پرداخت نشد — به حساب مشتری (بستانکاری)" : ""));
        if (cid == 0 && !payNow) { payNow = true; }
    }

    private void save() {
        int w = mwOf(eW);
        long per = moneyOf(eRate);
        if (w <= 0) { U.toast(this, "وزن معتبر وارد کنید"); return; }
        if (per <= 0) { U.toast(this, "نرخ خرید را وارد کنید"); return; }
        final long total = Math.round(w * per / 1000.0);
        SQLiteDatabase dbw = db.w();
        dbw.beginTransaction();
        try {
            long ts = System.currentTimeMillis();
            String tj = Jal.today();
            String desc = "خرید " + U.mw(w) + " گرم طلای " + U.karatName(karat)
                    + (cname.length() > 0 ? " از " + cname : "");
            if (U.str(eNote).length() > 0) desc += " • " + U.str(eNote);
            android.content.ContentValues gt = new android.content.ContentValues();
            gt.put("ts", ts); gt.put("date_j", tj); gt.put("kind", "in");
            gt.put("wmw", w); gt.put("karat", karat); gt.put("descr", desc); gt.put("cid", cid);
            dbw.insert("gold_tx", null, gt);
            if (payNow) {
                android.content.ContentValues cx = new android.content.ContentValues();
                cx.put("ts", ts); cx.put("date_j", tj); cx.put("kind", "out");
                cx.put("amount", total); cx.put("descr", desc + " (پرداخت نقد)"); cx.put("iid", 0);
                dbw.insert("cash_tx", null, cx);
            }
            if (cid > 0) {
                long eq = U.equiv750(w, karat);
                // طلا از مشتری گرفتیم: اگر بدهی طلایی دارد کم می‌شود؛ خالص دریافت
                android.content.ContentValues ct = new android.content.ContentValues();
                ct.put("cid", cid); ct.put("ts", ts); ct.put("date_j", tj);
                ct.put("goldmw", -eq);
                ct.put("cash", payNow ? 0 : -total);
                ct.put("descr", desc + (payNow ? "" : " (مانده به حساب: " + U.money(total) + " بستانکاری)"));
                dbw.insert("customer_tx", null, ct);
            }
            dbw.setTransactionSuccessful();
            U.toast(this, "خرید ثبت شد ✓");
            finish();
        } catch (Exception e) {
            msg("خطا", e.getMessage() == null ? "خطا" : e.getMessage());
        } finally {
            dbw.endTransaction();
        }
    }
}

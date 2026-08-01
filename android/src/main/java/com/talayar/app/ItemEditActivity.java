package com.talayar.app;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

/** افزودن / ویرایش جنس */
public class ItemEditActivity extends A {
    private int id = 0;
    private EditText eName, eCode, eWeight, eWage, eStoneVal, eDesc;
    private int karat = 750, wtype = 0, sold = 0;

    static final String[] K_LABELS = {"۲۴", "۲۲", "۲۱", "۱۸", "۱۴", "۹"};
    static final int[] K_VALS = {1000, 916, 875, 750, 585, 375};

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        id = getIntent().getIntExtra("id", 0);
        scaffold(id == 0 ? "جنس جدید" : "ویرایش جنس", true);

        body.addView(label("نام جنس"));
        eName = in("مثلاً النگو، انگشتر، سکه…");
        body.addView(eName);

        body.addView(label("کد جنس / بارکد"));
        eCode = in("کد عددی", true);
        body.addView(eCode);

        body.addView(label("عیار"));
        body.addView(chipsRow(K_LABELS, 3, new OnIdx() { public void ok(int i) { karat = K_VALS[i]; } }));

        body.addView(label("وزن (گرم)"));
        eWeight = in("مثلاً ۱۲٫۳۵۰", true);
        body.addView(eWeight);

        body.addView(label("نوع اجرت"));
        body.addView(chipsRow(new String[]{"درصدی", "تومان بر گرم", "ثابت (تومان)"}, 0, new OnIdx() {
            public void ok(int i) { wtype = i; eWage.setHint(wageHint()); }
        }));

        body.addView(label("مقدار اجرت"));
        eWage = in(wageHint(), true);
        body.addView(eWage);

        body.addView(label("ارزش سنگ / مزین (تومان، اختیاری)"));
        eStoneVal = in("۰", true);
        body.addView(eStoneVal);

        body.addView(label("توضیح"));
        eDesc = in("اختیاری");
        body.addView(eDesc);

        LinearLayout liveSum = cardHi();
        final LinearLayout sumBox = v();
        final android.widget.TextView sumTv = tv("—", U.GOLD, 14, true);
        sumBox.addView(tv("برآورد ارزش روز", U.SUB, 12, false));
        sumBox.addView(sumTv);
        liveSum.addView(sumBox);
        body.addView(liveSum);
        android.text.TextWatcher tw = new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { updSum(sumTv); }
            public void afterTextChanged(android.text.Editable s) {}
        };
        eWeight.addTextChangedListener(tw); eWage.addTextChangedListener(tw); eStoneVal.addTextChangedListener(tw);
        updSum(sumTv);

        addBtn(body, btn(id == 0 ? "ذخیره جنس" : "ذخیره تغییرات", new Tap() { public void go() { save(); } }));
        if (id != 0 && sold == 0) {
            addBtn(body, dbtn("حذف جنس", new Tap() { public void go() { del(); } }));
        }

        if (id != 0) load();
    }

    private String wageHint() {
        return wtype == 0 ? "درصد (مثلاً ۷)" : wtype == 1 ? "تومان به ازای هر گرم" : "مبلغ ثابت به تومان";
    }

    private void updSum(android.widget.TextView sumTv) {
        long rate = db.currentRate();
        if (rate <= 0) { sumTv.setText("ابتدا نرخ روز را ثبت کنید"); return; }
        int w = mwOf(eWeight);
        if (w <= 0) { sumTv.setText("—"); return; }
        long gold = Math.round(U.equiv750(w, karat) * rate / 1000.0);
        long wage = calcWage(gold, w, wtype, (double) U.parseMoney(U.str(eWage)));
        long tax = Math.round(wage * db.getL("tax", 10) / 100.0);
        long stone = moneyOf(eStoneVal);
        sumTv.setText("طلا: " + U.money(gold) + " • اجرت: " + U.money(wage) + " • مالیات: " + U.money(tax)
                + "\nجمع تقریبی: " + U.money(gold + wage + tax + stone) + " تومان");
    }

    public static long calcWage(long goldVal, long wmw, int wtype, long wval) {
        if (wtype == 0) return Math.round(goldVal * wval / 100.0);
        if (wtype == 1) return Math.round(wval * wmw / 1000.0);
        return wval;
    }

    private void load() {
        Cursor c = db.r().rawQuery("SELECT * FROM items WHERE id=?", new String[]{"" + id});
        if (c.moveToFirst()) {
            eName.setText(Db.cs(c, "name"));
            eCode.setText(U.dig(Db.ci(c, "code") + ""));
            eWeight.setText(U.dig(Db.ci(c, "wmw") / 1000 + (Db.ci(c, "wmw") % 1000 > 0 ? "." + String.format("%03d", Db.ci(c, "wmw") % 1000).replaceAll("0+$", "").replaceAll("\\.$", "") : "")));
            karat = Db.ci(c, "karat");
            wtype = Db.ci(c, "wtype");
            eWage.setText(U.dig(Db.ci(c, "wval") + ""));
            eStoneVal.setText(U.dig(Db.ci(c, "stoneval") + ""));
            eDesc.setText(Db.cs(c, "descr"));
            sold = "sold".equals(Db.cs(c, "status")) ? 1 : 0;
        }
        c.close();
    }

    private void save() {
        String name = txtOf(eName);
        if (name.length() == 0) { U.toast(this, "نام جنس را بنویسید"); return; }
        int w = mwOf(eWeight);
        if (w <= 0) { U.toast(this, "وزن معتبر وارد کنید"); return; }
        int code = (int) U.parseMoney(U.str(eCode));
        if (code <= 0) {
            Cursor c = db.r().rawQuery("SELECT COALESCE(MAX(code),0)+1 FROM items", null);
            c.moveToFirst(); code = c.getInt(0); c.close();
        }
        Cursor dup = db.r().rawQuery("SELECT id FROM items WHERE code=? AND id<>?", new String[]{"" + code, "" + id});
        boolean isDup = dup.moveToFirst(); dup.close();
        if (isDup) { U.toast(this, "این کد قبلاً استفاده شده است"); return; }

        android.content.ContentValues cv = new android.content.ContentValues();
        cv.put("code", code);
        cv.put("name", name);
        cv.put("karat", karat);
        cv.put("wmw", w);
        cv.put("wtype", wtype);
        cv.put("wval", U.parseMoney(U.str(eWage)));
        cv.put("stone_mw", 0);
        cv.put("stoneval", moneyOf(eStoneVal));
        cv.put("descr", txtOf(eDesc));
        if (id == 0) {
            cv.put("status", "stock");
            cv.put("cts", System.currentTimeMillis());
            db.ins("items", cv);
            U.toast(this, "جنس ذخیره شد ✓");
        } else {
            db.w().update("items", cv, "id=?", new String[]{"" + id});
            U.toast(this, "تغییرات ذخیره شد ✓");
        }
        finish();
    }

    private void del() {
        Cursor c = db.r().rawQuery("SELECT COUNT(*) FROM invoice_lines WHERE item_id=?", new String[]{"" + id});
        c.moveToFirst();
        int used = c.getInt(0);
        c.close();
        if (used > 0) { msg("قابل حذف نیست", "این جنس در فاکتورها استفاده شده و برای حفظ صحت اسناد قابل حذف نیست."); return; }
        confirm("جنس «" + txtOf(eName) + "» حذف شود؟", new Tap() {
            public void go() {
                db.w().delete("items", "id=?", new String[]{"" + id});
                U.toast(ItemEditActivity.this, "حذف شد");
                finish();
            }
        });
    }
}

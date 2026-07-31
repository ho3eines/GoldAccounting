package com.talayar.app;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** صندوق نقدینگی */
public class CashActivity extends A {
    private TextView balTv;
    private LinearLayout list;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        scaffold("صندوق", true);

        LinearLayout head = cardHi();
        head.addView(tv("تراز فعلی صندوق", U.SUB, 13, false));
        balTv = tv("", U.GOLD, 24, true);
        head.addView(balTv);
        body.addView(head);

        LinearLayout ops = card();
        LinearLayout row = h();
        row.addView(btn("دریافت (واریز)", new Tap() { public void go() { tx("in"); } }),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(wspace(8));
        row.addView(gbtn("پرداخت (برداشت)", new Tap() { public void go() { tx("out"); } }),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        ops.addView(row);
        body.addView(ops);

        TextView lt = tv("تراکنش‌های اخیر", U.GOLD, 15, true);
        lt.setPadding(dp(4), dp(8), 0, dp(2));
        body.addView(lt);
        list = v();
        body.addView(list);
        refresh();
    }

    @Override protected void onResume() { super.onResume(); if (list != null) refresh(); }

    private void tx(final String kind) {
        final LinearLayout box = v();
        box.setPadding(dp(16), dp(14), dp(16), dp(10));
        box.addView(tv("in".equals(kind) ? "دریافت به صندوق" : "پرداخت از صندوق", U.GOLD, 16, true));
        box.addView(space(6));
        final android.widget.EditText e = in("مبلغ به تومان", true);
        final android.widget.EditText ed = in("بابت / شرح");
        box.addView(e); box.addView(space(6)); box.addView(ed);
        box.addView(space(8));
        final android.app.AlertDialog d = sheet(box);
        LinearLayout br = h();
        br.setGravity(android.view.Gravity.CENTER);
        br.addView(btn("ثبت", new Tap() { public void go() {
            long amt = U.parseMoney(U.str(e));
            if (amt <= 0) { U.toast(CashActivity.this, "مبلغ نامعتبر"); return; }
            String desc = U.str(ed);
            if (desc.length() == 0) desc = "in".equals(kind) ? "واریز آزاد به صندوق" : "برداشت آزاد از صندوق";
            android.content.ContentValues cv = new android.content.ContentValues();
            cv.put("ts", System.currentTimeMillis());
            cv.put("date_j", Jal.today());
            cv.put("kind", kind);
            cv.put("amount", amt);
            cv.put("descr", desc);
            cv.put("iid", 0);
            db.ins("cash_tx", cv);
            d.dismiss();
            U.toast(CashActivity.this, "ثبت شد ✓");
            refresh();
        } }), new LinearLayout.LayoutParams(dp(130), ViewGroup.LayoutParams.WRAP_CONTENT));
        br.addView(wspace(10));
        br.addView(gbtn("انصراف", new Tap() { public void go() { d.dismiss(); } }),
                new LinearLayout.LayoutParams(dp(120), ViewGroup.LayoutParams.WRAP_CONTENT));
        box.addView(br);
    }

    private void refresh() {
        long bal = db.cashBalance();
        balTv.setText(U.money(bal) + " تومان");
        balTv.setTextColor(bal >= 0 ? U.GOLD : U.BAD);
        list.removeAllViews();
        Cursor c = db.r().rawQuery("SELECT * FROM cash_tx ORDER BY ts DESC, id DESC LIMIT 200", null);
        boolean any = false;
        while (c.moveToNext()) {
            any = true;
            final long id = Db.cl(c, "id");
            final boolean isIn = "in".equals(Db.cs(c, "kind"));
            LinearLayout card = card(dp(10));
            LinearLayout top = h();
            top.addView(tv(isIn ? "↙ دریافت" : "↗ پرداخت", isIn ? U.OK : 0xFFFFA9B1, 13, true));
            top.addView(wspace(8));
            top.addView(tv(U.dig(Db.cs(c, "date_j")), U.SUB, 12, false),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            top.addView(tvM((isIn ? "+" : "−") + U.money(Db.cl(c, "amount")), isIn ? U.OK : 0xFFFFA9B1, 14));
            card.addView(top);
            card.addView(tv(Db.cs(c, "descr"), U.TXT, 13, false));
            card.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    confirm("این تراکنش حذف شود؟ (تراز صندوق تغییر می‌کند)", new Tap() {
                        public void go() {
                            db.w().delete("cash_tx", "id=?", new String[]{"" + id});
                            U.toast(CashActivity.this, "حذف شد");
                            refresh();
                        }
                    });
                    return true;
                }
            });
            list.addView(card);
        }
        c.close();
        if (!any) {
            LinearLayout e = card();
            e.addView(tv("تراکنشی ثبت نشده است.", U.SUB, 13, false));
            list.addView(e);
        }
    }
}

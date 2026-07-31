package com.talayar.app;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** موجودی طلای آبشده / کارکرده */
public class GoldActivity extends A {
    private LinearLayout list;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        scaffold("طلای آبشده / کارکرده", true);

        LinearLayout head = cardHi();
        long[] g = db.goldBalance();
        head.addView(tv("موجودی طلا", U.SUB, 13, false));
        head.addView(tv(U.mw((int) g[1]) + " گرم (۱۸ معادل)", U.GOLD, 22, true));
        head.addView(tv("وزن خام: " + U.mw((int) g[0]) + " گرم", U.SUB, 13, false));
        long rate = db.currentRate();
        if (rate > 0) {
            head.addView(kv("ارزش روز تقریبی", U.money(Math.round(g[1] * rate / 1000.0)) + " تومان"));
        }
        body.addView(head);

        LinearLayout ops = card();
        LinearLayout row = h();
        row.addView(btn("＋ ورود دستی طلا", new Tap() { public void go() { manual("in"); } }),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(wspace(8));
        row.addView(gbtn("− خروج دستی طلا", new Tap() { public void go() { manual("out"); } }),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        ops.addView(row);
        body.addView(ops);

        TextView lt = tv("گردش طلا", U.GOLD, 15, true);
        lt.setPadding(dp(4), dp(8), 0, dp(2));
        body.addView(lt);
        list = v();
        body.addView(list);
        refresh();
    }

    @Override protected void onResume() { super.onResume(); if (list != null) refresh(); }

    private void manual(final String kind) {
        final LinearLayout box = v();
        box.setPadding(dp(16), dp(14), dp(16), dp(10));
        box.addView(tv("in".equals(kind) ? "ورود دستی طلا (آبشده/دستی)" : "خروج دستی طلا", U.GOLD, 16, true));
        box.addView(space(6));
        final android.widget.EditText ew = in("وزن (گرم)", true);
        box.addView(ew);
        box.addView(label("عیار"));
        final int[] karat = {750};
        box.addView(chipsRow(new String[]{"۲۴","۲۲","۲۱","۱۸","۱۴","۹"}, 3, new OnIdx() {
            public void ok(int i) { karat[0] = ItemEditActivity.K_VALS[i]; }
        }));
        final android.widget.EditText ed = in("شرح");
        box.addView(ed);
        box.addView(space(8));
        final android.app.AlertDialog d = sheet(box);
        LinearLayout br = h();
        br.setGravity(android.view.Gravity.CENTER);
        br.addView(btn("ثبت", new Tap() { public void go() {
            int w = U.parseMw(U.str(ew));
            if (w <= 0) { U.toast(GoldActivity.this, "وزن نامعتبر"); return; }
            if ("out".equals(kind)) {
                long[] g = db.goldBalance();
                if (U.equiv750(w, karat[0]) > g[1]) { U.toast(GoldActivity.this, "موجودی کافی نیست"); return; }
            }
            String desc = U.str(ed);
            if (desc.length() == 0) desc = ("in".equals(kind) ? "ورود دستی " : "خروج دستی ") + U.mw(w) + " گرم " + U.karatName(karat[0]);
            android.content.ContentValues gt = new android.content.ContentValues();
            gt.put("ts", System.currentTimeMillis());
            gt.put("date_j", Jal.today());
            gt.put("kind", kind);
            gt.put("wmw", w);
            gt.put("karat", karat[0]);
            gt.put("descr", desc);
            gt.put("cid", 0);
            db.ins("gold_tx", gt);
            d.dismiss();
            U.toast(GoldActivity.this, "ثبت شد ✓");
            recreate();
        } }), new LinearLayout.LayoutParams(dp(130), ViewGroup.LayoutParams.WRAP_CONTENT));
        br.addView(wspace(10));
        br.addView(gbtn("انصراف", new Tap() { public void go() { d.dismiss(); } }),
                new LinearLayout.LayoutParams(dp(110), ViewGroup.LayoutParams.WRAP_CONTENT));
        box.addView(br);
    }

    private void refresh() {
        list.removeAllViews();
        Cursor c = db.r().rawQuery("SELECT * FROM gold_tx ORDER BY ts DESC, id DESC LIMIT 200", null);
        boolean any = false;
        while (c.moveToNext()) {
            any = true;
            final long id = Db.cl(c, "id");
            final boolean isIn = !"out".equals(Db.cs(c, "kind"));
            LinearLayout card = card(dp(10));
            LinearLayout top = h();
            top.addView(tv(isIn ? "↙ ورود" : "↗ خروج", isIn ? U.OK : 0xFFFFA9B1, 13, true));
            top.addView(wspace(8));
            top.addView(tv(U.dig(Db.cs(c, "date_j")), U.SUB, 12, false),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            int karat = Db.ci(c, "karat");
            int w = Db.ci(c, "wmw");
            top.addView(tvM((isIn ? "+" : "−") + U.mw((int) U.equiv750(w, karat)) + " گرم", isIn ? U.OK : 0xFFFFA9B1, 13));
            card.addView(top);
            card.addView(tv(Db.cs(c, "descr") + " (" + U.mw(w) + " گرم " + U.karatName(karat) + ")", U.TXT, 12, false));
            card.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    confirm("این رکورد حذف شود؟ (موجودی طلا تغییر می‌کند)", new Tap() {
                        public void go() {
                            db.w().delete("gold_tx", "id=?", new String[]{"" + id});
                            U.toast(GoldActivity.this, "حذف شد");
                            recreate();
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
            e.addView(tv("تراکنشی ثبت نشده — از خرید طلای کارکرده یا ورود دستی استفاده کنید.", U.SUB, 13, false));
            list.addView(e);
        }
    }
}

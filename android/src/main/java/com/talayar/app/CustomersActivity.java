package com.talayar.app;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** حساب‌ها / مشتریان — کد حساب + گروه حساب، فیلتر (نام، تلفن، از کد تا کد)، مانده چند مشتری */
public class CustomersActivity extends A {
    private LinearLayout list;
    private EditText eSearch, eFrom, eTo;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        scaffold("حساب‌ها / مشتریان", true);

        // خلاصه
        LinearLayout add = cardHi();
        long[] sp = db.customerDebtSplit();
        add.addView(tv("وضعیت کلی حساب‌ها", U.GOLD, 14, true));
        add.addView(kv("مجموع بدهی نقدی مشتریان به ما", U.money(sp[0]) + " تومان", 0xFFFFCC80));
        add.addView(kv("مجموع بستانکاری نقدی ما به مشتریان", U.money(sp[1]) + " تومان", U.OK));
        add.addView(kv("مجموع بدهی طلایی مشتریان (۱۸ معادل)", U.gs((int) sp[2]), sp[2] > 0 ? 0xFFFFCC80 : U.TXT));
        LinearLayout ar = h();
        ar.addView(btn("＋ مشتری / حساب جدید", new Tap() {
            public void go() { editCustomer(CustomersActivity.this, 0, new Tap() { public void go() { refresh(); } }); }
        }),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        ar.addView(wspace(8));
        ar.addView(gbtn("📊 مانده چند مشتری (تفکیک گروه)", new Tap() { public void go() { groupBalance(); } }),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        add.addView(space(2));
        add.addView(ar);
        body.addView(add);

        // فیلترها
        LinearLayout f = card();
        f.addView(tv("فیلتر و جستجو", U.SUB, 12, false));
        eSearch = in("نام حساب یا شماره تلفن…", false);
        f.addView(eSearch);
        eSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b2, int c) {}
            public void onTextChanged(CharSequence s, int a, int b2, int c) { refresh(); }
            public void afterTextChanged(Editable s) {}
        });
        f.addView(space(4));
        LinearLayout rng = h();
        eFrom = in("از کد حساب", true);
        eTo = in("تا کد حساب", true);
        rng.addView(eFrom, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        rng.addView(wspace(6));
        rng.addView(eTo, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        rng.addView(wspace(6));
        rng.addView(gbtn("اعمال", new Tap() { public void go() { refresh(); } }));
        rng.addView(wspace(6));
        rng.addView(gbtn("همه", new Tap() {
            public void go() { eFrom.setText(""); eTo.setText(""); eSearch.setText(""); refresh(); }
        }));
        f.addView(rng);
        body.addView(f);

        list = v();
        body.addView(list);
        refresh();
    }

    @Override protected void onResume() { super.onResume(); if (list != null) refresh(); }

    /** افزودن/ویرایش مشتری (اشتراکی با پروندهٔ مشتری) — id=0 یعنی جدید */
    static void editCustomer(final A act, final int id, final Tap done) {
        String nm = "", ph = "", nt = "", adr = "", grp = "";
        int code = 1;
        Cursor nc = act.db.r().rawQuery("SELECT COALESCE(MAX(code),0)+1 FROM customers", null);
        if (nc.moveToFirst()) code = nc.getInt(0);
        nc.close();
        if (id > 0) {
            Cursor c = act.db.r().rawQuery("SELECT name, phone, note, code, grp, address FROM customers WHERE id=?",
                    new String[]{"" + id});
            if (c.moveToFirst()) {
                nm = c.getString(0);
                ph = c.getString(1) == null ? "" : c.getString(1);
                nt = c.getString(2) == null ? "" : c.getString(2);
                code = c.getInt(3);
                grp = c.getString(4) == null ? "" : c.getString(4);
                adr = c.getString(5) == null ? "" : c.getString(5);
            }
            c.close();
        }
        final String[] selGrp = {grp};
        final LinearLayout box = act.v();
        box.setPadding(act.dp(16), act.dp(14), act.dp(16), act.dp(10));
        box.addView(act.tv(id == 0 ? "ثبت مشتری / حساب جدید" : "ویرایش حساب", U.GOLD, 16, true));
        box.addView(act.space(6));
        final EditText eName = act.in("نام و نام خانوادگی");
        eName.setText(nm);
        final EditText eCode = act.in("کد حساب", true);
        eCode.setText(U.dig(code + ""));
        final TextView grpTv = act.tvM(selGrp[0].length() > 0 ? "گروه: " + selGrp[0] : "گروه حساب: انتخاب…", U.TXT, 13);
        final EditText ePhone = act.in("تلفن (اختیاری)", true);
        ePhone.setText(U.dig(ph));
        final EditText eAddr = act.in("نشانی (اختیاری)");
        eAddr.setText(adr);
        final EditText eNote = act.in("یادداشت (اختیاری)");
        eNote.setText(nt);
        box.addView(eName);
        box.addView(act.space(6));
        LinearLayout rc = act.h();
        rc.addView(eCode, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        rc.addView(act.wspace(6));
        LinearLayout gc = act.v();
        gc.setBackgroundDrawable(A.round(0xFF0E141F, 12, U.STROKE, 1));
        gc.setPadding(act.dp(10), act.dp(8), act.dp(10), act.dp(8));
        gc.addView(grpTv);
        gc.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Cursor c2 = act.db.defsOf("group");
                final java.util.ArrayList<String> gs = new java.util.ArrayList<String>();
                while (c2.moveToNext()) gs.add(c2.getString(2));
                c2.close();
                if (gs.isEmpty()) { U.toast(act, "ابتدا در «کدینگ‌ها» گروه بسازید"); return; }
                String[] arr = new String[gs.size()];
                for (int i2 = 0; i2 < gs.size(); i2++) arr[i2] = (String) gs.get(i2);
                act.choose("گروه حساب", arr, new OnIdx() {
                    public void ok(int i2) {
                        selGrp[0] = (String) gs.get(i2);
                        grpTv.setText("گروه: " + selGrp[0]);
                    }
                });
            }
        });
        rc.addView(gc, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        box.addView(rc);
        box.addView(act.space(6));
        box.addView(ePhone);
        box.addView(act.space(6));
        box.addView(eAddr);
        box.addView(act.space(6));
        box.addView(eNote);
        box.addView(act.space(8));
        final android.app.AlertDialog d = act.sheet(box);
        LinearLayout br = act.h();
        br.setGravity(android.view.Gravity.CENTER);
        br.addView(act.btn(id == 0 ? "ثبت" : "ذخیره", new Tap() {
            public void go() {
                String n = U.str(eName);
                if (n.length() == 0) { U.toast(act, "نام را بنویسید"); return; }
                int cd = (int) U.parseMoney(U.str(eCode));
                if (cd <= 0) { U.toast(act, "کد حساب معتبر نیست"); return; }
                Cursor cc = act.db.r().rawQuery("SELECT COUNT(*) FROM customers WHERE code=? AND id!=?",
                        new String[]{"" + cd, "" + id});
                cc.moveToFirst();
                int dup = cc.getInt(0);
                cc.close();
                if (dup > 0) { U.toast(act, "این کد حساب قبلاً استفاده شده"); return; }
                android.content.ContentValues cv = new android.content.ContentValues();
                cv.put("name", n);
                cv.put("code", cd);
                cv.put("grp", selGrp[0]);
                cv.put("phone", U.en(U.str(ePhone)));
                cv.put("address", U.str(eAddr));
                cv.put("note", U.str(eNote));
                if (id == 0) {
                    cv.put("cts", System.currentTimeMillis());
                    act.db.ins("customers", cv);
                } else {
                    act.db.w().update("customers", cv, "id=?", new String[]{"" + id});
                }
                d.dismiss();
                U.toast(act, "ذخیره شد ✓");
                done.go();
            }
        }), new LinearLayout.LayoutParams(act.dp(120), ViewGroup.LayoutParams.WRAP_CONTENT));
        br.addView(act.wspace(10));
        br.addView(act.gbtn("انصراف", new Tap() { public void go() { d.dismiss(); } }),
                new LinearLayout.LayoutParams(act.dp(110), ViewGroup.LayoutParams.WRAP_CONTENT));
        box.addView(br);
    }

    private void refresh() {
        list.removeAllViews();
        String q = U.en(U.str(eSearch)).trim();
        long cf = U.parseMoney(U.str(eFrom)), ct = U.parseMoney(U.str(eTo));
        Cursor c = db.r().rawQuery(
                "SELECT c.id, c.code, c.name, c.phone, c.grp, COALESCE(s.cash,0), COALESCE(s.gold,0) FROM customers c " +
                "LEFT JOIN (SELECT cid, SUM(cash) cash, SUM(goldmw) gold FROM customer_tx GROUP BY cid) s ON s.cid=c.id " +
                "ORDER BY c.code", null);
        boolean any = false;
        while (c.moveToNext()) {
            final int id = c.getInt(0);
            int code = c.getInt(1);
            String name = c.getString(2);
            String phone = c.getString(3);
            String grp = c.getString(4);
            long cash = c.getLong(5);
            long gold = c.getLong(6);
            if (q.length() > 0) {
                String hay = U.en(name + " " + (phone == null ? "" : phone));
                if (!hay.contains(q)) continue;
            }
            if (cf > 0 && code < cf) continue;
            if (ct > 0 && code > ct) continue;

            any = true;
            LinearLayout card = card(dp(10));
            LinearLayout top = h();
            top.addView(tv(U.dig(code + ""), U.GOLD2, 12, true));
            top.addView(wspace(6));
            top.addView(tv(name, U.TXT, 15, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            if (cash > 0) top.addView(badge("بدهکار " + U.money(cash), false));
            else if (cash < 0) top.addView(badge("بستانکار " + U.money(-cash), true));
            else top.addView(badge("تسویه", true));
            card.addView(top);
            LinearLayout sub = h();
            String info = (grp != null && grp.length() > 0 ? grp + " • " : "") +
                    (phone != null && phone.length() > 0 ? "☎ " + U.dig(phone) : "بدون تلفن");
            sub.addView(tv(info, U.SUB, 12, false), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            if (gold != 0) {
                sub.addView(tvM(U.gs((int) Math.abs(gold)) + " " + (gold > 0 ? "بدهکار" : "بستانکار"),
                        gold > 0 ? 0xFFFFCC80 : U.OK, 12));
            }
            card.addView(sub);
            card.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent i = new Intent(CustomersActivity.this, CustomerViewActivity.class);
                    i.putExtra("id", id);
                    startActivity(i);
                }
            });
            list.addView(card);
        }
        c.close();
        if (!any) {
            LinearLayout e = card();
            e.addView(tv("حسابی یافت نشد — با «مشتری/حساب جدید» اضافه کنید.", U.SUB, 13, false));
            list.addView(e);
        }
    }

    /** مانده چند مشتری — انتخاب گروه حساب و گزارش تجمیعی */
    private void groupBalance() {
        final java.util.ArrayList<String> gs = new java.util.ArrayList<String>();
        gs.add("— همهٔ گروه‌ها —");
        Cursor c = db.defsOf("group");
        while (c.moveToNext()) gs.add(c.getString(2));
        c.close();
        String[] arr = new String[gs.size()];
        for (int i = 0; i < gs.size(); i++) arr[i] = (String) gs.get(i);
        choose("گزارش مانده — انتخاب گروه حساب", arr, new OnIdx() {
            public void ok(int i) {
                showGroupBalance(i == 0 ? null : (String) gs.get(i));
            }
        });
    }

    private void showGroupBalance(final String grp) {
        final LinearLayout box = v();
        box.setPadding(dp(16), dp(14), dp(16), dp(10));
        box.addView(tv("مانده حساب — " + (grp == null ? "همهٔ گروه‌ها" : "گروه «" + grp + "»"), U.GOLD, 16, true));
        box.addView(tv("به‌همراه تلفن و کد حساب؛ مرتب‌شده بر اساس کد.", U.SUB, 11, false));
        box.addView(space(6));
        LinearLayout dr = h();
        final EditText eDate = in("تا تاریخ (اختیاری — ۱۴۰۵/۰۵/۰۹)", false);
        dr.addView(eDate, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        dr.addView(wspace(6));
        final LinearLayout rows = v();
        final TextView tot = tvM("", U.TXT, 13);
        dr.addView(gbtn("محاسبه", new Tap() {
            public void go() { fillGroupRows(rows, tot, grp, U.en(U.str(eDate)).trim()); }
        }));
        box.addView(dr);
        box.addView(space(6));
        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        sv.addView(rows);
        box.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(300)));
        box.addView(space(4));
        box.addView(tot);
        box.addView(space(8));
        final android.app.AlertDialog d = sheet(box);
        LinearLayout br = h();
        br.setGravity(android.view.Gravity.CENTER);
        br.addView(gbtn("بستن", new Tap() { public void go() { d.dismiss(); } }),
                new LinearLayout.LayoutParams(dp(120), ViewGroup.LayoutParams.WRAP_CONTENT));
        box.addView(br);
        fillGroupRows(rows, tot, grp, "");
    }

    private void fillGroupRows(LinearLayout rows, TextView tot, String grp, String maxDate) {
        rows.removeAllViews();
        if (maxDate.length() >= 10 && !maxDate.matches("[0-9]{4}/[0-9]{2}/[0-9]{2}")) maxDate = "";
        String dateCond = maxDate.length() >= 10 ? " AND date_j <= '" + maxDate + "'" : "";
        String grpCond = grp == null ? "" : " WHERE grp = ?";
        String[] args = grp == null ? new String[0] : new String[]{grp};
        Cursor c = db.r().rawQuery(
                "SELECT id, code, name, phone FROM customers" + grpCond + " ORDER BY code", args);
        long sumC = 0, sumG = 0;
        int n = 0;
        while (c.moveToNext()) {
            int id = c.getInt(0);
            int code = c.getInt(1);
            String name = c.getString(2);
            String phone = c.getString(3);
            Cursor s = db.r().rawQuery(
                    "SELECT COALESCE(SUM(cash),0), COALESCE(SUM(goldmw),0) FROM customer_tx WHERE cid=?" + dateCond,
                    new String[]{"" + id});
            long cs = 0, g = 0;
            if (s.moveToFirst()) { cs = s.getLong(0); g = s.getLong(1); }
            s.close();
            if (cs == 0 && g == 0) continue;
            n++;
            sumC += cs;
            sumG += g;
            LinearLayout r = h();
            r.addView(tv(U.dig(code + ""), U.GOLD2, 11, true));
            r.addView(wspace(6));
            LinearLayout mid = v();
            mid.addView(tv(name + (phone != null && phone.length() > 0 ? " • " + U.dig(phone) : ""), U.TXT, 13, false));
            r.addView(mid, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            LinearLayout vals = v();
            TextView tv1 = tvM(cs > 0 ? U.money(cs) + " بدهکار" : cs < 0 ? U.money(-cs) + " بستانکار" : "تسویه",
                    cs > 0 ? 0xFFFFCC80 : cs < 0 ? U.OK : U.SUB, 12);
            tv1.setGravity(android.view.Gravity.LEFT);
            vals.addView(tv1);
            if (g != 0) {
                TextView tv2 = tvM(U.gs((int) Math.abs(g)) + (g > 0 ? " بدهکار" : " بستانکار"),
                        g > 0 ? 0xFFFFCC80 : U.OK, 11);
                tv2.setGravity(android.view.Gravity.LEFT);
                vals.addView(tv2);
            }
            r.addView(vals);
            r.setPadding(0, dp(4), 0, dp(4));
            rows.addView(r);
            View dv = new View(this);
            dv.setBackgroundColor(0xFF1E2632);
            rows.addView(dv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        }
        c.close();
        if (n == 0) {
            rows.addView(tv("حسابی با ماندهٔ غیرصفر یافت نشد.", U.SUB, 13, false));
        }
        tot.setText("مجموع (" + U.intFa(n) + " حساب): " + U.money(sumC) + " تومان" +
                (sumG != 0 ? " + " + U.gs((int) sumG) : "") +
                (maxDate.length() >= 10 ? "  (تا تاریخ " + U.dig(maxDate) + ")" : ""));
    }
}

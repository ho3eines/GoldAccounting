package com.talayar.app;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** کدینگ‌ها — لیست نام ری‌گیری‌ها، ارزها، سکه‌ها، شمش‌ها، بانک‌ها، کارهای ساخته، گروه‌های حساب */
public class DefsActivity extends A {

    static final String[][] KINDS = {
            {"group",   "گروه‌های حساب"},
            {"rizgiri", "ری‌گیری‌ها"},
            {"curr",    "ارزها"},
            {"coin",    "سکه‌ها"},
            {"bullion", "شمش‌ها"},
            {"bank",    "بانک‌ها"},
            {"work",    "کارهای ساخته"},
            {"silver",  "نقره"},
    };

    static String kindName(String kind) {
        for (int i = 0; i < KINDS.length; i++) if (KINDS[i][0].equals(kind)) return KINDS[i][1];
        return kind;
    }

    private String kind = "group";
    private LinearLayout list;
    private TextView hdr;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        String k0 = getIntent().getStringExtra("kind");
        if (k0 != null && k0.length() > 0) kind = k0;

        scaffold("کدینگ‌ها", true);

        LinearLayout pick = card();
        pick.addView(tv("بخش کدینگ", U.SUB, 12, false));
        String[] names = new String[KINDS.length];
        int sel = 0;
        for (int i = 0; i < KINDS.length; i++) {
            names[i] = KINDS[i][1];
            if (KINDS[i][0].equals(kind)) sel = i;
        }
        pick.addView(chipsRow(names, sel, new OnIdx() {
            public void ok(int i) { kind = KINDS[i][0]; hdr.setText(kindName(kind)); refresh(); }
        }));
        body.addView(pick);

        LinearLayout add = card();
        hdr = tv(kindName(kind), U.GOLD, 15, true);
        add.addView(hdr);
        add.addView(tv("در اسناد و فرم‌ها از همین کدینگ‌ها استفاده می‌شود.", U.SUB, 11, false));
        addBtn(add, btn("＋ افزودن مورد جدید", new Tap() { public void go() { editDlg(0); } }));
        body.addView(add);

        list = v();
        body.addView(list);
        refresh();
    }

    @Override protected void onResume() { super.onResume(); if (list != null) refresh(); }

    private String subOf(Cursor c) {
        long x1 = Db.cl(c, "x1");
        long x2 = Db.cl(c, "x2");
        String x3 = Db.cs(c, "x3");
        if ("coin".equals(kind) || "bullion".equals(kind)) {
            return "وزن: " + U.mwG((int) x1) + " • عیار " + U.dig(x2 + "");
        }
        if ("silver".equals(kind)) return "عیار " + U.dig(x2 + "/1000");
        if ("bank".equals(kind)) return x3.length() > 0 ? "شماره حساب: " + U.dig(x3) : "";
        return x3;
    }

    private void refresh() {
        list.removeAllViews();
        Cursor c = db.defsOf(kind);
        boolean any = false;
        while (c.moveToNext()) {
            any = true;
            final int id = Db.ci(c, "id");
            final String name = Db.cs(c, "name");
            String sub = subOf(c);
            LinearLayout card = card(dp(10));
            LinearLayout top = h();
            top.addView(tv(name, U.TXT, 14, true),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            TextView ed = tv(" ✎ ", U.BLUE, 14, true);
            ed.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { editDlg(id); }
            });
            top.addView(ed);
            card.addView(top);
            if (sub.length() > 0) card.addView(tv(sub, U.SUB, 12, false));
            list.addView(card);
        }
        c.close();
        if (!any) {
            LinearLayout e = card();
            e.addView(tv("موردی ثبت نشده — از دکمهٔ «افزودن» استفاده کنید.", U.SUB, 13, false));
            list.addView(e);
        }
    }

    /** آیا این کدینگ در اسناد استفاده شده؟ (برای جلوگیری از حذف ناسازگار) */
    private boolean defUsed(String kind, int id, String name) {
        String tbl = null, where = null, arg = null;
        if ("group".equals(kind)) { tbl = "customers"; where = "grp=?"; arg = name; }
        else if ("bank".equals(kind)) {
            Cursor c = db.r().rawQuery(
                    "SELECT (SELECT COUNT(*) FROM bank_tx WHERE bank_id=?) + (SELECT COUNT(*) FROM checks WHERE bank_id=?)",
                    new String[]{"" + id, "" + id});
            c.moveToFirst();
            int n = c.getInt(0);
            c.close();
            return n > 0;
        } else if ("coin".equals(kind) || "bullion".equals(kind) || "curr".equals(kind) || "silver".equals(kind)) {
            String prefix = "coin".equals(kind) ? "coin" : "bullion".equals(kind) ? "bull" : "curr".equals(kind) ? "cur" : "sil";
            tbl = "assets_ledger"; where = "asset=?"; arg = prefix + "_d" + id;
        }
        if (tbl == null) return false;
        Cursor c = db.r().rawQuery("SELECT COUNT(*) FROM " + tbl + " WHERE " + where, new String[]{arg});
        c.moveToFirst();
        int n = c.getInt(0);
        c.close();
        return n > 0;
    }

    /** افزودن/ویرایش — id=0 یعنی جدید */
    private void editDlg(final int id) {
        String curName = "", curX3 = "";
        long curX1 = 0, curX2 = 0;
        if (id > 0) {
            Cursor c = db.r().rawQuery("SELECT name, x1, x2, x3 FROM defs WHERE id=?", new String[]{"" + id});
            if (c.moveToFirst()) {
                curName = c.getString(0);
                curX1 = c.getLong(1);
                curX2 = c.getLong(2);
                curX3 = c.getString(3) == null ? "" : c.getString(3);
            }
            c.close();
        }
        final boolean withW = "coin".equals(kind) || "bullion".equals(kind);
        final boolean withK = withW || "silver".equals(kind);
        final boolean withN = "bank".equals(kind);
        final LinearLayout box = v();
        box.setPadding(dp(16), dp(14), dp(16), dp(10));
        box.addView(tv((id == 0 ? "افزودن " : "ویرایش ") + kindName(kind), U.GOLD, 16, true));
        box.addView(space(6));
        final EditText eName = in("نام");
        eName.setText(curName);
        box.addView(eName);
        final EditText eW = in("۰", true);
        final EditText eK = in(withW ? "۹۰۰" : "۹۹۹", true);
        final EditText eNo = in("", true);
        if (curX1 > 0) eW.setText(U.mw((int) curX1));
        if (curX2 > 0) eK.setText(U.dig(curX2 + ""));
        eNo.setText(U.dig(curX3));
        if (withW) {
            box.addView(space(6));
            box.addView(label("وزن هر عدد (گرم — مثل ۸٫۱۳۳)"));
            box.addView(eW);
        }
        if (withK) {
            box.addView(label(withW ? "عیار (مثل ۹۰۰ یا ۹۹۵)" : "عیار (مثل ۹۹۹ یا ۹۲۵)"));
            box.addView(eK);
        }
        if (withN) {
            box.addView(space(6));
            box.addView(label("شماره حساب (اختیاری)"));
            box.addView(eNo);
        }
        box.addView(space(8));
        final android.app.AlertDialog d = sheet(box);
        LinearLayout br = h();
        br.setGravity(android.view.Gravity.CENTER);
        br.addView(btn(id == 0 ? "ثبت" : "ذخیره", new Tap() {
            public void go() {
                String nm = U.str(eName);
                if (nm.length() == 0) { U.toast(DefsActivity.this, "نام را بنویسید"); return; }
                ContentValues cv = new ContentValues();
                cv.put("kind", kind);
                cv.put("name", nm);
                if (withW) cv.put("x1", U.parseMw(U.str(eW)));
                if (withK) cv.put("x2", (long) U.parseDouble(U.str(eK)));
                if (withN) cv.put("x3", U.en(U.str(eNo)));
                if (id == 0) {
                    cv.put("cts", System.currentTimeMillis());
                    db.ins("defs", cv);
                } else {
                    db.w().update("defs", cv, "id=?", new String[]{"" + id});
                }
                d.dismiss();
                U.toast(DefsActivity.this, "ذخیره شد ✓");
                refresh();
            }
        }), new LinearLayout.LayoutParams(dp(110), ViewGroup.LayoutParams.WRAP_CONTENT));
        if (id > 0) {
            br.addView(wspace(6));
            br.addView(dbtn("حذف", new Tap() {
                public void go() {
                    Cursor c = db.r().rawQuery("SELECT name FROM defs WHERE id=?", new String[]{"" + id});
                    String nm = "";
                    if (c.moveToFirst()) nm = c.getString(0);
                    c.close();
                    if (defUsed(kind, id, nm)) {
                        d.dismiss();
                        msg("قابل حذف نیست", "این مورد در اسناد/حساب‌ها استفاده شده و برای حفظ صحت اسناد قابل حذف نیست. می‌توانید آن را ویرایش کنید.");
                        return;
                    }
                    db.w().delete("defs", "id=?", new String[]{"" + id});
                    d.dismiss();
                    U.toast(DefsActivity.this, "حذف شد");
                    refresh();
                }
            }), new LinearLayout.LayoutParams(dp(90), ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        br.addView(wspace(6));
        br.addView(gbtn("انصراف", new Tap() { public void go() { d.dismiss(); } }),
                new LinearLayout.LayoutParams(dp(90), ViewGroup.LayoutParams.WRAP_CONTENT));
        box.addView(br);
    }
}

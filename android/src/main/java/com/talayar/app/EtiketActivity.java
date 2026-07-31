package com.talayar.app;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/** اتیکت‌ها — لیست، جستجو/فیلتر (کد کار، نام، وزن، بازهٔ شناسه، به‌روزشده‌ها) + افزودن/ویرایش */
public class EtiketActivity extends A {
    static final int REQ_PHOTO = 42;

    // حالت مشترک دیالوگ ویرایش/عکس بین دو اکتیویتی اتیکت
    static String pendingPhoto = null;
    static TextView photoLbl = null;

    private LinearLayout list;
    private EditText eSearch;
    private int filter = 0;          // 0=همه 1=به‌روزشده‌ها 2=بازه شناسه 3=بازه وزن
    private long rngA = 0, rngB = 0; // بازهٔ شناسه یا وزن(میلی‌گرم)

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        scaffold("اتیکت‌ها", true);

        // اطلاعات جدول اتیکت‌ها
        refreshSummary();

        LinearLayout find = card();
        eSearch = in("جستجو: کد کار یا نام کار…", false);
        find.addView(eSearch);
        eSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b2, int c) {}
            public void onTextChanged(CharSequence s, int a, int b2, int c) { refresh(); }
            public void afterTextChanged(Editable s) {}
        });
        find.addView(space(4));
        find.addView(chipsRow(new String[]{"همه", "به‌روزشده‌ها", "بازهٔ شناسه", "بازهٔ وزن"}, 0, new OnIdx() {
            public void ok(int i) {
                if (i == 2) { askRange(true); return; }
                if (i == 3) { askRange(false); return; }
                filter = i;
                refresh();
            }
        }));
        body.addView(find);

        LinearLayout add = card();
        addBtn(add, btn("＋ اتیکت جدید", new Tap() {
            public void go() {
                editDlg(EtiketActivity.this, 0, new Tap() { public void go() { refreshAll(); } });
            }
        }));
        body.addView(add);

        list = v();
        body.addView(list);
        refresh();
    }

    private void refreshAll() { refreshSummary2(); refresh(); }

    @Override protected void onResume() { super.onResume(); if (list != null) refreshAll(); }

    // ---- خلاصه جدول ----
    private LinearLayout sumBox;
    private void refreshSummary() {
        sumBox = card();
        sumBox.addView(tv("📋 اطلاعات جدول اتیکت‌ها", U.GOLD, 14, true));
        body.addView(sumBox);
        refreshSummary2();
    }
    private void refreshSummary2() {
        if (sumBox == null) return;
        while (sumBox.getChildCount() > 1) sumBox.removeViewAt(1);
        Cursor c = db.r().rawQuery(
                "SELECT COUNT(*), COALESCE(SUM(wmw),0), " +
                "SUM(CASE WHEN updated_ts > cts + 60000 THEN 1 ELSE 0 END), " +
                "SUM(CASE WHEN rfid IS NOT NULL AND rfid != '' THEN 1 ELSE 0 END) FROM etiket", null);
        int n = 0, upd = 0, rf = 0;
        long w = 0;
        if (c.moveToFirst()) {
            n = c.getInt(0); w = c.getLong(1);
            upd = c.isNull(2) ? 0 : c.getInt(2);
            rf = c.isNull(3) ? 0 : c.getInt(3);
        }
        c.close();
        sumBox.addView(kv("تعداد اتیکت", U.intFa(n), U.TXT));
        sumBox.addView(kv("مجموع وزن", U.gs((int) w), U.GOLD));
        sumBox.addView(kv("به‌روزشده‌ها", U.intFa(upd) + " • دارای RFID: " + U.intFa(rf), U.SUB));
    }

    private void askRange(final boolean byId) {
        final LinearLayout box = v();
        box.setPadding(dp(16), dp(14), dp(16), dp(10));
        box.addView(tv(byId ? "لیست اتیکت‌ها (از شناسه کار تا شناسه کار)" : "فیلتر وزن (گرم)", U.GOLD, 15, true));
        box.addView(space(6));
        final EditText eA = in(byId ? "از شناسه" : "از وزن", true);
        final EditText eB = in(byId ? "تا شناسه" : "تا وزن", true);
        box.addView(eA); box.addView(space(6)); box.addView(eB); box.addView(space(8));
        final android.app.AlertDialog d = sheet(box);
        LinearLayout br = h();
        br.setGravity(android.view.Gravity.CENTER);
        br.addView(btn("اعمال", new Tap() {
            public void go() {
                if (byId) { rngA = U.parseMoney(U.str(eA)); rngB = U.parseMoney(U.str(eB)); filter = 2; }
                else { rngA = (long) U.parseMw(U.str(eA)); rngB = (long) U.parseMw(U.str(eB)); filter = 3; }
                if (rngA > 0 && rngB > 0 && rngA > rngB) { long t = rngA; rngA = rngB; rngB = t; }
                d.dismiss();
                refresh();
            }
        }), new LinearLayout.LayoutParams(dp(120), ViewGroup.LayoutParams.WRAP_CONTENT));
        br.addView(wspace(10));
        br.addView(gbtn("انصراف", new Tap() { public void go() { d.dismiss(); } }),
                new LinearLayout.LayoutParams(dp(110), ViewGroup.LayoutParams.WRAP_CONTENT));
        box.addView(br);
    }

    private void refresh() {
        list.removeAllViews();
        String q = U.en(U.str(eSearch)).trim();
        Cursor c = db.r().rawQuery("SELECT * FROM etiket ORDER BY code, id LIMIT 500", null);
        boolean any = false;
        while (c.moveToNext()) {
            final int id = Db.ci(c, "id");
            String code = Db.cs(c, "code");
            String name = Db.cs(c, "name");
            int w = Db.ci(c, "wmw");
            String mez = Db.cs(c, "mezane");
            String photo = Db.cs(c, "photo");
            String rfid = Db.cs(c, "rfid");
            long upd = Db.cl(c, "updated_ts");
            long cts = Db.cl(c, "cts");

            if (q.length() > 0) {
                String hay = U.en(code + " " + name);
                if (!hay.contains(q)) continue;
            }
            if (filter == 1 && !(upd > cts + 60000)) continue;
            if (filter == 2 && rngA > 0 && rngB > 0 && (id < rngA || id > rngB)) continue;
            if (filter == 3 && (w < rngA || w > rngB)) continue;

            any = true;
            LinearLayout card = card(dp(10));
            LinearLayout top = h();
            top.addView(tv(U.dig(code), U.GOLD, 15, true),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            if (photo.length() > 0) top.addView(tv(" 🖼 ", U.TXT, 12, false));
            if (rfid.length() > 0) top.addView(tv(" 📡 ", U.TXT, 12, false));
            if (upd > cts + 60000) top.addView(badge("به‌روزشده", true));
            card.addView(top);
            LinearLayout sub = h();
            sub.addView(tv(name, U.TXT, 14, false),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            sub.addView(tvM(U.gs(w), U.TXT, 13));
            card.addView(sub);
            if (mez.length() > 0) card.addView(tv("مزنه: " + mez, U.SUB, 12, false));
            card.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent it = new Intent(EtiketActivity.this, EtiketViewActivity.class);
                    it.putExtra("id", id);
                    startActivity(it);
                }
            });
            list.addView(card);
        }
        c.close();
        if (!any) {
            LinearLayout e = card();
            e.addView(tv("اتیکتی یافت نشد — با دکمهٔ «اتیکت جدید» بسازید.", U.SUB, 13, false));
            list.addView(e);
        }
    }

    // ---------- دیالوگ افزودن/ویرایش (اشتراکی با EtiketViewActivity) ----------
    static void editDlg(final A act, final int eid, final Tap done) {
        String code = "", name = "", mez = "", rfid = "", photo = "";
        int w = 0;
        if (eid > 0) {
            Cursor c = act.db.r().rawQuery("SELECT code,name,wmw,mezane,rfid,photo FROM etiket WHERE id=?", new String[]{"" + eid});
            if (c.moveToFirst()) {
                code = c.getString(0); name = c.getString(1); w = c.getInt(2);
                mez = c.getString(3) == null ? "" : c.getString(3);
                rfid = c.getString(4) == null ? "" : c.getString(4);
                photo = c.getString(5) == null ? "" : c.getString(5);
            }
            c.close();
        }
        pendingPhoto = photo;

        final LinearLayout box = act.v();
        box.setPadding(act.dp(16), act.dp(14), act.dp(16), act.dp(10));
        box.addView(act.tv(eid == 0 ? "اتیکت جدید" : "ویرایش اتیکت", U.GOLD, 16, true));
        box.addView(act.space(6));
        final EditText eCode = act.in("کد کار (شناسهٔ اتیکت)");
        eCode.setText(U.dig(code));
        final EditText eName = act.in("نام کار (مثل: النگو ستاره)");
        eName.setText(name);
        final EditText eW = act.in("وزن (گرم و سوت)", true);
        if (w > 0) eW.setText(U.mw(w));
        final EditText eMez = act.in("مزنه (اختیاری)");
        eMez.setText(mez);
        final EditText eRfid = act.in("کد RFID (اختیاری)");
        eRfid.setText(rfid);
        box.addView(eCode); box.addView(act.space(6));
        box.addView(eName); box.addView(act.space(6));
        box.addView(eW); box.addView(act.space(6));
        box.addView(eMez); box.addView(act.space(6));
        box.addView(eRfid); box.addView(act.space(8));
        LinearLayout pr = act.h();
        photoLbl = act.tv(photo.length() > 0 ? "عکس: " + photo : "بدون عکس", U.SUB, 12, false);
        pr.addView(photoLbl, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        pr.addView(act.gbtn("انتخاب عکس", new Tap() {
            public void go() {
                Intent it = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                it.addCategory(Intent.CATEGORY_OPENABLE);
                it.setType("image/*");
                act.startActivityForResult(it, REQ_PHOTO);
            }
        }));
        box.addView(pr);
        box.addView(act.space(8));
        final android.app.AlertDialog d = act.sheet(box);
        LinearLayout br = act.h();
        br.setGravity(android.view.Gravity.CENTER);
        br.addView(act.btn(eid == 0 ? "ثبت اتیکت" : "ذخیره", new Tap() {
            public void go() {
                String cd = U.en(U.str(eCode));
                if (cd.length() == 0) { U.toast(act, "کد کار را بنویسید"); return; }
                ContentValues cv = new ContentValues();
                cv.put("code", cd);
                cv.put("name", U.str(eName));
                cv.put("wmw", U.parseMw(U.str(eW)));
                cv.put("mezane", U.str(eMez));
                cv.put("rfid", U.str(eRfid));
                cv.put("photo", pendingPhoto == null ? "" : pendingPhoto);
                cv.put("updated_ts", System.currentTimeMillis());
                if (eid == 0) {
                    cv.put("cts", System.currentTimeMillis());
                    act.db.ins("etiket", cv);
                } else {
                    act.db.w().update("etiket", cv, "id=?", new String[]{"" + eid});
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

    /** نتیجه انتخاب عکس — از onActivityResult هر دو اکتیویتی فراخوانی می‌شود */
    static boolean photoResult(A act, int req, int res, Intent data) {
        if (req != REQ_PHOTO) return false;
        if (res != Activity.RESULT_OK || data == null || data.getData() == null) return true;
        String f = copyPhoto(act, data.getData());
        if (f != null) {
            pendingPhoto = f;
            if (photoLbl != null) photoLbl.setText("عکس: " + f);
            U.toast(act, "عکس ذخیره شد ✓");
        }
        return true;
    }

    /** کپی عکس انتخابی به حافظهٔ داخلی و برگرداندن «نام فایل» */
    static String copyPhoto(Context act, Uri uri) {
        try {
            String name = null;
            Cursor c = act.getContentResolver().query(uri, null, null, null, null);
            if (c != null) {
                if (c.moveToFirst()) {
                    int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (i >= 0) name = c.getString(i);
                }
                c.close();
            }
            if (name == null || name.length() == 0) name = "photo.jpg";
            name = name.replaceAll("[^A-Za-z0-9_.\\-]", "_");
            if (name.indexOf('.') < 0) name = name + ".jpg";
            File dir = new File(act.getFilesDir(), "etiket");
            dir.mkdirs();
            File out = new File(dir, name);
            InputStream is = act.getContentResolver().openInputStream(uri);
            FileOutputStream os = new FileOutputStream(out);
            byte[] buf = new byte[8192];
            int r;
            while ((r = is.read(buf)) > 0) os.write(buf, 0, r);
            os.close();
            is.close();
            return name;
        } catch (Exception e) {
            return null;
        }
    }

    static File photoFile(Context c, String name) {
        return new File(new File(c.getFilesDir(), "etiket"), name);
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        photoResult(this, req, res, data);
    }
}

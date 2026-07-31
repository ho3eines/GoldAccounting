package com.talayar.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.File;
import java.io.FileOutputStream;

/** پروندهٔ اتیکت — مشخصات از شناسهٔ کار (با/بدون تصویر)، مزنه، RFID، کارت بارکد، اشتراک */
public class EtiketViewActivity extends A {
    private int eid;
    private String code = "", name = "", mez = "", rfid = "", photo = "";
    private int w = 0;
    private long updTs = 0, cts = 0;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        eid = getIntent().getIntExtra("id", 0);
        if (!load()) {
            U.toast(this, "اتیکت یافت نشد");
            finish();
            return;
        }
        build();
    }

    @Override protected void onResume() {
        super.onResume();
        if (body != null && load()) { body.removeAllViews(); build(); }
    }

    private boolean load() {
        Cursor c = db.r().rawQuery("SELECT code,name,wmw,mezane,rfid,photo,updated_ts,cts FROM etiket WHERE id=?",
                new String[]{"" + eid});
        boolean ok = c.moveToFirst();
        if (ok) {
            code = c.getString(0);
            name = c.getString(1) == null ? "" : c.getString(1);
            w = c.getInt(2);
            mez = c.getString(3) == null ? "" : c.getString(3);
            rfid = c.getString(4) == null ? "" : c.getString(4);
            photo = c.getString(5) == null ? "" : c.getString(5);
            updTs = c.getLong(6);
            cts = c.getLong(7);
        }
        c.close();
        return ok;
    }

    private void build() {
        scaffold("اتیکت " + U.dig(code), true);

        // ── هدر ──
        LinearLayout head = cardHi();
        head.addView(tv(name.length() > 0 ? name : "بدون نام", U.GOLD, 18, true));
        LinearLayout badges = h();
        badges.addView(badge("کد کار " + U.dig(code), true));
        if (photo.length() > 0) { badges.addView(wspace(6)); badges.addView(badge("دارای عکس", true)); }
        if (rfid.length() > 0) { badges.addView(wspace(6)); badges.addView(badge("RFID", true)); }
        if (updTs > cts + 60000) { badges.addView(wspace(6)); badges.addView(badge("به‌روزشده", true)); }
        head.addView(space(4));
        head.addView(badges);
        body.addView(head);

        // ── عکس ──
        if (photo.length() > 0) {
            File f = EtiketActivity.photoFile(this, photo);
            if (f.exists()) {
                LinearLayout pc = card();
                Bitmap bmp = decodeScaled(f.getAbsolutePath(), 900);
                if (bmp != null) {
                    ImageView iv = new ImageView(this);
                    iv.setImageBitmap(bmp);
                    iv.setAdjustViewBounds(true);
                    pc.addView(iv);
                }
                pc.addView(kv("فایل تصویر", photo, U.BLUE));
                addBtn(pc, gbtn("📤 اشتراک تصویر", new Tap() {
                    public void go() { shareFile("etiket/" + photo, "image/*", "اشتراک تصویر اتیکت " + U.dig(code)); }
                }));
                body.addView(pc);
            }
        }

        // ── مشخصات ──
        LinearLayout info = card();
        info.addView(tv("مشخصات اتیکت (بر اساس شناسهٔ کار)", U.GOLD, 14, true));
        info.addView(kv("شناسهٔ رکورد", U.intFa(eid)));
        info.addView(kv("کد کار", U.dig(code), U.GOLD));
        info.addView(kv("نام کار", name.length() > 0 ? name : "—"));
        info.addView(kv("وزن", U.gs(w) + " (" + U.mwG(w) + ")", U.TXT));
        info.addView(kv("مزنه", mez.length() > 0 ? mez : "—"));
        info.addView(kv("RFID", rfid.length() > 0 ? rfid : "—"));
        info.addView(kv("آخرین به‌روزرسانی", Jal.of(updTs).fa(), U.SUB));
        body.addView(info);

        // ── عملیات ──
        LinearLayout ops = card();
        LinearLayout r1 = h();
        r1.addView(btn("✎ به‌روزرسانی مزنه", new Tap() { public void go() { updMezane(); } }),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        r1.addView(wspace(8));
        r1.addView(gbtn("🏷 کارت اتیکت + بارکد", new Tap() { public void go() { showCard(); } }),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        ops.addView(r1);
        ops.addView(space(6));
        LinearLayout r2 = h();
        r2.addView(gbtn("📋 کپی مشخصات (بدون تصویر)", new Tap() { public void go() { copyInfo(); } }),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        r2.addView(wspace(8));
        r2.addView(gbtn("ویرایش ✎", new Tap() {
            public void go() {
                EtiketActivity.editDlg(EtiketViewActivity.this, eid, new Tap() { public void go() { } });
            }
        }), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        ops.addView(r2);
        ops.addView(space(6));
        LinearLayout r3 = h();
        r3.addView(dbtn("📡 حذف RFID (اتیکت خارج/فروخته‌شده)", new Tap() { public void go() { clearRfid(); } }),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        r3.addView(wspace(8));
        r3.addView(dbtn("🗑 حذف اتیکت", new Tap() { public void go() { del(); } }),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        ops.addView(r3);
        body.addView(ops);
    }

    private void updMezane() {
        input("به‌روزرسانی مزنهٔ اتیکت " + U.dig(code), "مزنه", mez, false, new OnText() {
            public void ok(String s) {
                ContentValues cv = new ContentValues();
                cv.put("mezane", s);
                cv.put("updated_ts", System.currentTimeMillis());
                db.w().update("etiket", cv, "id=?", new String[]{"" + eid});
                U.toast(EtiketViewActivity.this, "مزنه به‌روزرسانی شد ✓");
            }
        });
    }

    private void copyInfo() {
        String txt = "اتیکت طلایار\nکد کار: " + U.dig(code) + "\nنام کار: " + name +
                "\nوزن: " + U.gs(w) + "\nمزنه: " + (mez.length() > 0 ? mez : "—") +
                (rfid.length() > 0 ? "\nRFID: " + rfid : "") +
                "\nتاریخ: " + U.dig(Jal.today());
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("etiket", txt));
        U.toast(this, "مشخصات کپی شد ✓");
    }

    private void clearRfid() {
        if (rfid.length() == 0) { U.toast(this, "این اتیکت RFID ندارد"); return; }
        confirm("RFID این اتیکت حذف شود؟\n(برای اتیکت‌های خارج/فروخته‌شده استفاده می‌شود)", new Tap() {
            public void go() {
                ContentValues cv = new ContentValues();
                cv.put("rfid", "");
                cv.put("updated_ts", System.currentTimeMillis());
                db.w().update("etiket", cv, "id=?", new String[]{"" + eid});
                U.toast(EtiketViewActivity.this, "RFID حذف شد ✓");
            }
        });
    }

    private void del() {
        confirm("اتیکت «" + U.dig(code) + "» برای همیشه حذف شود؟", new Tap() {
            public void go() {
                db.w().delete("etiket", "id=?", new String[]{"" + eid});
                if (photo.length() > 0) EtiketActivity.photoFile(EtiketViewActivity.this, photo).delete();
                U.toast(EtiketViewActivity.this, "حذف شد");
                finish();
            }
        });
    }

    // ---------- کارت بارکد ----------
    private Bitmap makeCard() {
        int W = 1000, H = 640;
        Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new LinearGradient(0, 0, W, H, 0xFFF6D988, 0xFFC99327, Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, W, H, p);
        p.setShader(null);
        int m = 26;
        p.setColor(0xFFFCF9EF);
        cv.drawRoundRect(new RectF(m, m, W - m, H - m), 30, 30, p);

        Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
        t.setTextAlign(Paint.Align.CENTER);
        t.setTypeface(U.FB);
        t.setColor(0xFF6B5518);
        t.setTextSize(44);
        String shop = db.getS("shop", "");
        cv.drawText(shop.length() > 0 ? shop : "طلایار — نظام اتیکت طلا", W / 2, m + 66, t);

        t.setTextSize(42);
        t.setColor(0xFF23180A);
        cv.drawText(name.length() > 0 ? name : "—", W / 2, m + 132, t);

        t.setTypeface(U.FM);
        t.setTextSize(30);
        t.setColor(0xFF5B4A16);
        String line = "کد کار " + U.dig(code) + "   •   " + U.gs(w);
        cv.drawText(line, W / 2, m + 184, t);
        if (mez.length() > 0) cv.drawText("مزنه: " + mez, W / 2, m + 226, t);

        Bitmap bc = Barcode.code128(U.en(code), 3, 110);
        int bx = (W - bc.getWidth()) / 2;
        cv.drawBitmap(bc, bx, m + 260, null);
        t.setTextSize(30);
        t.setColor(0xFF000000);
        cv.drawText(U.dig(code), W / 2, m + 260 + 110 + 46, t);

        t.setTextSize(24);
        t.setColor(0xFF8F6A16);
        cv.drawText(U.dig(Jal.today()) + "   |   صادرشده توسط «طلایار»", W / 2, H - m - 26, t);
        return bmp;
    }

    private void showCard() {
        final Bitmap card = makeCard();
        LinearLayout box = v();
        box.setPadding(dp(8), dp(8), dp(8), dp(8));
        ImageView iv = new ImageView(this);
        iv.setImageBitmap(card);
        iv.setAdjustViewBounds(true);
        box.addView(iv);
        box.addView(space(6));
        final android.app.AlertDialog d = sheet(box);
        LinearLayout br = h();
        br.setGravity(android.view.Gravity.CENTER);
        br.addView(btn("📤 اشتراک کارت", new Tap() {
            public void go() {
                d.dismiss();
                shareBitmap(card);
            }
        }), new LinearLayout.LayoutParams(dp(140), ViewGroup.LayoutParams.WRAP_CONTENT));
        br.addView(wspace(10));
        br.addView(gbtn("بستن", new Tap() { public void go() { d.dismiss(); } }),
                new LinearLayout.LayoutParams(dp(110), ViewGroup.LayoutParams.WRAP_CONTENT));
        box.addView(br);
    }

    private void shareBitmap(Bitmap bmp) {
        try {
            File dir = new File(getFilesDir(), "share");
            dir.mkdirs();
            File f = new File(dir, "etiket_" + U.en(code) + ".png");
            FileOutputStream os = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
            os.close();
            shareFile("share/" + f.getName(), "image/png", "کارت اتیکت " + U.dig(code));
        } catch (Exception e) {
            msg("خطا", "ساخت فایل کارت ممکن نشد.");
        }
    }

    private void shareFile(String rel, String mime, String title) {
        try {
            Uri uri = ShareProvider.uriFor(this, rel);
            Intent it = new Intent(Intent.ACTION_SEND);
            it.setType(mime);
            it.putExtra(Intent.EXTRA_STREAM, uri);
            it.putExtra(Intent.EXTRA_SUBJECT, title);
            it.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(it, title));
        } catch (Exception e) {
            msg("خطا در اشتراک", e.getMessage() == null ? "" : e.getMessage());
        }
    }

    static Bitmap decodeScaled(String path, int maxW) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, o);
        int scale = 1;
        while (o.outWidth > 0 && o.outWidth / scale > maxW * 1.5f) scale *= 2;
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeFile(path, o2);
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        EtiketActivity.photoResult(this, req, res, data);
    }
}

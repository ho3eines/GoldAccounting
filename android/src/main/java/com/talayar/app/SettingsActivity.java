package com.talayar.app;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/** تنظیمات + API قیمت + پشتیبان‌گیری و بازیابی */
public class SettingsActivity extends A {
    static final String[] TABLES = {"settings","rates","customers","customer_tx","items","invoices",
            "invoice_lines","cash_tx","gold_tx","defs","docs","doc_rows","assets_ledger","banks",
            "bank_tx","checks","prices","etiket"};
    private static final int REQ_BACKUP = 11, REQ_RESTORE = 12;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        scaffold("تنظیمات", true);

        LinearLayout c1 = card();
        c1.addView(tv("اطلاعات فروشگاه", U.GOLD, 15, true));
        c1.addView(label("نام فروشگاه (در فاکتور نمایش داده می‌شود)"));
        final EditText eShop = in("مثلاً طلای شب‌پوش");
        eShop.setText(db.getS("shop", ""));
        c1.addView(eShop);
        c1.addView(label("تلفن فروشگاه"));
        final EditText eTel = in("اختیاری", true);
        eTel.setText(U.dig(db.getS("shopTel", "")));
        c1.addView(eTel);
        addBtn(c1, btn("ذخیره", new Tap() {
            public void go() {
                db.setS("shop", U.str(eShop));
                db.setS("shopTel", U.en(U.str(eTel)));
                U.toast(SettingsActivity.this, "ذخیره شد ✓");
            }
        }));
        body.addView(c1);

        LinearLayout c2 = card();
        c2.addView(tv("پارامترهای حسابداری", U.GOLD, 15, true));
        c2.addView(label("درصد مالیات بر ارزش افزوده (روی اجرت)"));
        final EditText eTax = in("", true);
        eTax.setText(U.dig(db.getS("tax", "10")));
        c2.addView(eTax);
        c2.addView(label("رسوب/ری‌گیری پیش‌فرض خرید طلای کارکرده (٪)"));
        final EditText eRes = in("", true);
        eRes.setText(U.dig(db.getS("resub", "0")));
        c2.addView(eRes);
        addBtn(c2, btn("ذخیره", new Tap() {
            public void go() {
                db.setS("tax", (long) U.parseDouble(U.str(eTax)) + "");
                db.setS("resub", (long) U.parseDouble(U.str(eRes)) + "");
                U.toast(SettingsActivity.this, "ذخیره شد ✓");
            }
        }));
        body.addView(c2);

        LinearLayout c2b = card();
        c2b.addView(tv("قیمت‌خوانی آنلاین (تنظیم پیشرفته API)", U.GOLD, 15, true));
        TextView apiHint = tv("برنامه هیچ سرور میانی ندارد. می‌توانید از بین APIهای پیش‌فرض انتخاب کنید یا آدرس سفارشی و نگاشت دلخواه را تنظیم کنید.", U.SUB, 12, false);
        apiHint.setLineSpacing(3, 1.2f);
        c2b.addView(apiHint);
        c2b.addView(space(4));

        c2b.addView(label("انتخاب منبع پیش‌فرض (API)"));
        final LinearLayout apiPresetBox = h();
        apiPresetBox.addView(gbtn("TGJU", new Tap() {
            public void go() {
                eApi.setText("https://api.tgju.org/v1/data/sana/json");
                eMap.setText("");
                db.setS("api_url", "https://api.tgju.org/v1/data/sana/json");
                db.setS("api_map", "");
                U.toast(SettingsActivity.this, "منبع TGJU انتخاب شد ✓");
            }
        }), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        apiPresetBox.addView(wspace(6));
        apiPresetBox.addView(gbtn("BrsApi", new Tap() {
            public void go() {
                eApi.setText("https://Api.BrsApi.ir/Market/Gold_Currency.php?key=BF9gAKuXX4XTksfXYdBFzaFDrQ2ahfvd");
                eMap.setText("gold,currency,cryptocurrency");
                db.setS("api_url", "https://Api.BrsApi.ir/Market/Gold_Currency.php?key=BF9gAKuXX4XTksfXYdBFzaFDrQ2ahfvd");
                db.setS("api_map", "gold,currency,cryptocurrency");
                U.toast(SettingsActivity.this, "منبع BrsApi انتخاب شد ✓");
            }
        }), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        c2b.addView(apiPresetBox);
        c2b.addView(space(6));

        c2b.addView(label("آدرس API قیمت‌ها"));
        final EditText eApi = in(PricesActivity.DEFAULT_API);
        eApi.setText(db.getS("api_url", PricesActivity.DEFAULT_API));
        c2b.addView(eApi);

        c2b.addView(label("مسیرهای آرایه یا دسته‌ها در JSON (با کاما جدا کنید، مثلاً: gold,currency,cryptocurrency یا خالی برای خودکار)"));
        final EditText eMap = in("خالی = تشخیص خودکار هوشمند");
        eMap.setText(db.getS("api_map", ""));
        c2b.addView(eMap);

        LinearLayout rApi = h();
        rApi.addView(btn("ذخیره تنظیمات API", new Tap() {
            public void go() {
                String u = U.str(eApi);
                if (u.length() == 0) u = PricesActivity.DEFAULT_API;
                String m = U.str(eMap);
                db.setS("api_url", u);
                db.setS("api_map", m);
                eApi.setText(u);
                U.toast(SettingsActivity.this, "ذخیره شد ✓");
            }
        }), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        rApi.addView(wspace(8));
        rApi.addView(gbtn("پیش‌فرض", new Tap() {
            public void go() {
                db.setS("api_url", PricesActivity.DEFAULT_API);
                db.setS("api_map", "");
                eApi.setText(PricesActivity.DEFAULT_API);
                eMap.setText("");
                U.toast(SettingsActivity.this, "به پیش‌فرض برگشت ✓");
            }
        }), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        c2b.addView(space(2));
        c2b.addView(rApi);
        body.addView(c2b);

        LinearLayout c3 = card();
        c3.addView(tv("پشتیبان‌گیری و بازیابی", U.GOLD, 15, true));
        c3.addView(tv("از همهٔ داده‌ها (فاکتورها، اسناد، مشتریان، اجناس، اتیکت‌ها، چک‌ها، بانک‌ها، نرخ‌ها و تراکنش‌ها) فایل بکاپ JSON ساخته می‌شود. بازیابی، داده‌های فعلی را جایگزین می‌کند.", U.SUB, 12, false));
        c3.addView(space(6));
        LinearLayout r = h();
        r.addView(btn("⬆ ایجاد فایل بکاپ", new Tap() { public void go() { doBackup(); } }),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        r.addView(wspace(8));
        r.addView(gbtn("⬇ بازیابی از فایل", new Tap() { public void go() { doRestore(); } }),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        c3.addView(r);
        body.addView(c3);

        LinearLayout c4 = card();
        c4.addView(tv("درباره", U.GOLD, 15, true));
        c4.addView(kv("نسخه", "۲٫۱"));
        c4.addView(kv("حالت", "آفلاین کامل + دریافت اختیاری قیمت مستقیم از API عمومی (بدون سرور میانی)"));
        c4.addView(kv("داده‌ها", "ذخیره محلی روی همین گوشی (SQLite)"));
        body.addView(c4);
    }

    private void doBackup() {
        try {
            JSONObject root = new JSONObject();
            root.put("app", "talayar-backup");
            root.put("version", 1);
            root.put("date", Jal.today());
            for (String t : TABLES) {
                JSONArray arr = new JSONArray();
                Cursor c = db.r().rawQuery("SELECT * FROM " + t, null);
                String[] cols = c.getColumnNames();
                while (c.moveToNext()) {
                    JSONObject o = new JSONObject();
                    for (String col : cols) {
                        if (c.isNull(c.getColumnIndex(col))) continue;
                        o.put(col, c.getString(c.getColumnIndex(col)));
                    }
                    arr.put(o);
                }
                c.close();
                root.put(t, arr);
            }
            String name = "talayar-backup-" + Jal.today().replace("/", "") + ".json";
            pendingBackup = root.toString();
            Intent it = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            it.addCategory(Intent.CATEGORY_OPENABLE);
            it.setType("application/json");
            it.putExtra(Intent.EXTRA_TITLE, name);
            startActivityForResult(it, REQ_BACKUP);
        } catch (Exception e) {
            msg("خطا", e.getMessage() == null ? "خطا" : e.getMessage());
        }
    }

    private String pendingBackup;

    private void doRestore() {
        confirm("بازیابی بکاپ، همهٔ داده‌های فعلی برنامه را پاک و جایگزین می‌کند. ادامه می‌دهید؟", new Tap() {
            public void go() {
                Intent it = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                it.addCategory(Intent.CATEGORY_OPENABLE);
                it.setType("*/*");
                startActivityForResult(it, REQ_RESTORE);
            }
        });
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (req == REQ_BACKUP) {
            try {
                OutputStream os = getContentResolver().openOutputStream(uri, "wt");
                if (os != null) {
                    os.write(pendingBackup.getBytes("UTF-8"));
                    os.flush();
                    os.close();
                    U.toast(this, "فایل بکاپ ذخیره شد ✓");
                }
            } catch (Exception e) {
                msg("خطا در ذخیره", e.getMessage() == null ? "خطا" : e.getMessage());
            }
        } else if (req == REQ_RESTORE) {
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                restoreFromJson(sb.toString());
                // بازخوانی سینگلتون دیتابیس لازم نیست—همان پایگاه است
                msg("بازیابی کامل شد ✓", "داده‌ها با موفقیت بازیابی شدند. برنامه را دوباره باز کنید.");
            } catch (Exception e) {
                msg("خطا در بازیابی", e.getMessage() == null ? "خطا" : e.getMessage());
            }
        }
    }

    private void restoreFromJson(String s) throws Exception {
        JSONObject root = new JSONObject(s);
        if (!"talayar-backup".equals(root.optString("app"))) throw new Exception("فایل بکاپ معتبر نیست");
        android.database.sqlite.SQLiteDatabase w = db.w();
        w.beginTransaction();
        try {
            for (String t : TABLES) w.delete(t, null, null);
            for (String t : TABLES) {
                JSONArray arr = root.getJSONArray(t);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    android.content.ContentValues cv = new android.content.ContentValues();
                    java.util.Iterator<String> it = o.keys();
                    while (it.hasNext()) {
                        String col = (String) it.next();
                        String v = o.getString(col);
                        cv.put(col, v);
                    }
                    w.insert(t, null, cv);
                }
            }
            w.setTransactionSuccessful();
        } finally {
            w.endTransaction();
        }
    }
}

package com.talayar.app;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;

/** دریافت قیمت‌ها از API عمومی — مستقیم از گوشی، بدون سرور میانی */
public class Net {

    public interface Done { void ok(String err); }

    /** کلیدهای استاندارد نرخ‌ها */
    public static final String[][] KEYS = {
            {"gold18", "گرم طلای ۱۸ عیار"},
            {"mesghal", "مثقال طلای ۱۷ عیار"},
            {"gold24", "گرم طلای ۲۴ عیار"},
            {"ons", "انس جهانی طلا (دلار)"},
            {"silver", "نقره"},
            {"coin_imami", "سکه امامی"},
            {"coin_bahar", "سکه بهار آزادی"},
            {"coin_nim", "نیم سکه"},
            {"coin_rob", "ربع سکه"},
            {"coin_gerami", "سکه گرمی"},
            {"usd", "دلار"},
            {"eur", "یورو"},
            {"aed", "درهم امارات"},
            {"try_", "لیر ترکیه"},
    };

    public static String keyName(String k) {
        for (String[] x : KEYS) if (x[0].equals(k)) return x[1];
        return k;
    }

    /** نگاشت کلیدهای TGJU به کلیدهای ما */
    static String mapTgju(String tg) {
        if (tg.equals("geram18")) return "gold18";
        if (tg.equals("mesghal")) return "mesghal";
        if (tg.equals("geram24")) return "gold24";
        if (tg.equals("ons") || tg.equals("ons_dollar")) return "ons";
        if (tg.equals("silver")) return "silver";
        if (tg.equals("sekee") || tg.equals("sekee_emami") || tg.equals("emami_sell")) return "coin_imami";
        if (tg.equals("sekeb")) return "coin_bahar";
        if (tg.equals("nim")) return "coin_nim";
        if (tg.equals("rob")) return "coin_rob";
        if (tg.equals("gerami")) return "coin_gerami";
        if (tg.equals("price_dollar_rl")) return "usd";
        if (tg.equals("price_eur")) return "eur";
        if (tg.equals("price_aed")) return "aed";
        if (tg.equals("price_try")) return "try_";
        return null;
    }

    /** تلاش برای استخراج قیمت عددی از یک گره JSON (TGJU: معمولاً فیلد p) */
    static long extract(JSONObject o) {
        String[] cand = {"p", "price", "latest", "value", "l"};
        for (String c : cand) {
            String s = o.optString(c, "");
            if (s.length() == 0) continue;
            try { return Math.round(Double.parseDouble(s.replace(",", ""))); } catch (Exception e) {}
        }
        return 0;
    }

    /** پارس بادقت JSON قیمت — بازگشت تعداد کلیدهای پیدا شده */
    public static int parsePrices(Db db, String json) throws Exception {
        int found = 0;
        JSONObject root = new JSONObject(json);
        // ساختار TGJU: {"data": {"geram18": {...}, ...}}
        JSONObject data = root.optJSONObject("data");
        if (data == null) data = root;
        Iterator<String> it = data.keys();
        while (it.hasNext()) {
            String k = (String) it.next();
            Object v = data.opt(k);
            if (!(v instanceof JSONObject)) continue;
            String mapped = mapTgju(k);
            long price = extract((JSONObject) v);
            if (mapped != null && price > 0) {
                db.priceSet(mapped, price);
                found++;
            }
        }
        return found;
    }

    /** دریافت قیمت‌ها در نخ پس‌زمینه */
    public static void fetchPrices(final Db db, final String url, final android.app.Activity act, final Done done) {
        new Thread(new Runnable() {
            public void run() {
                String err = null;
                int found = 0;
                HttpURLConnection con = null;
                try {
                    URL u = new URL(url);
                    con = (HttpURLConnection) u.openConnection();
                    con.setConnectTimeout(12000);
                    con.setReadTimeout(15000);
                    con.setRequestMethod("GET");
                    con.setRequestProperty("User-Agent", "Talayar-GoldAccounting/1.0");
                    con.setRequestProperty("Accept", "application/json,*/*");
                    int code = con.getResponseCode();
                    InputStream is = code >= 200 && code < 300 ? con.getInputStream() : con.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();
                    if (code < 200 || code >= 300) throw new Exception("کد پاسخ: " + code);
                    found = parsePrices(db, sb.toString());
                    if (found == 0) throw new Exception("قیمتی در پاسخ یافت نشد");
                } catch (Exception e) {
                    err = e.getMessage() == null ? "خطای شبکه" : e.getMessage();
                } finally {
                    if (con != null) con.disconnect();
                }
                final int f = found;
                final String e2 = err;
                final long g18 = db.priceGet("gold18");
                if (e2 == null && g18 > 0) {
                    // ثبت خودکار نرخ روز طلای ۱۸ در دفتر نرخ‌ها
                    android.content.ContentValues cv = new android.content.ContentValues();
                    cv.put("ts", System.currentTimeMillis());
                    cv.put("date_j", Jal.today());
                    cv.put("rate", g18);
                    db.ins("rates", cv);
                }
                act.runOnUiThread(new Runnable() {
                    public void run() {
                        done.ok(e2 != null ? e2 : ("دریافت شد — " + U.dig(f + "") + " قیمت به‌روزرسانی شد ✓"));
                    }
                });
            }
        }).start();
    }
}

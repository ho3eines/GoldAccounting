package com.talayar.app;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;

/** دریافت قیمت‌ها از API عمومی یا صفحات وب HTML — مستقیم از گوشی، بدون سرور میانی */
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

    /** نگاشت کلیدهای TGJU و BrsApi به کلیدهای ما */
    static String mapSymbol(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toLowerCase();
        // TGJU keys
        if (s.equals("geram18")) return "gold18";
        if (s.equals("mesghal")) return "mesghal";
        if (s.equals("geram24")) return "gold24";
        if (s.equals("ons") || s.equals("ons_dollar")) return "ons";
        if (s.equals("silver")) return "silver";
        if (s.equals("sekee") || s.equals("sekee_emami") || s.equals("emami_sell")) return "coin_imami";
        if (s.equals("sekeb")) return "coin_bahar";
        if (s.equals("nim")) return "coin_nim";
        if (s.equals("rob")) return "coin_rob";
        if (s.equals("gerami")) return "coin_gerami";
        if (s.equals("price_dollar_rl")) return "usd";
        if (s.equals("price_eur")) return "eur";
        if (s.equals("price_aed")) return "aed";
        if (s.equals("price_try")) return "try_";

        // BrsApi symbols
        if (s.equals("ir_gold_18k") || s.equals("gold_18k") || s.equals("18k")) return "gold18";
        if (s.equals("ir_gold_melted") || s.equals("gold_melted") || s.equals("melted")) return "mesghal";
        if (s.equals("ir_gold_24k") || s.equals("gold_24k") || s.equals("24k")) return "gold24";
        if (s.equals("xauusd") || s.equals("gold_ounce")) return "ons";
        if (s.equals("ir_coin_emami") || s.equals("coin_emami") || s.equals("emami")) return "coin_imami";
        if (s.equals("ir_coin_bahar") || s.equals("coin_bahar") || s.equals("bahar")) return "coin_bahar";
        if (s.equals("ir_coin_half") || s.equals("coin_half") || s.equals("nim")) return "coin_nim";
        if (s.equals("ir_coin_quarter") || s.equals("coin_quarter") || s.equals("rob")) return "coin_rob";
        if (s.equals("ir_coin_1g") || s.equals("coin_1g") || s.equals("gerami")) return "coin_gerami";
        if (s.equals("usd") || s.equals("us_dollar")) return "usd";
        if (s.equals("eur") || s.equals("euro")) return "eur";
        if (s.equals("aed") || s.equals("uae_dirham")) return "aed";
        if (s.equals("try") || s.equals("turkish_lira")) return "try_";
        if (s.equals("silver") || s.equals("ir_silver")) return "silver";

        return null;
    }

    /** تلاش برای استخراج قیمت عددی از یک گره JSON (TGJU/BrsApi) */
    static long extract(JSONObject o) {
        String[] cand = {"p", "price", "latest", "value", "l"};
        for (String c : cand) {
            String s = o.optString(c, "");
            if (s.length() == 0) continue;
            try { return Math.round(Double.parseDouble(s.replace(",", ""))); } catch (Exception e) {}
        }
        return 0;
    }

    /** پارس قیمت‌ها از صفحات HTML یا متن غیر JSON (مانند تابلوهای طلا) */
    public static int parseHtmlPrices(Db db, String html) {
        int found = 0;
        try {
            // حذف تگ‌های اسکریپت و استایل
            String clean = html.replaceAll("(?s)<script[^>]*>.*?</script>", " ")
                               .replaceAll("(?s)<style[^>]*>.*?</style>", " ");
            // حذف تمام تگ‌های HTML دیگر و جایگزینی با فاصله
            clean = clean.replaceAll("<[^>]*>", " ");
            clean = clean.replace("&nbsp;", " ")
                         .replace("&zwnj;", " ")
                         .replace("&amp;", "&")
                         .replace("&quot;", "\"");

            String[] lines = clean.split("\n|\\|");
            for (String line : lines) {
                String l = line.trim().toLowerCase();
                if (l.length() == 0) continue;

                String mapped = null;
                if ((l.contains("18") || l.contains("۱۸")) && (l.contains("طلا") || l.contains("عیار") || l.contains("geram") || l.contains("gram"))) {
                    mapped = "gold18";
                } else if (l.contains("مظنه") || l.contains("مثقال") || l.contains("melted")) {
                    mapped = "mesghal";
                } else if ((l.contains("24") || l.contains("۲۴")) && (l.contains("طلا") || l.contains("عیار"))) {
                    mapped = "gold24";
                } else if (l.contains("سکه امامی") || l.contains("امامی") || l.contains("emami")) {
                    mapped = "coin_imami";
                } else if (l.contains("بهار آزادی") || l.contains("طرح قدیم") || l.contains("sekeb")) {
                    mapped = "coin_bahar";
                } else if (l.contains("نیم سکه") || l.contains("نیم")) {
                    mapped = "coin_nim";
                } else if (l.contains("ربع سکه") || l.contains("ربع")) {
                    mapped = "coin_rob";
                } else if (l.contains("سکه گرمی") || l.contains("گرمی")) {
                    mapped = "coin_gerami";
                } else if (l.contains("دلار") || l.contains("usd") || l.contains("dollar")) {
                    mapped = "usd";
                } else if (l.contains("یورو") || l.contains("eur")) {
                    mapped = "eur";
                } else if (l.contains("درهم") || l.contains("aed")) {
                    mapped = "aed";
                } else if (l.contains("لیر") || l.contains("try")) {
                    mapped = "try_";
                } else if (l.contains("انس") || l.contains("ounce") || l.contains("xau")) {
                    mapped = "ons";
                } else if (l.contains("نقره") || l.contains("silver")) {
                    mapped = "silver";
                }

                if (mapped != null) {
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("[\\d,۰-۹٬]+").matcher(line);
                    while (m.find()) {
                        String numStr = m.group();
                        long val = U.parseMoney(numStr);
                        if (val > 0) {
                            if (mapped.equals("ons") && val >= 1000 && val <= 50000) {
                                db.priceSet(mapped, val);
                                found++;
                                break;
                            } else if (mapped.equals("usd") && val >= 10000 && val <= 1000000) {
                                db.priceSet(mapped, val);
                                found++;
                                break;
                            } else if ((mapped.equals("gold18") || mapped.equals("mesghal") || mapped.equals("coin_imami")) && val >= 500000) {
                                db.priceSet(mapped, val);
                                found++;
                                break;
                            } else if (val >= 10000) {
                                db.priceSet(mapped, val);
                                found++;
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {}
        return found;
    }

    /** پارس بادقت JSON قیمت — با قابلیت خواندن مسیرهای سفارشی یا تشخیص خودکار و پشتیبانی از HTML */
    public static int parsePrices(Db db, String json, String customMap) throws Exception {
        int found = 0;
        try {
            JSONObject root = new JSONObject(json);

            // اگر کاربر مسیرهای سفارشی مشخص کرده باشد (مثلاً: gold,currency,cryptocurrency یا data)
            if (customMap != null && customMap.trim().length() > 0) {
                String[] paths = customMap.split(",");
                for (String p : paths) {
                    String path = p.trim();
                    if (path.length() == 0) continue;
                    org.json.JSONArray arr = root.optJSONArray(path);
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            Object item = arr.opt(i);
                            if (item instanceof JSONObject) {
                                JSONObject obj = (JSONObject) item;
                                String sym = obj.optString("symbol", obj.optString("key", obj.optString("name_en", "")));
                                String mapped = mapSymbol(sym);
                                long price = extract(obj);
                                if (mapped != null && price > 0) {
                                    db.priceSet(mapped, price);
                                    found++;
                                }
                            }
                        }
                    } else {
                        JSONObject subObj = root.optJSONObject(path);
                        if (subObj != null) {
                            Iterator<String> it = subObj.keys();
                            while (it.hasNext()) {
                                String k = (String) it.next();
                                Object v = subObj.opt(k);
                                if (v instanceof JSONObject) {
                                    String mapped = mapSymbol(k);
                                    long price = extract((JSONObject) v);
                                    if (mapped != null && price > 0) {
                                        db.priceSet(mapped, price);
                                        found++;
                                    }
                                }
                            }
                        }
                    }
                }
                if (found > 0) return found;
            }

            // جستجوی خودکار پیش‌فرض در تمام دسته‌های آرایه‌ای رایج
            String[] categories = {"gold", "currency", "cryptocurrency", "bullion", "coin", "data_arr", "items", "rates", "market"};
            for (String cat : categories) {
                org.json.JSONArray arr = root.optJSONArray(cat);
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        Object item = arr.opt(i);
                        if (item instanceof JSONObject) {
                            JSONObject obj = (JSONObject) item;
                            String sym = obj.optString("symbol", obj.optString("key", obj.optString("name_en", "")));
                            String mapped = mapSymbol(sym);
                            long price = extract(obj);
                            if (mapped != null && price > 0) {
                                db.priceSet(mapped, price);
                                found++;
                            }
                        }
                    }
                }
            }

            // بررسی ساختار شیء کلید-مقدار (مانند TGJU: {"data": {...}} یا ریشه)
            JSONObject data = root.optJSONObject("data");
            if (data == null) data = root;
            Iterator<String> it = data.keys();
            while (it.hasNext()) {
                String k = (String) it.next();
                Object v = data.opt(k);
                if (!(v instanceof JSONObject)) continue;
                String mapped = mapSymbol(k);
                long price = extract((JSONObject) v);
                if (mapped != null && price > 0) {
                    db.priceSet(mapped, price);
                    found++;
                }
            }
        } catch (Exception e) {
            // اگر پاسخ JSON نبود یا خطا داد، تلاش برای پارس به عنوان صفحه HTML/متن
            return parseHtmlPrices(db, json);
        }

        if (found == 0) {
            found = parseHtmlPrices(db, json);
        }
        return found;
    }

    public static int parsePrices(Db db, String json) throws Exception {
        return parsePrices(db, json, null);
    }

    /** همگام‌سازی آفلاین اندروید با سرور ابری بلیزور همراه با کد یکتا (Idempotent) */
    public static void syncWithServer(final Db db, final String serverUrl, final android.app.Activity act, final Done done) {
        new Thread(new Runnable() {
            public void run() {
                String err = null;
                HttpURLConnection con = null;
                try {
                    JSONObject payload = new JSONObject();
                    String transferCode = "AND-SYNC-" + java.util.UUID.randomUUID().toString();
                    payload.put("TransferCode", transferCode);
                    payload.put("DeviceId", "Android-Device-" + android.os.Build.MODEL);

                    org.json.JSONArray invArray = new org.json.JSONArray();
                    android.database.Cursor c = db.r().rawQuery("SELECT * FROM invoices ORDER BY id DESC LIMIT 20", null);
                    String[] cols = c.getColumnNames();
                    while (c.moveToNext()) {
                        JSONObject o = new JSONObject();
                        for (String col : cols) {
                            if (c.isNull(c.getColumnIndex(col))) continue;
                            o.put(col, c.getString(c.getColumnIndex(col)));
                        }
                        if (!o.has("TransferCode") || o.optString("TransferCode").length() == 0) {
                            o.put("TransferCode", "INV-" + java.util.UUID.randomUUID().toString());
                        }
                        invArray.put(o);
                    }
                    c.close();
                    payload.put("Invoices", invArray);

                    URL u = new URL(serverUrl.endsWith("/") ? serverUrl + "api/sync/push" : serverUrl + "/api/sync/push");
                    con = (HttpURLConnection) u.openConnection();
                    con.setConnectTimeout(15000);
                    con.setReadTimeout(15000);
                    con.setRequestMethod("POST");
                    con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    con.setRequestProperty("Accept", "application/json");
                    con.setDoOutput(true);

                    java.io.OutputStream os = con.getOutputStream();
                    os.write(payload.toString().getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    int code = con.getResponseCode();
                    InputStream is = code >= 200 && code < 300 ? con.getInputStream() : con.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();

                    if (code < 200 || code >= 300) throw new Exception("کد پاسخ سرور: " + code + " - " + sb.toString());

                } catch (Exception e) {
                    err = e.getMessage() == null ? "خطای همگام‌سازی" : e.getMessage();
                } finally {
                    if (con != null) con.disconnect();
                }

                final String e2 = err;
                act.runOnUiThread(new Runnable() {
                    public void run() {
                        done.ok(e2);
                    }
                });
            }
        }).start();
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
                    con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                    con.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/json,*/*");
                    int code = con.getResponseCode();
                    InputStream is = code >= 200 && code < 300 ? con.getInputStream() : con.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();
                    if (code < 200 || code >= 300) throw new Exception("کد پاسخ: " + code);
                    String customMap = db.getS("api_map", "");
                    found = parsePrices(db, sb.toString(), customMap);
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

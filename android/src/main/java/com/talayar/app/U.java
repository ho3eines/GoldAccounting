package com.talayar.app;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.widget.Toast;

/** کمکی‌های عمومی: رنگ‌ها، فونت، اعداد فارسی، تبدیل‌ها */
public final class U {
    private U() {}

    // ---------- colors ----------
    public static final int BG        = 0xFF0B0F16;
    public static final int BG2       = 0xFF10161F;
    public static final int CARD      = 0xFF141B26;
    public static final int CARD_HI   = 0xFF1B2331;
    public static final int STROKE    = 0xFF28323F;
    public static final int GOLD      = 0xFFF1C24A;
    public static final int GOLD2     = 0xFFC99327;
    public static final int GOLD_DEEP = 0xFF8F6A16;
    public static final int TXT       = 0xFFF2EFE6;
    public static final int SUB       = 0xFF9AA7B6;
    public static final int OK        = 0xFF4CC779;
    public static final int BAD       = 0xFFE4525F;
    public static final int BLUE      = 0xFF5AC8FA;
    public static final int VIOLET    = 0xFFB39DDB;

    // ---------- fonts ----------
    public static Typeface F, FM, FB, FSB;

    public static void initFonts(Context c) {
        if (F != null) return;
        try {
            AssetManager am = c.getApplicationContext().getAssets();
            F   = Typeface.createFromAsset(am, "fonts/vazir.ttf");
            FM  = Typeface.createFromAsset(am, "fonts/vazirmed.ttf");
            FSB = Typeface.createFromAsset(am, "fonts/vazirsb.ttf");
            FB  = Typeface.createFromAsset(am, "fonts/vazirbold.ttf");
        } catch (Throwable t) {
            F = FM = FSB = Typeface.DEFAULT;
            FB = Typeface.DEFAULT_BOLD;
        }
    }

    // ---------- units ----------
    public static int dp(Context c, float v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    // ---------- digits ----------
    static final char[] FA = {'۰','۱','۲','۳','۴','۵','۶','۷','۸','۹'};

    public static String dig(String s) {
        if (s == null) return "۰";
        char[] cs = s.toCharArray();
        for (int i = 0; i < cs.length; i++) {
            if (cs[i] >= '0' && cs[i] <= '9') cs[i] = FA[cs[i] - '0'];
        }
        return new String(cs);
    }

    /** تبدیل ارقام فارسی/عربی به انگلیسی و حذف جداکننده‌ها */
    public static String en(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= '۰' && ch <= '۹') b.append((char) ('0' + (ch - '۰')));
            else if (ch >= '٠' && ch <= '٩') b.append((char) ('0' + (ch - '٠')));
            else if (ch == '٬' || ch == ',' || ch == ' ' || ch == '‌') { /* skip */ }
            else if (ch == '٫' || ch == '/') b.append('.');
            else b.append(ch);
        }
        return b.toString();
    }

    // ---------- money (تومان، بدون اعشار) ----------
    public static String money(long v) {
        boolean neg = v < 0;
        if (neg) v = -v;
        String s = Long.toString(v);
        StringBuilder b = new StringBuilder();
        int n = s.length();
        for (int i = 0; i < n; i++) {
            if (i > 0 && (n - i) % 3 == 0) b.append('٬');
            b.append(FA[s.charAt(i) - '0']);
        }
        return (neg ? "-" : "") + b.toString();
    }
    public static String moneyT(long v) { return money(v) + " تومان"; }

    /** پارس مبلغ تومان از ورودی کاربر */
    public static long parseMoney(String s) {
        s = en(s).replace(".", "").trim();
        if (s.length() == 0) return 0;
        try { return Long.parseLong(s); } catch (Exception e) { return 0; }
    }

    // ---------- weight (گرم، دقت میلی‌گرم؛ ذخیره به صورت int میلی‌گرم) ----------
    public static String mw(int wmg) {
        boolean neg = wmg < 0;
        if (neg) wmg = -wmg;
        int g = wmg / 1000, f = wmg % 1000;
        String out = intFa(g);
        if (f > 0) {
            String fr = (f < 100 ? "0" : "") + (f < 10 ? "0" : "") + f;
            while (fr.endsWith("0")) fr = fr.substring(0, fr.length() - 1);
            out += "٫" + fr;
        }
        return (neg ? "-" : "") + out;
    }
    public static String mwG(int wmg) { return mw(wmg) + " گرم"; }

    static String intFa(long v) {
        boolean neg = v < 0; if (neg) v = -v;
        String s = Long.toString(v);
        StringBuilder b = new StringBuilder();
        int n = s.length();
        for (int i = 0; i < n; i++) {
            if (i > 0 && (n - i) % 3 == 0) b.append('٬');
            b.append(FA[s.charAt(i) - '0']);
        }
        return (neg ? "-" : "") + b.toString();
    }

    /** پارس وزن گرم از ورودی کاربر (اعشار تا ۳ رقم) → میلی‌گرم */
    public static int parseMw(String s) {
        s = en(s).trim();
        if (s.length() == 0) return 0;
        try {
            double d = Double.parseDouble(s);
            return (int) Math.round(d * 1000.0);
        } catch (Exception e) { return 0; }
    }

    // ---------- percent ----------
    public static String pct(int v) { return dig(v + "") + "٪"; }

    public static double parseDouble(String s) {
        s = en(s).trim();
        if (s.length() == 0) return 0;
        try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
    }

    // ---------- karat ----------
    public static final int[] KARATS = {1000, 916, 875, 750, 585, 375};
    public static String karatName(int k) {
        switch (k) {
            case 1000: return "۲۴ عیار";
            case 916:  return "۲۲ عیار";
            case 875:  return "۲۱ عیار";
            case 750:  return "۱۸ عیار";
            case 585:  return "۱۴ عیار";
            case 375:  return "۹ عیار";
        }
        return dig(k + "/1000");
    }

    /** وزن معادل عیار ۱۸ (۷۵۰) به میلی‌گرم */
    public static long equiv750(long wmg, int karat) {
        return Math.round((double) wmg * karat / 750.0);
    }

    public static void toast(Context c, String m) {
        Toast t = Toast.makeText(c, m, Toast.LENGTH_SHORT);
        try { if (t.getView() instanceof android.view.ViewGroup) { } } catch (Throwable ignored) {}
        t.show();
    }

    // ---------- گرم و سوت ----------
    /** نمایش وزن به صورت «۱۲ گرم و ۳۵۰ سوت»؛ سوت = هزارم گرم */
    public static String gs(int wmg) {
        long w = wmg;
        boolean neg = w < 0;
        if (neg) w = -w;
        long g = w / 1000, s = w % 1000;
        String out;
        if (g == 0) out = s == 0 ? "۰ گرم" : intFa(s) + " سوت";
        else if (s == 0) out = intFa(g) + " گرم";
        else out = intFa(g) + " گرم و " + intFa(s) + " سوت";
        return neg ? "-" + out : out;
    }

    /** نمایش مبلغ به ریال */
    public static String rial(long toman) { return money(toman * 10) + " ریال"; }

    /** نمایش مبلغ دوتایی: تومان (ریال) — مخصوص ترازها */
    public static String moneyR(long toman) { return money(toman) + " تومان (" + money(toman * 10) + " ریال)"; }

    /** ترکیب امن چند بخش برای نمایش */
    public static String join(String sep, String... parts) {
        StringBuilder b = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.length() == 0) continue;
            if (b.length() > 0) b.append(sep);
            b.append(p);
        }
        return b.toString();
    }

    public static String str(android.widget.EditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }
}

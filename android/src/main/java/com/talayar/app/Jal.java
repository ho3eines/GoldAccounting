package com.talayar.app;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/** تقویم جلالی – تبدیل میلادی↔جلالی (الگوریتم کلاسیک jdf) */
public final class Jal {
    public int y, m, d;
    public Jal(int y, int m, int d) { this.y = y; this.m = m; this.d = d; }

    public String str() { return y + "/" + (m < 10 ? "0" + m : "" + m) + "/" + (d < 10 ? "0" + d : "" + d); }
    public String fa()  { return U.dig(str()); }

    static final String[] MONTHS = {"فروردین","اردیبهشت","خرداد","تیر","مرداد","شهریور","مهر","آبان","آذر","دی","بهمن","اسفند"};
    public String longFa() { return U.dig(d + "") + " " + MONTHS[m - 1] + " " + U.dig(y + ""); }
    static final String[] DOW = {"یکشنبه","دوشنبه","سه‌شنبه","چهارشنبه","پنجشنبه","جمعه","شنبه"};

    public static Jal g2j(int gy, int gm, int gd) {
        int[] gdm = {0,31,59,90,120,151,181,212,243,273,304,334};
        int jy;
        int gy2;
        if (gy > 1600) { jy = 979; gy -= 1600; } else { jy = 0; gy -= 621; }
        gy2 = (gm > 2) ? (gy + 1) : gy;
        long days = (365L * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) - 80
                + gd + gdm[gm - 1];
        jy += 33 * (int) (days / 12053); days %= 12053;
        jy += 4 * (int) (days / 1461); days %= 1461;
        if (days > 365) { jy += (int) ((days - 1) / 365); days = (days - 1) % 365; }
        int jm, jd;
        if (days < 186) { jm = 1 + (int) (days / 31); jd = 1 + (int) (days % 31); }
        else { jm = 7 + (int) ((days - 186) / 30); jd = 1 + (int) ((days - 186) % 30); }
        return new Jal(jy, jm, jd);
    }

    public static int[] j2g(int jy0, int jm, int jd0) {
        int jy = jy0;
        int gy;
        if (jy > 979) { gy = 1600; jy -= 979; } else { gy = 621; }
        long days = (365L * jy) + (jy / 33) * 8 + ((jy % 33) + 3) / 4 + 78 + jd0
                + (jm < 7 ? (jm - 1) * 31 : (jm - 7) * 30 + 186);
        gy += 400 * (int) (days / 146097); days %= 146097;
        if (days >= 36525) {
            days--;
            gy += 100 * (int) (days / 36524); days %= 36524;
            if (days >= 365) days++;
        }
        gy += 4 * (int) (days / 1461); days %= 1461;
        int gd;
        if (days >= 366) { gy += (int) ((days - 1) / 365); days = (days - 1) % 365; }
        gd = (int) days + 1;
        boolean leap = (gy % 4 == 0 && gy % 100 != 0) || gy % 400 == 0;
        int[] sf = {0,31, leap ? 29 : 28,31,30,31,30,31,31,30,31,30,31};
        int gm = 0;
        for (int i = 1; i <= 12; i++) {
            if (gd <= sf[i]) { gm = i; break; }
            gd -= sf[i];
        }
        return new int[]{gy, gm, gd};
    }

    public static Jal of(long ts) {
        Calendar c = Calendar.getInstance(TimeZone.getDefault(), Locale.US);
        c.setTimeInMillis(ts);
        return g2j(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    public static String today()  { return of(System.currentTimeMillis()).str(); }
    public static Jal   now()     { return of(System.currentTimeMillis()); }

    public static String longToday() {
        long ts = System.currentTimeMillis();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        int dow = c.get(Calendar.DAY_OF_WEEK); // 1=Sunday
        return DOW[dow - 1] + " " + of(ts).longFa();
    }

    /** پیشوند ماه «۱۴۰۲/۰۵» برای مقایسه */
    public static String monthPrefix(String dateJ) {
        return dateJ != null && dateJ.length() >= 7 ? dateJ.substring(0, 7) : dateJ;
    }
    public static String thisMonth() { return monthPrefix(today()); }

    /** هفتهٔ جاری: ۶ روز گذشته تا امروز */
    public static boolean inLast7(String dateJ, long tsOfDate) {
        long now = System.currentTimeMillis();
        return now - tsOfDate <= 7L * 24 * 3600 * 1000;
    }
}

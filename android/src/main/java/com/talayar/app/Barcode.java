package com.talayar.app;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

/** بارکد Code-128B — رندر مستقیم روی Bitmap بدون هیچ کتابخانهٔ خارجی */
public final class Barcode {
    private Barcode() {}

    /** جدول استاندارد Code-128 (۱۰۷ الگو؛ آخری Stop با ۷ رقم) */
    static final String[] P = {
            "212222","222122","222221","121223","121322","131222","122213","122312","132212","221213",
            "221312","231212","112232","122132","122231","113222","123122","123221","223211","221132",
            "221231","213212","223112","312131","311222","321122","321221","312212","322112","322211",
            "212123","212321","232121","111323","131123","131321","112313","132113","132311","211313",
            "231113","231311","112133","112331","132131","113123","113321","133121","313121","211331",
            "231131","213113","213311","213131","311123","311321","331121","312113","312311","332111",
            "314111","221411","431111","111224","111422","121124","121421","141122","141221","112214",
            "112412","122114","122411","142112","142211","241211","221114","413111","241112","134111",
            "111242","121142","121241","114212","124112","124211","411212","421112","421211","212141",
            "214121","412121","111143","111341","131141","114113","114311","411113","411311","113141",
            "114131","311141","411131","211412","211214","211232","2331112"
    };

    /** تولید بیت‌مپ بارکد Code-128B برای متن ASCII */
    public static Bitmap code128(String text, int barW, int height) {
        StringBuilder sb = new StringBuilder();
        if (text != null) {
            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
                sb.append(ch >= 32 && ch <= 126 ? ch : '-');
            }
        }
        if (sb.length() == 0) sb.append('-');
        String t = sb.toString();
        int n = t.length();

        int[] vals = new int[n + 3];
        vals[0] = 104; // Start B
        int sum = 104;
        for (int i = 0; i < n; i++) {
            int v = t.charAt(i) - 32;
            vals[i + 1] = v;
            sum += (i + 1) * v;
        }
        vals[n + 1] = sum % 103; // checksum
        vals[n + 2] = 106;       // Stop

        if (barW < 1) barW = 1;
        int modules = 0;
        for (int i = 0; i < vals.length; i++) modules += P[vals[i]].length();
        int quiet = 10;
        int w = (modules + 2 * quiet) * barW;

        Bitmap bmp = Bitmap.createBitmap(w, height, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        Paint p = new Paint();
        p.setColor(0xFFFFFFFF);
        cv.drawRect(0, 0, w, height, p);
        p.setColor(0xFF000000);
        int x = quiet * barW;
        for (int i = 0; i < vals.length; i++) {
            String pat = P[vals[i]];
            for (int j = 0; j < pat.length(); j++) {
                int ww = (pat.charAt(j) - '0') * barW;
                if (j % 2 == 0) cv.drawRect(x, 0, x + ww, height, p);
                x += ww;
            }
        }
        return bmp;
    }
}

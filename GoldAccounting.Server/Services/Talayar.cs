using System.Globalization;
using System.Text;

namespace GoldAccounting.Server.Services
{
    /// <summary>کمکی‌های عمومی: تقویم جلالی، اعداد فارسی، وزن/گرم و سوت، عیار — پورت مستقیم Jal.java و U.java اندروید</summary>
    public static class Talayar
    {
        // ---------- ارقام فارسی ----------
        static readonly char[] FA = { '۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹' };

        public static string Dig(string s)
        {
            if (s == null) return "۰";
            var cs = s.ToCharArray();
            for (int i = 0; i < cs.Length; i++)
                if (cs[i] >= '0' && cs[i] <= '9') cs[i] = FA[cs[i] - '0'];
            return new string(cs);
        }

        public static string Dig(long v) => Dig(v.ToString());

        /// <summary>تبدیل ارقام فارسی/عربی به انگلیسی و حذف جداکننده‌ها</summary>
        public static string En(string s)
        {
            if (s == null) return "";
            var b = new StringBuilder();
            foreach (char ch in s)
            {
                if (ch >= '۰' && ch <= '۹') b.Append((char)('0' + (ch - '۰')));
                else if (ch >= '٠' && ch <= '٩') b.Append((char)('0' + (ch - '٠')));
                else if (ch == '٬' || ch == ',' || ch == ' ' || ch == '\u200c') { /* skip */ }
                else if (ch == '٫' || ch == '/') b.Append('.');
                else b.Append(ch);
            }
            return b.ToString();
        }

        static string IntFa(long v)
        {
            bool neg = v < 0; if (neg) v = -v;
            string s = v.ToString();
            var b = new StringBuilder();
            int n = s.Length;
            for (int i = 0; i < n; i++)
            {
                if (i > 0 && (n - i) % 3 == 0) b.Append('٬');
                b.Append(FA[s[i] - '0']);
            }
            return (neg ? "-" : "") + b.ToString();
        }

        /// <summary>مبلغ به تومان با جداکننده و ارقام فارسی</summary>
        public static string Money(long v) => IntFa(v);
        public static string MoneyT(long v) => Money(v) + " تومان";

        /// <summary>پارس مبلغ تومان از ورودی کاربر</summary>
        public static long ParseMoney(string s)
        {
            s = En(s).Replace(".", "").Trim();
            if (s.Length == 0) return 0;
            return long.TryParse(s, out long v) ? v : 0;
        }

        /// <summary>نمایش وزن میلی‌گرمی به گرم با اعشار (ارقام فارسی)</summary>
        public static string Mw(long wmg)
        {
            bool neg = wmg < 0; if (neg) wmg = -wmg;
            long g = wmg / 1000, f = wmg % 1000;
            string out_ = IntFa(g);
            if (f > 0)
            {
                string fr = (f < 100 ? "0" : "") + (f < 10 ? "0" : "") + f;
                while (fr.EndsWith("0")) fr = fr.Substring(0, fr.Length - 1);
                out_ += "٫" + fr;
            }
            return (neg ? "-" : "") + out_;
        }
        public static string MwG(long wmg) => Mw(wmg) + " گرم";

        /// <summary>پارس وزن گرم (اعشار تا ۳ رقم) به میلی‌گرم</summary>
        public static long ParseMw(string s)
        {
            s = En(s).Trim();
            if (s.Length == 0) return 0;
            if (double.TryParse(s, NumberStyles.Float, CultureInfo.InvariantCulture, out double d))
                return (long)Math.Round(d * 1000.0);
            return 0;
        }

        public static double ParseDouble(string s)
        {
            s = En(s).Trim();
            if (s.Length == 0) return 0;
            return double.TryParse(s, NumberStyles.Float, CultureInfo.InvariantCulture, out double d) ? d : 0;
        }

        public static string Pct(long v) => Dig(v.ToString()) + "٪";

        // ---------- عیار ----------
        public static readonly int[] KARATS = { 1000, 916, 875, 750, 585, 375 };
        public static readonly string[] KARAT_LABELS = { "۲۴", "۲۲", "۲۱", "۱۸", "۱۴", "۹" };

        public static string KaratName(int k)
        {
            switch (k)
            {
                case 1000: return "۲۴ عیار";
                case 916: return "۲۲ عیار";
                case 875: return "۲۱ عیار";
                case 750: return "۱۸ عیار";
                case 585: return "۱۴ عیار";
                case 375: return "۹ عیار";
            }
            return Dig(k.ToString()) + "/1000";
        }

        /// <summary>وزن معادل عیار ۱۸ (۷۵۰) به میلی‌گرم</summary>
        public static long Equiv750(long wmg, int karat) => (long)Math.Round((double)wmg * karat / 750.0);

        // ---------- گرم و سوت ----------
        /// <summary>نمایش وزن به صورت «۱۲ گرم و ۳۵۰ سوت»؛ سوت = هزارم گرم</summary>
        public static string Gs(long wmg)
        {
            long w = wmg;
            bool neg = w < 0; if (neg) w = -w;
            long g = w / 1000, s = w % 1000;
            string out_;
            if (g == 0) out_ = s == 0 ? "۰ گرم" : IntFa(s) + " سوت";
            else if (s == 0) out_ = IntFa(g) + " گرم";
            else out_ = IntFa(g) + " گرم و " + IntFa(s) + " سوت";
            return neg ? "-" + out_ : out_;
        }

        public static string Rial(long toman) => Money(toman * 10) + " ریال";
        public static string MoneyR(long toman) => Money(toman) + " تومان (" + Money(toman * 10) + " ریال)";

        public static string Join(string sep, params string[] parts)
        {
            var b = new StringBuilder();
            foreach (var p in parts)
            {
                if (string.IsNullOrEmpty(p)) continue;
                if (b.Length > 0) b.Append(sep);
                b.Append(p);
            }
            return b.ToString();
        }

        // ---------- تقویم جلالی (پورت Jal.java) ----------
        public class Jal
        {
            public int Y, M, D;
            public Jal(int y, int m, int d) { Y = y; M = m; D = d; }

            public string Str() => Y + "/" + (M < 10 ? "0" : "") + M + "/" + (D < 10 ? "0" : "") + D;
            public string Fa() => Dig(Str());

            static readonly string[] MONTHS = { "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند" };
            public string LongFa() => Dig(D.ToString()) + " " + MONTHS[M - 1] + " " + Dig(Y.ToString());
            static readonly string[] DOW = { "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه", "شنبه" };

            public static Jal G2J(int gy, int gm, int gd)
            {
                int[] gdm = { 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334 };
                int jy;
                if (gy > 1600) { jy = 979; gy -= 1600; } else { jy = 0; gy -= 621; }
                int gy2 = (gm > 2) ? (gy + 1) : gy;
                long days = (365L * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) - 80
                            + gd + gdm[gm - 1];
                jy += 33 * (int)(days / 12053); days %= 12053;
                jy += 4 * (int)(days / 1461); days %= 1461;
                if (days > 365) { jy += (int)((days - 1) / 365); days = (days - 1) % 365; }
                int jm, jd;
                if (days < 186) { jm = 1 + (int)(days / 31); jd = 1 + (int)(days % 31); }
                else { jm = 7 + (int)((days - 186) / 30); jd = 1 + (int)((days - 186) % 30); }
                return new Jal(jy, jm, jd);
            }

            public static int[] J2G(int jy0, int jm, int jd0)
            {
                int jy = jy0;
                int gy;
                if (jy > 979) { gy = 1600; jy -= 979; } else { gy = 621; }
                long days = (365L * jy) + (jy / 33) * 8 + ((jy % 33) + 3) / 4 + 78 + jd0
                            + (jm < 7 ? (jm - 1) * 31 : (jm - 7) * 30 + 186);
                gy += 400 * (int)(days / 146097); days %= 146097;
                if (days >= 36525)
                {
                    days--;
                    gy += 100 * (int)(days / 36524); days %= 36524;
                    if (days >= 365) days++;
                }
                gy += 4 * (int)(days / 1461); days %= 1461;
                int gd;
                if (days >= 366) { gy += (int)((days - 1) / 365); days = (days - 1) % 365; }
                gd = (int)days + 1;
                bool leap = (gy % 4 == 0 && gy % 100 != 0) || gy % 400 == 0;
                int[] sf = { 0, 31, leap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
                int gm = 0;
                for (int i = 1; i <= 12; i++)
                {
                    if (gd <= sf[i]) { gm = i; break; }
                    gd -= sf[i];
                }
                return new[] { gy, gm, gd };
            }

            public static Jal Of(long ts)
            {
                var dt = DateTimeOffset.FromUnixTimeMilliseconds(ts).ToLocalTime();
                return G2J(dt.Year, dt.Month, dt.Day);
            }

            public static Jal Now() => Of(DateTimeOffset.Now.ToUnixTimeMilliseconds());
            public static string Today() => Now().Str();
            public static string LongToday()
            {
                var dt = DateTimeOffset.Now;
                int dow = (int)dt.DayOfWeek; // 0=Sunday
                return DOW[dow] + " " + Of(dt.ToUnixTimeMilliseconds()).LongFa();
            }

            public static string MonthPrefix(string? dateJ) =>
                dateJ != null && dateJ.Length >= 7 ? dateJ.Substring(0, 7) : dateJ;
            public static string ThisMonth() => MonthPrefix(Today());

            /// <summary>اعتبارسنجی تاریخ شمسی</summary>
            public static bool Valid(string d)
            {
                d = En(d).Trim();
                if (d.Length < 10) return false;
                var parts = d.Split('/');
                if (parts.Length != 3) return false;
                if (!int.TryParse(parts[0], out int y) || !int.TryParse(parts[1], out int m) || !int.TryParse(parts[2], out int dd)) return false;
                return y >= 1300 && m >= 1 && m <= 12 && dd >= 1 && dd <= 31;
            }
        }

        // ---------- بارکد Code-128B (پورت Barcode.java → SVG) ----------
        static readonly string[] C128 = {
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

        /// <summary>تولید SVG بارکد Code-128B برای متن</summary>
        public static string BarcodeSvg(string text, int barW = 2, int height = 60)
        {
            var sb = new StringBuilder();
            foreach (char ch in text ?? "")
                sb.Append(ch >= 32 && ch <= 126 ? ch : '-');
            if (sb.Length == 0) sb.Append('-');
            string t = sb.ToString();
            int n = t.Length;

            int[] vals = new int[n + 3];
            vals[0] = 104; // Start B
            int sum = 104;
            for (int i = 0; i < n; i++)
            {
                int v = t[i] - 32;
                vals[i + 1] = v;
                sum += (i + 1) * v;
            }
            vals[n + 1] = sum % 103; // checksum
            vals[n + 2] = 106;       // Stop

            if (barW < 1) barW = 1;
            int modules = 0;
            foreach (int v in vals) modules += C128[v].Length;
            int quiet = 10;
            int width = (modules + 2 * quiet) * barW;

            var svg = new StringBuilder();
            svg.Append($"<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"{width}\" height=\"{height}\" viewBox=\"0 0 {width} {height}\">");
            svg.Append($"<rect width=\"{width}\" height=\"{height}\" fill=\"white\"/>");
            int x = quiet * barW;
            for (int i = 0; i < vals.Length; i++)
            {
                string pat = C128[vals[i]];
                bool black = true;
                foreach (char c in pat)
                {
                    int w = (c - '0') * barW;
                    if (black) svg.Append($"<rect x=\"{x}\" y=\"0\" width=\"{w}\" height=\"{height}\" fill=\"black\"/>");
                    x += w;
                    black = !black;
                }
            }
            svg.Append("</svg>");
            return svg.ToString();
        }
    }
}

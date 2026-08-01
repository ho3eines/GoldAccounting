using System.Text;
using GoldAccounting.Server.Data;

namespace GoldAccounting.Server.Services
{
    /// <summary>
    /// تولید گزارش‌های HTML خودکفا (آفلاین): فایل دانلودشده بدون نیاز به سرور/اینترنت
    /// در مرورگر باز می‌شود و قابلیت چاپ و اشتراک دارد.
    /// </summary>
    public static class ReportHtml
    {
        const string CSS = @"
body{font-family:Tahoma,'Segoe UI',sans-serif;direction:rtl;background:#f6f5f2;margin:0;padding:20px;color:#1f2937}
.wrap{max-width:900px;margin:auto;background:#fff;border-radius:16px;box-shadow:0 4px 20px rgba(0,0,0,.08);padding:24px}
h1{color:#8F6A16;font-size:20px;margin:0 0 4px}
.meta{color:#6b7280;font-size:12px;margin-bottom:16px}
table{width:100%;border-collapse:collapse;font-size:13px}
th{background:#141B26;color:#F1C24A;padding:8px 10px;text-align:right}
td{border-bottom:1px solid #e5e7eb;padding:8px 10px}
tr:nth-child(even) td{background:#faf9f6}
.sum{background:#fdf6e3;border:1px solid #F1C24A;border-radius:12px;padding:12px 16px;margin-top:16px;font-weight:bold}
.ok{color:#15803d}.bad{color:#b91c1c}
@media print{body{background:#fff;padding:0}.wrap{box-shadow:none;padding:0}}
";

        public static string Shell(string title, string body, string meta)
        {
            return $"<!DOCTYPE html><html lang='fa' dir='rtl'><head><meta charset='utf-8'>" +
                   $"<meta name='viewport' content='width=device-width,initial-scale=1'>" +
                   $"<title>{title}</title><style>{CSS}</style></head><body><div class='wrap'>" +
                   $"<h1>🪙 {title}</h1><div class='meta'>{meta}</div>{body}</div></body></html>";
        }

        static string Esc(object? v) => System.Net.WebUtility.HtmlEncode(v?.ToString() ?? "");

        /// <summary>گزارش جزئیات اسناد</summary>
        public static string DocsReport(IEnumerable<Doc> docs, IEnumerable<DocRow> rows, string branchName)
        {
            var sb = new StringBuilder();
            sb.Append("<table><thead><tr><th>شماره</th><th>تاریخ</th><th>شعبه</th><th>شرح / ردیف‌ها</th></tr></thead><tbody>");
            int n = 0;
            foreach (var d in docs)
            {
                n++;
                var docRows = rows.Where(r => r.DocId == d.Id).OrderBy(r => r.Seq).ToList();
                string txt = string.Join("<br>", docRows.Select(r => "▪ " + Esc(r.Txt)));
                sb.Append($"<tr><td>{d.Id}</td><td>{Esc(d.DateJ)}</td><td>{Esc(d.BranchId > 0 ? branchName : "اصلی")}</td><td>{txt}</td></tr>");
            }
            sb.Append("</tbody></table>");
            sb.Append($"<div class='sum'>تعداد اسناد: {n}</div>");
            return Shell("گزارش جزئیات اسناد", sb.ToString(), "تولیدشده توسط طلایار — قابل مشاهده آفلاین");
        }

        /// <summary>گزارش مانده حساب‌ها (لیستی)</summary>
        public static string BalancesReport(IEnumerable<(Customer c, long cash, long gold)> list)
        {
            var sb = new StringBuilder();
            sb.Append("<table><thead><tr><th>کد</th><th>نام</th><th>گروه</th><th>تلفن</th><th>مانده نقدی</th><th>مانده طلایی</th></tr></thead><tbody>");
            long sumC = 0, sumG = 0;
            foreach (var (c, cash, gold) in list)
            {
                sumC += cash; sumG += gold;
                string cashTxt = cash > 0 ? Esc(Talayar.Money(cash)) + " بدهکار" : cash < 0 ? Esc(Talayar.Money(-cash)) + " بستانکار" : "تسویه";
                string goldTxt = gold != 0 ? Esc(Talayar.Gs(Math.Abs(gold))) + (gold > 0 ? " بدهکار" : " بستانکار") : "—";
                sb.Append($"<tr><td>{c.Code}</td><td>{Esc(c.Name)}</td><td>{Esc(c.Grp)}</td><td dir='ltr'>{Esc(c.Phone)}</td><td>{cashTxt}</td><td>{goldTxt}</td></tr>");
            }
            sb.Append("</tbody></table>");
            sb.Append($"<div class='sum'>مجموع نقدی: {Esc(Talayar.Money(sumC))} تومان &nbsp;|&nbsp; مجموع طلایی: {Esc(Talayar.Gs(sumG))}</div>");
            return Shell("گزارش مانده حساب‌ها", sb.ToString(), "تولیدشده توسط طلایار — قابل مشاهده آفلاین");
        }

        /// <summary>گزارش اتیکت‌ها</summary>
        public static string EtiketsReport(IEnumerable<Etiket> list, string branchName)
        {
            var sb = new StringBuilder();
            sb.Append("<table><thead><tr><th>کد</th><th>نام</th><th>وزن</th><th>مزنه</th><th>شعبه</th><th>وضعیت</th></tr></thead><tbody>");
            long w = 0;
            foreach (var e in list)
            {
                w += e.Wmw;
                string st = e.Status == "stock" ? "موجود" : e.Status == "sold" ? "فروخته‌شده" : "خارج‌شده";
                string cls = e.Status == "stock" ? "ok" : "bad";
                sb.Append($"<tr><td>{Esc(e.Code)}</td><td>{Esc(e.Name)}</td><td>{Esc(Talayar.Gs(e.Wmw))}</td><td>{Esc(e.Mezane)}</td><td>{Esc(e.BranchId > 0 ? branchName : "اصلی")}</td><td class='{cls}'>{st}</td></tr>");
            }
            sb.Append("</tbody></table>");
            sb.Append($"<div class='sum'>تعداد: {list.Count()} &nbsp;|&nbsp; مجموع وزن: {Esc(Talayar.Gs(w))}</div>");
            return Shell("گزارش اتیکت‌ها", sb.ToString(), "تولیدشده توسط طلایار — قابل مشاهده آفلاین");
        }
    }
}

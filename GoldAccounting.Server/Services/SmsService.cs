using GoldAccounting.Server.Data;
using System.Net.Http;

namespace GoldAccounting.Server.Services
{
    /// <summary>
    /// سرویس پیامک — درگاه قابل‌تنظیم (پیش‌فرض کاوه‌نگار)
    /// تنظیمات در جدول Settings:
    ///   sms_enabled (1/0)، sms_provider (kavenegar)، sms_api_key، sms_sender
    /// در حالت تست (بدون کلید معتبر) پیام در جدول SmsLogs با وضعیت logged ثبت می‌شود.
    /// </summary>
    public class SmsService
    {
        private readonly AppDbContext db;
        private readonly HttpClient http;
        private readonly TalayarService svc;
        private readonly ILogger<SmsService> log;

        public SmsService(AppDbContext db, HttpClient http, TalayarService svc, ILogger<SmsService> log)
        {
            this.db = db;
            this.http = http;
            this.svc = svc;
            this.log = log;
        }

        private bool Enabled => svc.GetS("sms_enabled", "0") == "1";
        private string ApiKey => svc.GetS("sms_api_key", "");
        private string Sender => svc.GetS("sms_sender", "10004346");

        public string BaseUrl => svc.GetS("sms_base_url", "https://api.kavenegar.com/v1");

        /// <summary>ارسال پیامک (واقعی یا ثبت در لاگ در حالت تست)</summary>
        public async Task<string> SendAsync(string phone, string message, string kind)
        {
            phone = Talayar.En(phone).Trim();
            if (phone.Length < 10)
            {
                LogSms(phone, kind, message, "error", "شماره نامعتبر");
                return "شماره نامعتبر";
            }

            if (!Enabled || string.IsNullOrEmpty(ApiKey))
            {
                LogSms(phone, kind, message, "logged", "SMS غیرفعال — فقط ثبت شد");
                return "logged";
            }

            try
            {
                var url = $"{BaseUrl}/{ApiKey}/sms/send.json";
                var content = new FormUrlEncodedContent(new Dictionary<string, string>
                {
                    ["receptor"] = phone,
                    ["sender"] = Sender,
                    ["message"] = message
                });
                var resp = await http.PostAsync(url, content);
                string body = await resp.Content.ReadAsStringAsync();
                if (resp.IsSuccessStatusCode)
                {
                    LogSms(phone, kind, message, "sent", "");
                    return "sent";
                }
                LogSms(phone, kind, message, "error", body);
                return "error: " + body;
            }
            catch (Exception ex)
            {
                LogSms(phone, kind, message, "error", ex.Message);
                return "error: " + ex.Message;
            }
        }

        private void LogSms(string phone, string kind, string message, string status, string error)
        {
            try
            {
                db.SmsLogs.Add(new SmsLog
                {
                    Phone = phone,
                    Kind = kind,
                    Message = message,
                    Status = status,
                    Error = error,
                    Ts = DateTimeOffset.Now.ToUnixTimeMilliseconds(),
                    DateJ = Talayar.Jal.Today()
                });
                db.SaveChanges();
            }
            catch { }
        }

        // ── پیام‌های آماده ──

        /// <summary>مانده حساب پس از ثبت سند/فاکتور</summary>
        public async Task<string> SendBalanceAsync(Customer c, long cashBalance, long goldBalance)
        {
            string cashTxt = cashBalance > 0 ? Talayar.Money(cashBalance) + " تومان بدهکار"
                : cashBalance < 0 ? Talayar.Money(-cashBalance) + " تومان بستانکار" : "تسویه";
            string goldTxt = goldBalance != 0 ? Talayar.Gs(Math.Abs(goldBalance)) + (goldBalance > 0 ? " بدهکار" : " بستانکار") : "—";
            string msg = $"طلایار: {c.Name} عزیز، مانده حساب شما پس از ثبت سند:\nنقدی: {cashTxt}\nطلایی (۱۸ معادل): {goldTxt}";
            return await SendAsync(c.Phone, msg, "balance");
        }

        /// <summary>پیامک تبریک تولد</summary>
        public async Task<string> SendBirthdayAsync(Customer c)
        {
            string msg = $"طلایار: {c.Name} عزیز، تولدتان مبارک! 🎂 آرزوی موفقیت و شادکامی برای شما داریم.";
            return await SendAsync(c.Phone, msg, "birthday");
        }

        /// <summary>پیامک تبریک سالگرد ازدواج</summary>
        public async Task<string> SendAnniversaryAsync(Customer c)
        {
            string msg = $"طلایار: {c.Name} عزیز، سالگرد ازدواجتان مبارک! 💍 سال‌هایی پر از عشق و شادی برایتان آرزومندیم.";
            return await SendAsync(c.Phone, msg, "anniversary");
        }

        /// <summary>پیامک لینک دانلود گزارش</summary>
        public async Task<string> SendReportLinkAsync(Customer c, string reportName, string link)
        {
            string msg = $"طلایار: {c.Name} عزیز، گزارش «{reportName}» آماده است.\nدانلود: {link}";
            return await SendAsync(c.Phone, msg, "report_link");
        }

        /// <summary>سرویس روزانه: ارسال تبریک تولد/سالگرد به مشتریان دارای تاریخ</summary>
        public async Task<int> SendDailyGreetingsAsync()
        {
            string today = Talayar.Jal.Today(); // yyyy/MM/dd
            string md = today.Length >= 7 ? today.Substring(5) : ""; // MM/dd
            int sent = 0;
            var customers = db.Customers.Where(c => c.Phone.Length >= 10 && c.SmsConsent).ToList();
            foreach (var c in customers)
            {
                bool birthday = c.BirthdayJ.Length >= 10 && c.BirthdayJ.Substring(5) == md;
                bool anniversary = c.AnniversaryJ.Length >= 10 && c.AnniversaryJ.Substring(5) == md;
                if (birthday) { await SendBirthdayAsync(c); sent++; }
                if (anniversary) { await SendAnniversaryAsync(c); sent++; }
            }
            return sent;
        }
    }
}

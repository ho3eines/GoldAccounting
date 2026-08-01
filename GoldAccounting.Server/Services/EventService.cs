using GoldAccounting.Server.Data;

namespace GoldAccounting.Server.Services
{
    /// <summary>ثبت رویدادهای سیستم (ورود/خروج کاربر، ثبت اسناد و…) — برای پایش نسخهٔ ویندوز و وب</summary>
    public class EventService
    {
        private readonly AppDbContext db;

        public EventService(AppDbContext db) { this.db = db; }

        /// <summary>ثبت یک رویداد</summary>
        public void Log(string action, string targetType, int targetId, string details, string? actor = null, int branchId = 0)
        {
            try
            {
                db.Events.Add(new EventLog
                {
                    Ts = DateTimeOffset.Now.ToUnixTimeMilliseconds(),
                    DateJ = Talayar.Jal.Today(),
                    Actor = string.IsNullOrEmpty(actor) ? "system" : actor,
                    Action = action,
                    TargetType = targetType,
                    TargetId = targetId,
                    Details = details ?? "",
                    BranchId = branchId
                });
                db.SaveChanges();
            }
            catch { /* لاگ نباید جریان اصلی را بشکند */ }
        }

        public void LogLogin(string username, bool success, string? device = null)
            => Log(success ? "login" : "login_failed", "User", 0,
                   $"{(success ? "ورود موفق" : "ورود ناموفق")} کاربر {username}" + (device != null ? $" از {device}" : ""), username);

        public void LogLogout(string username)
            => Log("logout", "User", 0, $"خروج کاربر {username}", username);

        public void LogDoc(int docId, string type, string descr, string? actor = null, int branchId = 0)
            => Log(type == "delete" ? "doc_delete" : "doc_create", "Doc", docId, descr, actor, branchId);

        public void LogInvoice(int invId, string descr, string? actor = null, int branchId = 0)
            => Log("invoice_create", "Invoice", invId, descr, actor, branchId);

        public void LogBackup(string kind, string details)
            => Log(kind == "download" ? "backup_download" : "backup_restore", "Backup", 0, details);
    }
}

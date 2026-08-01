using Microsoft.Data.SqlClient;
using System.Diagnostics;

namespace GoldAccounting.Server.Services
{
    /// <summary>
    /// اجراکنندهٔ اسکریپت‌های TSQL (DB-First / Schema Migration)
    /// ─────────────────────────────────────────────────────────
    /// • پوشه: wwwroot/resources
    /// • نام فایل: [DateTime]_[Create|Alter]_[Tb|Sp|Fn]_[Name].sql
    ///   مثال: 20260801_100000_Create_Tb_Customers.sql
    /// • پس از اجرا نتیجه در جدول dbo.Resources ثبت می‌شود:
    ///   (Id, FileName, Success, ExecuteTime, ExecuteMs, ErrorText)
    /// • فایل‌هایی که قبلاً در جدول Resources ثبت شده‌اند دیگر اجرا نمی‌شوند.
    /// </summary>
    public class ResourceRunner
    {
        private readonly string _connectionString;
        private readonly string _resourcesDir;
        private readonly ILogger<ResourceRunner> _log;

        public ResourceRunner(string connectionString, string webRootPath, ILogger<ResourceRunner> log)
        {
            _connectionString = connectionString;
            _resourcesDir = Path.Combine(webRootPath, "resources");
            _log = log;
        }

        /// <summary>اجرای همهٔ اسکریپت‌های اجرانشده به ترتیب نام (ترتیب تاریخ)</summary>
        public void RunPending()
        {
            if (!Directory.Exists(_resourcesDir))
            {
                _log.LogWarning("پوشهٔ اسکریپت‌ها یافت نشد: {Dir}", _resourcesDir);
                return;
            }

            EnsureResourcesTable();

            var files = Directory.GetFiles(_resourcesDir, "*.sql")
                .OrderBy(f => Path.GetFileName(f), StringComparer.OrdinalIgnoreCase)
                .ToList();

            _log.LogInformation("تعداد اسکریپت‌های TSQL در پوشه: {Count}", files.Count);

            foreach (var file in files)
            {
                var fileName = Path.GetFileName(file);

                if (IsExecuted(fileName))
                {
                    _log.LogInformation("اسکریپت قبلاً اجرا شده، رد شد: {File}", fileName);
                    continue;
                }

                _log.LogInformation("در حال اجرای اسکریپت: {File}", fileName);
                var sw = Stopwatch.StartNew();
                bool success = false;
                string errorText = "";

                try
                {
                    ExecuteScript(file);
                    success = true;
                }
                catch (Exception ex)
                {
                    errorText = ex.Message;
                    _log.LogError(ex, "خطا در اجرای اسکریپت {File}", fileName);
                }
                finally
                {
                    sw.Stop();
                }

                LogResult(fileName, success, sw.ElapsedMilliseconds, errorText);
            }
        }

        /// <summary>ایجاد جدول Resources اگر وجود نداشته باشد (bootstrap)</summary>
        public void EnsureResourcesTable()
        {
            const string sql = @"
IF OBJECT_ID(N'dbo.Resources', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Resources
    (
        Id          INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Resources PRIMARY KEY,
        FileName    NVARCHAR(300)     NOT NULL,
        Success     BIT               NOT NULL CONSTRAINT DF_Resources_Success DEFAULT (0),
        ExecuteTime DATETIME2         NOT NULL CONSTRAINT DF_Resources_ExecuteTime DEFAULT (SYSUTCDATETIME()),
        ExecuteMs   INT               NOT NULL CONSTRAINT DF_Resources_ExecuteMs DEFAULT (0),
        ErrorText   NVARCHAR(MAX)     NULL
    );

    CREATE UNIQUE INDEX UX_Resources_FileName ON dbo.Resources(FileName);
END";
            using var conn = new SqlConnection(_connectionString);
            conn.Open();
            using var cmd = new SqlCommand(sql, conn);
            cmd.ExecuteNonQuery();
        }

        /// <summary>آیا این فایل قبلاً در جدول Resources ثبت شده؟ (اجرای مجدد ممنوع)</summary>
        private bool IsExecuted(string fileName)
        {
            using var conn = new SqlConnection(_connectionString);
            conn.Open();
            using var cmd = new SqlCommand(
                "SELECT COUNT(1) FROM dbo.Resources WHERE FileName = @fn", conn);
            cmd.Parameters.AddWithValue("@fn", fileName);
            return (int)cmd.ExecuteScalar()! > 0;
        }

        /// <summary>ثبت نتیجهٔ اجرا در جدول Resources</summary>
        private void LogResult(string fileName, bool success, long executeMs, string errorText)
        {
            using var conn = new SqlConnection(_connectionString);
            conn.Open();
            using var cmd = new SqlCommand(@"
INSERT INTO dbo.Resources (FileName, Success, ExecuteTime, ExecuteMs, ErrorText)
VALUES (@fn, @ok, SYSUTCDATETIME(), @ms, @err)", conn);
            cmd.Parameters.AddWithValue("@fn", fileName);
            cmd.Parameters.AddWithValue("@ok", success);
            cmd.Parameters.AddWithValue("@ms", executeMs);
            cmd.Parameters.AddWithValue("@err", (object?)errorText ?? DBNull.Value);
            cmd.ExecuteNonQuery();
        }

        /// <summary>اجرای اسکریپت با تفکیک دسته‌های GO</summary>
        private void ExecuteScript(string filePath)
        {
            string script = File.ReadAllText(filePath);

            // تفکیک دسته‌ها روی خط‌های GO (مستقل از بزرگی/کوچکی حروف)
            var batches = script
                .Split(new[] { "\r\nGO\r\n", "\nGO\n", "\r\nGO\n", "\nGO\r\n" }, StringSplitOptions.None)
                .Select(b => b.Trim())
                .Where(b => b.Length > 0)
                .ToList();

            using var conn = new SqlConnection(_connectionString);
            conn.Open();

            foreach (var batch in batches)
            {
                using var cmd = new SqlCommand(batch, conn);
                cmd.CommandTimeout = 300; // ۵ دقیقه برای هر دسته
                cmd.ExecuteNonQuery();
            }
        }
    }
}

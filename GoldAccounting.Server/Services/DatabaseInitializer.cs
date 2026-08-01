using Microsoft.Data.SqlClient;

namespace GoldAccounting.Server.Services
{
    /// <summary>
    /// DB-First: اگر دیتابیس وجود نداشته باشد آن را می‌سازد.
    /// سپس اسکریپت‌های TSQL پوشهٔ wwwroot/resources توسط ResourceRunner اجرا می‌شوند
    /// و ساختار (و هر تغییر بعدی) از همان اسکریپت‌ها می‌آید.
    /// </summary>
    public static class DatabaseInitializer
    {
        public static void EnsureDatabase(string connectionString)
        {
            var csb = new SqlConnectionStringBuilder(connectionString);
            string dbName = csb.InitialCatalog;

            if (string.IsNullOrWhiteSpace(dbName))
                throw new InvalidOperationException("نام دیتابیس در کانکشن‌استرینگ مشخص نشده است.");

            // اتصال به master برای ساخت دیتابیس
            var masterCsb = new SqlConnectionStringBuilder(connectionString)
            {
                InitialCatalog = "master"
            };

            using var conn = new SqlConnection(masterCsb.ConnectionString);
            conn.Open();

            string escaped = dbName.Replace("]", "]]");
            string sql = $@"
IF DB_ID(N'{escaped}') IS NULL
BEGIN
    CREATE DATABASE [{escaped}];
END";
            using var cmd = new SqlCommand(sql, conn);
            cmd.ExecuteNonQuery();
        }
    }
}

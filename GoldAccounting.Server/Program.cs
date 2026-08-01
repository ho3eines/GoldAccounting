using Microsoft.EntityFrameworkCore;
using GoldAccounting.Server.Data;
using GoldAccounting.Server.Services;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.
builder.Services.AddRazorPages();
builder.Services.AddServerSideBlazor();

// SQL Server (DB-First) — ساختار دیتابیس از اسکریپت‌های wwwroot/resources ساخته می‌شود
var connectionString = builder.Configuration.GetConnectionString("DefaultConnection")
    ?? throw new InvalidOperationException("ConnectionStrings:DefaultConnection تنظیم نشده است.");

builder.Services.AddDbContext<AppDbContext>(options =>
    options.UseSqlServer(connectionString));

// Automated Accounting Service
builder.Services.AddScoped<AccountingService>();
builder.Services.AddScoped<TalayarService>();

// اجراکنندهٔ اسکریپت‌های TSQL (DB-First)
builder.Services.AddScoped<ResourceRunner>(sp =>
{
    var env = sp.GetRequiredService<IWebHostEnvironment>();
    var log = sp.GetRequiredService<ILoggerFactory>().CreateLogger<ResourceRunner>();
    return new ResourceRunner(connectionString, env.WebRootPath, log);
});

// HttpClient برای دریافت قیمت‌های آنلاین از سمت سرور
builder.Services.AddScoped(sp => new HttpClient { Timeout = TimeSpan.FromSeconds(30) });

var app = builder.Build();

// ── راه‌اندازی DB-First: ساخت دیتابیس + اجرای اسکریپت‌های TSQL اجرانشده ──
using (var scope = app.Services.CreateScope())
{
    var logger = scope.ServiceProvider.GetRequiredService<ILoggerFactory>().CreateLogger<ResourceRunner>();
    var env = scope.ServiceProvider.GetRequiredService<IWebHostEnvironment>();

    try
    {
        // ۱) ساخت دیتابیس اگر وجود نداشته باشد
        DatabaseInitializer.EnsureDatabase(connectionString);
        logger.LogInformation("دیتابیس SQL Server آماده است.");

        // ۲) اجرای اسکریپت‌های TSQL پوشهٔ wwwroot/resources (اجرانشده‌ها رد می‌شوند)
        var runner = new ResourceRunner(connectionString, env.WebRootPath, logger);
        runner.RunPending();

        // ۳) داده‌های اولیه (idempotent)
        var db = scope.ServiceProvider.GetRequiredService<AppDbContext>();

        // کاربر مدیر پیش‌فرض
        if (!db.UserAccounts.Any())
        {
            db.UserAccounts.Add(new UserAccount { Username = "admin", Password = "admin123", FullName = "مدیر سیستم", Role = "Admin", Token = "TOKEN-ADMIN-12345", Cts = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() });
            db.SaveChanges();
        }

        // کالاهای نمونه برای فروشگاه آنلاین (فقط در نصب تازه)
        if (!db.Items.Any())
        {
            long its = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            db.Items.Add(new Item { Code = 101, Name = "انگشتر طلای طرح ملکه", Karat = 750, Wmw = 5400, WType = 0, WVal = 7, StoneVal = 12000000, Descr = "بسیار شیک و پرطرفدار — مناسب هدیه", Status = "stock", Cts = its });
            db.Items.Add(new Item { Code = 102, Name = "دستبند طلا کارتیه", Karat = 750, Wmw = 12500, WType = 0, WVal = 5, StoneVal = 0, Descr = "مدل جدید و ظریف", Status = "stock", Cts = its });
            db.Items.Add(new Item { Code = 103, Name = "سرویس کامل طلا عروس", Karat = 750, Wmw = 45000, WType = 0, WVal = 6, StoneVal = 250000000, Descr = "مخصوص مراسم و سرمایه‌گذاری", Status = "stock", Cts = its });
            db.Items.Add(new Item { Code = 104, Name = "النگوی آب‌شده ۲۱ عیار", Karat = 875, Wmw = 8000, WType = 1, WVal = 150000, StoneVal = 0, Descr = "آب‌شده — بدون مالیات", Status = "stock", Cts = its });
            db.SaveChanges();
        }

        // قیمت‌های اولیه بازار
        if (!db.MarketPrices.Any())
        {
            db.MarketPrices.Add(new MarketPrice { Key = "gold18", Val = 18665400, Ts = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() });
            db.MarketPrices.Add(new MarketPrice { Key = "usd", Val = 192400, Ts = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() });
            db.MarketPrices.Add(new MarketPrice { Key = "coin_imami", Val = 188010000, Ts = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() });
            db.SaveChanges();
        }

        // کدینگ‌های پیش‌فرض (هماهنگ با اندروید)
        if (!db.Defs.Any())
        {
            long ts = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            foreach (var g in new[] { "مشتریان", "تولیدکنندگان", "بنکداران", "همکاران", "پرسنل" })
                db.Defs.Add(new Def { Kind = "group", Name = g, Cts = ts });
            db.Defs.Add(new Def { Kind = "coin", Name = "سکه امامی", X1 = 8133, X2 = 900, Cts = ts });
            db.Defs.Add(new Def { Kind = "coin", Name = "سکه بهار آزادی", X1 = 8133, X2 = 900, Cts = ts });
            db.Defs.Add(new Def { Kind = "coin", Name = "نیم سکه", X1 = 4066, X2 = 900, Cts = ts });
            db.Defs.Add(new Def { Kind = "coin", Name = "ربع سکه", X1 = 2033, X2 = 900, Cts = ts });
            db.Defs.Add(new Def { Kind = "coin", Name = "سکه گرمی", X1 = 1010, X2 = 900, Cts = ts });
            db.Defs.Add(new Def { Kind = "bullion", Name = "شمش ۱۰ گرمی ۹۹۵", X1 = 10000, X2 = 995, Cts = ts });
            db.Defs.Add(new Def { Kind = "bullion", Name = "شمش ۱۰۰ گرمی ۹۹۵", X1 = 100000, X2 = 995, Cts = ts });
            db.Defs.Add(new Def { Kind = "bullion", Name = "شمش ۱ کیلویی ۹۹۵", X1 = 1000000, X2 = 995, Cts = ts });
            foreach (var c in new[] { "دلار آمریکا", "یورو", "درهم امارات", "لیر ترکیه", "پوند انگلیس", "تتر USDT" })
                db.Defs.Add(new Def { Kind = "curr", Name = c, Cts = ts });
            db.Defs.Add(new Def { Kind = "silver", Name = "نقره ۹۹۹", X2 = 999, Cts = ts });
            db.Defs.Add(new Def { Kind = "silver", Name = "نقره ۹۲۵", X2 = 925, Cts = ts });
            db.Defs.Add(new Def { Kind = "silver", Name = "نقره ۸۴۰", X2 = 840, Cts = ts });
            foreach (var r in new[] { "بدون رسوب", "ری‌گیری ۱٪", "ری‌گیری ۲٪", "ری‌گیری ۳٪", "رسوب مخصوص" })
                db.Defs.Add(new Def { Kind = "rizgiri", Name = r, Cts = ts });
            db.SaveChanges();
        }
    }
    catch (Exception ex)
    {
        logger.LogCritical(ex, "راه‌اندازی دیتابیس ناموفق بود. اطمینان حاصل کنید SQL Server در دسترس است و کانکشن‌استرینگ درست است.");
        throw;
    }
}

// Configure the HTTP request pipeline.
if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler("/Error");
}

app.UseStaticFiles();
app.UseRouting();

app.MapControllers();
app.MapBlazorHub();
app.MapFallbackToPage("/_Host");

app.Run();

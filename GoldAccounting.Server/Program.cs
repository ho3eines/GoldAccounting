using Microsoft.EntityFrameworkCore;
using GoldAccounting.Server.Data;
using GoldAccounting.Server.Services;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.
builder.Services.AddRazorPages();
builder.Services.AddServerSideBlazor();

// SQLite Database-First Configuration
builder.Services.AddDbContext<AppDbContext>(options =>
    options.UseSqlite("Data Source=talayar_server.db"));

// Automated Accounting Service
builder.Services.AddScoped<AccountingService>();
builder.Services.AddScoped<TalayarService>();

// HttpClient برای دریافت قیمت‌های آنلاین از سمت سرور
builder.Services.AddScoped(sp => new HttpClient { Timeout = TimeSpan.FromSeconds(30) });

var app = builder.Build();

// Ensure database created & seeded
using (var scope = app.Services.CreateScope())
{
    var db = scope.ServiceProvider.GetRequiredService<AppDbContext>();
    db.Database.EnsureCreated();
    
        // Seed default admin user
        if (!db.UserAccounts.Any())
        {
            db.UserAccounts.Add(new UserAccount { Username = "admin", Password = "admin123", FullName = "مدیر سیستم", Role = "Admin", Token = "TOKEN-ADMIN-12345", Cts = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() });
            db.SaveChanges();
        }
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

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

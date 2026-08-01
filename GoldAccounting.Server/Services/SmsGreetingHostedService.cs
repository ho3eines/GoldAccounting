namespace GoldAccounting.Server.Services
{
    /// <summary>
    /// سرویس پس‌زمینه: هر روز یک‌بار (و در شروع برنامه) تاریخ تولد و سالگرد
    /// ازدواج مشتریان را بررسی و پیامک تبریک ارسال می‌کند.
    /// </summary>
    public class SmsGreetingHostedService : BackgroundService
    {
        private readonly IServiceScopeFactory _scopeFactory;
        private readonly ILogger<SmsGreetingHostedService> _log;
        private DateTime _lastCheck = DateTime.MinValue;

        public SmsGreetingHostedService(IServiceScopeFactory scopeFactory, ILogger<SmsGreetingHostedService> log)
        {
            _scopeFactory = scopeFactory;
            _log = log;
        }

        protected override async Task ExecuteAsync(CancellationToken stoppingToken)
        {
            // بررسی اولیه در شروع برنامه
            await CheckAsync(stoppingToken);

            while (!stoppingToken.IsCancellationRequested)
            {
                try { await Task.Delay(TimeSpan.FromHours(6), stoppingToken); }
                catch (OperationCanceledException) { break; }
                await CheckAsync(stoppingToken);
            }
        }

        private async Task CheckAsync(CancellationToken ct)
        {
            string today = Talayar.Jal.Today();
            if (_lastCheck.Date == DateTime.Today) return;
            _lastCheck = DateTime.Today;

            try
            {
                using var scope = _scopeFactory.CreateScope();
                var sms = scope.ServiceProvider.GetRequiredService<SmsService>();
                int sent = await sms.SendDailyGreetingsAsync();
                if (sent > 0)
                    _log.LogInformation("پیامک تبریک ارسال شد: {Count} (تاریخ {Date})", sent, today);
            }
            catch (Exception ex)
            {
                _log.LogError(ex, "خطا در سرویس پیامک تبریک");
            }
        }
    }
}

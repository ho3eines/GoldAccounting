using Microsoft.AspNetCore.Mvc;
using GoldAccounting.Server.Data;
using GoldAccounting.Server.Services;

namespace GoldAccounting.Server.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class SyncController : ControllerBase
    {
        private readonly AppDbContext _db;
        private readonly AccountingService _accountingService;

        public SyncController(AppDbContext db, AccountingService accountingService)
        {
            _db = db;
            _accountingService = accountingService;
        }

        [HttpPost("push")]
        public IActionResult PushSync([FromBody] SyncPayload payload)
        {
            if (payload == null || string.IsNullOrEmpty(payload.TransferCode))
            {
                return BadRequest(new { status = "error", message = "کد انتقال (TransferCode) معتبر نیست." });
            }

            // بررسی تکراری نبودن (Idempotency Check)
            var existingLog = _db.SyncLogs.FirstOrDefault(s => s.TransferCode == payload.TransferCode);
            if (existingLog != null)
            {
                return Ok(new { 
                    status = "success", 
                    duplicate = true, 
                    transfer_code = payload.TransferCode, 
                    message = "این تراکنش قبلاً با موفقیت همگام‌سازی شده است (تکراری جلوگیری شد)." 
                });
            }

            try
            {
                // پردازش فاکتورهای ارسال شده
                if (payload.Invoices != null)
                {
                    foreach (var invDto in payload.Invoices)
                    {
                        var existsInv = _db.Invoices.FirstOrDefault(i => i.TransferCode == invDto.TransferCode);
                        if (existsInv == null)
                        {
                            var inv = new Invoice
                            {
                                Ts = invDto.Ts,
                                DateJ = invDto.DateJ,
                                Cid = invDto.Cid,
                                Cname = invDto.Cname,
                                Rate = invDto.Rate,
                                GoldVal = invDto.GoldVal,
                                Wage = invDto.Wage,
                                Stone = invDto.Stone,
                                Tax = invDto.Tax,
                                Total = invDto.Total,
                                Pcash = invDto.Pcash,
                                PgoldMw = invDto.PgoldMw,
                                PgoldVal = invDto.PgoldVal,
                                PgoldKarat = invDto.PgoldKarat,
                                Debt = invDto.Debt,
                                Note = invDto.Note,
                                TransferCode = invDto.TransferCode
                            };
                            _db.Invoices.Add(inv);
                            _db.SaveChanges();

                            // تولید سند حسابداری اتوماتیک
                            _accountingService.GenerateVoucherForInvoice(inv);
                        }
                    }
                }

                // ثبت لاگ همگام‌سازی موفق با کد یکتا
                var syncLog = new SyncLog
                {
                    TransferCode = payload.TransferCode,
                    DeviceId = payload.DeviceId ?? "AndroidClient",
                    Ts = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                    PayloadSummary = $"Sync invoices count: {payload.Invoices?.Count ?? 0}",
                    Status = "success",
                    Message = "همگام‌سازی و سند حسابداری اتوماتیک با موفقیت انجام شد."
                };
                _db.SyncLogs.Add(syncLog);
                _db.SaveChanges();

                return Ok(new { 
                    status = "success", 
                    duplicate = false,
                    transfer_code = payload.TransferCode,
                    server_ts = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                    message = "داده‌ها با موفقیت دریافت و سند حسابداری اتوماتیک ثبت شد ✓" 
                });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { status = "error", message = ex.Message });
            }
        }

        [HttpGet("pull")]
        public IActionResult PullSync()
        {
            var invoices = _db.Invoices.OrderByDescending(i => i.Id).Take(50).ToList();
            var customers = _db.Customers.ToList();
            var items = _db.Items.ToList();
            var prices = _db.MarketPrices.ToList();

            return Ok(new {
                status = "success",
                invoices,
                customers,
                items,
                prices,
                server_time = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
            });
        }
    }

    public class SyncPayload
    {
        public string TransferCode { get; set; } = string.Empty;
        public string DeviceId { get; set; } = string.Empty;
        public List<InvoiceDto>? Invoices { get; set; }
    }

    public class InvoiceDto
    {
        public long Ts { get; set; }
        public string DateJ { get; set; } = string.Empty;
        public int Cid { get; set; }
        public string Cname { get; set; } = string.Empty;
        public long Rate { get; set; }
        public long GoldVal { get; set; }
        public long Wage { get; set; }
        public long Stone { get; set; }
        public long Tax { get; set; }
        public long Total { get; set; }
        public long Pcash { get; set; }
        public long PgoldMw { get; set; }
        public long PgoldVal { get; set; }
        public int PgoldKarat { get; set; }
        public long Debt { get; set; }
        public string Note { get; set; } = string.Empty;
        public string TransferCode { get; set; } = string.Empty;
    }
}

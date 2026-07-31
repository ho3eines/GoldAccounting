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
            // احراز هویت توکن اندروید
            string authHeader = Request.Headers["Authorization"].FirstOrDefault() ?? "";
            string token = authHeader.StartsWith("Bearer ") ? authHeader.Substring(7) : payload.Token;

            if (!string.IsNullOrEmpty(token))
            {
                var user = _db.UserAccounts.FirstOrDefault(u => u.Token == token);
                if (user == null)
                {
                    return Unauthorized(new { status = "error", message = "توکن احراز هویت نامعتبر است. لطفا در تنظیمات اندروید دوباره لاگین کنید." });
                }
            }

            var existingLog = _db.SyncLogs.FirstOrDefault(s => s.TransferCode == payload.TransferCode);
            if (existingLog != null)
            {
                return Ok(new { 
                    status = "success", 
                    duplicate = true, 
                    transfer_code = payload.TransferCode, 
                    message = "این تراکنش قبلاً با موفقیت همگام‌سازی شده است." 
                });
            }

            try
            {
                // 1. Invoices
                if (payload.Invoices != null)
                {
                    foreach (var dto in payload.Invoices)
                    {
                        if (!_db.Invoices.Any(i => i.TransferCode == dto.TransferCode))
                        {
                            var inv = new Invoice
                            {
                                Ts = dto.Ts,
                                DateJ = dto.DateJ,
                                Cid = dto.Cid,
                                Cname = dto.Cname,
                                Rate = dto.Rate,
                                GoldVal = dto.GoldVal,
                                Wage = dto.Wage,
                                Stone = dto.Stone,
                                Tax = dto.Tax,
                                Total = dto.Total,
                                Pcash = dto.Pcash,
                                PgoldMw = dto.PgoldMw,
                                PgoldVal = dto.PgoldVal,
                                PgoldKarat = dto.PgoldKarat,
                                Debt = dto.Debt,
                                Note = dto.Note,
                                TransferCode = dto.TransferCode
                            };
                            _db.Invoices.Add(inv);
                            _db.SaveChanges();
                            _accountingService.GenerateVoucherForInvoice(inv);
                        }
                    }
                }

                // 2. Checks
                if (payload.Checks != null)
                {
                    foreach (var dto in payload.Checks)
                    {
                        if (!_db.CheckTransactions.Any(c => c.TransferCode == dto.TransferCode))
                        {
                            _db.CheckTransactions.Add(new CheckTransaction
                            {
                                Ts = dto.Ts,
                                DateJ = dto.DateJ,
                                DueJ = dto.DueJ,
                                Amount = dto.Amount,
                                BankId = dto.BankId,
                                Cid = dto.Cid,
                                Cname = dto.Cname,
                                Kind = dto.Kind,
                                No = dto.No,
                                Status = dto.Status,
                                Descr = dto.Descr,
                                DocId = dto.DocId,
                                TransferCode = dto.TransferCode
                            });
                        }
                    }
                    _db.SaveChanges();
                }

                // 3. Cash & Bank
                if (payload.CashTransactions != null)
                {
                    foreach (var dto in payload.CashTransactions)
                    {
                        if (!_db.CashTransactions.Any(c => c.Descr == dto.Descr && c.Amount == dto.Amount))
                        {
                            _db.CashTransactions.Add(new CashTransaction
                            {
                                Ts = dto.Ts,
                                DateJ = dto.DateJ,
                                Kind = dto.Kind,
                                Amount = dto.Amount,
                                Descr = dto.Descr,
                                Iid = dto.Iid
                            });
                        }
                    }
                    _db.SaveChanges();
                }

                // 4. Gold Tx
                if (payload.GoldTransactions != null)
                {
                    foreach (var dto in payload.GoldTransactions)
                    {
                        if (!_db.GoldTransactions.Any(g => g.Descr == dto.Descr && g.Wmw == dto.Wmw))
                        {
                            _db.GoldTransactions.Add(new GoldTransactionEntity
                            {
                                Ts = dto.Ts,
                                DateJ = dto.DateJ,
                                Kind = dto.Kind,
                                Wmw = dto.Wmw,
                                Karat = dto.Karat,
                                Descr = dto.Descr,
                                Cid = dto.Cid,
                                DocId = dto.DocId,
                                TransferCode = dto.TransferCode ?? Guid.NewGuid().ToString()
                            });
                        }
                    }
                    _db.SaveChanges();
                }

                // 5. Customers & Items
                if (payload.Customers != null)
                {
                    foreach (var cDto in payload.Customers)
                    {
                        if (!_db.Customers.Any(c => c.Code == cDto.Code || c.Name == cDto.Name))
                        {
                            _db.Customers.Add(new Customer
                            {
                                Code = cDto.Code,
                                Name = cDto.Name,
                                Phone = cDto.Phone,
                                Grp = cDto.Grp,
                                Address = cDto.Address,
                                Note = cDto.Note,
                                Cts = cDto.Cts
                            });
                        }
                    }
                    _db.SaveChanges();
                }

                if (payload.Items != null)
                {
                    foreach (var iDto in payload.Items)
                    {
                        if (!_db.Items.Any(i => i.Code == iDto.Code))
                        {
                            _db.Items.Add(new Item
                            {
                                Code = iDto.Code,
                                Name = iDto.Name,
                                Karat = iDto.Karat,
                                Wmw = iDto.Wmw,
                                WType = iDto.WType,
                                WVal = iDto.WVal,
                                StoneMw = iDto.StoneMw,
                                StoneVal = iDto.StoneVal,
                                Descr = iDto.Descr,
                                Status = iDto.Status,
                                Cts = iDto.Cts
                            });
                        }
                    }
                    _db.SaveChanges();
                }

                _db.SyncLogs.Add(new SyncLog
                {
                    TransferCode = payload.TransferCode,
                    DeviceId = payload.DeviceId ?? "AndroidClient",
                    Ts = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                    PayloadSummary = $"Invoices: {payload.Invoices?.Count ?? 0}, Checks: {payload.Checks?.Count ?? 0}, Customers: {payload.Customers?.Count ?? 0}",
                    Status = "success",
                    Message = "همگام‌سازی کامل تمام امکانات اندروید با موفقیت انجام شد."
                });
                _db.SaveChanges();

                return Ok(new { 
                    status = "success", 
                    duplicate = false,
                    transfer_code = payload.TransferCode,
                    server_ts = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                    message = "تمامی داده‌ها و چک‌های اندروید با موفقیت دریافت و همگام‌سازی شدند ✓" 
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
            return Ok(new {
                status = "success",
                invoices = _db.Invoices.ToList(),
                checks = _db.CheckTransactions.ToList(),
                customers = _db.Customers.ToList(),
                items = _db.Items.ToList(),
                cashTransactions = _db.CashTransactions.ToList(),
                goldTransactions = _db.GoldTransactions.ToList(),
                prices = _db.MarketPrices.ToList(),
                server_time = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
            });
        }
    }

    public class SyncPayload
    {
        public string TransferCode { get; set; } = string.Empty;
        public string DeviceId { get; set; } = string.Empty;
        public string Token { get; set; } = string.Empty;
        public List<InvoiceDto>? Invoices { get; set; }
        public List<CheckDto>? Checks { get; set; }
        public List<CashDto>? CashTransactions { get; set; }
        public List<GoldDto>? GoldTransactions { get; set; }
        public List<CustomerDto>? Customers { get; set; }
        public List<ItemDto>? Items { get; set; }
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

    public class CheckDto
    {
        public long Ts { get; set; }
        public string DateJ { get; set; } = string.Empty;
        public string DueJ { get; set; } = string.Empty;
        public long Amount { get; set; }
        public int BankId { get; set; }
        public int Cid { get; set; }
        public string Cname { get; set; } = string.Empty;
        public string Kind { get; set; } = string.Empty;
        public string No { get; set; } = string.Empty;
        public string Status { get; set; } = string.Empty;
        public string Descr { get; set; } = string.Empty;
        public int DocId { get; set; }
        public string TransferCode { get; set; } = string.Empty;
    }

    public class CashDto
    {
        public long Ts { get; set; }
        public string DateJ { get; set; } = string.Empty;
        public string Kind { get; set; } = string.Empty;
        public long Amount { get; set; }
        public string Descr { get; set; } = string.Empty;
        public int Iid { get; set; }
    }

    public class GoldDto
    {
        public long Ts { get; set; }
        public string DateJ { get; set; } = string.Empty;
        public string Kind { get; set; } = string.Empty;
        public long Wmw { get; set; }
        public int Karat { get; set; }
        public string Descr { get; set; } = string.Empty;
        public int Cid { get; set; }
        public int DocId { get; set; }
        public string TransferCode { get; set; } = string.Empty;
    }

    public class CustomerDto
    {
        public int Code { get; set; }
        public string Name { get; set; } = string.Empty;
        public string Phone { get; set; } = string.Empty;
        public string Grp { get; set; } = string.Empty;
        public string Address { get; set; } = string.Empty;
        public string Note { get; set; } = string.Empty;
        public long Cts { get; set; }
    }

    public class ItemDto
    {
        public long Code { get; set; }
        public string Name { get; set; } = string.Empty;
        public int Karat { get; set; }
        public long Wmw { get; set; }
        public int WType { get; set; }
        public long WVal { get; set; }
        public long StoneMw { get; set; }
        public long StoneVal { get; set; }
        public string Descr { get; set; } = string.Empty;
        public string Status { get; set; } = string.Empty;
        public long Cts { get; set; }
    }
}

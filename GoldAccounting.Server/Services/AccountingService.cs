using GoldAccounting.Server.Data;

namespace GoldAccounting.Server.Services
{
    public class AccountingService
    {
        private readonly AppDbContext _db;

        public AccountingService(AppDbContext db)
        {
            _db = db;
        }

        public void GenerateVoucherForInvoice(Invoice inv)
        {
            // Check if voucher already exists for this invoice transfer code
            var existing = _db.AccountingVouchers.FirstOrDefault(v => v.RefType == "Invoice" && v.RefId == inv.Id);
            if (existing != null) return;

            var voucher = new AccountingVoucher
            {
                Ts = inv.Ts > 0 ? inv.Ts : DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                DateJ = string.IsNullOrEmpty(inv.DateJ) ? "1405/05/09" : inv.DateJ,
                Title = $"سند فروش فاکتور شماره {inv.Id} - {inv.Cname}",
                RefType = "Invoice",
                RefId = inv.Id,
                Descr = $"ثبت اتوماتیک فروش فاکتور برای مشتری {inv.Cname}"
            };

            long total = inv.Total;
            long pcash = inv.Pcash;
            long pgold = inv.PgoldVal;
            long debt = inv.Debt;

            var rows = new List<VoucherRow>();

            // 1. بدهکار: صندوق (نقدی دریافتی)
            if (pcash > 0)
            {
                rows.Add(new VoucherRow { AccountCode = "101", AccountName = "صندوق / وجه نقد", Debit = pcash, Credit = 0, Descr = "پرداخت نقدی فاکتور" });
            }

            // 2. بدهکار: حساب طلای پرداختی مشتری
            if (pgold > 0)
            {
                rows.Add(new VoucherRow { AccountCode = "103", AccountName = "حساب طلای پرداختی", Debit = pgold, Credit = 0, Descr = "طلای آب‌شده تحویلی جهت تسویه" });
            }

            // 3. بدهکار: حساب بدهکاران / مشتریان (مانده نسیه)
            if (debt > 0)
            {
                rows.Add(new VoucherRow { AccountCode = "201", AccountName = $"حساب مشتریان ({inv.Cname})", Debit = debt, Credit = 0, Descr = "مانده بدهی فاکتور" });
            }

            // 4. بستانکار: درآمد فروش طلا و اجرت
            long salesRevenue = inv.GoldVal + inv.Wage + inv.Stone + inv.Tax;
            if (salesRevenue > 0)
            {
                rows.Add(new VoucherRow { AccountCode = "401", AccountName = "درآمد فروش طلا و اجرت", Debit = 0, Credit = salesRevenue, Descr = "فروش طلا، اجرت، سنگ و مالیات" });
            }

            voucher.Rows = rows;
            _db.AccountingVouchers.Add(voucher);
            _db.SaveChanges();
        }
    }
}

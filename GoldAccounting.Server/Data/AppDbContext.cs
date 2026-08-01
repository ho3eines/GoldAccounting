using Microsoft.EntityFrameworkCore;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace GoldAccounting.Server.Data
{
    public class AppDbContext : DbContext
    {
        public AppDbContext(DbContextOptions<AppDbContext> options) : base(options) { }

        public DbSet<Customer> Customers { get; set; }
        public DbSet<Item> Items { get; set; }
        public DbSet<Invoice> Invoices { get; set; }
        public DbSet<InvoiceLine> InvoiceLines { get; set; }
        public DbSet<CashTransaction> CashTransactions { get; set; }
        public DbSet<GoldTransactionEntity> GoldTransactions { get; set; }
        public DbSet<AccountingVoucher> AccountingVouchers { get; set; }
        public DbSet<VoucherRow> VoucherRows { get; set; }
        public DbSet<SyncLog> SyncLogs { get; set; }
        public DbSet<UserAccount> UserAccounts { get; set; }
        public DbSet<CheckTransaction> CheckTransactions { get; set; }
        public DbSet<BankTransaction> BankTransactions { get; set; }
        public DbSet<AssetLedgerEntity> AssetLedgers { get; set; }

        // مدل کامل نسخه ۴ (هماهنگ با اندروید)
        public DbSet<Setting> Settings { get; set; }
        public DbSet<Rate> Rates { get; set; }
        public DbSet<Def> Defs { get; set; }
        public DbSet<Doc> Docs { get; set; }
        public DbSet<DocRow> DocRows { get; set; }
        public DbSet<CustomerTx> CustomerTxs { get; set; }
        public DbSet<Etiket> Etikets { get; set; }
        public DbSet<MarketPrice> MarketPrices { get; set; }
        public DbSet<ResourceLog> ResourceLogs { get; set; }

        protected override void OnModelCreating(ModelBuilder modelBuilder)
        {
            base.OnModelCreating(modelBuilder);
            modelBuilder.Entity<SyncLog>().HasIndex(s => s.TransferCode).IsUnique();
            modelBuilder.Entity<CustomerTx>().HasIndex(c => c.Cid);
            modelBuilder.Entity<DocRow>().HasIndex(d => d.DocId);
            modelBuilder.Entity<Def>().HasIndex(d => d.Kind);
            modelBuilder.Entity<AssetLedgerEntity>().HasIndex(a => new { a.Scope, a.Asset });
        }
    }

    public class Customer
    {
        [Key]
        public int Id { get; set; }
        public int Code { get; set; }
        public string Name { get; set; } = string.Empty;
        public string Phone { get; set; } = string.Empty;
        public string Grp { get; set; } = string.Empty;
        public string Address { get; set; } = string.Empty;
        public string Note { get; set; } = string.Empty;
        public long Cts { get; set; }
    }

    public class Item
    {
        [Key]
        public int Id { get; set; }
        public long Code { get; set; }
        public string Name { get; set; } = string.Empty;
        public int Karat { get; set; }
        public long Wmw { get; set; } // weight in milligram
        public int WType { get; set; }
        public long WVal { get; set; }
        public long StoneMw { get; set; }
        public long StoneVal { get; set; }
        public string Descr { get; set; } = string.Empty;
        public string Status { get; set; } = "stock"; // stock, sold, out
        public long Cts { get; set; }
    }

    public class Invoice
    {
        [Key]
        public int Id { get; set; }
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
        public string TransferCode { get; set; } = string.Empty; // Idempotent check
        public List<InvoiceLine> Lines { get; set; } = new();
    }

    public class InvoiceLine
    {
        [Key]
        public int Id { get; set; }
        public int Iid { get; set; }
        public int ItemId { get; set; }
        public string Title { get; set; } = string.Empty;
        public int Karat { get; set; }
        public long Wmw { get; set; }
        public long Unit { get; set; }
        public long Wage { get; set; }
        public long Stone { get; set; }
        public long Tax { get; set; }
        public long Total { get; set; }

        /// <summary>ارزش طلای خام قلم (هماهنگ با محاسبهٔ اندروید)</summary>
        [NotMapped]
        public long GoldVal => (long)Math.Round(Wmw * Unit / 1000.0);
    }

    public class CashTransaction
    {
        [Key]
        public int Id { get; set; }
        public long Ts { get; set; }
        public string DateJ { get; set; } = string.Empty;
        public string Kind { get; set; } = "in"; // in / out
        public long Amount { get; set; }
        public string Descr { get; set; } = string.Empty;
        public int Iid { get; set; }
    }

    // سند حسابداری اتوماتیک (Double-Entry Accounting Vouchers)
    public class AccountingVoucher
    {
        [Key]
        public int Id { get; set; }
        public long Ts { get; set; }
        public string DateJ { get; set; } = string.Empty;
        public string Title { get; set; } = string.Empty;
        public string RefType { get; set; } = string.Empty; // Invoice, Cash, Gold, Manual
        public int RefId { get; set; }
        public string Descr { get; set; } = string.Empty;
        public List<VoucherRow> Rows { get; set; } = new();
    }

    public class VoucherRow
    {
        [Key]
        public int Id { get; set; }
        public int VoucherId { get; set; }
        public string AccountCode { get; set; } = string.Empty; // e.g. "101-صندوق", "201-حساب مشتریان"
        public string AccountName { get; set; } = string.Empty;
        public long Debit { get; set; }  // بدهکار
        public long Credit { get; set; } // بستانکار
        public string Descr { get; set; } = string.Empty;
    }

    // ثبت همگام‌سازی اندروید با کد رهگیری یکتا برای جلوگیری از تکرار
    public class SyncLog
    {
        [Key]
        public int Id { get; set; }
        public string TransferCode { get; set; } = string.Empty; // Unique Idempotency Key
        public string DeviceId { get; set; } = string.Empty;
        public long Ts { get; set; }
        public string PayloadSummary { get; set; } = string.Empty;
        public string Status { get; set; } = "success";
        public string Message { get; set; } = string.Empty;
    }

    public class MarketPrice
    {
        [Key]
        public int Id { get; set; }
        public long Ts { get; set; }
        public string Key { get; set; } = string.Empty;
        public long Val { get; set; }
    }
}

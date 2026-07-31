using System.ComponentModel.DataAnnotations;

namespace GoldAccounting.Server.Data
{
    public class CheckTransaction
    {
        [Key]
        public int Id { get; set; }
        public long Ts { get; set; }
        public string DateJ { get; set; } = string.Empty;
        public string DueJ { get; set; } = string.Empty;
        public long Amount { get; set; }
        public int BankId { get; set; }
        public int Cid { get; set; }
        public string Cname { get; set; } = string.Empty;
        public string Kind { get; set; } = "receive"; // receive / pay
        public string No { get; set; } = string.Empty;
        public string Status { get; set; } = "pending"; // pending / passed / bounced
        public string Descr { get; set; } = string.Empty;
        public int DocId { get; set; }
        public string TransferCode { get; set; } = string.Empty;
    }

    public class BankTransaction
    {
        [Key]
        public int Id { get; set; }
        public long Ts { get; set; }
        public string DateJ { get; set; } = string.Empty;
        public int BankId { get; set; }
        public string Kind { get; set; } = "in"; // in / out
        public long Amount { get; set; }
        public string Descr { get; set; } = string.Empty;
        public int Cid { get; set; }
        public int DocId { get; set; }
        public string TransferCode { get; set; } = string.Empty;
    }

    public class GoldTransactionEntity
    {
        [Key]
        public int Id { get; set; }
        public long Ts { get; set; }
        public string DateJ { get; set; } = string.Empty;
        public string Kind { get; set; } = "in"; // in / out
        public long Wmw { get; set; }
        public int Karat { get; set; }
        public string Descr { get; set; } = string.Empty;
        public int Cid { get; set; }
        public int DocId { get; set; }
        public string TransferCode { get; set; } = string.Empty;
    }

    public class AssetLedgerEntity
    {
        [Key]
        public int Id { get; set; }
        public int DocId { get; set; }
        public long Ts { get; set; }
        public string DateJ { get; set; } = string.Empty;
        public string Scope { get; set; } = string.Empty;
        public string Asset { get; set; } = string.Empty;
        public double Qty { get; set; }
        public int Karat { get; set; }
        public int Cid { get; set; }
        public string Descr { get; set; } = string.Empty;
        public string TransferCode { get; set; } = string.Empty;
    }
}

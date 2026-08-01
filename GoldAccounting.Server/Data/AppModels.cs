using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace GoldAccounting.Server.Data
{
    // ---------- تنظیمات (settings) ----------
    public class Setting
    {
        [Key]
        public string K { get; set; } = "";
        public string V { get; set; } = "";
    }

    // ---------- نرخ روز طلا (rates) ----------
    public class Rate
    {
        [Key]
        public int Id { get; set; }
        public long Ts { get; set; }
        public string DateJ { get; set; } = "";
        public long RateVal { get; set; }
    }

    // ---------- کدینگ‌ها (defs) ----------
    public class Def
    {
        [Key]
        public int Id { get; set; }
        public string Kind { get; set; } = "";
        public string Name { get; set; } = "";
        public long X1 { get; set; }
        public long X2 { get; set; }
        public string X3 { get; set; } = "";
        public long Cts { get; set; }
    }

    // ---------- سند مرکزی (docs + doc_rows) ----------
    public class Doc
    {
        [Key]
        public int Id { get; set; }
        public long Ts { get; set; }
        public string DateJ { get; set; } = "";
        public string Descr { get; set; } = "";
        public long UpdTs { get; set; }
        public List<DocRow> Rows { get; set; } = new();
    }

    public class DocRow
    {
        [Key]
        public int Id { get; set; }
        public int DocId { get; set; }
        public int Seq { get; set; }
        public string Txt { get; set; } = "";
    }

    // ---------- گردش حساب مشتری (customer_tx) ----------
    public class CustomerTx
    {
        [Key]
        public int Id { get; set; }
        public int Cid { get; set; }
        public long Ts { get; set; }
        public string DateJ { get; set; } = "";
        public long Cash { get; set; }
        public long Goldmw { get; set; }
        public string Descr { get; set; } = "";
        public long Iid { get; set; }
    }

    // ---------- اتیکت (etiket) ----------
    public class Etiket
    {
        [Key]
        public int Id { get; set; }
        public string Code { get; set; } = "";
        public string Name { get; set; } = "";
        public long Wmw { get; set; }
        public int ItemId { get; set; }
        public string Photo { get; set; } = "";
        public string Mezane { get; set; } = "";
        public string Rfid { get; set; } = "";
        public long UpdatedTs { get; set; }
        public long Cts { get; set; }
        public string Status { get; set; } = "stock"; // stock | sold | out
    }
}

namespace GoldAccounting.Server.Data
{
    // ---------- جدول ثبت اسکریپت‌های TSQL (DB-First Resource Runner) ----------
    public class ResourceLog
    {
        [Key]
        public int Id { get; set; }
        public string FileName { get; set; } = "";
        public bool Success { get; set; }
        public DateTime ExecuteTime { get; set; }
        public int ExecuteMs { get; set; }
        public string? ErrorText { get; set; }
    }
}

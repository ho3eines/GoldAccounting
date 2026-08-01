using Microsoft.AspNetCore.Mvc;
using GoldAccounting.Server.Data;
using GoldAccounting.Server.Services;
using System.Text.Json;
using System.Text.Json.Nodes;

namespace GoldAccounting.Server.Controllers
{
    /// <summary>پشتیبان‌گیری و بازیابی کامل داده‌ها — فرمت JSON سازگار با بکاپ اندروید</summary>
    [ApiController]
    [Route("api/[controller]")]
    public class BackupController : ControllerBase
    {
        private readonly AppDbContext _db;
        private readonly EventService _events;

        public BackupController(AppDbContext db, EventService events) { _db = db; _events = events; }

        [HttpGet("download")]
        public IActionResult Download()
        {
            _events.LogBackup("download", "دانلود فایل پشتیبان");
            var root = new JsonObject
            {
                ["app"] = "talayar-backup",
                ["version"] = 1,
                ["date"] = Talayar.Jal.Today()
            };

            root["settings"] = JsonSerializer.SerializeToNode(_db.Settings.ToList().Select(s => new { s.K, s.V }));
            root["rates"] = JsonSerializer.SerializeToNode(_db.Rates.ToList().Select(r => new { r.Id, r.Ts, date_j = r.DateJ, rate = r.RateVal }));
            root["customers"] = JsonSerializer.SerializeToNode(_db.Customers.ToList().Select(c => new { c.Id, c.Code, c.Name, c.Phone, c.Note, c.Cts, c.Grp, c.Address }));
            root["customer_tx"] = JsonSerializer.SerializeToNode(_db.CustomerTxs.ToList().Select(c => new { c.Id, c.Cid, c.Ts, date_j = c.DateJ, c.Cash, c.Goldmw, c.Descr, c.Iid }));
            root["items"] = JsonSerializer.SerializeToNode(_db.Items.ToList().Select(i => new { i.Id, i.Code, i.Name, i.Karat, i.Wmw, i.WType, i.WVal, stone_mw = i.StoneMw, stoneval = i.StoneVal, i.Descr, i.Status, i.Cts }));
            root["invoices"] = JsonSerializer.SerializeToNode(_db.Invoices.ToList().Select(i => new { i.Id, i.Ts, date_j = i.DateJ, i.Cid, i.Cname, i.Rate, i.GoldVal, i.Wage, i.Stone, i.Tax, i.Total, i.Pcash, i.PgoldMw, i.PgoldVal, i.PgoldKarat, i.Debt, i.Note }));
            root["invoice_lines"] = JsonSerializer.SerializeToNode(_db.InvoiceLines.ToList().Select(l => new { l.Id, iid = l.Iid, item_id = l.ItemId, l.Title, l.Karat, l.Wmw, l.Unit, l.Wage, l.Stone, l.Tax, l.Total }));
            root["cash_tx"] = JsonSerializer.SerializeToNode(_db.CashTransactions.ToList().Select(c => new { c.Id, c.Ts, date_j = c.DateJ, c.Kind, c.Amount, c.Descr, c.Iid }));
            root["gold_tx"] = JsonSerializer.SerializeToNode(_db.GoldTransactions.ToList().Select(g => new { g.Id, g.Ts, date_j = g.DateJ, g.Kind, g.Wmw, g.Karat, g.Descr, g.Cid, g.DocId }));
            root["defs"] = JsonSerializer.SerializeToNode(_db.Defs.ToList().Select(d => new { d.Id, d.Kind, d.Name, d.X1, d.X2, d.X3, d.Cts }));
            root["docs"] = JsonSerializer.SerializeToNode(_db.Docs.ToList().Select(d => new { d.Id, d.Ts, date_j = d.DateJ, d.Descr, upd_ts = d.UpdTs }));
            root["doc_rows"] = JsonSerializer.SerializeToNode(_db.DocRows.ToList().Select(r => new { r.Id, doc_id = r.DocId, r.Seq, txt = r.Txt }));
            root["assets_ledger"] = JsonSerializer.SerializeToNode(_db.AssetLedgers.ToList().Select(a => new { a.Id, doc_id = a.DocId, a.Ts, date_j = a.DateJ, a.Scope, a.Asset, a.Qty, a.Karat, a.Cid, a.Descr }));
            root["bank_tx"] = JsonSerializer.SerializeToNode(_db.BankTransactions.ToList().Select(b => new { b.Id, b.Ts, date_j = b.DateJ, bank_id = b.BankId, b.Kind, b.Amount, b.Descr, b.Cid, doc_id = b.DocId }));
            root["checks"] = JsonSerializer.SerializeToNode(_db.CheckTransactions.ToList().Select(c => new { c.Id, c.Ts, date_j = c.DateJ, due_j = c.DueJ, c.Amount, bank_id = c.BankId, c.Cid, c.Cname, c.Kind, c.No, c.Status, c.Descr, doc_id = c.DocId }));
            root["prices"] = JsonSerializer.SerializeToNode(_db.MarketPrices.ToList().Select(p => new { p.Id, p.Ts, p.Key, p.Val }));
            root["etiket"] = JsonSerializer.SerializeToNode(_db.Etikets.ToList().Select(e => new { e.Id, e.Code, e.Name, e.Wmw, item_id = e.ItemId, e.Photo, e.Mezane, e.Rfid, updated_ts = e.UpdatedTs, e.Cts, e.Status }));

            byte[] bytes = System.Text.Encoding.UTF8.GetBytes(root.ToJsonString(new JsonSerializerOptions { WriteIndented = true }));
            string name = "talayar-backup-" + Talayar.Jal.Today().Replace("/", "") + ".json";
            return File(bytes, "application/json", name);
        }

        [HttpPost("restore")]
        public async Task<IActionResult> Restore()
        {
            try
            {
                _events.LogBackup("restore", "بازیابی از فایل پشتیبان");
                using var reader = new StreamReader(Request.Body);
                string body = await reader.ReadToEndAsync();
                using var doc = JsonDocument.Parse(body);
                var root = doc.RootElement;

                // حذف داده‌های فعلی (با ترتیب امن)
                _db.DocRows.RemoveRange(_db.DocRows);
                _db.Docs.RemoveRange(_db.Docs);
                _db.AssetLedgers.RemoveRange(_db.AssetLedgers);
                _db.BankTransactions.RemoveRange(_db.BankTransactions);
                _db.CheckTransactions.RemoveRange(_db.CheckTransactions);
                _db.GoldTransactions.RemoveRange(_db.GoldTransactions);
                _db.CustomerTxs.RemoveRange(_db.CustomerTxs);
                _db.CashTransactions.RemoveRange(_db.CashTransactions);
                _db.InvoiceLines.RemoveRange(_db.InvoiceLines);
                _db.Invoices.RemoveRange(_db.Invoices);
                _db.Items.RemoveRange(_db.Items);
                _db.Customers.RemoveRange(_db.Customers);
                _db.Rates.RemoveRange(_db.Rates);
                _db.Defs.RemoveRange(_db.Defs);
                _db.Etikets.RemoveRange(_db.Etikets);
                _db.MarketPrices.RemoveRange(_db.MarketPrices);
                _db.Settings.RemoveRange(_db.Settings);
                _db.SaveChanges();

                int n = 0;

                if (root.TryGetProperty("settings", out var arr) && arr.ValueKind == JsonValueKind.Array)
                    foreach (var el in arr.EnumerateArray())
                    {
                        _db.Settings.Add(new Setting { K = S(el, "k"), V = S(el, "v") });
                        n++;
                    }

                if (root.TryGetProperty("rates", out arr) && arr.ValueKind == JsonValueKind.Array)
                    foreach (var el in arr.EnumerateArray())
                    {
                        _db.Rates.Add(new Rate { Id = I(el, "id"), Ts = L(el, "ts"), DateJ = S(el, "date_j"), RateVal = L(el, "rate") });
                        n++;
                    }

                if (root.TryGetProperty("customers", out arr) && arr.ValueKind == JsonValueKind.Array)
                    foreach (var el in arr.EnumerateArray())
                    {
                        _db.Customers.Add(new Customer
                        {
                            Id = I(el, "id"),
                            Code = (int)L(el, "code"),
                            Name = S(el, "name"),
                            Phone = S(el, "phone"),
                            Note = S(el, "note"),
                            Grp = S(el, "grp"),
                            Address = S(el, "address"),
                            Cts = L(el, "cts")
                        });
                        n++;
                    }

                if (root.TryGetProperty("customer_tx", out arr) && arr.ValueKind == JsonValueKind.Array)
                    foreach (var el in arr.EnumerateArray())
                    {
                        _db.CustomerTxs.Add(new CustomerTx
                        {
                            Id = I(el, "id"),
                            Cid = I(el, "cid"),
                            Ts = L(el, "ts"),
                            DateJ = S(el, "date_j"),
                            Cash = L(el, "cash"),
                            Goldmw = L(el, "goldmw"),
                            Descr = S(el, "descr"),
                            Iid = L(el, "iid")
                        });
                        n++;
                    }

                if (root.TryGetProperty("items", out arr) && arr.ValueKind == JsonValueKind.Array)
                    foreach (var el in arr.EnumerateArray())
                    {
                        _db.Items.Add(new Item
                        {
                            Id = I(el, "id"),
                            Code = L(el, "code"),
                            Name = S(el, "name"),
                            Karat = I(el, "karat"),
                            Wmw = L(el, "wmw"),
                            WType = I(el, "wtype"),
                            WVal = L(el, "wval"),
                            StoneMw = L(el, "stone_mw"),
                            StoneVal = L(el, "stoneval"),
                            Descr = S(el, "descr"),
                            Status = S(el, "status").Length > 0 ? S(el, "status") : "stock",
                            Cts = L(el, "cts")
                        });
                        n++;
                    }

                if (root.TryGetProperty("invoices", out arr) && arr.ValueKind == JsonValueKind.Array)
                    foreach (var el in arr.EnumerateArray())
                    {
                        _db.Invoices.Add(new Invoice
                        {
                            Id = I(el, "id"),
                            Ts = L(el, "ts"),
                            DateJ = S(el, "date_j"),
                            Cid = I(el, "cid"),
                            Cname = S(el, "cname"),
                            Rate = L(el, "rate"),
                            GoldVal = L(el, "goldval"),
                            Wage = L(el, "wage"),
                            Stone = L(el, "stone"),
                            Tax = L(el, "tax"),
                            Total = L(el, "total"),
                            Pcash = L(el, "pcash"),
                            PgoldMw = L(el, "pgold_mw"),
                            PgoldVal = L(el, "pgold_val"),
                            PgoldKarat = I(el, "pgold_karat"),
                            Debt = L(el, "debt"),
                            Note = S(el, "note"),
                            TransferCode = "restore-" + (el.TryGetProperty("id", out var idEl) ? idEl.ToString() : Guid.NewGuid().ToString())
                        });
                        n++;
                    }

                if (root.TryGetProperty("invoice_lines", out arr) && arr.ValueKind == JsonValueKind.Array)
                    foreach (var el in arr.EnumerateArray())
                    {
                        _db.InvoiceLines.Add(new InvoiceLine
                        {
                            Id = I(el, "id"),
                            Iid = I(el, "iid"),
                            ItemId = I(el, "item_id"),
                            Title = S(el, "title"),
                            Karat = I(el, "karat"),
                            Wmw = L(el, "wmw"),
                            Unit = L(el, "unit"),
                            Wage = L(el, "wage"),
                            Stone = L(el, "stone"),
                            Tax = L(el, "tax"),
                            Total = L(el, "total")
                        });
                        n++;
                    }

                if (root.TryGetProperty("cash_tx", out arr) && arr.ValueKind == JsonValueKind.Array)
                    foreach (var el in arr.EnumerateArray())
                    {
                        _db.CashTransactions.Add(new CashTransaction
                        {
                            Id = I(el, "id"),
                            Ts = L(el, "ts"),
                            DateJ = S(el, "date_j"),
                            Kind = S(el, "kind"),
                            Amount = L(el, "amount"),
                            Descr = S(el, "descr"),
                            Iid = I(el, "iid")
                        });
                        n++;
                    }

                if (root.TryGetProperty("gold_tx", out arr) && arr.ValueKind == JsonValueKind.Array)
                    foreach (var el in arr.EnumerateArray())
                    {
                        _db.GoldTransactions.Add(new GoldTransactionEntity
                        {
                            Id = I(el, "id"),
                            Ts = L(el, "ts"),
                            DateJ = S(el, "date_j"),
                            Kind = S(el, "kind"),
                            Wmw = L(el, "wmw"),
                            Karat = I(el, "karat"),
                            Descr = S(el, "descr"),
                            Cid = I(el, "cid"),
                            DocId = I(el, "doc_id"),
                            TransferCode = "restore-g-" + (el.TryGetProperty("id", out var idEl2) ? idEl2.ToString() : Guid.NewGuid().ToString())
                        });
                        n++;
                    }

                if (root.TryGetProperty("defs", out arr) && arr.ValueKind == JsonValueKind.Array)
                    foreach (var el in arr.EnumerateArray())
                    {
                        _db.Defs.Add(new Def
                        {
                            Id = I(el, "id"),
                            Kind = S(el, "kind"),
                            Name = S(el, "name"),
                            X1 = L(el, "x1"),
                            X2 = L(el, "x2"),
                            X3 = S(el, "x3"),
                            Cts = L(el, "cts")
                        });
                        n++;
                    }

                if (root.TryGetProperty("docs", out arr) && arr.ValueKind == JsonValueKind.Array)
                    foreach (var el in arr.EnumerateArray())
                    {
                        _db.Docs.Add(new Doc
                        {
                            Id = I(el, "id"),
                            Ts = L(el, "ts"),
                            DateJ = S(el, "date_j"),
                            Descr = S(el, "descr"),
                            UpdTs = L(el, "upd_ts")
                        });
                        n++;
                    }

                if (root.TryGetProperty("doc_rows", out arr) && arr.ValueKind == JsonValueKind.Array)
                    foreach (var el in arr.EnumerateArray())
                    {
                        _db.DocRows.Add(new DocRow
                        {
                            Id = I(el, "id"),
                            DocId = I(el, "doc_id"),
                            Seq = I(el, "seq"),
                            Txt = S(el, "txt")
                        });
                        n++;
                    }

                if (root.TryGetProperty("assets_ledger", out arr) && arr.ValueKind == JsonValueKind.Array)
                    foreach (var el in arr.EnumerateArray())
                    {
                        _db.AssetLedgers.Add(new AssetLedgerEntity
                        {
                            Id = I(el, "id"),
                            DocId = I(el, "doc_id"),
                            Ts = L(el, "ts"),
                            DateJ = S(el, "date_j"),
                            Scope = S(el, "scope"),
                            Asset = S(el, "asset"),
                            Qty = D(el, "qty"),
                            Karat = I(el, "karat"),
                            Cid = I(el, "cid"),
                            Descr = S(el, "descr"),
                            TransferCode = "restore-a-" + (el.TryGetProperty("id", out var idEl3) ? idEl3.ToString() : Guid.NewGuid().ToString())
                        });
                        n++;
                    }

                if (root.TryGetProperty("bank_tx", out arr) && arr.ValueKind == JsonValueKind.Array)
                    foreach (var el in arr.EnumerateArray())
                    {
                        _db.BankTransactions.Add(new BankTransaction
                        {
                            Id = I(el, "id"),
                            Ts = L(el, "ts"),
                            DateJ = S(el, "date_j"),
                            BankId = I(el, "bank_id"),
                            Kind = S(el, "kind"),
                            Amount = L(el, "amount"),
                            Descr = S(el, "descr"),
                            Cid = I(el, "cid"),
                            DocId = I(el, "doc_id"),
                            TransferCode = "restore-b-" + (el.TryGetProperty("id", out var idEl4) ? idEl4.ToString() : Guid.NewGuid().ToString())
                        });
                        n++;
                    }

                if (root.TryGetProperty("checks", out arr) && arr.ValueKind == JsonValueKind.Array)
                    foreach (var el in arr.EnumerateArray())
                    {
                        _db.CheckTransactions.Add(new CheckTransaction
                        {
                            Id = I(el, "id"),
                            Ts = L(el, "ts"),
                            DateJ = S(el, "date_j"),
                            DueJ = S(el, "due_j"),
                            Amount = L(el, "amount"),
                            BankId = I(el, "bank_id"),
                            Cid = I(el, "cid"),
                            Cname = S(el, "cname"),
                            Kind = S(el, "kind"),
                            No = S(el, "no"),
                            Status = S(el, "status").Length > 0 ? S(el, "status") : "open",
                            Descr = S(el, "descr"),
                            DocId = I(el, "doc_id"),
                            TransferCode = "restore-c-" + (el.TryGetProperty("id", out var idEl5) ? idEl5.ToString() : Guid.NewGuid().ToString())
                        });
                        n++;
                    }

                if (root.TryGetProperty("prices", out arr) && arr.ValueKind == JsonValueKind.Array)
                    foreach (var el in arr.EnumerateArray())
                    {
                        _db.MarketPrices.Add(new MarketPrice
                        {
                            Id = I(el, "id"),
                            Ts = L(el, "ts"),
                            Key = S(el, "key"),
                            Val = L(el, "val")
                        });
                        n++;
                    }

                if (root.TryGetProperty("etiket", out arr) && arr.ValueKind == JsonValueKind.Array)
                    foreach (var el in arr.EnumerateArray())
                    {
                        _db.Etikets.Add(new Etiket
                        {
                            Id = I(el, "id"),
                            Code = S(el, "code"),
                            Name = S(el, "name"),
                            Wmw = L(el, "wmw"),
                            ItemId = I(el, "item_id"),
                            Photo = S(el, "photo"),
                            Mezane = S(el, "mezane"),
                            Rfid = S(el, "rfid"),
                            UpdatedTs = L(el, "updated_ts"),
                            Cts = L(el, "cts"),
                            Status = S(el, "status").Length > 0 ? S(el, "status") : "stock"
                        });
                        n++;
                    }

                _db.SaveChanges();
                return Ok(new { status = "success", restored = n, message = "بازیابی کامل انجام شد ✓" });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { status = "error", message = ex.Message });
            }
        }

        static string S(JsonElement el, string key) =>
            el.TryGetProperty(key, out var v) && v.ValueKind == JsonValueKind.String ? v.GetString() ?? "" : "";
        static long L(JsonElement el, string key)
        {
            if (!el.TryGetProperty(key, out var v)) return 0;
            switch (v.ValueKind)
            {
                case JsonValueKind.Number:
                    return v.TryGetInt64(out long n) ? n : (long)v.GetDouble();
                case JsonValueKind.String:
                    return long.TryParse(v.GetString(), out long r) ? r : 0;
            }
            return 0;
        }
        static int I(JsonElement el, string key) => (int)L(el, key);
        static double D(JsonElement el, string key)
        {
            if (!el.TryGetProperty(key, out var v)) return 0;
            switch (v.ValueKind)
            {
                case JsonValueKind.Number:
                    return v.TryGetDouble(out double d) ? d : 0;
                case JsonValueKind.String:
                    return double.TryParse(v.GetString(), out double r) ? r : 0;
            }
            return 0;
        }
    }
}

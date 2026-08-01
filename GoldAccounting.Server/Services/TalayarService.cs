using GoldAccounting.Server.Data;
using Microsoft.EntityFrameworkCore;

namespace GoldAccounting.Server.Services
{
    // ---------- مدل‌های ورودی ----------
    public class InvoiceLineDraft
    {
        public int ItemId { get; set; }       // 0 = دستی
        public string Title { get; set; } = "";
        public int Karat { get; set; } = 750;
        public long Wmw { get; set; }         // میلی‌گرم
        public int Wtype { get; set; }        // 0=%  1=تومان/گرم  2=ثابت
        public long Wval { get; set; }
        public long Stone { get; set; }
        // محاسبه‌شده
        public long Unit { get; set; }
        public long GoldVal { get; set; }
        public long Wage { get; set; }
        public long Tax { get; set; }
        public long Total { get; set; }
    }

    public class InvoiceDraft
    {
        public string DateJ { get; set; } = "";
        public int Cid { get; set; }
        public string Cname { get; set; } = "";
        public long Rate { get; set; }
        public List<InvoiceLineDraft> Lines { get; set; } = new();
        public long Pcash { get; set; }
        public long PgoldMw { get; set; }
        public long PgoldVal { get; set; }
        public int PgoldKarat { get; set; }
        public long Debt { get; set; }
        public string Note { get; set; } = "";
    }

    public class DocDraft
    {
        public string Type { get; set; } = "";
        public string DateJ { get; set; } = "";
        public int Cid { get; set; }
        public string Cname { get; set; } = "";
        public int Karat { get; set; } = 750;
        public int DefId { get; set; }
        public string DefName { get; set; } = "";
        public int BankId { get; set; }
        public string BankName { get; set; } = "";
        public long Wmg { get; set; }
        public long Count { get; set; }
        public double QtyCur { get; set; }
        public long Money { get; set; }
        public string CheckNo { get; set; } = "";
        public string CheckDue { get; set; } = "";
        public int Settle { get; set; }        // 0=به حساب 1=نقدی 2=بانکی
        public int EtiketId { get; set; }
        public string Note { get; set; } = "";
    }

    /// <summary>موتور کامل حسابداری طلایار — پورت Db.java + Post.java + عملیات ثبت/ابطال اندروید</summary>
    public class TalayarService
    {
        private readonly AppDbContext db;

        public TalayarService(AppDbContext db) { this.db = db; }

        // ---------- settings ----------
        public string GetS(string k, string def)
        {
            var s = db.Settings.FirstOrDefault(x => x.K == k);
            return s != null ? s.V : def;
        }
        public void SetS(string k, string v)
        {
            var s = db.Settings.FirstOrDefault(x => x.K == k);
            if (s != null) s.V = v;
            else db.Settings.Add(new Setting { K = k, V = v });
            db.SaveChanges();
        }
        public long GetL(string k, long def)
        {
            var s = db.Settings.FirstOrDefault(x => x.K == k);
            if (s == null) return def;
            return long.TryParse(s.V, out long v) ? v : def;
        }

        // ---------- rate ----------
        public long CurrentRate()
        {
            var r = db.Rates.OrderByDescending(x => x.Ts).ThenByDescending(x => x.Id).FirstOrDefault();
            return r != null ? r.RateVal : 0;
        }

        // ---------- صندوق ----------
        public (long inflow, long outflow) CashInOut()
        {
            long inflow = db.CashTransactions.Where(c => c.Kind == "in").Sum(c => (long?)c.Amount) ?? 0;
            long outflow = db.CashTransactions.Where(c => c.Kind == "out").Sum(c => (long?)c.Amount) ?? 0;
            return (inflow, outflow);
        }
        public long CashBalance() { var p = CashInOut(); return p.inflow - p.outflow; }

        // ---------- اجناس ----------
        public (long count, long wmw) ItemsStock()
        {
            var list = db.Items.Where(i => i.Status == "stock").ToList();
            return (list.Count, list.Sum(i => i.Wmw));
        }

        // ---------- طلای آبشده ----------
        public (long raw, long eq) GoldBalance()
        {
            long raw = 0, eq = 0;
            foreach (var g in db.GoldTransactions.ToList())
            {
                long sign = g.Kind == "out" ? -1 : 1;
                raw += sign * g.Wmw;
                eq += sign * Talayar.Equiv750(g.Wmw, g.Karat);
            }
            return (raw, eq);
        }

        // ---------- بدهی مشتریان ----------
        public (long cash, long gold) CustomerDebts()
        {
            long cash = db.CustomerTxs.Sum(c => (long?)c.Cash) ?? 0;
            long gold = db.CustomerTxs.Sum(c => (long?)c.Goldmw) ?? 0;
            return (cash, gold);
        }
        public (long debtC, long credC, long debtG, long credG) CustomerDebtSplit()
        {
            long debtC = 0, credC = 0, debtG = 0, credG = 0;
            foreach (var g in db.CustomerTxs.GroupBy(c => c.Cid).ToList())
            {
                long cs = g.Sum(x => x.Cash), gl = g.Sum(x => x.Goldmw);
                if (cs > 0) debtC += cs; else credC += -cs;
                if (gl > 0) debtG += gl; else credG += -gl;
            }
            return (debtC, credC, debtG, credG);
        }

        // ---------- آمار فروش ----------
        public long[] SalesStats(string likePrefix)
        {
            var list = db.Invoices.Where(i => i.DateJ.StartsWith(likePrefix)).ToList();
            return new long[]
            {
                list.Count,
                list.Sum(i => i.Total),
                list.Sum(i => i.Wage),
                list.Sum(i => i.Tax),
                list.Sum(i => i.Pcash),
                list.Sum(i => i.Debt)
            };
        }

        // ---------- بانک ----------
        public long BankBalance(int bankId)
        {
            long inflow = db.BankTransactions.Where(b => b.BankId == bankId && b.Kind == "in").Sum(b => (long?)b.Amount) ?? 0;
            long outflow = db.BankTransactions.Where(b => b.BankId == bankId && b.Kind == "out").Sum(b => (long?)b.Amount) ?? 0;
            return inflow - outflow;
        }
        public long BanksTotal()
        {
            long inflow = db.BankTransactions.Where(b => b.Kind == "in").Sum(b => (long?)b.Amount) ?? 0;
            long outflow = db.BankTransactions.Where(b => b.Kind == "out").Sum(b => (long?)b.Amount) ?? 0;
            return inflow - outflow;
        }

        // ---------- دارایی generic ----------
        public double StockOf(string asset)
        {
            return db.AssetLedgers.Where(a => a.Scope == "stock" && a.Asset == asset).Sum(a => (double?)a.Qty) ?? 0;
        }

        // ---------- موتور ثبت اسناد (Post.java) ----------
        public long PostDoc(string dateJ, string descr)
        {
            long ts = DateTimeOffset.Now.ToUnixTimeMilliseconds();
            var d = new Doc { Ts = ts, DateJ = dateJ, Descr = descr, UpdTs = ts };
            db.Docs.Add(d);
            db.SaveChanges();
            return d.Id;
        }
        public void PostLine(long docId, int seq, string txt)
        {
            db.DocRows.Add(new DocRow { DocId = (int)docId, Seq = seq, Txt = txt });
        }
        public void PostCash(long docId, string dateJ, string kind, long amount, string descr)
        {
            db.CashTransactions.Add(new CashTransaction
            {
                Ts = DateTimeOffset.Now.ToUnixTimeMilliseconds(),
                DateJ = dateJ,
                Kind = kind,
                Amount = amount,
                Descr = descr,
                Iid = (int)(-docId)
            });
        }
        public void PostGold(long docId, string dateJ, string kind, long wmg, int karat, string descr, int cid)
        {
            db.GoldTransactions.Add(new GoldTransactionEntity
            {
                Ts = DateTimeOffset.Now.ToUnixTimeMilliseconds(),
                DateJ = dateJ,
                Kind = kind,
                Wmw = wmg,
                Karat = karat,
                Descr = descr,
                Cid = cid,
                DocId = (int)docId,
                TransferCode = "doc" + docId + "-g" + Guid.NewGuid().ToString("N").Substring(0, 8)
            });
        }
        public void PostCust(long docId, string dateJ, int cid, long cash, long goldmw, string descr)
        {
            if (cid <= 0) return;
            db.CustomerTxs.Add(new CustomerTx
            {
                Cid = cid,
                Ts = DateTimeOffset.Now.ToUnixTimeMilliseconds(),
                DateJ = dateJ,
                Cash = cash,
                Goldmw = goldmw,
                Descr = descr,
                Iid = -docId
            });
        }
        public void PostBank(string dateJ, int bankId, string kind, long amount, string descr, int cid, long docId)
        {
            if (bankId <= 0) return;
            db.BankTransactions.Add(new BankTransaction
            {
                Ts = DateTimeOffset.Now.ToUnixTimeMilliseconds(),
                DateJ = dateJ,
                BankId = bankId,
                Kind = kind,
                Amount = amount,
                Descr = descr,
                Cid = cid,
                DocId = (int)docId,
                TransferCode = "doc" + docId + "-b" + Guid.NewGuid().ToString("N").Substring(0, 8)
            });
        }
        public void PostCheck(string dateJ, string dueJ, long amount, int bankId, int cid, string cname,
                              string kind, string no, string descr, long docId)
        {
            db.CheckTransactions.Add(new CheckTransaction
            {
                Ts = DateTimeOffset.Now.ToUnixTimeMilliseconds(),
                DateJ = dateJ,
                DueJ = dueJ,
                Amount = amount,
                BankId = bankId,
                Cid = cid,
                Cname = cname,
                Kind = kind,
                No = no,
                Status = "open",
                Descr = descr,
                DocId = (int)docId,
                TransferCode = "doc" + docId + "-c" + Guid.NewGuid().ToString("N").Substring(0, 8)
            });
        }
        public void PostAsset(long docId, string dateJ, string scope, string asset, double qty, int karat, int cid, string descr)
        {
            db.AssetLedgers.Add(new AssetLedgerEntity
            {
                DocId = (int)docId,
                Ts = DateTimeOffset.Now.ToUnixTimeMilliseconds(),
                DateJ = dateJ,
                Scope = scope,
                Asset = asset,
                Qty = qty,
                Karat = karat,
                Cid = cid,
                Descr = descr,
                TransferCode = "doc" + docId + "-a" + Guid.NewGuid().ToString("N").Substring(0, 8)
            });
        }

        public string AssetName(string asset)
        {
            if (asset.StartsWith("work")) return "کارساخته (وزنی)";
            int id = ParseAssetId(asset);
            if (id > 0)
            {
                var d = db.Defs.FirstOrDefault(x => x.Id == id);
                if (d != null) return d.Name;
            }
            return asset;
        }
        static int ParseAssetId(string asset)
        {
            int i = asset.LastIndexOf("_d");
            if (i < 0) return 0;
            return int.TryParse(asset.Substring(i + 2), out int v) ? v : 0;
        }
        public string AssetUnit(string asset)
        {
            if (asset.StartsWith("coin") || asset.StartsWith("bull")) return "عدد";
            if (asset.StartsWith("cur") || asset.StartsWith("sil") || asset.StartsWith("work")) return "گرم/واحد";
            return "";
        }
        public string FmtQty(string asset, double qty)
        {
            if (asset.StartsWith("coin") || asset.StartsWith("bull")) return Talayar.Dig((long)Math.Round(qty)) + " عدد";
            if (asset.StartsWith("work") || asset.StartsWith("sil")) return Talayar.Gs((long)Math.Round(qty));
            if (asset.StartsWith("cur")) return Talayar.Dig((((long)Math.Round(qty * 100)) / 100.0).ToString());
            return Talayar.Dig((long)Math.Round(qty));
        }

        // ---------- محاسبات فاکتور ----------
        public static long CalcWage(long goldVal, long wmw, int wtype, long wval)
        {
            if (wtype == 0) return (long)Math.Round(goldVal * wval / 100.0);
            if (wtype == 1) return (long)Math.Round(wval * wmw / 1000.0);
            return wval;
        }

        public static void ComputeLine(InvoiceLineDraft ln, long rate, long taxPct)
        {
            ln.Unit = (long)Math.Round(rate * ln.Karat / 750.0);
            ln.GoldVal = (long)Math.Round(ln.Wmw * ln.Unit / 1000.0);
            ln.Wage = CalcWage(ln.GoldVal, ln.Wmw, ln.Wtype, ln.Wval);
            if (ln.Title != null && ln.Title.Contains("ابشده")) ln.Tax = 0;
            else ln.Tax = (long)Math.Round(ln.Wage * taxPct / 100.0);
            ln.Total = ln.GoldVal + ln.Wage + ln.Stone + ln.Tax;
        }

        public (long gold, long wage, long stone, long tax, long total) InvoiceTotals(List<InvoiceLineDraft> lines)
        {
            long rate = CurrentRate();
            long taxPct = GetL("tax", 10);
            long gold = 0, wage = 0, stone = 0, tax = 0, total = 0;
            foreach (var ln in lines)
            {
                ComputeLine(ln, rate, taxPct);
                gold += ln.GoldVal; wage += ln.Wage; stone += ln.Stone; tax += ln.Tax; total += ln.Total;
            }
            return (gold, wage, stone, tax, total);
        }

        /// <summary>ارزش طلای دریافتی از مشتری با رسوب</summary>
        public (long w, long eq, long val) PayEval(long wmg, int karat, double rasub)
        {
            long rate = CurrentRate();
            long eq = Talayar.Equiv750(wmg, karat);
            long val = (long)Math.Round(wmg * (rate * karat / 750.0) / 1000.0 * (100 - rasub) / 100.0);
            return (wmg, eq, val);
        }

        // ---------- ثبت فاکتور ----------
        public long SaveInvoice(InvoiceDraft d)
        {
            long ts = DateTimeOffset.Now.ToUnixTimeMilliseconds();
            var inv = new Invoice
            {
                Ts = ts,
                DateJ = d.DateJ,
                Cid = d.Cid,
                Cname = d.Cname,
                Rate = d.Rate,
                GoldVal = d.Lines.Sum(l => l.GoldVal),
                Wage = d.Lines.Sum(l => l.Wage),
                Stone = d.Lines.Sum(l => l.Stone),
                Tax = d.Lines.Sum(l => l.Tax),
                Total = d.Lines.Sum(l => l.Total),
                Pcash = d.Pcash,
                PgoldMw = d.PgoldMw,
                PgoldVal = d.PgoldVal,
                PgoldKarat = d.PgoldKarat,
                Debt = d.Debt,
                Note = d.Note,
                TransferCode = "web-inv-" + ts + "-" + Guid.NewGuid().ToString("N").Substring(0, 6)
            };
            db.Invoices.Add(inv);
            db.SaveChanges();

            foreach (var ln in d.Lines)
            {
                db.InvoiceLines.Add(new InvoiceLine
                {
                    Iid = inv.Id,
                    ItemId = ln.ItemId,
                    Title = ln.Title,
                    Karat = ln.Karat,
                    Wmw = ln.Wmw,
                    Unit = ln.Unit,
                    Wage = ln.Wage,
                    Stone = ln.Stone,
                    Tax = ln.Tax,
                    Total = ln.Total
                });
                if (ln.ItemId > 0)
                {
                    var item = db.Items.FirstOrDefault(i => i.Id == ln.ItemId);
                    if (item != null) item.Status = "sold";
                }
            }
            if (d.Pcash > 0)
                db.CashTransactions.Add(new CashTransaction { Ts = ts, DateJ = d.DateJ, Kind = "in", Amount = d.Pcash, Descr = "دریافت نقدی بابت فاکتور شماره " + Talayar.Dig(inv.Id), Iid = inv.Id });
            if (d.PgoldMw > 0)
                db.GoldTransactions.Add(new GoldTransactionEntity { Ts = ts, DateJ = d.DateJ, Kind = "in", Wmw = d.PgoldMw, Karat = d.PgoldKarat > 0 ? d.PgoldKarat : 750, Descr = "طلا دریافتی از مشتری بابت فاکتور " + Talayar.Dig(inv.Id), Cid = d.Cid, DocId = 0, TransferCode = "web-inv-g-" + inv.Id });
            if (d.Debt != 0)
                db.CustomerTxs.Add(new CustomerTx { Cid = d.Cid, Ts = ts, DateJ = d.DateJ, Cash = d.Debt, Goldmw = 0, Descr = (d.Debt > 0 ? "مانده فاکتور شماره " : "اضافه‌پرداخت فاکتور شماره ") + Talayar.Dig(inv.Id), Iid = inv.Id });
            db.SaveChanges();
            return inv.Id;
        }

        // ---------- ابطال فاکتور (سند معکوس) ----------
        public void VoidInvoice(int iid)
        {
            var inv = db.Invoices.FirstOrDefault(i => i.Id == iid);
            if (inv == null) return;
            // برگشت اقلام به انبار
            foreach (var l in db.InvoiceLines.Where(l => l.Iid == iid).ToList())
            {
                if (l.ItemId > 0)
                {
                    var item = db.Items.FirstOrDefault(i => i.Id == l.ItemId);
                    if (item != null) item.Status = "stock";
                }
            }
            long ts = DateTimeOffset.Now.ToUnixTimeMilliseconds();
            string tj = Talayar.Jal.Today();
            if (inv.Pcash > 0)
                db.CashTransactions.Add(new CashTransaction { Ts = ts, DateJ = tj, Kind = "out", Amount = inv.Pcash, Descr = "ابطال فاکتور شماره " + Talayar.Dig(iid) + " (برگشت وجه نقد)", Iid = 0 });
            if (inv.PgoldMw > 0)
                db.GoldTransactions.Add(new GoldTransactionEntity { Ts = ts, DateJ = tj, Kind = "out", Wmw = inv.PgoldMw, Karat = inv.PgoldKarat > 0 ? inv.PgoldKarat : 750, Descr = "ابطال فاکتور " + Talayar.Dig(iid) + " (برگشت طلای دریافتی)", Cid = inv.Cid, DocId = 0, TransferCode = "web-void-g-" + iid });
            if (inv.Debt != 0)
                db.CustomerTxs.Add(new CustomerTx { Cid = inv.Cid, Ts = ts, DateJ = tj, Cash = -inv.Debt, Goldmw = 0, Descr = "ابطال فاکتور شماره " + Talayar.Dig(iid), Iid = iid });
            db.InvoiceLines.RemoveRange(db.InvoiceLines.Where(l => l.Iid == iid));
            db.Invoices.Remove(inv);
            db.SaveChanges();
        }

        // ---------- انواع سند مرکزی ----------
        public static readonly string[][] DOC_GROUPS = {
            new[] { "⚖️ آبشده و متفرقه (طلا)", "gold_buy,gold_sell,gold_in,gold_out,gold_talab,gold_bedehi" },
            new[] { "🪙 سکه", "coin_buy,coin_sell,coin_in,coin_out,coin_talab,coin_bedehi" },
            new[] { "🧱 شمش", "bull_buy,bull_sell,bull_in,bull_out,bull_talab,bull_bedehi" },
            new[] { "💵 ارز", "cur_buy,cur_sell,cur_talab,cur_bedehi" },
            new[] { "🥈 نقره", "sil_buy,sil_sell,sil_talab,sil_bedehi" },
            new[] { "💍 کارساخته", "work_buy,work_sell,work_in,work_out,work_talab,work_bedehi" },
            new[] { "💰 نقدی", "cash_income,cash_expense,cash_in,cash_out,cash_talab,cash_bedehi" },
            new[] { "🏦 بانکی", "bank_income,bank_expense,bank_recv,bank_pay" },
            new[] { "📄 چک", "check_recv,check_pay" },
            new[] { "🏷️ تخفیف", "disc_us,disc_cust" },
            new[] { "🏷 اتیکت", "etiket_buy,etiket_sell,etiket_in,etiket_out" },
        };

        public static readonly (string key, string label)[] DOC_TYPES = {
            ("gold_buy","خرید طلا/آبشده از مشتری"),("gold_sell","فروش طلا/آبشده به مشتری"),
            ("gold_in","ورود آبشده و متفرقه"),("gold_out","خروج آبشده و متفرقه"),
            ("gold_talab","طلب ما طلایی از مشتری (وزنی)"),("gold_bedehi","بدهی ما طلایی به مشتری (وزنی)"),
            ("coin_buy","خرید سکه"),("coin_sell","فروش سکه"),("coin_in","ورود سکه"),("coin_out","خروج سکه"),
            ("coin_talab","طلب ما سکه از مشتری"),("coin_bedehi","بدهی ما سکه به مشتری"),
            ("bull_buy","خرید شمش (تعدادی)"),("bull_sell","فروش شمش (تعدادی)"),
            ("bull_in","ورود شمش (تعدادی)"),("bull_out","خروج شمش (تعدادی)"),
            ("bull_talab","طلب ما شمش از مشتری"),("bull_bedehi","بدهی ما شمش به مشتری"),
            ("cur_buy","خرید ارز"),("cur_sell","فروش ارز"),
            ("cur_talab","طلب ما ارزی از مشتری"),("cur_bedehi","بدهی ما ارزی به مشتری"),
            ("sil_buy","خرید نقره"),("sil_sell","فروش نقره"),
            ("sil_talab","طلب ما نقره از مشتری"),("sil_bedehi","بدهی ما نقره به مشتری"),
            ("work_buy","خرید کارساخته"),("work_sell","فروش کارساخته"),
            ("work_in","ورود کارساخته"),("work_out","خروج کارساخته"),
            ("work_talab","طلب ما کارساخته از مشتری (وزنی)"),("work_bedehi","بدهی ما کارساخته به مشتری (وزنی)"),
            ("cash_income","درآمد نقدی"),("cash_expense","هزینه نقدی"),
            ("cash_in","ورود وجه نقد"),("cash_out","خروج وجه نقد"),
            ("cash_talab","طلب ما مالی از مشتری"),("cash_bedehi","بدهی ما مالی به مشتری"),
            ("bank_income","درآمد به بانک"),("bank_expense","هزینه از بانک"),
            ("bank_recv","دریافت از مشتری به بانک"),("bank_pay","پرداخت به مشتری از بانک"),
            ("check_recv","چک دریافتی (ورود چک)"),("check_pay","چک پرداختنی (خروج چک)"),
            ("disc_us","تخفیف ما به مشتری"),("disc_cust","تخفیف مشتری به ما"),
            ("etiket_buy","خرید اتیکت"),("etiket_sell","فروش اتیکت (خروج از انبار)"),
            ("etiket_in","ورود اتیکت به انبار"),("etiket_out","خروج اتیکت از انبار"),
        };

        public static string TypeLabel(string key)
        {
            foreach (var t in DOC_TYPES) if (t.key == key) return t.label;
            return key;
        }

        // ---------- ثبت سند مرکزی ----------
        public long SaveDoc(DocDraft d)
        {
            var lines = BuildDocLines(d);
            string note = d.Note ?? "";
            string descr = TypeLabel(d.Type) + (note.Length > 0 ? " — " + note : "");
            long docId = PostDoc(d.DateJ, descr);
            int seq = 1;
            foreach (var l in lines) { PostLine(docId, seq++, l); }
            ApplyEffects(docId, d);
            db.SaveChanges();
            return docId;
        }

        public List<string> BuildDocLines(DocDraft d)
        {
            var L = new List<string>();
            string who = d.Cname.Length > 0 ? d.Cname : "فروشگاه";
            string L1 = TypeLabel(d.Type);
            L.Add(L1 + (d.Cid > 0 ? " — طرف حساب: " + who : ""));
            if (d.Type.StartsWith("etiket") && d.EtiketId > 0)
            {
                var et = db.Etikets.FirstOrDefault(x => x.Id == d.EtiketId);
                if (et != null) L.Add("اتیکت #" + et.Id + " («" + Talayar.Dig(et.Code) + "») " + et.Name + " — " + Talayar.Gs(et.Wmw));
            }
            if (d.Type.StartsWith("gold") && d.Wmg > 0)
                L.Add(Talayar.Gs(d.Wmg) + " طلای " + Talayar.KaratName(d.Karat));
            if ((d.Type.StartsWith("coin") || d.Type.StartsWith("bull")) && d.Count > 0 && d.DefId > 0)
                L.Add(Talayar.Dig(d.Count) + " عدد " + d.DefName);
            if (d.Type.StartsWith("cur") && d.QtyCur > 0 && d.DefId > 0)
                L.Add(Talayar.Dig(d.QtyCur.ToString()) + " " + d.DefName);
            if ((d.Type.StartsWith("sil") || d.Type.StartsWith("work")) && d.Wmg > 0)
                L.Add(Talayar.Gs(d.Wmg) + (d.DefName.Length > 0 ? " " + d.DefName : "") + (d.Type.StartsWith("work") ? " • " + Talayar.KaratName(d.Karat) : ""));
            if (d.Money > 0 && NeedMoney(d.Type))
            {
                string t = "مبلغ: " + Talayar.Money(d.Money) + " تومان";
                if (NeedSettle(d.Type)) t += " — " + SettleTxt(d);
                L.Add(t);
            }
            if (d.Type.StartsWith("check") && d.CheckNo.Length > 0)
                L.Add("چک شماره " + Talayar.Dig(d.CheckNo) + " • سررسید " + d.CheckDue + (d.BankName.Length > 0 ? " • " + d.BankName : ""));
            if (d.Type.StartsWith("bank") && d.BankName.Length > 0) L.Add("بانک: " + d.BankName);
            if (d.Note.Length > 0) L.Add("شرح: " + d.Note);
            if (L.Count == 1) L.Add("…");
            return L;
        }

        static string SettleTxt(DocDraft d) =>
            d.Settle == 0 ? "به حساب مشتری" : d.Settle == 1 ? "نقدی از صندوق" : "بانکی" + (d.BankName.Length > 0 ? " (" + d.BankName + ")" : "");

        static bool OneOf(string type, params string[] ks)
        {
            foreach (var k in ks) if (type == k) return true;
            return false;
        }

        public static bool NeedCustomer(string type) =>
            !OneOf(type, "gold_in", "gold_out", "coin_in", "coin_out", "bull_in", "bull_out", "work_in", "work_out",
                   "cash_income", "cash_expense", "cash_in", "cash_out", "bank_income", "bank_expense", "etiket_in", "etiket_out");
        public static bool NeedEtiket(string type) => type.StartsWith("etiket");
        public static bool NeedWeight(string type) => type.StartsWith("gold") || type.StartsWith("sil") || type.StartsWith("work");
        public static bool NeedKarat(string type) => type.StartsWith("gold") || type.StartsWith("work");
        public static bool NeedDef(string type) => type.StartsWith("coin") || type.StartsWith("bull") || type.StartsWith("cur") || type.StartsWith("sil") || type.StartsWith("work");
        public static string DefKind(string type)
        {
            if (type.StartsWith("coin")) return "coin";
            if (type.StartsWith("bull")) return "bullion";
            if (type.StartsWith("cur")) return "curr";
            if (type.StartsWith("sil")) return "silver";
            if (type.StartsWith("work")) return "work";
            return "";
        }
        public static string DefTitle(string type)
        {
            if (type.StartsWith("coin")) return "نوع سکه";
            if (type.StartsWith("bull")) return "نوع شمش";
            if (type.StartsWith("cur")) return "نوع ارز";
            if (type.StartsWith("sil")) return "نوع نقره";
            if (type.StartsWith("work")) return "نام کار (کارساخته)";
            return "نوع";
        }
        public static bool NeedCount(string type) => type.StartsWith("coin") || type.StartsWith("bull");
        public static bool NeedQty(string type) => type.StartsWith("cur");
        public static bool NeedMoney(string type) =>
            OneOf(type, "gold_buy", "gold_sell", "coin_buy", "coin_sell", "bull_buy", "bull_sell", "cur_buy", "cur_sell",
                  "sil_buy", "sil_sell", "work_buy", "work_sell",
                  "cash_income", "cash_expense", "cash_in", "cash_out", "cash_talab", "cash_bedehi",
                  "bank_income", "bank_expense", "bank_recv", "bank_pay", "check_recv", "check_pay", "disc_us", "disc_cust",
                  "etiket_buy", "etiket_sell");
        public static bool NeedSettle(string type) =>
            OneOf(type, "gold_buy", "gold_sell", "coin_buy", "coin_sell", "bull_buy", "bull_sell", "cur_buy", "cur_sell",
                  "sil_buy", "sil_sell", "work_buy", "work_sell", "etiket_buy", "etiket_sell");
        public static bool NeedBank(string type, int settle) => type.StartsWith("bank") || type.StartsWith("check") || (NeedSettle(type) && settle == 2);
        public static bool NeedCheck(string type) => type.StartsWith("check");
        public static bool NeedWage(string type) => OneOf(type, "work_buy", "work_sell");
        public static bool NeedRateCol(string type) => OneOf(type, "cur_buy", "cur_sell");

        /// <summary>اثرات حسابداری هر نوع سند (پورت applyEffects اندروید)</summary>
        void ApplyEffects(long docId, DocDraft d)
        {
            string type = d.Type;
            int w = (int)d.Wmg;
            long nCnt = d.Count;
            double qCur = d.QtyCur;
            long m = d.Money;
            string who = d.Cname.Length > 0 ? d.Cname : "فروشگاه";
            string gAsset = "";
            if (d.DefId > 0)
            {
                if (type.StartsWith("coin")) gAsset = "coin_d" + d.DefId;
                else if (type.StartsWith("bull")) gAsset = "bull_d" + d.DefId;
                else if (type.StartsWith("cur")) gAsset = "cur_d" + d.DefId;
                else if (type.StartsWith("sil")) gAsset = "sil_d" + d.DefId;
            }
            string d1 = "سند " + Talayar.Dig(docId) + "؛ ";
            string dateJ = d.DateJ;

            // آبشده و طلا
            if (type == "gold_buy")
            {
                long eq = Talayar.Equiv750(w, d.Karat);
                PostGold(docId, dateJ, "in", w, d.Karat, d1 + "خرید طلا از " + who, d.Cid);
                PostCust(docId, dateJ, d.Cid, SettleMoney(d, -1, m), -eq, d1 + "خرید " + Talayar.Gs(w) + " طلا توسط فروشگاه");
                SettleOut(docId, dateJ, m, d);
            }
            else if (type == "gold_sell")
            {
                long eq = Talayar.Equiv750(w, d.Karat);
                PostGold(docId, dateJ, "out", w, d.Karat, d1 + "فروش طلا به " + who, d.Cid);
                PostCust(docId, dateJ, d.Cid, SettleMoney(d, +1, m), +eq, d1 + "فروش " + Talayar.Gs(w) + " طلا به مشتری");
                SettleIn(docId, dateJ, m, d);
            }
            else if (type == "gold_in") PostGold(docId, dateJ, "in", w, d.Karat, d1 + "ورود آبشده دستی", 0);
            else if (type == "gold_out") PostGold(docId, dateJ, "out", w, d.Karat, d1 + "خروج آبشده دستی", 0);
            else if (type == "gold_talab") PostCust(docId, dateJ, d.Cid, 0, +Talayar.Equiv750(w, d.Karat), d1 + "طلب طلایی " + Talayar.Gs(w));
            else if (type == "gold_bedehi") PostCust(docId, dateJ, d.Cid, 0, -Talayar.Equiv750(w, d.Karat), d1 + "بدهی طلایی " + Talayar.Gs(w));
            // سکه
            else if (type == "coin_buy")
            {
                PostAsset(docId, dateJ, "stock", gAsset, +nCnt, 0, 0, d1 + "خرید " + WhoTxt(d, nCnt));
                PostAsset(docId, dateJ, "customer", gAsset, -nCnt, 0, d.Cid, d1 + "خرید سکه توسط فروشگاه");
                PostCust(docId, dateJ, d.Cid, SettleMoney(d, -1, m), 0, d1 + "خرید سکه از مشتری");
                SettleOut(docId, dateJ, m, d);
            }
            else if (type == "coin_sell")
            {
                PostAsset(docId, dateJ, "stock", gAsset, -nCnt, 0, 0, d1 + "فروش " + WhoTxt(d, nCnt));
                PostAsset(docId, dateJ, "customer", gAsset, +nCnt, 0, d.Cid, d1 + "فروش سکه به مشتری");
                PostCust(docId, dateJ, d.Cid, SettleMoney(d, +1, m), 0, d1 + "فروش سکه به مشتری");
                SettleIn(docId, dateJ, m, d);
            }
            else if (type == "coin_in") PostAsset(docId, dateJ, "stock", gAsset, +nCnt, 0, 0, d1 + "ورود سکه");
            else if (type == "coin_out") PostAsset(docId, dateJ, "stock", gAsset, -nCnt, 0, 0, d1 + "خروج سکه");
            else if (type == "coin_talab") PostAsset(docId, dateJ, "customer", gAsset, +nCnt, 0, d.Cid, d1 + "طلب سکه " + WhoTxt(d, nCnt));
            else if (type == "coin_bedehi") PostAsset(docId, dateJ, "customer", gAsset, -nCnt, 0, d.Cid, d1 + "بدهی سکه " + WhoTxt(d, nCnt));
            // شمش — همان الگوی سکه
            else if (type == "bull_buy")
            {
                PostAsset(docId, dateJ, "stock", gAsset, +nCnt, 0, 0, d1 + "خرید " + WhoTxt(d, nCnt));
                PostAsset(docId, dateJ, "customer", gAsset, -nCnt, 0, d.Cid, d1 + "خرید شمش توسط فروشگاه");
                PostCust(docId, dateJ, d.Cid, SettleMoney(d, -1, m), 0, d1 + "خرید شمش از مشتری");
                SettleOut(docId, dateJ, m, d);
            }
            else if (type == "bull_sell")
            {
                PostAsset(docId, dateJ, "stock", gAsset, -nCnt, 0, 0, d1 + "فروش " + WhoTxt(d, nCnt));
                PostAsset(docId, dateJ, "customer", gAsset, +nCnt, 0, d.Cid, d1 + "فروش شمش به مشتری");
                PostCust(docId, dateJ, d.Cid, SettleMoney(d, +1, m), 0, d1 + "فروش شمش به مشتری");
                SettleIn(docId, dateJ, m, d);
            }
            else if (type == "bull_in") PostAsset(docId, dateJ, "stock", gAsset, +nCnt, 0, 0, d1 + "ورود شمش");
            else if (type == "bull_out") PostAsset(docId, dateJ, "stock", gAsset, -nCnt, 0, 0, d1 + "خروج شمش");
            else if (type == "bull_talab") PostAsset(docId, dateJ, "customer", gAsset, +nCnt, 0, d.Cid, d1 + "طلب شمش");
            else if (type == "bull_bedehi") PostAsset(docId, dateJ, "customer", gAsset, -nCnt, 0, d.Cid, d1 + "بدهی شمش");
            // ارز
            else if (type == "cur_buy")
            {
                PostAsset(docId, dateJ, "stock", gAsset, +qCur, 0, 0, d1 + "خرید " + who);
                PostAsset(docId, dateJ, "customer", gAsset, -qCur, 0, d.Cid, d1 + "خرید ارز توسط فروشگاه");
                PostCust(docId, dateJ, d.Cid, SettleMoney(d, -1, m), 0, d1 + "خرید ارز از مشتری");
                SettleOut(docId, dateJ, m, d);
            }
            else if (type == "cur_sell")
            {
                PostAsset(docId, dateJ, "stock", gAsset, -qCur, 0, 0, d1 + "فروش " + who);
                PostAsset(docId, dateJ, "customer", gAsset, +qCur, 0, d.Cid, d1 + "فروش ارز به مشتری");
                PostCust(docId, dateJ, d.Cid, SettleMoney(d, +1, m), 0, d1 + "فروش ارز به مشتری");
                SettleIn(docId, dateJ, m, d);
            }
            else if (type == "cur_talab") PostAsset(docId, dateJ, "customer", gAsset, +qCur, 0, d.Cid, d1 + "طلب ارزی");
            else if (type == "cur_bedehi") PostAsset(docId, dateJ, "customer", gAsset, -qCur, 0, d.Cid, d1 + "بدهی ارزی");
            // نقره
            else if (type == "sil_buy")
            {
                PostAsset(docId, dateJ, "stock", gAsset, +w, 0, 0, d1 + "خرید " + Talayar.Gs(w) + " نقره");
                PostAsset(docId, dateJ, "customer", gAsset, -w, 0, d.Cid, d1 + "خرید نقره توسط فروشگاه");
                PostCust(docId, dateJ, d.Cid, SettleMoney(d, -1, m), 0, d1 + "خرید نقره از مشتری");
                SettleOut(docId, dateJ, m, d);
            }
            else if (type == "sil_sell")
            {
                PostAsset(docId, dateJ, "stock", gAsset, -w, 0, 0, d1 + "فروش " + Talayar.Gs(w) + " نقره");
                PostAsset(docId, dateJ, "customer", gAsset, +w, 0, d.Cid, d1 + "فروش نقره به مشتری");
                PostCust(docId, dateJ, d.Cid, SettleMoney(d, +1, m), 0, d1 + "فروش نقره به مشتری");
                SettleIn(docId, dateJ, m, d);
            }
            else if (type == "sil_talab") PostAsset(docId, dateJ, "customer", gAsset, +w, 0, d.Cid, d1 + "طلب نقره");
            else if (type == "sil_bedehi") PostAsset(docId, dateJ, "customer", gAsset, -w, 0, d.Cid, d1 + "بدهی نقره");
            // کارساخته
            else if (type == "work_buy")
            {
                PostAsset(docId, dateJ, "stock", "work_mg", +w, d.Karat, 0, d1 + "خرید کارساخته " + d.DefName);
                PostAsset(docId, dateJ, "customer", "work_mg", -w, d.Karat, d.Cid, d1 + "خرید کارساخته توسط فروشگاه");
                PostCust(docId, dateJ, d.Cid, SettleMoney(d, -1, m), 0, d1 + "خرید کارساخته از مشتری");
                SettleOut(docId, dateJ, m, d);
            }
            else if (type == "work_sell")
            {
                PostAsset(docId, dateJ, "stock", "work_mg", -w, d.Karat, 0, d1 + "فروش کارساخته " + d.DefName);
                PostAsset(docId, dateJ, "customer", "work_mg", +w, d.Karat, d.Cid, d1 + "فروش کارساخته به مشتری");
                PostCust(docId, dateJ, d.Cid, SettleMoney(d, +1, m), 0, d1 + "فروش کارساخته به مشتری");
                SettleIn(docId, dateJ, m, d);
            }
            else if (type == "work_in") PostAsset(docId, dateJ, "stock", "work_mg", +w, d.Karat, 0, d1 + "ورود کارساخته " + d.DefName);
            else if (type == "work_out") PostAsset(docId, dateJ, "stock", "work_mg", -w, d.Karat, 0, d1 + "خروج کارساخته " + d.DefName);
            else if (type == "work_talab") PostAsset(docId, dateJ, "customer", "work_mg", +w, d.Karat, d.Cid, d1 + "طلب کارساخته (وزنی)");
            else if (type == "work_bedehi") PostAsset(docId, dateJ, "customer", "work_mg", -w, d.Karat, d.Cid, d1 + "بدهی کارساخته (وزنی)");
            // نقدی
            else if (type == "cash_income") PostCash(docId, dateJ, "in", m, d1 + "درآمد نقدی");
            else if (type == "cash_expense") PostCash(docId, dateJ, "out", m, d1 + "هزینه نقدی");
            else if (type == "cash_in") PostCash(docId, dateJ, "in", m, d1 + "ورود وجه نقد");
            else if (type == "cash_out") PostCash(docId, dateJ, "out", m, d1 + "خروج وجه نقد");
            else if (type == "cash_talab") PostCust(docId, dateJ, d.Cid, +m, 0, d1 + "طلب مالی " + Talayar.Money(m));
            else if (type == "cash_bedehi") PostCust(docId, dateJ, d.Cid, -m, 0, d1 + "بدهی مالی " + Talayar.Money(m));
            // بانکی
            else if (type == "bank_income") PostBank(dateJ, d.BankId, "in", m, d1 + "درآمد به بانک " + d.BankName, 0, docId);
            else if (type == "bank_expense") PostBank(dateJ, d.BankId, "out", m, d1 + "هزینه از بانک " + d.BankName, 0, docId);
            else if (type == "bank_recv")
            {
                PostBank(dateJ, d.BankId, "in", m, d1 + "دریافت از " + who + " به بانک " + d.BankName, d.Cid, docId);
                PostCust(docId, dateJ, d.Cid, -m, 0, d1 + "دریافت مبلغ به بانک از مشتری");
            }
            else if (type == "bank_pay")
            {
                PostBank(dateJ, d.BankId, "out", m, d1 + "پرداخت به " + who + " از بانک " + d.BankName, d.Cid, docId);
                PostCust(docId, dateJ, d.Cid, +m, 0, d1 + "پرداخت مبلغ از بانک به مشتری");
            }
            // چک
            else if (type == "check_recv")
            {
                PostCheck(dateJ, d.CheckDue, m, d.BankId, d.Cid, who, "recv", d.CheckNo, "سند " + docId, docId);
                PostCust(docId, dateJ, d.Cid, -m, 0, d1 + "دریافت چک شماره " + Talayar.Dig(d.CheckNo) + " از مشتری (سررسید " + d.CheckDue + ")");
            }
            else if (type == "check_pay")
            {
                PostCheck(dateJ, d.CheckDue, m, d.BankId, d.Cid, who, "pay", d.CheckNo, "سند " + docId, docId);
                PostCust(docId, dateJ, d.Cid, +m, 0, d1 + "صدور چک شماره " + Talayar.Dig(d.CheckNo) + " به مشتری (سررسید " + d.CheckDue + ")");
            }
            // تخفیف
            else if (type == "disc_us") PostCust(docId, dateJ, d.Cid, -m, 0, d1 + "تخفیف ما به مشتری " + Talayar.Money(m));
            else if (type == "disc_cust") PostCust(docId, dateJ, d.Cid, +m, 0, d1 + "تخفیف مشتری به ما " + Talayar.Money(m));
            // اتیکت — وضعیت + موجودی کارساخته وزنی + تسویه مالی
            else if (type.StartsWith("etiket"))
            {
                var et = db.Etikets.FirstOrDefault(x => x.Id == d.EtiketId);
                if (et != null)
                {
                    bool inflow = type == "etiket_buy" || type == "etiket_in";
                    et.Status = type == "etiket_sell" ? "sold" : type == "etiket_out" ? "out" : "stock";
                    et.UpdatedTs = DateTimeOffset.Now.ToUnixTimeMilliseconds();
                    if (!inflow) et.Rfid = ""; // اتیکت خارج/فروخته‌شده → حذف RFID
                    PostAsset(docId, dateJ, "stock", "work_mg", inflow ? +et.Wmw : -et.Wmw, 750, 0,
                        d1 + "اتیکت " + Talayar.Dig(et.Code) + " (" + et.Name + ")");
                    if (type == "etiket_buy")
                    {
                        PostCust(docId, dateJ, d.Cid, SettleMoney(d, -1, m), 0, d1 + "خرید اتیکت «" + Talayar.Dig(et.Code) + "» از مشتری");
                        SettleOut(docId, dateJ, m, d);
                    }
                    else if (type == "etiket_sell")
                    {
                        PostCust(docId, dateJ, d.Cid, SettleMoney(d, +1, m), 0, d1 + "فروش اتیکت «" + Talayar.Dig(et.Code) + "» به مشتری");
                        SettleIn(docId, dateJ, m, d);
                    }
                }
            }
        }

        string WhoTxt(DocDraft d, long n) => Talayar.Dig(n) + " عدد " + d.DefName;

        long SettleMoney(DocDraft d, int sign, long m) => d.Settle == 0 ? sign * m : 0;

        void SettleOut(long docId, string dateJ, long m, DocDraft d)
        {
            if (d.Settle == 1 && m > 0) PostCash(docId, dateJ, "out", m, "سند " + Talayar.Dig(docId) + "؛ پرداخت نقدی بابت خرید");
            if (d.Settle == 2 && m > 0) PostBank(dateJ, d.BankId, "out", m, "سند " + Talayar.Dig(docId) + "؛ پرداخت بابت خرید", d.Cid, docId);
        }
        void SettleIn(long docId, string dateJ, long m, DocDraft d)
        {
            if (d.Settle == 1 && m > 0) PostCash(docId, dateJ, "in", m, "سند " + Talayar.Dig(docId) + "؛ دریافت نقدی بابت فروش");
            if (d.Settle == 2 && m > 0) PostBank(dateJ, d.BankId, "in", m, "سند " + Talayar.Dig(docId) + "؛ دریافت بابت فروش", d.Cid, docId);
        }

        // ---------- حذف سند (برگشت کامل اثرات) ----------
        public void DeleteDoc(int id)
        {
            // برگشت وضعیت اتیکت‌های این سند به «موجود»
            foreach (var r in db.DocRows.Where(r => r.DocId == id).ToList())
            {
                int p = r.Txt.IndexOf("اتیکت #");
                while (p >= 0)
                {
                    int i = p + 7;
                    while (i < r.Txt.Length && char.IsDigit(r.Txt[i])) i++;
                    if (i > p + 7 && int.TryParse(r.Txt.Substring(p + 7, i - p - 7), out int eid))
                    {
                        var et = db.Etikets.FirstOrDefault(x => x.Id == eid);
                        if (et != null) { et.Status = "stock"; et.UpdatedTs = DateTimeOffset.Now.ToUnixTimeMilliseconds(); }
                    }
                    p = r.Txt.IndexOf("اتیکت #", i);
                }
            }
            db.DocRows.RemoveRange(db.DocRows.Where(r => r.DocId == id));
            db.AssetLedgers.RemoveRange(db.AssetLedgers.Where(a => a.DocId == id));
            db.BankTransactions.RemoveRange(db.BankTransactions.Where(b => b.DocId == id));
            db.CheckTransactions.RemoveRange(db.CheckTransactions.Where(c => c.DocId == id));
            db.GoldTransactions.RemoveRange(db.GoldTransactions.Where(g => g.DocId == id));
            db.CustomerTxs.RemoveRange(db.CustomerTxs.Where(c => c.Iid == -id));
            db.CashTransactions.RemoveRange(db.CashTransactions.Where(c => c.Iid == -id));
            var doc = db.Docs.FirstOrDefault(x => x.Id == id);
            if (doc != null) db.Docs.Remove(doc);
            db.SaveChanges();
        }

        // ---------- چک‌ها ----------
        public static string CheckStatusName(string s)
        {
            if (s == "open") return "باز";
            if (s == "pass") return "پاس‌شده";
            if (s == "ret") return "برگشتی";
            if (s == "void") return "باطل";
            return s;
        }

        public void SetCheckStatus(int id, string st)
        {
            var chk = db.CheckTransactions.FirstOrDefault(c => c.Id == id);
            if (chk == null) return;
            chk.Status = st;
            string dateJ = Talayar.Jal.Today();
            if (st == "pass")
            {
                if (chk.Kind == "recv")
                {
                    PostBank(dateJ, chk.BankId, "in", chk.Amount, "پاس شدن چک دریافتی " + Talayar.Dig(chk.No), chk.Cid, 0);
                    PostCust(0, dateJ, chk.Cid, -chk.Amount, 0, "تأمین چک دریافتی " + Talayar.Dig(chk.No) + " (پاس شد)");
                }
                else
                {
                    PostBank(dateJ, chk.BankId, "out", chk.Amount, "پاس شدن چک پرداختنی " + Talayar.Dig(chk.No), chk.Cid, 0);
                    PostCust(0, dateJ, chk.Cid, +chk.Amount, 0, "تأمین چک پرداختنی " + Talayar.Dig(chk.No) + " (پاس شد)");
                }
            }
            else if (st == "ret" || st == "void")
            {
                if (chk.Kind == "recv") PostCust(0, dateJ, chk.Cid, +chk.Amount, 0, "برگشت چک دریافتی " + Talayar.Dig(chk.No));
                else PostCust(0, dateJ, chk.Cid, -chk.Amount, 0, "ابطال چک پرداختنی " + Talayar.Dig(chk.No));
            }
            db.SaveChanges();
        }

        // ---------- عملیات مشتری ----------
        public (long cash, long gold) CustomerSums(int cid)
        {
            long cash = db.CustomerTxs.Where(c => c.Cid == cid).Sum(c => (long?)c.Cash) ?? 0;
            long gold = db.CustomerTxs.Where(c => c.Cid == cid).Sum(c => (long?)c.Goldmw) ?? 0;
            return (cash, gold);
        }
        public (long cash, long gold) CustomerSumsUpTo(int cid, string maxDate)
        {
            long cash = db.CustomerTxs.Where(c => c.Cid == cid && string.Compare(c.DateJ, maxDate) <= 0).Sum(c => (long?)c.Cash) ?? 0;
            long gold = db.CustomerTxs.Where(c => c.Cid == cid && string.Compare(c.DateJ, maxDate) <= 0).Sum(c => (long?)c.Goldmw) ?? 0;
            return (cash, gold);
        }

        /// <summary>دریافت وجه نقد از مشتری</summary>
        public void ReceiveCash(int cid, long amt, string descr)
        {
            long ts = DateTimeOffset.Now.ToUnixTimeMilliseconds();
            string tj = Talayar.Jal.Today();
            db.CashTransactions.Add(new CashTransaction { Ts = ts, DateJ = tj, Kind = "in", Amount = amt, Descr = descr, Iid = 0 });
            db.CustomerTxs.Add(new CustomerTx { Cid = cid, Ts = ts, DateJ = tj, Cash = -amt, Goldmw = 0, Descr = descr, Iid = 0 });
            db.SaveChanges();
        }

        /// <summary>دریافت طلا از مشتری (کارکرده) — معادل ۱۸ عیار</summary>
        public void ReceiveGold(int cid, long wmg, int karat, string descr)
        {
            long ts = DateTimeOffset.Now.ToUnixTimeMilliseconds();
            string tj = Talayar.Jal.Today();
            long eq = Talayar.Equiv750(wmg, karat);
            var s = CustomerSums(cid);
            long rate = CurrentRate();
            db.GoldTransactions.Add(new GoldTransactionEntity { Ts = ts, DateJ = tj, Kind = "in", Wmw = wmg, Karat = karat, Descr = descr, Cid = cid, DocId = 0, TransferCode = "web-g-" + Guid.NewGuid().ToString("N").Substring(0, 8) });
            // کسر از بدهی طلایی، سپس مازاد → معادل نقدی با نرخ روز از بدهی نقدی
            long cashAdj = 0, goldAdj = -eq;
            if (s.gold <= 0 && rate > 0)
            {
                cashAdj = -(long)Math.Round(eq * rate / 1000.0);
                goldAdj = 0;
            }
            else if (eq > s.gold && s.gold > 0 && rate > 0)
            {
                long extra = eq - s.gold;
                goldAdj = -s.gold;
                cashAdj = -(long)Math.Round(extra * rate / 1000.0);
            }
            db.CustomerTxs.Add(new CustomerTx { Cid = cid, Ts = ts, DateJ = tj, Cash = cashAdj, Goldmw = goldAdj, Descr = descr, Iid = 0 });
            db.SaveChanges();
        }

        /// <summary>آیا مشتری قابل حذف است؟</summary>
        public bool CustomerHasHistory(int cid)
        {
            bool tx = db.CustomerTxs.Any(c => c.Cid == cid);
            var s = CustomerSums(cid);
            return tx || s.cash != 0 || s.gold != 0;
        }

        // ---------- خرید طلای کارکرده ----------
        public void SaveBuy(int cid, string cname, long wmg, int karat, long perGram, bool payNow, string note)
        {
            long ts = DateTimeOffset.Now.ToUnixTimeMilliseconds();
            string tj = Talayar.Jal.Today();
            long total = (long)Math.Round(wmg * perGram / 1000.0);
            string desc = "خرید " + Talayar.Mw(wmg) + " گرم طلای " + Talayar.KaratName(karat)
                          + (cname.Length > 0 ? " از " + cname : "");
            if (note.Length > 0) desc += " • " + note;
            db.GoldTransactions.Add(new GoldTransactionEntity { Ts = ts, DateJ = tj, Kind = "in", Wmw = wmg, Karat = karat, Descr = desc, Cid = cid, DocId = 0, TransferCode = "web-buy-" + Guid.NewGuid().ToString("N").Substring(0, 8) });
            if (payNow)
                db.CashTransactions.Add(new CashTransaction { Ts = ts, DateJ = tj, Kind = "out", Amount = total, Descr = desc + " (پرداخت نقد)", Iid = 0 });
            if (cid > 0)
            {
                long eq = Talayar.Equiv750(wmg, karat);
                db.CustomerTxs.Add(new CustomerTx { Cid = cid, Ts = ts, DateJ = tj, Goldmw = -eq, Cash = payNow ? 0 : -total, Descr = desc + (payNow ? "" : " (مانده به حساب: " + Talayar.Money(total) + " بستانکاری)"), Iid = 0 });
            }
            db.SaveChanges();
        }

        // ---------- کدینگ ----------
        public bool DefUsed(string kind, int id, string name)
        {
            if (kind == "group") return db.Customers.Any(c => c.Grp == name);
            if (kind == "bank")
            {
                int n = db.BankTransactions.Count(b => b.BankId == id) + db.CheckTransactions.Count(c => c.BankId == id);
                return n > 0;
            }
            if (kind == "coin" || kind == "bullion" || kind == "curr" || kind == "silver")
            {
                string prefix = kind == "coin" ? "coin" : kind == "bullion" ? "bull" : kind == "curr" ? "cur" : "sil";
                return db.AssetLedgers.Any(a => a.Asset == prefix + "_d" + id);
            }
            return false;
        }
    }
}

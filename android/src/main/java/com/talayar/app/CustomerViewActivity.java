package com.talayar.app;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** پرونده مشتری: مانده (نقدی/طلایی/سکه/ارز/شمش/نقره/کارساخته)، تسویه، گردش، مانده تا تاریخ */
public class CustomerViewActivity extends A {
    private int cid;
    private int code = 0;
    private String name = "";
    private String phone = "", grp = "", addr = "", note = "";

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        cid = getIntent().getIntExtra("id", 0);
        loadCustomer();
        scaffold("پرونده مشتری", true);

        headBox = card();
        body.addView(headBox);
        fillHead();

        balBox = cardHi();
        body.addView(balBox);
        fillBalance();

        opsBox = card();
        body.addView(opsBox);
        fillOps();

        TextView lt = tv("گردش حساب", U.GOLD, 15, true);
        lt.setPadding(dp(4), dp(10), 0, dp(4));
        body.addView(lt);
        ledgerBox = v();
        body.addView(ledgerBox);
        fillLedger();
    }

    private void loadCustomer() {
        Cursor c = db.r().rawQuery("SELECT name, phone, note, code, grp, address FROM customers WHERE id=?",
                new String[]{"" + cid});
        if (c.moveToFirst()) {
            name = c.getString(0);
            phone = c.getString(1) == null ? "" : c.getString(1);
            note = c.getString(2) == null ? "" : c.getString(2);
            code = c.getInt(3);
            grp = c.getString(4) == null ? "" : c.getString(4);
            addr = c.getString(5) == null ? "" : c.getString(5);
        }
        c.close();
    }

    private LinearLayout headBox, balBox, opsBox, ledgerBox;

    private void fillHead() {
        headBox.removeAllViews();
        headBox.addView(tv(name, U.GOLD, 19, true));
        StringBuilder inf = new StringBuilder();
        inf.append("کد حساب ").append(U.dig(code + ""));
        if (grp.length() > 0) inf.append(" • گروه ").append(grp);
        headBox.addView(tv(inf.toString(), U.SUB, 12, false));
        if (phone.length() > 0) headBox.addView(tv("☎ " + U.dig(phone), U.SUB, 13, false));
        if (addr.length() > 0) headBox.addView(tv("📍 " + addr, U.SUB, 12, false));
        if (note.length() > 0) headBox.addView(tv(note, U.SUB, 12, false));
    }

    private long[] sums() {
        return sumsUpTo(null);
    }
    private long[] sumsUpTo(String maxDate) {
        String cond = "cid=?";
        if (maxDate != null) cond += " AND date_j <= '" + maxDate + "'";
        Cursor c = db.r().rawQuery("SELECT COALESCE(SUM(cash),0), COALESCE(SUM(goldmw),0) FROM customer_tx WHERE " + cond,
                new String[]{"" + cid});
        long[] r = {0, 0};
        if (c.moveToFirst()) { r[0] = c.getLong(0); r[1] = c.getLong(1); }
        c.close();
        return r;
    }

    @Override protected void onResume() {
        super.onResume();
        if (ledgerBox != null) {
            loadCustomer();
            fillHead();
            fillBalance();
            fillLedger();
        }
    }

    private void fillBalance() {
        long[] s = sums();
        balBox.removeAllViews();
        balBox.addView(tv("مانده حساب", U.GOLD, 14, true));
        // نقدی — دو واحد تومان و ریال
        String cashTxt;
        int cashCol;
        if (s[0] > 0) { cashTxt = U.money(s[0]) + " تومان (" + U.money(s[0] * 10) + " ریال) بدهکار"; cashCol = 0xFFFFCC80; }
        else if (s[0] < 0) { cashTxt = U.money(-s[0]) + " تومان (" + U.money(-s[0] * 10) + " ریال) بستانکار"; cashCol = U.OK; }
        else { cashTxt = "تسویه"; cashCol = U.SUB; }
        balBox.addView(kv("💰 مانده نقدی/مالی", cashTxt, cashCol));
        // طلایی — گرم و سوت
        long g = s[1];
        String goldTxt = g == 0 ? "—" : U.gs((int) Math.abs(g)) + (g > 0 ? " بدهکار" : " بستانکار") + " (۱۸ معادل)";
        balBox.addView(kv("⚖️ مانده طلایی", goldTxt, g > 0 ? 0xFFFFCC80 : U.TXT));

        // سایر دارایی‌ها (سکه/شمش/ارز/نقره/کارساخته)
        Cursor a = db.r().rawQuery(
                "SELECT asset, SUM(qty) FROM assets_ledger WHERE scope='customer' AND cid=? GROUP BY asset HAVING SUM(qty) != 0",
                new String[]{"" + cid});
        boolean any = false;
        while (a.moveToNext()) {
            any = true;
            String asset = a.getString(0);
            double q = a.getDouble(1);
            boolean debt = q > 0;
            String line = Post.fmtQty(asset, Math.abs(q)) + (debt ? " بدهکار" : " بستانکار");
            balBox.addView(kv(assetIcon(asset) + " " + Post.assetName(db, asset), line, debt ? 0xFFFFCC80 : U.OK));
        }
        a.close();
        if (!any) balBox.addView(tv("سکه/شمش/ارز/نقره/کارساخته‌ای نزد این مشتری ثبت نشده.", U.SUB, 11, false));

        // مانده تا تاریخ
        LinearLayout r = h();
        r.addView(gbtn("📅 مانده تا تاریخ مشخص", new Tap() { public void go() { askDateBalance(); } }),
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        balBox.addView(space(4));
        balBox.addView(r);
    }

    static String assetIcon(String asset) {
        if (asset.startsWith("coin")) return "🪙";
        if (asset.startsWith("bull")) return "🧱";
        if (asset.startsWith("cur")) return "💵";
        if (asset.startsWith("sil")) return "🥈";
        if (asset.startsWith("work")) return "💍";
        return "▪";
    }

    private void askDateBalance() {
        input("مانده «" + name + "» تا تاریخ", "مثل ۱۴۰۵/۰۵/۰۹", Jal.today(), false, new OnText() {
            public void ok(String s) {
                String d = U.en(s).trim();
                if (!d.matches("[0-9]{4}/[0-9]{2}/[0-9]{2}")) { U.toast(CustomerViewActivity.this, "تاریخ را کامل بنویسید (۱۴۰۵/۰۵/۰۹)"); return; }
                long[] s2 = sumsUpTo(d);
                StringBuilder m = new StringBuilder();
                m.append("مانده تا تاریخ ").append(U.dig(d)).append("\n\n");
                m.append("نقدی: ");
                if (s2[0] > 0) m.append(U.money(s2[0])).append(" تومان بدهکار");
                else if (s2[0] < 0) m.append(U.money(-s2[0])).append(" تومان بستانکار");
                else m.append("تسویه");
                m.append("\nطلایی (۱۸ معادل): ");
                if (s2[1] != 0) m.append(U.gs((int) Math.abs(s2[1]))).append(s2[1] > 0 ? " بدهکار" : " بستانکار");
                else m.append("—");
                Cursor a = db.r().rawQuery(
                        "SELECT asset, SUM(qty) FROM assets_ledger WHERE scope='customer' AND cid=? AND date_j <= ? " +
                        "GROUP BY asset HAVING SUM(qty) != 0",
                        new String[]{"" + cid, d});
                while (a.moveToNext()) {
                    String asset = a.getString(0);
                    double q = a.getDouble(1);
                    m.append("\n").append(Post.assetName(db, asset)).append(": ")
                     .append(Post.fmtQty(asset, Math.abs(q))).append(q > 0 ? " بدهکار" : " بستانکار");
                }
                a.close();
                msg("مانده تا تاریخ", m.toString());
            }
        });
    }

    private void fillOps() {
        opsBox.removeAllViews();
        LinearLayout row = h();
        row.addView(btn("دریافت وجه", new Tap() { public void go() { payCash(); } }),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(wspace(8));
        row.addView(btn("دریافت طلا", new Tap() { public void go() { payGold(); } }),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        opsBox.addView(row);
        opsBox.addView(space(6));
        LinearLayout rowMid = h();
        rowMid.addView(gbtn("📜 سند جدید برای این مشتری", new Tap() {
            public void go() {
                Intent it = new Intent(CustomerViewActivity.this, DocNewActivity.class);
                it.putExtra("cid", cid);
                it.putExtra("cname", name);
                startActivity(it);
            }
        }), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        rowMid.addView(wspace(8));
        rowMid.addView(gbtn("ویرایش پرونده ✎", new Tap() {
            public void go() {
                CustomersActivity.editCustomer(CustomerViewActivity.this, cid, new Tap() {
                    public void go() { onResume(); }
                });
            }
        }), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        opsBox.addView(rowMid);
        opsBox.addView(space(6));
        LinearLayout row2 = h();
        row2.addView(dbtn("حذف حساب 🗑", new Tap() { public void go() { delCustomer(); } }),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        opsBox.addView(row2);
    }

    private void fillLedger() {
        ledgerBox.removeAllViews();
        Cursor c = db.r().rawQuery("SELECT * FROM customer_tx WHERE cid=? ORDER BY ts DESC, id DESC LIMIT 200",
                new String[]{"" + cid});
        boolean any = false;
        while (c.moveToNext()) {
            any = true;
            String date = Db.cs(c, "date_j");
            long cash = Db.cl(c, "cash");
            long gold = Db.cl(c, "goldmw");
            String desc = Db.cs(c, "descr");
            LinearLayout card = card(dp(10));
            LinearLayout top = h();
            top.addView(tv(U.dig(date), U.SUB, 12, false),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            if (cash != 0) top.addView(tvM((cash > 0 ? "+" : "") + U.money(cash), cash > 0 ? 0xFFFFCC80 : U.OK, 13));
            if (gold != 0) top.addView(wspace(6));
            if (gold != 0) top.addView(tvM((gold > 0 ? "+" : "") + U.mw((int) gold) + " گرم", gold > 0 ? 0xFFFFCC80 : U.OK, 13));
            card.addView(top);
            if (desc.length() > 0) card.addView(tv(desc, U.TXT, 13, false));
            ledgerBox.addView(card);
        }
        c.close();
        if (!any) {
            LinearLayout e = card();
            e.addView(tv("تراکنشی ثبت نشده است.", U.SUB, 13, false));
            ledgerBox.addView(e);
        }
    }

    /** دریافت وجه نقد از مشتری */
    private void payCash() {
        final long[] s = sums();
        final LinearLayout box = v();
        box.setPadding(dp(16), dp(14), dp(16), dp(10));
        box.addView(tv("دریافت وجه از " + name, U.GOLD, 16, true));
        if (s[0] > 0) box.addView(tv("مانده فعلی: " + U.money(s[0]) + " تومان بدهی", U.SUB, 12, false));
        box.addView(space(6));
        final android.widget.EditText e = in("مبلغ به تومان", true);
        final android.widget.EditText ed = in("شرح (اختیاری)");
        box.addView(e); box.addView(space(6)); box.addView(ed);
        box.addView(space(8));
        final android.app.AlertDialog d = sheet(box);
        LinearLayout br = h();
        br.setGravity(android.view.Gravity.CENTER);
        br.addView(btn("ثبت دریافت", new Tap() { public void go() {
            long amt = U.parseMoney(U.str(e));
            if (amt <= 0) { U.toast(CustomerViewActivity.this, "مبلغ نامعتبر"); return; }
            String desc = U.str(ed);
            if (desc.length() == 0) desc = "دریافت وجه از " + name;
            long ts = System.currentTimeMillis();
            android.content.ContentValues tx = new android.content.ContentValues();
            tx.put("ts", ts); tx.put("date_j", Jal.today()); tx.put("kind", "in");
            tx.put("amount", amt); tx.put("descr", desc); tx.put("iid", 0);
            db.ins("cash_tx", tx);
            android.content.ContentValues ct = new android.content.ContentValues();
            ct.put("cid", cid); ct.put("ts", ts); ct.put("date_j", Jal.today());
            ct.put("cash", -amt); ct.put("goldmw", 0); ct.put("descr", desc);
            db.ins("customer_tx", ct);
            d.dismiss();
            U.toast(CustomerViewActivity.this, "ثبت شد ✓");
            fillLedger();
            fillBalance();
        } }), new LinearLayout.LayoutParams(dp(140), ViewGroup.LayoutParams.WRAP_CONTENT));
        br.addView(wspace(10));
        br.addView(gbtn("انصراف", new Tap() { public void go() { d.dismiss(); } }),
                new LinearLayout.LayoutParams(dp(110), ViewGroup.LayoutParams.WRAP_CONTENT));
        box.addView(br);
    }

    /** دریافت طلا از مشتری (کارکرده) — معادل ۱۸عیار */
    private void payGold() {
        final long rate = db.currentRate();
        final long[] s = sums();
        final LinearLayout box = v();
        box.setPadding(dp(16), dp(14), dp(16), dp(10));
        box.addView(tv("دریافت طلا از " + name, U.GOLD, 16, true));
        box.addView(tv("طلا به موجودی آبشده اضافه و از بدهی طلایی/نقدی مشتری کم می‌شود.", U.SUB, 12, false));
        box.addView(space(6));
        final android.widget.EditText ew = in("وزن (گرم)", true);
        box.addView(label("عیار:"));
        final int[] karat = {750};
        box.addView(chipsRow(new String[]{"۲۴","۲۲","۲۱","۱۸","۱۴","۹"}, 3, new OnIdx() {
            public void ok(int i) { karat[0] = ItemEditActivity.K_VALS[i]; }
        }));
        final android.widget.EditText ed = in("شرح (اختیاری)");
        box.addView(ew); box.addView(space(6)); box.addView(ed);
        box.addView(space(8));
        final android.app.AlertDialog d = sheet(box);
        LinearLayout br = h();
        br.setGravity(android.view.Gravity.CENTER);
        br.addView(btn("ثبت دریافت", new Tap() { public void go() {
            int w = U.parseMw(U.str(ew));
            if (w <= 0) { U.toast(CustomerViewActivity.this, "وزن نامعتبر"); return; }
            long eq = U.equiv750(w, karat[0]);
            String desc = U.str(ed);
            if (desc.length() == 0) desc = "دریافت " + U.mw(w) + " گرم طلای " + U.karatName(karat[0]) + " از " + name;
            long ts = System.currentTimeMillis();
            android.content.ContentValues gt = new android.content.ContentValues();
            gt.put("ts", ts); gt.put("date_j", Jal.today()); gt.put("kind", "in");
            gt.put("wmw", w); gt.put("karat", karat[0]); gt.put("descr", desc); gt.put("cid", cid);
            db.ins("gold_tx", gt);
            // کسر از بدهی طلایی، سپس مازاد → معادل نقدی با نرخ روز از بدهی نقدی
            long cashAdj = 0, goldAdj = -eq;
            if (s[1] <= 0 && rate > 0) {
                cashAdj = -Math.round(eq * rate / 1000.0);
                goldAdj = 0;
            } else if (eq > s[1] && s[1] > 0 && rate > 0) {
                long extra = eq - s[1];
                goldAdj = -s[1];
                cashAdj = -Math.round(extra * rate / 1000.0);
            }
            android.content.ContentValues ct = new android.content.ContentValues();
            ct.put("cid", cid); ct.put("ts", ts); ct.put("date_j", Jal.today());
            ct.put("cash", cashAdj); ct.put("goldmw", goldAdj); ct.put("descr", desc);
            db.ins("customer_tx", ct);
            d.dismiss();
            U.toast(CustomerViewActivity.this, "ثبت شد ✓");
            fillLedger();
            fillBalance();
        } }), new LinearLayout.LayoutParams(dp(140), ViewGroup.LayoutParams.WRAP_CONTENT));
        br.addView(wspace(10));
        br.addView(gbtn("انصراف", new Tap() { public void go() { d.dismiss(); } }),
                new LinearLayout.LayoutParams(dp(110), ViewGroup.LayoutParams.WRAP_CONTENT));
        box.addView(br);
    }

    private void delCustomer() {
        Cursor c = db.r().rawQuery("SELECT COUNT(*) FROM customer_tx WHERE cid=?", new String[]{"" + cid});
        c.moveToFirst();
        int n = c.getInt(0);
        c.close();
        long[] s = sums();
        if (n > 0 || s[0] != 0 || s[1] != 0) {
            msg("قابل حذف نیست", "این مشتری گردش حساب دارد و برای حفظ صحت اسناد قابل حذف نیست.");
            return;
        }
        confirm("مشتری «" + name + "» حذف شود؟", new Tap() {
            public void go() {
                db.w().delete("customers", "id=?", new String[]{"" + cid});
                U.toast(CustomerViewActivity.this, "حذف شد");
                finish();
            }
        });
    }
}

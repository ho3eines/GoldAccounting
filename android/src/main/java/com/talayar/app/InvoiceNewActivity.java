package com.talayar.app;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/** صدور فاکتور فروش طلا */
public class InvoiceNewActivity extends A {

    static class Line {
        int itemId;           // 0 = دستی
        String title;
        int karat = 750;
        int wmw;              // میلی‌گرم
        int wtype;            // 0 %  —  1 تومان/گرم  —  2 ثابت
        long wval;
        long stone;
        // محاسبه‌شده:
        long unit, goldVal, wage, tax, total;
        int code;
    }

    private final ArrayList<Line> lines = new ArrayList<Line>();
    private int cid = 0;
    private String cname = "";
    private TextView custTv, dateTv, sumTv, debtTv;
    private EditText eCash, eGWeight, eRasub, eNote;
    private int payKarat = 750;
    private String dateJ;
    private LinearLayout linesBox;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        scaffold("فاکتور فروش جدید", true);
        dateJ = Jal.today();

        // مشتری و تاریخ
        LinearLayout top = card();
        LinearLayout tr = h();
        custTv = tv("بدون مشتری (عابر)", U.TXT, 14, true);
        LinearLayout cc = v();
        cc.addView(tv("مشتری", U.SUB, 11, false));
        cc.addView(custTv);
        tr.addView(cc, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView pick = gbtn("انتخاب", new Tap() {
            public void go() {
                pickCustomer(new OnCustomer() {
                    public void ok(int id, String name) {
                        cid = id; cname = name;
                        custTv.setText(id == 0 ? "بدون مشتری (عابر)" : name);
                        updTotals();
                    }
                });
            }
        });
        tr.addView(pick);
        top.addView(tr);
        LinearLayout dr = h();
        dc.addLabel(dr, "تاریخ");
        dateTv = tvM(U.dig(dateJ), U.TXT, 14);
        dr.addView(dateTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView cal = gbtn("تغییر", new Tap() { public void go() { askDate(); } });
        dr.addView(cal);
        top.addView(dr);
        body.addView(top);

        // اقلام
        LinearLayout linesCard = card();
        linesCard.addView(tv("اقلام فاکتور", U.GOLD, 15, true));
        linesBox = v();
        linesCard.addView(linesBox);
        LinearLayout addRow = h();
        addRow.addView(btn("＋ قلم از انبار", new Tap() { public void go() { addFromStock(); } }),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        addRow.addView(wspace(8));
        addRow.addView(gbtn("＋ قلم دستی", new Tap() { public void go() { addManual(); } }),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        linesCard.addView(space(8));
        linesCard.addView(addRow);
        body.addView(linesCard);

        // تسویه
        LinearLayout pay = card();
        pay.addView(tv("تسویه", U.GOLD, 15, true));
        pay.addView(label("پرداخت نقدی (تومان)"));
        eCash = in("۰", true);
        pay.addView(eCash);
        pay.addView(label("طلا دریافتی از مشتری (کارکرده) — وزن گرم"));
        eGWeight = in("۰", true);
        pay.addView(eGWeight);
        pay.addView(label("عیار طلای دریافتی"));
        pay.addView(chipsRow(new String[]{"۲۴","۲۲","۲۱","۱۸","۱۴","۹"}, 3, new OnIdx() {
            public void ok(int i) { payKarat = ItemEditActivity.K_VALS[i]; updTotals(); }
        }));
        pay.addView(label("رسوب / ری‌گیری (٪)"));
        eRasub = in(db.getS("resub", "0"), true);
        pay.addView(eRasub);
        pay.addView(label("یادداشت فاکتور"));
        eNote = in("اختیاری");
        pay.addView(eNote);
        body.addView(pay);

        // جمع
        sumCard = cardHi();
        sumTv = tv("", U.TXT, 13, false);
        sumTv.setLineSpacing(6, 1.25f);
        sumCard.addView(sumTv);
        debtTv = tv("", U.GOLD, 16, true);
        sumCard.addView(debtTv);
        body.addView(sumCard);

        addBtn(body, btn("⬇  ثبت فاکتور", new Tap() { public void go() { save(false); } }));
        addBtn(body, gbtn("📄 پیش‌نمایش پیش‌فاکتور (PDF)", new Tap() { public void go() { exportPreInvoicePdf(); } }));

        android.text.TextWatcher tw = new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { updTotals(); }
            public void afterTextChanged(android.text.Editable s) {}
        };
        eCash.addTextChangedListener(tw);
        eGWeight.addTextChangedListener(tw);
        eRasub.addTextChangedListener(tw);
        renderLines();
        updTotals();
    }

    LinearLayout sumCard;

    static class dc { static void addLabel(LinearLayout p, String t) {
        TextView v = new TextView(p.getContext());
        v.setText(t + ": ");
        v.setTextColor(U.SUB);
        v.setTextSize(12);
        v.setTypeface(U.F);
        p.addView(v);
    } }

    private void askDate() {
        final Jal j = Jal.now();
        final EditText ey = in("سال", true); ey.setText(U.dig(j.y + ""));
        final EditText em = in("ماه (۱-۱۲)", true); em.setText(U.dig(j.m + ""));
        final EditText eD = in("روز", true); eD.setText(U.dig(j.d + ""));
        final LinearLayout box = v();
        box.setPadding(dp(16), dp(14), dp(16), dp(10));
        box.addView(tv("تاریخ فاکتور", U.GOLD, 16, true));
        LinearLayout r = h();
        r.addView(eD, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        r.addView(wspace(6));
        r.addView(em, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        r.addView(wspace(6));
        r.addView(ey, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        box.addView(space(6)); box.addView(r); box.addView(space(8));
        final android.app.AlertDialog d = sheet(box);
        LinearLayout br = h();
        br.setGravity(android.view.Gravity.CENTER);
        br.addView(btn("ثبت", new Tap() { public void go() {
            int y = (int) U.parseMoney(U.str(ey));
            int m = (int) U.parseMoney(U.str(em));
            int dd = (int) U.parseMoney(U.str(eD));
            if (y < 1300 || m < 1 || m > 12 || dd < 1 || dd > 31) { U.toast(InvoiceNewActivity.this, "تاریخ نامعتبر"); return; }
            dateJ = y + "/" + (m < 10 ? "0" + m : "" + m) + "/" + (dd < 10 ? "0" + dd : "" + dd);
            dateTv.setText(U.dig(dateJ));
            d.dismiss();
        } }), new LinearLayout.LayoutParams(dp(120), ViewGroup.LayoutParams.WRAP_CONTENT));
        br.addView(wspace(10));
        br.addView(gbtn("انصراف", new Tap() { public void go() { d.dismiss(); } }),
                new LinearLayout.LayoutParams(dp(110), ViewGroup.LayoutParams.WRAP_CONTENT));
        box.addView(br);
    }

    /** افزودن قلم از انبار */
    private void addFromStock() {
        Cursor c = db.r().rawQuery("SELECT id, code, name, karat, wmw FROM items WHERE status='stock' ORDER BY cts DESC", null);
        final ArrayList<Integer> ids = new ArrayList<Integer>();
        ArrayList<String> names = new ArrayList<String>();
        while (c.moveToNext()) {
            ids.add(c.getInt(0));
            names.add(c.getString(2) + " — " + U.karatName(c.getInt(3)) + " • " + U.mw(c.getInt(4)) + " گرم (کد " + U.dig(c.getInt(1) + "") + ")");
        }
        c.close();
        if (names.isEmpty()) { msg("انبار خالی است", "هیچ جنس موجودی در انبار نیست؛ از «قلم دستی» استفاده کنید یا ابتدا جنس اضافه کنید."); return; }
        String[] arr = new String[names.size()];
        for (int i = 0; i < names.size(); i++) arr[i] = (String) names.get(i);
        choose("انتخاب جنس از انبار", arr, new OnIdx() {
            public void ok(int i) {
                Cursor c2 = db.r().rawQuery("SELECT * FROM items WHERE id=?", new String[]{"" + ((Integer) ids.get(i)).intValue()});
                if (c2.moveToFirst()) {
                    Line ln = new Line();
                    ln.itemId = Db.ci(c2, "id");
                    ln.code = Db.ci(c2, "code");
                    ln.title = Db.cs(c2, "name");
                    ln.karat = Db.ci(c2, "karat");
                    ln.wmw = Db.ci(c2, "wmw");
                    ln.wtype = Db.ci(c2, "wtype");
                    ln.wval = Db.cl(c2, "wval");
                    ln.stone = Db.cl(c2, "stoneval");
                    lines.add(ln);
                }
                c2.close();
                renderLines();
                updTotals();
            }
        });
    }

    /** افزودن قلم دستی */
    private void addManual() { editManual(null); }

    private void editManual(final Line existing) {
        final Line ln = existing != null ? existing : new Line();
        final LinearLayout box = v();
        box.setPadding(dp(16), dp(14), dp(16), dp(10));
        box.addView(tv(existing == null ? "قلم دستی" : "ویرایش قلم", U.GOLD, 16, true));
        box.addView(space(4));
        final EditText eT = in("عنوان (مثلاً النگو، آبشده)", false);
        eT.setText(ln.title == null ? "" : ln.title);
        box.addView(label2("عنوان")); box.addView(eT);
        box.addView(label2("عیار"));
        final int[] karat = {ln.karat};
        int ki = 3;
        for (int i = 0; i < ItemEditActivity.K_VALS.length; i++) if (ItemEditActivity.K_VALS[i] == ln.karat) ki = i;
        box.addView(chipsRow(new String[]{"۲۴","۲۲","۲۱","۱۸","۱۴","۹"}, ki, new OnIdx() {
            public void ok(int i) { karat[0] = ItemEditActivity.K_VALS[i]; }
        }));
        box.addView(label2("وزن (گرم)"));
        final EditText eW = in("۰", true);
        if (ln.wmw > 0) eW.setText(U.dig(U.mw(ln.wmw)));
        box.addView(eW);
        box.addView(label2("نوع اجرت"));
        final int[] wtype = {ln.wtype};
        box.addView(chipsRow(new String[]{"درصدی", "تومان بر گرم", "ثابت"}, ln.wtype, new OnIdx() {
            public void ok(int i) { wtype[0] = i; }
        }));
        box.addView(label2("مقدار اجرت"));
        final EditText eWv = in("۰", true);
        if (ln.wval > 0) eWv.setText(U.dig(ln.wval + ""));
        box.addView(eWv);
        box.addView(label2("سنگ / مزین (تومان)"));
        final EditText eS = in("۰", true);
        if (ln.stone > 0) eS.setText(U.dig(ln.stone + ""));
        box.addView(eS);
        box.addView(space(8));
        final android.app.AlertDialog d = sheet(box);
        LinearLayout br = h();
        br.setGravity(android.view.Gravity.CENTER);
        br.addView(btn("افزودن", new Tap() { public void go() {
            String t = U.str(eT);
            int w = U.parseMw(U.str(eW));
            if (t.length() == 0) { U.toast(InvoiceNewActivity.this, "عنوان را بنویسید"); return; }
            if (w <= 0) { U.toast(InvoiceNewActivity.this, "وزن معتبر وارد کنید"); return; }
            ln.title = t;
            ln.karat = karat[0];
            ln.wmw = w;
            ln.wtype = wtype[0];
            ln.wval = U.parseMoney(U.str(eWv));
            ln.stone = U.parseMoney(U.str(eS));
            if (existing == null) lines.add(ln);
            d.dismiss();
            renderLines();
            updTotals();
        } }), new LinearLayout.LayoutParams(dp(120), ViewGroup.LayoutParams.WRAP_CONTENT));
        br.addView(wspace(10));
        br.addView(gbtn("انصراف", new Tap() { public void go() { d.dismiss(); } }),
                new LinearLayout.LayoutParams(dp(110), ViewGroup.LayoutParams.WRAP_CONTENT));
        box.addView(br);
    }

    private TextView label2(String s) { return label(s); }

    private long[] totals() {
        long rate = db.currentRate();
        long taxp = db.getL("tax", 10);
        long gold = 0, wage = 0, stone = 0, tax = 0, total = 0;
        for (int li = 0; li < lines.size(); li++) {
            Line ln = (Line) lines.get(li);
            ln.unit = Math.round(rate * ln.karat / 750.0);
            ln.goldVal = Math.round(ln.wmw * ln.unit / 1000.0);
            ln.wage = ItemEditActivity.calcWage(ln.goldVal, ln.wmw, ln.wtype, ln.wval);
            ln.tax = Math.round((ln.wage) * taxp / 100.0);
            ln.total = ln.goldVal + ln.wage + ln.stone + ln.tax;
            gold += ln.goldVal; wage += ln.wage; stone += ln.stone; tax += ln.tax; total += ln.total;
        }
        return new long[]{gold, wage, stone, tax, total};
    }

    private long[] payEval() {
        long rate = db.currentRate();
        int w = mwOf(eGWeight);
        long rasub = (long) U.parseDouble(U.str(eRasub));
        long eq = U.equiv750(w, payKarat);
        long val = Math.round(w * (rate * payKarat / 750.0) / 1000.0 * (100 - rasub) / 100.0);
        return new long[]{w, eq, val};
    }

    private void renderLines() {
        linesBox.removeAllViews();
        if (lines.isEmpty()) {
            TextView e = tv("هنوز قلمی اضافه نشده است.", U.SUB, 13, false);
            e.setPadding(0, dp(6), 0, dp(6));
            linesBox.addView(e);
            return;
        }
        long taxp = db.getL("tax", 10);
        for (int i = 0; i < lines.size(); i++) {
            final Line ln = (Line) lines.get(i);
            final int idx = i;
            LinearLayout r = card(dp(8));
            r.setBackgroundDrawable(round(0xFF101723, 12, 0xFF232E40, 1));
            LinearLayout top = h();
            top.addView(tv(U.dig((i + 1) + "") + ". " + ln.title, U.TXT, 14, true),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            TextView del = tv(" ✕ ", U.BAD, 14, true);
            del.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { lines.remove(idx); renderLines(); updTotals(); }
            });
            top.addView(del);
            r.addView(top);
            // محاسبات برای نمایش
            long rate = db.currentRate();
            ln.unit = Math.round(rate * ln.karat / 750.0);
            ln.goldVal = Math.round(ln.wmw * ln.unit / 1000.0);
            ln.wage = ItemEditActivity.calcWage(ln.goldVal, ln.wmw, ln.wtype, ln.wval);
            ln.tax = Math.round(ln.wage * taxp / 100.0);
            ln.total = ln.goldVal + ln.wage + ln.stone + ln.tax;
            r.addView(tv(U.karatName(ln.karat) + " • " + U.mw(ln.wmw) + " گرم × " + U.money(ln.unit), U.SUB, 12, false));
            r.addView(kv("طلای خام", U.money(ln.goldVal)));
            if (ln.wage > 0) r.addView(kv("اجرت", U.money(ln.wage)));
            if (ln.stone > 0) r.addView(kv("سنگ/مزین", U.money(ln.stone)));
            if (ln.tax > 0) r.addView(kv("مالیات (" + U.pct((int) taxp) + ")", U.money(ln.tax)));
            LinearLayout tot = h();
            tot.addView(tv("جمع قلم", U.SUB, 12, false), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            tot.addView(tvM(U.money(ln.total) + " تومان", U.GOLD, 14));
            r.addView(tot);
            if (ln.itemId == 0) {
                TextView ed = gbtn("ویرایش", new Tap() { public void go() { editManual(ln); } });
                r.addView(ed);
            }
            linesBox.addView(r);
        }
    }

    private void updTotals() {
        long[] t = totals();
        long[] pe = payEval();
        long pcash = moneyOf(eCash);
        long total = t[4];
        long debt = total - pcash - pe[2];
        StringBuilder sb = new StringBuilder();
        sb.append("طلای خام: ").append(U.money(t[0])).append(" تومان\n");
        sb.append("اجرت: ").append(U.money(t[1])).append("  •  سنگ: ").append(U.money(t[2])).append("\n");
        sb.append("مالیات: ").append(U.money(t[3])).append(" تومان\n");
        sb.append("جمع کل: ").append(U.money(total)).append(" تومان");
        if (pe[0] > 0) {
            sb.append("\nطلا دریافتی: ").append(U.mw((int) pe[0])).append(" گرم ");
            sb.append(U.karatName(payKarat)).append(" ≈ ").append(U.money(pe[2])).append(" تومان");
        }
        sumTv.setText(sb.toString());
        if (debt > 0) { debtTv.setText("مانده (بدهی مشتری): " + U.money(debt) + " تومان"); debtTv.setTextColor(0xFFFFCC80); }
        else if (debt < 0) { debtTv.setText("اضافه‌پرداخت (بستانکاری): " + U.money(-debt) + " تومان"); debtTv.setTextColor(U.OK); }
        else { debtTv.setText("تسویه کامل ✓"); debtTv.setTextColor(U.OK); }
    }

    private void save(boolean x) {
        long rate = db.currentRate();
        if (rate <= 0) { msg("نرخ روز ثبت نشده", "قبل از صدور فاکتور، نرخ امروز طلا را از بخش «نرخ طلا» ثبت کنید."); return; }
        if (lines.isEmpty()) { U.toast(this, "حداقل یک قلم اضافه کنید"); return; }
        long[] t = totals();
        long[] pe = payEval();
        long pcash = moneyOf(eCash);
        long total = t[4];
        long debt = total - pcash - pe[2];
        if (debt != 0 && cid == 0) {
            msg("مشتری انتخاب نشده", "فاکتور با مانده (بدهی/بستانکاری) نیاز به انتخاب مشتری دارد. یا تسویه را کامل کنید.");
            return;
        }
        if (debt < 0 && pcash + pe[2] > total && cid == 0) { /* ناچیز */ }

        SQLiteDatabase w = db.w();
        w.beginTransaction();
        try {
            long ts = System.currentTimeMillis();
            android.content.ContentValues iv = new android.content.ContentValues();
            iv.put("ts", ts);
            iv.put("date_j", dateJ);
            iv.put("cid", cid);
            iv.put("cname", cname);
            iv.put("rate", rate);
            iv.put("goldval", t[0]);
            iv.put("wage", t[1]);
            iv.put("stone", t[2]);
            iv.put("tax", t[3]);
            iv.put("total", total);
            iv.put("pcash", pcash);
            iv.put("pgold_mw", pe[0]);
            iv.put("pgold_val", pe[2]);
            iv.put("pgold_karat", pe[0] > 0 ? payKarat : 0);
            iv.put("debt", debt);
            iv.put("note", U.str(eNote));
            long iid = w.insert("invoices", null, iv);

            for (int li = 0; li < lines.size(); li++) {
            Line ln = (Line) lines.get(li);
                android.content.ContentValues lv = new android.content.ContentValues();
                lv.put("iid", iid);
                lv.put("item_id", ln.itemId);
                lv.put("title", ln.title);
                lv.put("karat", ln.karat);
                lv.put("wmw", ln.wmw);
                lv.put("unit", ln.unit);
                lv.put("wage", ln.wage);
                lv.put("stone", ln.stone);
                lv.put("tax", ln.tax);
                lv.put("total", ln.total);
                w.insert("invoice_lines", null, lv);
                if (ln.itemId > 0) {
                    android.content.ContentValues upd = new android.content.ContentValues();
                    upd.put("status", "sold");
                    w.update("items", upd, "id=?", new String[]{"" + ln.itemId});
                }
            }
            if (pcash > 0) {
                android.content.ContentValues cx = new android.content.ContentValues();
                cx.put("ts", ts); cx.put("date_j", dateJ); cx.put("kind", "in");
                cx.put("amount", pcash); cx.put("descr", "دریافت نقدی بابت فاکتور شماره " + U.dig(iid + "")); cx.put("iid", iid);
                w.insert("cash_tx", null, cx);
            }
            if (pe[0] > 0) {
                android.content.ContentValues gt = new android.content.ContentValues();
                gt.put("ts", ts); gt.put("date_j", dateJ); gt.put("kind", "in");
                gt.put("wmw", pe[0]); gt.put("karat", payKarat);
                gt.put("descr", "طلا دریافتی از مشتری بابت فاکتور " + U.dig(iid + "")); gt.put("cid", cid);
                w.insert("gold_tx", null, gt);
            }
            if (debt != 0) {
                android.content.ContentValues ct = new android.content.ContentValues();
                ct.put("cid", cid); ct.put("ts", ts); ct.put("date_j", dateJ);
                ct.put("cash", debt); ct.put("goldmw", 0);
                ct.put("descr", (debt > 0 ? "مانده فاکتور شماره " : "اضافه‌پرداخت فاکتور شماره ") + U.dig(iid + ""));
                ct.put("iid", iid);
                w.insert("customer_tx", null, ct);
            }
            w.setTransactionSuccessful();
            U.toast(this, "فاکتور شماره " + U.dig(iid + "") + " ثبت شد ✓");
            Intent it = new Intent(this, InvoiceViewActivity.class);
            it.putExtra("id", (int) iid);
            startActivity(it);
            finish();
        } catch (Exception e) {
            msg("خطا در ثبت", e.getMessage() == null ? "خطای ناشناخته" : e.getMessage());
        } finally {
            w.endTransaction();
        }
    }

    private void exportPreInvoicePdf() {
        long rate = db.currentRate();
        if (rate <= 0) {
            msg("نرخ روز ثبت نشده", "قبل از صدور پیش‌فاکتور، نرخ امروز طلا را از بخش «نرخ طلا» ثبت کنید.");
            return;
        }
        if (lines.isEmpty()) {
            U.toast(this, "حداقل یک قلم اضافه کنید");
            return;
        }
        long[] t = totals();
        long[] pe = payEval();
        long pcash = moneyOf(eCash);
        long total = t[4];
        long debt = total - pcash - pe[2];

        ArrayList<InvoicePdf.LineInfo> pdfLines = new ArrayList<InvoicePdf.LineInfo>();
        for (int i = 0; i < lines.size(); i++) {
            Line ln = (Line) lines.get(i);
            pdfLines.add(new InvoicePdf.LineInfo(ln.title, ln.karat, ln.wmw, ln.unit, ln.total));
        }

        InvoicePdf.generateAndShare(this, db, true, 0, dateJ, cname, rate, total, pcash, pe[0], pe[2], payKarat, debt, U.str(eNote), pdfLines);
    }
}

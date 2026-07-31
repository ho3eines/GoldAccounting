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

/** ثبت سند مرکزی — ~۴۲ نوع سند با فرم پویا */
public class DocNewActivity extends A {

    // گروه‌ها: {عنوان گروه, کلیدهای نوع}
    static final String[][] GROUPS = {
            {"⚖️ آبشده و متفرقه (طلا)", "gold_buy,gold_sell,gold_in,gold_out,gold_talab,gold_bedehi"},
            {"🪙 سکه", "coin_buy,coin_sell,coin_in,coin_out,coin_talab,coin_bedehi"},
            {"🧱 شمش", "bull_buy,bull_sell,bull_in,bull_out,bull_talab,bull_bedehi"},
            {"💵 ارز", "cur_buy,cur_sell,cur_talab,cur_bedehi"},
            {"🥈 نقره", "sil_buy,sil_sell,sil_talab,sil_bedehi"},
            {"💍 کارساخته", "work_buy,work_sell,work_in,work_out,work_talab,work_bedehi"},
            {"💰 نقدی", "cash_income,cash_expense,cash_in,cash_out,cash_talab,cash_bedehi"},
            {"🏦 بانکی", "bank_income,bank_expense,bank_recv,bank_pay"},
            {"📄 چک", "check_recv,check_pay"},
            {"🏷️ تخفیف", "disc_us,disc_cust"},
    };
    static final String[][] TYPES = {
            {"gold_buy","خرید طلا/آبشده از مشتری"},{"gold_sell","فروش طلا/آبشده به مشتری"},
            {"gold_in","ورود آبشده و متفرقه"},{"gold_out","خروج آبشده و متفرقه"},
            {"gold_talab","طلب ما طلایی از مشتری (وزنی)"},{"gold_bedehi","بدهی ما طلایی به مشتری (وزنی)"},
            {"coin_buy","خرید سکه"},{"coin_sell","فروش سکه"},{"coin_in","ورود سکه"},{"coin_out","خروج سکه"},
            {"coin_talab","طلب ما سکه از مشتری"},{"coin_bedehi","بدهی ما سکه به مشتری"},
            {"bull_buy","خرید شمش (تعدادی)"},{"bull_sell","فروش شمش (تعدادی)"},
            {"bull_in","ورود شمش (تعدادی)"},{"bull_out","خروج شمش (تعدادی)"},
            {"bull_talab","طلب ما شمش از مشتری"},{"bull_bedehi","بدهی ما شمش به مشتری"},
            {"cur_buy","خرید ارز"},{"cur_sell","فروش ارز"},
            {"cur_talab","طلب ما ارزی از مشتری"},{"cur_bedehi","بدهی ما ارزی به مشتری"},
            {"sil_buy","خرید نقره"},{"sil_sell","فروش نقره"},
            {"sil_talab","طلب ما نقره از مشتری"},{"sil_bedehi","بدهی ما نقره به مشتری"},
            {"work_buy","خرید کارساخته"},{"work_sell","فروش کارساخته"},
            {"work_in","ورود کارساخته"},{"work_out","خروج کارساخته"},
            {"work_talab","طلب ما کارساخته از مشتری (وزنی)"},{"work_bedehi","بدهی ما کارساخته به مشتری (وزنی)"},
            {"cash_income","درآمد نقدی"},{"cash_expense","هزینه نقدی"},
            {"cash_in","ورود وجه نقد"},{"cash_out","خروج وجه نقد"},
            {"cash_talab","طلب ما مالی از مشتری"},{"cash_bedehi","بدهی ما مالی به مشتری"},
            {"bank_income","درآمد به بانک"},{"bank_expense","هزینه از بانک"},
            {"bank_recv","دریافت از مشتری به بانک"},{"bank_pay","پرداخت به مشتری از بانک"},
            {"check_recv","چک دریافتی (ورود چک)"},{"check_pay","چک پرداختنی (خروج چک)"},
            {"disc_us","تخفیف ما به مشتری"},{"disc_cust","تخفیف مشتری به ما"},
    };

    static String typeLabel(String key) {
        for (String[] t : TYPES) if (t[0].equals(key)) return t[1];
        return key;
    }

    // حالت فرم
    private String type = "";
    private int cid = 0;
    private String cname = "";
    private int karat = 750, defId = 0, bankId = 0;
    private String defName = "", bankName = "";
    private int settle = 0; // 0=به حساب 1=نقدی 2=بانکی
    private String dateJ;

    private TextView typeTv, custTv, defTv, bankTv, dateTv;
    private EditText eW, eKar, eCount, eQty, eMoney, eRate, eWage, eNo, eDue, eNote;
    private LinearLayout formBox, previewBox;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        scaffold("ثبت سند جدید", true);
        dateJ = Jal.today();

        // کارت انتخاب نوع
        LinearLayout sel = cardHi();
        sel.addView(tv("نوع سند", U.SUB, 12, false));
        typeTv = tv("انتخاب کنید…", U.GOLD, 17, true);
        typeTv.setPadding(0, dp(4), 0, dp(4));
        sel.addView(typeTv);
        sel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pickGroup(); }
        });
        body.addView(sel);

        // تاریخ و طرف حساب
        LinearLayout top = card();
        LinearLayout dr = h();
        dr.addView(tv("تاریخ سند: ", U.SUB, 12, false));
        dateTv = tvM(U.dig(dateJ), U.TXT, 14);
        dr.addView(dateTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        dr.addView(gbtn("تغییر", new Tap() { public void go() { askDate(); } }));
        top.addView(dr);
        LinearLayout cr = h();
        cr.addView(tv("طرف حساب: ", U.SUB, 12, false));
        custTv = tvM("—", U.TXT, 14);
        cr.addView(custTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        cr.addView(gbtn("انتخاب", new Tap() { public void go() {
            pickCustomer(new OnCustomer() {
                public void ok(int id, String name) {
                    cid = id; cname = name;
                    custTv.setText(name != null && name.length() > 0 ? name : "—");
                    preview();
                }
            });
        } }));
        top.addView(cr);
        body.addView(top);

        // جعبه فیلدهای پویا
        LinearLayout dynCard = card();
        dynCard.addView(tv("مشخصات سند", U.GOLD, 13, true));
        formBox = v();
        dynCard.addView(formBox);
        body.addView(dynCard);

        // پیش‌نمایش ردیف‌ها
        LinearLayout pv = cardHi();
        pv.addView(tv("پیش‌نمایش اثرات سند", U.SUB, 12, false));
        previewBox = v();
        pv.addView(previewBox);
        body.addView(pv);

        addBtn(body, btn("⬇  ثبت سند", new Tap() { public void go() { save(); } }));

        // ورود با پارامتر (مثلاً از پروندهٔ مشتری)
        Intent it0 = getIntent();
        int c0 = it0.getIntExtra("cid", 0);
        if (c0 > 0) {
            cid = c0;
            String nm = it0.getStringExtra("cname");
            if (nm == null || nm.length() == 0) {
                android.database.Cursor qc = db.r().rawQuery("SELECT name FROM customers WHERE id=?",
                        new String[]{"" + c0});
                if (qc.moveToFirst()) nm = qc.getString(0);
                qc.close();
            }
            cname = nm == null ? "" : nm;
            custTv.setText(cname.length() > 0 ? cname : "—");
        }
        String t0 = it0.getStringExtra("type");
        if (t0 != null && t0.length() > 0) {
            type = t0;
            typeTv.setText(typeLabel(type));
            buildForm();
        }
        preview();
    }

    // ---------- انتخاب نوع ----------
    private void pickGroup() {
        String[] gs = new String[GROUPS.length];
        for (int i = 0; i < GROUPS.length; i++) gs[i] = GROUPS[i][0];
        choose("گروه سند", gs, new OnIdx() {
            public void ok(int gi) {
                final String[] keys = GROUPS[gi][1].split(",");
                String[] ls = new String[keys.length];
                for (int i = 0; i < keys.length; i++) ls[i] = typeLabel(keys[i]);
                choose("نوع سند — " + GROUPS[gi][0], ls, new OnIdx() {
                    public void ok(int ti) {
                        type = keys[ti];
                        typeTv.setText(typeLabel(type));
                        buildForm();
                    }
                });
            }
        });
    }

    // ---------- ساختارهای پویا ----------
    private boolean is(String s) { return type.startsWith(s); }
    private boolean oneOf(String... ks) { for (String k : ks) if (type.equals(k)) return true; return false; }

    private boolean needCustomer() { return !oneOf("gold_in","gold_out","coin_in","coin_out","bull_in","bull_out","work_in","work_out","cash_income","cash_expense","cash_in","cash_out","bank_income","bank_expense"); }
    private boolean needWeight() { return is("gold") || is("sil") || is("work"); }
    private boolean needKarat()  { return is("gold") || is("work"); }
    private boolean needDef()    { return is("coin") || is("bull") || is("cur") || is("sil") || is("work"); }
    private String defKind() {
        if (is("coin")) return "coin";
        if (is("bull")) return "bullion";
        if (is("cur")) return "curr";
        if (is("sil")) return "silver";
        if (is("work")) return "work";
        return "";
    }
    private String defTitle() {
        if (is("coin")) return "نوع سکه";
        if (is("bull")) return "نوع شمش";
        if (is("cur")) return "نوع ارز";
        if (is("sil")) return "نوع نقره";
        if (is("work")) return "نام کار (کارساخته)";
        return "نوع";
    }
    private boolean needCount()   { return is("coin") || is("bull"); }
    private boolean needQty()     { return is("cur"); }
    private boolean needMoney()   {
        return oneOf("gold_buy","gold_sell","coin_buy","coin_sell","bull_buy","bull_sell","cur_buy","cur_sell",
                "sil_buy","sil_sell","work_buy","work_sell",
                "cash_income","cash_expense","cash_in","cash_out","cash_talab","cash_bedehi",
                "bank_income","bank_expense","bank_recv","bank_pay","check_recv","check_pay","disc_us","disc_cust");
    }
    private boolean needSettle()  { return oneOf("gold_buy","gold_sell","coin_buy","coin_sell","bull_buy","bull_sell","cur_buy","cur_sell","sil_buy","sil_sell","work_buy","work_sell"); }
    private boolean needBank()    { return is("bank") || is("check") || (needSettle() && settle == 2); }
    private boolean needCheck()   { return is("check"); }
    private boolean needWage()    { return oneOf("work_buy","work_sell"); }
    private boolean needRateCol() { return oneOf("cur_buy","cur_sell"); }
    private boolean isFlowOut()   { return type.endsWith("_buy") || type.equals("bank_pay") || type.equals("cash_expense") || type.equals("cash_out") || oneOf("gold_buy"); }
    private boolean movement()    { return needSettle() || oneOf("gold_in","gold_out","coin_in","coin_out","bull_in","bull_out","work_in","work_out"); }

    private void buildForm() {
        formBox.removeAllViews();

        if (needDef()) {
            formBox.addView(label(defTitle()));
            LinearLayout rr = h();
            defTv = tvM(defName.length() == 0 ? "انتخاب…" : defName, U.TXT, 14);
            rr.addView(defTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            rr.addView(gbtn("انتخاب", new Tap() { public void go() { pickDef(); } }));
            formBox.addView(rr);
        }
        if (needWeight()) {
            formBox.addView(label(is("work") || is("sil") ? "وزن (گرم، با سوت — مثلاً ۱۲٫۳۵۰)" : "وزن (گرم و سوت — مثلاً ۱۲٫۳۵۰)"));
            eW = in("۰", true); formBox.addView(eW);
        }
        if (needCount()) {
            formBox.addView(label("تعداد (عدد)"));
            eCount = in("۰", true); formBox.addView(eCount);
        }
        if (needQty()) {
            formBox.addView(label("مقدار ارز"));
            eQty = in("۰", true); formBox.addView(eQty);
        }
        if (needKarat()) {
            formBox.addView(label("عیار"));
            formBox.addView(chipsRow(new String[]{"۲۴","۲۲","۲۱","۱۸","۱۴","۹"}, 3, new OnIdx() {
                public void ok(int i) { karat = ItemEditActivity.K_VALS[i]; preview(); }
            }));
        }
        if (needRateCol()) {
            formBox.addView(label("نرخ هر واحد ارز به تومان"));
            eRate = in("", true); formBox.addView(eRate);
            eRate.addTextChangedListener(watcher());
        }
        if (needWage()) {
            formBox.addView(label("اجرت/کارمزد هر گرم (تومان، اختیاری)"));
            eWage = in("", true); formBox.addView(eWage);
        }
        if (needMoney()) {
            formBox.addView(label(is("check") || type.startsWith("disc") ? "مبلغ (تومان)" : "مبلغ مالی (تومان)"));
            eMoney = in("۰", true); formBox.addView(eMoney);
            eMoney.addTextChangedListener(watcher());
        }
        if (needCheck()) {
            formBox.addView(label("شماره چک"));
            eNo = in("", true); formBox.addView(eNo);
            formBox.addView(label("سررسید (مثل ۱۴۰۵/۰۶/۱۵)"));
            eDue = in(Jal.today(), false); formBox.addView(eDue);
        }
        if (needSettle()) {
            formBox.addView(label("نحوه تسویهٔ مالی"));
            formBox.addView(chipsRow(new String[]{"به حساب مشتری", "نقدی از صندوق", "بانکی"}, 0, new OnIdx() {
                public void ok(int i) { settle = i; buildForm(); }
            }));
        }
        if (needBank()) {
            formBox.addView(label("بانک"));
            LinearLayout rr = h();
            bankTv = tvM(bankName.length() == 0 ? "انتخاب بانک…" : bankName, U.TXT, 14);
            rr.addView(bankTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            rr.addView(gbtn("انتخاب", new Tap() { public void go() { pickBank(); } }));
            formBox.addView(rr);
        }
        formBox.addView(label("شرح سند"));
        eNote = in("اختیاری");
        formBox.addView(eNote);
        preview();
    }

    private android.text.TextWatcher watcher() {
        return new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { preview(); }
            public void afterTextChanged(android.text.Editable s) {}
        };
    }

    private void pickDef() {
        final String kind = defKind();
        defPicker(kind, defTitle(), new OnDef() {
            public void ok(int id, String name) { defId = id; defName = name; defTv.setText(name); preview(); }
        });
    }
    private void pickBank() {
        defPicker("bank", "انتخاب بانک", new OnDef() {
            public void ok(int id, String name) { bankId = id; bankName = name; bankTv.setText(name); preview(); }
        });
    }

    interface OnDef { void ok(int id, String name); }
    void defPicker(String kind, String title, final OnDef cb) {
        Cursor c = db.defsOf(kind);
        final java.util.ArrayList<Integer> ids = new java.util.ArrayList<Integer>();
        final java.util.ArrayList<String> names = new java.util.ArrayList<String>();
        while (c.moveToNext()) { ids.add(c.getInt(0)); names.add(c.getString(2)); }
        c.close();
        if (names.isEmpty()) {
            msg("تعریف نشده", "ابتدا از بخش «کدینگ‌ها» یک " + title + " تعریف کنید.");
            return;
        }
        String[] arr = new String[names.size()];
        for (int i = 0; i < names.size(); i++) arr[i] = (String) names.get(i);
        choose(title, arr, new OnIdx() {
            public void ok(int i) { cb.ok(((Integer) ids.get(i)).intValue(), (String) names.get(i)); }
        });
    }

    // ---------- پیش‌نمایش ----------
    private String moneyF() {
        if (eMoney != null) return U.str(eMoney);
        return "0";
    }
    private long amountMoney() {
        if (needRateCol() && eQty != null && eRate != null) {
            double q = U.parseDouble(U.str(eQty));
            long r = U.parseMoney(U.str(eRate));
            if (q > 0 && r > 0) return Math.round(q * r);
        }
        return U.parseMoney(moneyF());
    }

    private String settleTxt() {
        return settle == 0 ? "به حساب مشتری" : settle == 1 ? "نقدی از صندوق" : "بانکی" + (bankName.length() > 0 ? " (" + bankName + ")" : "");
    }

    private void preview() {
        if (previewBox == null) return;
        previewBox.removeAllViews();
        if (type.length() == 0) {
            previewBox.addView(tv("ابتدا نوع سند را انتخاب کنید.", U.SUB, 13, false));
            return;
        }
        java.util.ArrayList<String> lines = buildLines(false);
        for (int i = 0; i < lines.size(); i++) {
            TextView t = tv("• " + (String) lines.get(i), U.TXT, 13, false);
            t.setPadding(0, dp(2), 0, dp(2));
            previewBox.addView(t);
        }
    }

    /** ساخت ردیف‌های شرح سند برای پیش‌نمایش/ثبت */
    private java.util.ArrayList<String> buildLines(boolean finalForm) {
        java.util.ArrayList<String> L = new java.util.ArrayList<String>();
        String who = cname != null && cname.length() > 0 ? cname : "فروشگاه";
        String L1 = typeLabel(type);
        L.add(L1 + (cid > 0 ? " — طرف حساب: " + who : ""));
        if (is("gold") && eW != null) {
            int w = U.parseMw(U.str(eW));
            if (w > 0) L.add(U.gs(w) + " طلای " + U.karatName(karat));
        }
        if ((is("coin") || is("bull")) && eCount != null && defId > 0) {
            long n = U.parseMoney(U.str(eCount));
            L.add(U.intFa(n) + " عدد " + defName);
        }
        if (is("cur") && eQty != null && defId > 0) {
            double q = U.parseDouble(U.str(eQty));
            if (q > 0) L.add(U.dig(q + "") + " " + defName);
        }
        if ((is("sil") || is("work")) && eW != null && needWeight()) {
            int w = U.parseMw(U.str(eW));
            if (w > 0 && !is("gold")) L.add(U.gs(w) + (defName.length() > 0 ? " " + defName : "") + (is("work") ? " • " + U.karatName(karat) : ""));
        }
        long m = amountMoney();
        if (m > 0 && needMoney()) {
            String t = "مبلغ: " + U.money(m) + " تومان";
            if (needSettle()) t += " — " + settleTxt();
            L.add(t);
        }
        if (needCheck() && eNo != null && U.str(eNo).length() > 0) {
            L.add("چک شماره " + U.dig(U.str(eNo)) + " • سررسید " + U.str(eDue) + (bankName.length() > 0 ? " • " + bankName : ""));
        }
        if (is("bank") && bankName.length() > 0) L.add("بانک: " + bankName);
        String note = eNote != null ? U.str(eNote) : "";
        if (note.length() > 0) L.add("شرح: " + note);
        if (L.size() == 1) L.add("…");
        return L;
    }

    // ---------- ثبت ----------
    private void save() {
        if (type.length() == 0) { U.toast(this, "نوع سند را انتخاب کنید"); return; }
        if (needCustomer() && cid <= 0) { msg("طرف حساب لازم است", "برای این نوع سند باید یک مشتری/حساب انتخاب شود."); return; }
        if (needDef() && defId <= 0 && !is("gold")) { msg(defTitle() + "؟", "ابتدا «" + defTitle() + "» را انتخاب کنید."); return; }
        if (needBank() && bankId <= 0) { msg("بانک؟", "بانک را انتخاب کنید."); return; }
        long m = amountMoney();
        int w = needWeight() && eW != null ? U.parseMw(U.str(eW)) : 0;
        long nCnt = needCount() && eCount != null ? U.parseMoney(U.str(eCount)) : 0;
        double qCur = needQty() && eQty != null ? U.parseDouble(U.str(eQty)) : 0;
        if (needWeight() && w <= 0) { U.toast(this, "وزن معتبر وارد کنید"); return; }
        if (needCount() && nCnt <= 0) { U.toast(this, "تعداد معتبر وارد کنید"); return; }
        if (needQty() && qCur <= 0) { U.toast(this, "مقدار ارز را وارد کنید"); return; }
        if (needMoney() && m <= 0) { U.toast(this, "مبلغ معتبر وارد کنید"); return; }

        final java.util.ArrayList<String> lines = buildLines(true);
        SQLiteDatabase wdb = db.w();
        wdb.beginTransaction();
        try {
            String note = eNote != null ? U.str(eNote) : "";
            String descr = typeLabel(type) + (note.length() > 0 ? " — " + note : "");
            long docId = Post.doc(db, dateJ, descr);
            int seq = 1;
            for (int i = 0; i < lines.size(); i++) Post.line(db, docId, seq++, (String) lines.get(i));
            applyEffects(docId, w, karat, nCnt, qCur, m, who());
            wdb.setTransactionSuccessful();
            U.toast(this, "سند شماره " + U.dig(docId + "") + " ثبت شد ✓");
            Intent it = new Intent(this, DocsActivity.class);
            it.putExtra("doc", (int) docId);
            startActivity(it);
            finish();
        } catch (Exception e) {
            msg("خطا در ثبت", e.getMessage() == null ? "خطا" : e.getMessage());
        } finally {
            wdb.endTransaction();
        }
    }

    private String who() { return cname != null && cname.length() > 0 ? cname : "فروشگاه"; }

    /** اثرات حسابداری هر نوع سند */
    private void applyEffects(long docId, int w, int karat, long nCnt, double qCur, long m, String who) {
        String gAsset = defId > 0 ? defKind().substring(0, Math.min(4, defKind().length())) + "_d" + defId : "";
        if (is("coin")) gAsset = "coin_d" + defId;
        if (is("bull")) gAsset = "bull_d" + defId;
        if (is("cur"))  gAsset = "cur_d" + defId;
        if (is("sil"))  gAsset = "sil_d" + defId;
        String d1 = "سند " + U.dig(docId + "") + "؛ ";

        // آبشده و طلا
        if (type.equals("gold_buy")) {
            long eq = U.equiv750(w, karat);
            Post.gold(db, docId, dateJ, "in", w, karat, d1 + "خرید طلا از " + who, cid);
            Post.cust(db, docId, dateJ, cid, settleMoney(-1, m), -eq, d1 + "خرید " + U.gs(w) + " طلا توسط فروشگاه");
            settleOut(docId, m);
        } else if (type.equals("gold_sell")) {
            long eq = U.equiv750(w, karat);
            Post.gold(db, docId, dateJ, "out", w, karat, d1 + "فروش طلا به " + who, cid);
            Post.cust(db, docId, dateJ, cid, settleMoney(+1, m), +eq, d1 + "فروش " + U.gs(w) + " طلا به مشتری");
            settleIn(docId, m);
        } else if (type.equals("gold_in")) {
            Post.gold(db, docId, dateJ, "in", w, karat, d1 + "ورود آبشده دستی", 0);
        } else if (type.equals("gold_out")) {
            Post.gold(db, docId, dateJ, "out", w, karat, d1 + "خروج آبشده دستی", 0);
        } else if (type.equals("gold_talab")) {
            Post.cust(db, docId, dateJ, cid, 0, +U.equiv750(w, karat), d1 + "طلب طلایی " + U.gs(w));
        } else if (type.equals("gold_bedehi")) {
            Post.cust(db, docId, dateJ, cid, 0, -U.equiv750(w, karat), d1 + "بدهی طلایی " + U.gs(w));
        }
        // سکه
        else if (type.equals("coin_buy")) {
            Post.asset(db, docId, dateJ, "stock", gAsset, +nCnt, 0, 0, d1 + "خرید " + whoTxt(nCnt));
            Post.asset(db, docId, dateJ, "customer", gAsset, -nCnt, 0, cid, d1 + "خرید سکه توسط فروشگاه");
            Post.cust(db, docId, dateJ, cid, settleMoney(-1, m), 0, d1 + "خرید سکه از مشتری");
            settleOut(docId, m);
        } else if (type.equals("coin_sell")) {
            Post.asset(db, docId, dateJ, "stock", gAsset, -nCnt, 0, 0, d1 + "فروش " + whoTxt(nCnt));
            Post.asset(db, docId, dateJ, "customer", gAsset, +nCnt, 0, cid, d1 + "فروش سکه به مشتری");
            Post.cust(db, docId, dateJ, cid, settleMoney(+1, m), 0, d1 + "فروش سکه به مشتری");
            settleIn(docId, m);
        } else if (type.equals("coin_in")) {
            Post.asset(db, docId, dateJ, "stock", gAsset, +nCnt, 0, 0, d1 + "ورود سکه");
        } else if (type.equals("coin_out")) {
            Post.asset(db, docId, dateJ, "stock", gAsset, -nCnt, 0, 0, d1 + "خروج سکه");
        } else if (type.equals("coin_talab")) {
            Post.asset(db, docId, dateJ, "customer", gAsset, +nCnt, 0, cid, d1 + "طلب سکه " + whoTxt(nCnt));
        } else if (type.equals("coin_bedehi")) {
            Post.asset(db, docId, dateJ, "customer", gAsset, -nCnt, 0, cid, d1 + "بدهی سکه " + whoTxt(nCnt));
        }
        // شمش — همان الگوی سکه
        else if (type.equals("bull_buy")) {
            Post.asset(db, docId, dateJ, "stock", gAsset, +nCnt, 0, 0, d1 + "خرید " + whoTxt(nCnt));
            Post.asset(db, docId, dateJ, "customer", gAsset, -nCnt, 0, cid, d1 + "خرید شمش توسط فروشگاه");
            Post.cust(db, docId, dateJ, cid, settleMoney(-1, m), 0, d1 + "خرید شمش از مشتری");
            settleOut(docId, m);
        } else if (type.equals("bull_sell")) {
            Post.asset(db, docId, dateJ, "stock", gAsset, -nCnt, 0, 0, d1 + "فروش " + whoTxt(nCnt));
            Post.asset(db, docId, dateJ, "customer", gAsset, +nCnt, 0, cid, d1 + "فروش شمش به مشتری");
            Post.cust(db, docId, dateJ, cid, settleMoney(+1, m), 0, d1 + "فروش شمش به مشتری");
            settleIn(docId, m);
        } else if (type.equals("bull_in")) {
            Post.asset(db, docId, dateJ, "stock", gAsset, +nCnt, 0, 0, d1 + "ورود شمش");
        } else if (type.equals("bull_out")) {
            Post.asset(db, docId, dateJ, "stock", gAsset, -nCnt, 0, 0, d1 + "خروج شمش");
        } else if (type.equals("bull_talab")) {
            Post.asset(db, docId, dateJ, "customer", gAsset, +nCnt, 0, cid, d1 + "طلب شمش");
        } else if (type.equals("bull_bedehi")) {
            Post.asset(db, docId, dateJ, "customer", gAsset, -nCnt, 0, cid, d1 + "بدهی شمش");
        }
        // ارز
        else if (type.equals("cur_buy")) {
            Post.asset(db, docId, dateJ, "stock", gAsset, +qCur, 0, 0, d1 + "خرید " + who());
            Post.asset(db, docId, dateJ, "customer", gAsset, -qCur, 0, cid, d1 + "خرید ارز توسط فروشگاه");
            Post.cust(db, docId, dateJ, cid, settleMoney(-1, m), 0, d1 + "خرید ارز از مشتری");
            settleOut(docId, m);
        } else if (type.equals("cur_sell")) {
            Post.asset(db, docId, dateJ, "stock", gAsset, -qCur, 0, 0, d1 + "فروش " + who());
            Post.asset(db, docId, dateJ, "customer", gAsset, +qCur, 0, cid, d1 + "فروش ارز به مشتری");
            Post.cust(db, docId, dateJ, cid, settleMoney(+1, m), 0, d1 + "فروش ارز به مشتری");
            settleIn(docId, m);
        } else if (type.equals("cur_talab")) {
            Post.asset(db, docId, dateJ, "customer", gAsset, +qCur, 0, cid, d1 + "طلب ارزی");
        } else if (type.equals("cur_bedehi")) {
            Post.asset(db, docId, dateJ, "customer", gAsset, -qCur, 0, cid, d1 + "بدهی ارزی");
        }
        // نقره
        else if (type.equals("sil_buy")) {
            Post.asset(db, docId, dateJ, "stock", gAsset, +w, 0, 0, d1 + "خرید " + U.gs(w) + " نقره");
            Post.asset(db, docId, dateJ, "customer", gAsset, -w, 0, cid, d1 + "خرید نقره توسط فروشگاه");
            Post.cust(db, docId, dateJ, cid, settleMoney(-1, m), 0, d1 + "خرید نقره از مشتری");
            settleOut(docId, m);
        } else if (type.equals("sil_sell")) {
            Post.asset(db, docId, dateJ, "stock", gAsset, -w, 0, 0, d1 + "فروش " + U.gs(w) + " نقره");
            Post.asset(db, docId, dateJ, "customer", gAsset, +w, 0, cid, d1 + "فروش نقره به مشتری");
            Post.cust(db, docId, dateJ, cid, settleMoney(+1, m), 0, d1 + "فروش نقره به مشتری");
            settleIn(docId, m);
        } else if (type.equals("sil_talab")) {
            Post.asset(db, docId, dateJ, "customer", gAsset, +w, 0, cid, d1 + "طلب نقره");
        } else if (type.equals("sil_bedehi")) {
            Post.asset(db, docId, dateJ, "customer", gAsset, -w, 0, cid, d1 + "بدهی نقره");
        }
        // کارساخته
        else if (type.equals("work_buy")) {
            Post.asset(db, docId, dateJ, "stock", "work_mg", +w, karat, 0, d1 + "خرید کارساخته " + defName);
            Post.asset(db, docId, dateJ, "customer", "work_mg", -w, karat, cid, d1 + "خرید کارساخته توسط فروشگاه");
            Post.cust(db, docId, dateJ, cid, settleMoney(-1, m), 0, d1 + "خرید کارساخته از مشتری");
            settleOut(docId, m);
        } else if (type.equals("work_sell")) {
            Post.asset(db, docId, dateJ, "stock", "work_mg", -w, karat, 0, d1 + "فروش کارساخته " + defName);
            Post.asset(db, docId, dateJ, "customer", "work_mg", +w, karat, cid, d1 + "فروش کارساخته به مشتری");
            Post.cust(db, docId, dateJ, cid, settleMoney(+1, m), 0, d1 + "فروش کارساخته به مشتری");
            settleIn(docId, m);
        } else if (type.equals("work_in")) {
            Post.asset(db, docId, dateJ, "stock", "work_mg", +w, karat, 0, d1 + "ورود کارساخته " + defName);
        } else if (type.equals("work_out")) {
            Post.asset(db, docId, dateJ, "stock", "work_mg", -w, karat, 0, d1 + "خروج کارساخته " + defName);
        } else if (type.equals("work_talab")) {
            Post.asset(db, docId, dateJ, "customer", "work_mg", +w, karat, cid, d1 + "طلب کارساخته (وزنی)");
        } else if (type.equals("work_bedehi")) {
            Post.asset(db, docId, dateJ, "customer", "work_mg", -w, karat, cid, d1 + "بدهی کارساخته (وزنی)");
        }
        // نقدی
        else if (type.equals("cash_income"))  { Post.cash(db, docId, dateJ, "in", m, d1 + "درآمد نقدی" + whoTail()); }
        else if (type.equals("cash_expense")) { Post.cash(db, docId, dateJ, "out", m, d1 + "هزینه نقدی" + whoTail()); }
        else if (type.equals("cash_in"))      { Post.cash(db, docId, dateJ, "in", m, d1 + "ورود وجه نقد" + whoTail()); }
        else if (type.equals("cash_out"))     { Post.cash(db, docId, dateJ, "out", m, d1 + "خروج وجه نقد" + whoTail()); }
        else if (type.equals("cash_talab"))   { Post.cust(db, docId, dateJ, cid, +m, 0, d1 + "طلب مالی " + U.money(m)); }
        else if (type.equals("cash_bedehi"))  { Post.cust(db, docId, dateJ, cid, -m, 0, d1 + "بدهی مالی " + U.money(m)); }
        // بانکی
        else if (type.equals("bank_income"))  { Post.bank(db, dateJ, bankId, "in", m, d1 + "درآمد به بانک " + bankName, 0, docId); }
        else if (type.equals("bank_expense")) { Post.bank(db, dateJ, bankId, "out", m, d1 + "هزینه از بانک " + bankName, 0, docId); }
        else if (type.equals("bank_recv")) {
            Post.bank(db, dateJ, bankId, "in", m, d1 + "دریافت از " + who() + " به بانک " + bankName, cid, docId);
            Post.cust(db, docId, dateJ, cid, -m, 0, d1 + "دریافت مبلغ به بانک از مشتری");
        }
        else if (type.equals("bank_pay")) {
            Post.bank(db, dateJ, bankId, "out", m, d1 + "پرداخت به " + who() + " از بانک " + bankName, cid, docId);
            Post.cust(db, docId, dateJ, cid, +m, 0, d1 + "پرداخت مبلغ از بانک به مشتری");
        }
        // چک
        else if (type.equals("check_recv")) {
            String no = U.str(eNo), due = U.str(eDue);
            Post.check(db, dateJ, due, m, bankId, cid, who(), "recv", no, "سند " + docId, docId);
            Post.cust(db, docId, dateJ, cid, -m, 0, d1 + "دریافت چک شماره " + U.dig(no) + " از مشتری (سررسید " + due + ")");
        }
        else if (type.equals("check_pay")) {
            String no = U.str(eNo), due = U.str(eDue);
            Post.check(db, dateJ, due, m, bankId, cid, who(), "pay", no, "سند " + docId, docId);
            Post.cust(db, docId, dateJ, cid, +m, 0, d1 + "صدور چک شماره " + U.dig(no) + " به مشتری (سررسید " + due + ")");
        }
        // تخفیف
        else if (type.equals("disc_us"))   { Post.cust(db, docId, dateJ, cid, -m, 0, d1 + "تخفیف ما به مشتری " + U.money(m)); }
        else if (type.equals("disc_cust")) { Post.cust(db, docId, dateJ, cid, +m, 0, d1 + "تخفیف مشتری به ما " + U.money(m)); }
    }

    private String whoTxt(long n) { return U.intFa(n) + " عدد " + defName; }
    private String whoTail() { return ""; }

    /** مالی سند خرید/فروش — اگر به حساب باشد در customer_tx لحاظ می‌شود */
    private long settleMoney(int sign, long m) {
        return settle == 0 ? sign * m : 0;
    }
    /** خروج مالی فوری (خرید) */
    private void settleOut(long docId, long m) {
        if (settle == 1 && m > 0) Post.cash(db, docId, dateJ, "out", m, "سند " + U.dig(docId + "") + "؛ پرداخت نقدی بابت خرید");
        if (settle == 2 && m > 0) Post.bank(db, dateJ, bankId, "out", m, "سند " + U.dig(docId + "") + "؛ پرداخت بابت خرید", cid, docId);
    }
    /** ورود مالی فوری (فروش) */
    private void settleIn(long docId, long m) {
        if (settle == 1 && m > 0) Post.cash(db, docId, dateJ, "in", m, "سند " + U.dig(docId + "") + "؛ دریافت نقدی بابت فروش");
        if (settle == 2 && m > 0) Post.bank(db, dateJ, bankId, "in", m, "سند " + U.dig(docId + "") + "؛ دریافت بابت فروش", cid, docId);
    }

    private void askDate() {
        final Jal j = Jal.now();
        final EditText ey = in("سال", true); ey.setText(U.dig(j.y + ""));
        final EditText em = in("ماه", true); em.setText(U.dig(j.m + ""));
        final EditText eD = in("روز", true); eD.setText(U.dig(j.d + ""));
        final LinearLayout box = v();
        box.setPadding(dp(16), dp(14), dp(16), dp(10));
        box.addView(tv("تاریخ سند", U.GOLD, 16, true));
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
            if (y < 1300 || m < 1 || m > 12 || dd < 1 || dd > 31) { U.toast(DocNewActivity.this, "تاریخ نامعتبر"); return; }
            dateJ = y + "/" + (m < 10 ? "0" + m : "" + m) + "/" + (dd < 10 ? "0" + dd : "" + dd);
            dateTv.setText(U.dig(dateJ));
            d.dismiss();
        } }), new LinearLayout.LayoutParams(dp(120), ViewGroup.LayoutParams.WRAP_CONTENT));
        br.addView(wspace(10));
        br.addView(gbtn("انصراف", new Tap() { public void go() { d.dismiss(); } }),
                new LinearLayout.LayoutParams(dp(110), ViewGroup.LayoutParams.WRAP_CONTENT));
        box.addView(br);
    }
}

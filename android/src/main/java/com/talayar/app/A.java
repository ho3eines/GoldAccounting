package com.talayar.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** اکتیویتی پایه + جعبه‌ابزار رابط کاربری طلایی/تیره (راست‌به‌چپ) */
public class A extends Activity {
    protected Db db;
    protected LinearLayout body;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        U.initFonts(this);
        db = Db.get(this);
        Window w = getWindow();
        w.setStatusBarColor(0xFF070A10);
        w.setNavigationBarColor(0xFF070A10);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    }

    /** ساختار کلی صفحه: هدر + بدنه اسکرولی */
    protected void scaffold(String title, boolean back) {
        getWindow().getDecorView().setBackgroundColor(U.BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(U.BG);

        // هدر
        LinearLayout hd = new LinearLayout(this);
        hd.setOrientation(LinearLayout.HORIZONTAL);
        hd.setGravity(Gravity.CENTER_VERTICAL);
        hd.setPadding(dp(14), dp(14), dp(14), dp(10));
        if (back) {
            TextView bck = tv("❮", U.GOLD, 24, true);
            bck.setPadding(dp(4), 0, dp(10), 0);
            bck.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { finish(); } });
            hd.addView(bck);
        }
        LinearLayout tc = new LinearLayout(this);
        tc.setOrientation(LinearLayout.VERTICAL);
        tc.addView(tv(title, U.TXT, 20, true));
        hd.addView(tc, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        root.addView(hd);

        View line = new View(this);
        line.setBackgroundDrawable(grad(U.GOLD2, 0x00C99327, 0, dp(1), 0));
        root.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(false);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(12), dp(10), dp(12), dp(28));
        sv.addView(body);
        root.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
    }

    protected int dp(float v) { return U.dp(this, v); }

    // ---------- views ----------
    public TextView tv(String s, int color, float sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(sp);
        t.setTypeface(bold ? U.FB : U.F);
        t.setIncludeFontPadding(true);
        return t;
    }
    public TextView tvM(String s, int color, float sp) {
        TextView t = tv(s, color, sp, false);
        t.setTypeface(U.FM);
        return t;
    }
    public TextView label(String s) {
        TextView t = tv(s, U.SUB, 13, false);
        t.setPadding(dp(2), dp(8), dp(2), dp(4));
        return t;
    }

    public LinearLayout v() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }
    public LinearLayout h() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }
    public View space(float hd) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(hd)));
        return v;
    }
    public View wspace(float wd) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(wd), 1));
        return v;
    }

    /** کارت */
    public LinearLayout card() { return card(dp(12)); }
    public LinearLayout card(int pad) {
        LinearLayout l = v();
        l.setBackgroundDrawable(cardBg());
        l.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(5), 0, dp(5));
        l.setLayoutParams(lp);
        return l;
    }
    public LinearLayout cardHi() {
        LinearLayout l = card();
        l.setBackgroundDrawable(round(0xFF1B2331, 16, 0xFF3A4A63, 1));
        return l;
    }

    // ---------- drawable factories ----------
    public static GradientDrawable round(int fill, int radDp, int stroke, int strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(radDp * 2.8f);
        if (strokeDp > 0) g.setStroke((int)(strokeDp * 2.8f), stroke);
        return g;
    }
    public GradientDrawable grad(int c1, int c2, int angle, int strokeDp, int stroke) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{c1, c2});
        g.setCornerRadius(strokeDp > -1 ? 0 : 0);
        return g;
    }
    public GradientDrawable goldGrad(int radDp) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.BL_TR,
                new int[]{0xFFF6D988, 0xFFF1C24A, 0xFFC99327, 0xFF9C7214});
        g.setCornerRadius(radDp * 2.8f);
        return g;
    }
    public GradientDrawable cardBg() { return round(U.CARD, 16, U.STROKE, 1); }

    // ---------- inputs ----------
    public EditText in(String hint) { return in(hint, false); }
    public EditText in(String hint, boolean numeric) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(0xFF5B6577);
        e.setTextColor(U.TXT);
        e.setTextSize(15);
        e.setTypeface(U.FM);
        e.setBackgroundDrawable(round(0xFF0E141F, 12, U.STROKE, 1));
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        if (numeric) e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setSingleLine(true);
        return e;
    }
    public long moneyOf(EditText e) { return U.parseMoney(U.str(e)); }
    public int  mwOf(EditText e)    { return U.parseMw(U.str(e)); }
    public String txtOf(EditText e) { return U.str(e); }

    // ---------- buttons ----------
    public interface Tap { void go(); }
    public static TextView makeBtn(Context c, String s, int kind, final Tap tap) {
        TextView b = new TextView(c);
        b.setText(s);
        b.setGravity(Gravity.CENTER);
        b.setTextSize(15);
        b.setPadding(dp2(c, 14), dp2(c, 11), dp2(c, 14), dp2(c, 11));
        if (kind == 0) {
            b.setTypeface(U.FB);
            b.setTextColor(0xFF23180A);
            GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.BL_TR,
                    new int[]{0xFFF6D988, 0xFFF1C24A, 0xFFC99327, 0xFF9C7214});
            g.setCornerRadius(dp2(c, 13));
            b.setBackgroundDrawable(g);
        } else if (kind == 1) {
            b.setTypeface(U.FM);
            b.setTextColor(U.TXT);
            b.setBackgroundDrawable(round(0xFF1D2634, 13, 0xFF39455B, 1));
        } else {
            b.setTypeface(U.FM);
            b.setTextColor(0xFFFFB9C0);
            b.setBackgroundDrawable(round(0xFF3A1F24, 13, 0xFF6E2B32, 1));
        }
        b.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { tap.go(); } });
        return b;
    }
    static int dp2(Context c, float v) { return U.dp(c, v); }

    public TextView btn(String s, Tap t)  { return makeBtn(this, s, 0, t); }
    public TextView gbtn(String s, Tap t) { return makeBtn(this, s, 1, t); }
    public TextView dbtn(String s, Tap t) { return makeBtn(this, s, 2, t); }

    public void addBtn(LinearLayout parent, TextView b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6), 0, dp(6));
        parent.addView(b, lp);
    }

    /** چیپ انتخابی (عیار و غیره) */
    public TextView chip(String s, boolean sel) {
        TextView t = tv(sel ? s : s, sel ? 0xFF23180A : U.SUB, 13, sel);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(12), dp(7), dp(12), dp(7));
        if (sel) t.setBackgroundDrawable(goldGrad(22));
        else t.setBackgroundDrawable(round(0x00000000, 22, 0xFF3A465A, 1));
        return t;
    }

    public TextView badge(String s, boolean okGreen) {
        TextView t = tv(s, okGreen ? 0xFF7CE0A4 : 0xFFFFA9B1, 11, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(8), dp(3), dp(8), dp(3));
        t.setBackgroundDrawable(round(okGreen ? 0xFF1E3A2A : 0xFF3A1F24, 10, 0, 0));
        return t;
    }

    /** ردیف کلید/مقدار داخل کارت */
    public LinearLayout kv(String k, String v, int vc) {
        LinearLayout r = h();
        TextView tk = tv(k, U.SUB, 13, false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        r.addView(tk, lp);
        r.addView(tvM(v, vc, 14));
        r.setPadding(0, dp(3), 0, dp(3));
        return r;
    }
    public LinearLayout kv(String k, String v) { return kv(k, v, U.TXT); }

    // ---------- dialogs ----------
    public void msg(String title, String m) {
        final AlertDialog d = new AlertDialog.Builder(this).create();
        LinearLayout box = v();
        box.setPadding(dp(16), dp(14), dp(16), dp(10));
        box.addView(tv(title, U.GOLD, 16, true));
        box.addView(space(6));
        TextView mm = tv(m, U.TXT, 14, false);
        mm.setLineSpacing(4, 1.15f);
        box.addView(mm);
        box.addView(space(10));
        LinearLayout br = h();
        br.setGravity(Gravity.CENTER);
        br.addView(btn("باشه", new Tap() { public void go() { d.dismiss(); } }),
                new LinearLayout.LayoutParams(dp(140), ViewGroup.LayoutParams.WRAP_CONTENT));
        box.addView(br);
        showDlg(d, box);
    }

    public void confirm(String m, final Tap yes) {
        final AlertDialog d = new AlertDialog.Builder(this).create();
        LinearLayout box = v();
        box.setPadding(dp(16), dp(14), dp(16), dp(10));
        TextView mm = tv(m, U.TXT, 15, false);
        mm.setLineSpacing(4, 1.15f);
        box.addView(mm);
        box.addView(space(8));
        LinearLayout br = h();
        br.setGravity(Gravity.CENTER);
        TextView y = btn("تأیید", new Tap() { public void go() { d.dismiss(); yes.go(); } });
        TextView n = gbtn("انصراف", new Tap() { public void go() { d.dismiss(); } });
        br.addView(y, new LinearLayout.LayoutParams(dp(120), ViewGroup.LayoutParams.WRAP_CONTENT));
        br.addView(wspace(10));
        br.addView(n, new LinearLayout.LayoutParams(dp(120), ViewGroup.LayoutParams.WRAP_CONTENT));
        box.addView(br);
        showDlg(d, box);
    }

    public interface OnText { void ok(String s); }
    public void input(String title, String hint, boolean numeric, final OnText cb) { input(title, hint, "", numeric, cb); }
    public void input(String title, String hint, String def, boolean numeric, final OnText cb) {
        final AlertDialog d = new AlertDialog.Builder(this).create();
        LinearLayout box = v();
        box.setPadding(dp(16), dp(14), dp(16), dp(10));
        box.addView(tv(title, U.GOLD, 16, true));
        box.addView(space(6));
        final EditText e = in(hint, numeric);
        if (def != null && def.length() > 0) e.setText(def);
        box.addView(e);
        box.addView(space(8));
        LinearLayout br = h();
        br.setGravity(Gravity.CENTER);
        TextView y = btn("ثبت", new Tap() { public void go() {
            d.dismiss(); hideKb(); cb.ok(U.str(e)); } });
        TextView n = gbtn("انصراف", new Tap() { public void go() { d.dismiss(); } });
        br.addView(y, new LinearLayout.LayoutParams(dp(120), ViewGroup.LayoutParams.WRAP_CONTENT));
        br.addView(wspace(10));
        br.addView(n, new LinearLayout.LayoutParams(dp(120), ViewGroup.LayoutParams.WRAP_CONTENT));
        box.addView(br);
        showDlg(d, box);
    }

    public interface OnIdx { void ok(int i); }
    public void choose(String title, final String[] items, final OnIdx cb) {
        final AlertDialog d = new AlertDialog.Builder(this).create();
        LinearLayout box = v();
        box.setPadding(dp(16), dp(14), dp(16), dp(10));
        box.addView(tv(title, U.GOLD, 16, true));
        box.addView(space(6));
        ScrollView sv = new ScrollView(this);
        LinearLayout col = v();
        sv.addView(col);
        for (int i = 0; i < items.length; i++) {
            final int idx = i;
            TextView t = tv(items[i], U.TXT, 15, false);
            t.setPadding(dp(10), dp(10), dp(10), dp(10));
            t.setBackgroundDrawable(round(0x00000000, 10, 0, 0));
            t.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { d.dismiss(); cb.ok(idx); } });
            col.addView(t);
            if (i < items.length - 1) {
                View dv = new View(this);
                dv.setBackgroundColor(0xFF222B39);
                col.addView(dv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
            }
        }
        box.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.min(dp(300), items.length * dp(44) + dp(10))));
        showDlg(d, box);
    }

    /** دیالوگ با محتوای سفارشی */
    public AlertDialog sheet(View content) {
        final AlertDialog d = new AlertDialog.Builder(this).create();
        showDlg(d, content);
        return d;
    }

    private void showDlg(final AlertDialog d, View content) {
        d.setView(content);
        d.show();
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(round(0xFF151C28, 18, 0xFF2E3A4E, 1));
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.CENTER);
            w.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        }
    }

    public void hideKb() {
        try {
            InputMethodManager im = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            View v = getCurrentFocus();
            if (v != null) im.hideSoftInputFromWindow(v.getWindowToken(), 0);
        } catch (Throwable ignored) {}
    }

    /** انتخاب مشتری — برمی‌گرداند اندیس؛ با کال‌بک آی‌دی */
    public interface OnCustomer { void ok(int cid, String name); }
    public void pickCustomer(final OnCustomer cb) {
        android.database.Cursor c = db.r().rawQuery("SELECT id, name, phone FROM customers ORDER BY name", null);
        final java.util.ArrayList<int[]> ids = new java.util.ArrayList<int[]>();
        final java.util.ArrayList<String> names = new java.util.ArrayList<String>();
        names.add("— بدون مشتری (عابر) —");
        ids.add(new int[]{0});
        while (c.moveToNext()) {
            ids.add(new int[]{c.getInt(0)});
            String nm = c.getString(1);
            String ph = c.getString(2);
            names.add(ph == null || ph.length() == 0 ? nm : nm + " • " + U.dig(ph));
        }
        c.close();
        String[] arr = new String[names.size()];
        for (int i = 0; i < names.size(); i++) arr[i] = (String) names.get(i);
        choose("انتخاب مشتری", arr, new OnIdx() {
            public void ok(int i) {
                int id = ((int[]) ids.get(i))[0];
                if (id == 0) { cb.ok(0, ""); return; }
                android.database.Cursor cc = db.r().rawQuery("SELECT name FROM customers WHERE id=?", new String[]{"" + id});
                String nm = "";
                if (cc.moveToFirst()) nm = cc.getString(0);
                cc.close();
                cb.ok(id, nm);
            }
        });
    }

    /** چیدن چیپ‌ها در دو سطر */
    public LinearLayout chipsRow(String[] labels, int sel, final OnIdx onSel) {
        final java.util.ArrayList<TextView> views = new java.util.ArrayList<TextView>();
        final LinearLayout wrap = v();
        LinearLayout row = h();
        int i = 0;
        for (String s : labels) {
            final int idx = i;
            final TextView t = chip(s, idx == sel);
            t.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    for (int j = 0; j < views.size(); j++) {
                        TextView x = (TextView) views.get(j);
                        boolean sl = j == idx;
                        x.setTextColor(sl ? 0xFF23180A : U.SUB);
                        x.setTypeface(sl ? U.FB : U.F);
                        x.setBackgroundDrawable(sl ? goldGrad(22) : round(0x00000000, 22, 0xFF3A465A, 1));
                    }
                    onSel.ok(idx);
                }
            });
            views.add(t);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, dp(3), dp(6), dp(3));
            row.addView(t, lp);
            i++;
            if (i % 3 == 0 && i < labels.length) { wrap.addView(row); row = h(); }
        }
        wrap.addView(row);
        return wrap;
    }
}

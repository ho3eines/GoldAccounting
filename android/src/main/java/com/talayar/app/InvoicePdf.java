package com.talayar.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

/** تولید و خروجی پیش‌فاکتور و فاکتور فروش در قالب PDF استاندارد در اندروید */
public class InvoicePdf {

    public static class LineInfo {
        public String title;
        public int karat;
        public int wmw;
        public long unit;
        public long total;

        public LineInfo(String t, int k, int w, long u, long tot) {
            title = t;
            karat = k;
            wmw = w;
            unit = u;
            total = tot;
        }
    }

    public static void generateAndShare(Context context, Db db, boolean isPreInvoice, int invoiceId, String dateJ, String cname, long rate, long total, long pcash, long pgoldMw, long pgoldVal, long pgoldKarat, long debt, String note, ArrayList<LineInfo> items) {
        try {
            PdfDocument document = new PdfDocument();
            // A4 size: 595 x 842 points
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            canvas.drawColor(Color.WHITE);

            Paint paint = new Paint();
            paint.setAntiAlias(true);

            // Header Banner
            paint.setColor(Color.rgb(241, 194, 74)); // Gold accent
            canvas.drawRect(30, 30, 565, 105, paint);

            paint.setColor(Color.rgb(11, 15, 22));
            paint.setTextSize(18);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            String title = isPreInvoice ? "پیش‌فاکتور فروش طلا" : ("فاکتور فروش طلا — شماره " + U.dig(invoiceId + ""));
            canvas.drawText(title, 45, 65, paint);

            paint.setTextSize(11);
            paint.setTypeface(Typeface.DEFAULT);
            String shop = db.getS("shop", "طلایار");
            String shopTel = db.getS("shopTel", "");
            String shopInfo = shop + (shopTel.length() > 0 ? " | تلفن: " + U.dig(shopTel) : "");
            canvas.drawText(shopInfo, 45, 90, paint);

            // Metadata section
            int y = 135;
            paint.setTextSize(12);
            paint.setColor(Color.rgb(40, 50, 65));

            canvas.drawText("تاریخ: " + U.dig(dateJ), 45, y, paint);
            canvas.drawText("مشتری: " + (cname != null && cname.length() > 0 ? cname : "مشتری عابر (نقدی)"), 300, y, paint);
            y += 24;
            canvas.drawText("نرخ روز طلای ۱۸ عیار: " + U.money(rate) + " تومان", 45, y, paint);
            if (!isPreInvoice) {
                canvas.drawText("شماره فاکتور: " + U.dig(invoiceId + ""), 300, y, paint);
            }
            y += 18;

            // Divider line
            paint.setColor(Color.rgb(210, 215, 220));
            canvas.drawLine(30, y, 565, y, paint);
            y += 22;

            // Table Header background
            paint.setColor(Color.rgb(240, 243, 246));
            canvas.drawRect(30, y - 16, 565, y + 12, paint);
            paint.setColor(Color.BLACK);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextSize(10);
            canvas.drawText("ردیف", 40, y, paint);
            canvas.drawText("شرح قلم کالا / طلا", 90, y, paint);
            canvas.drawText("عیار", 280, y, paint);
            canvas.drawText("وزن (گرم)", 350, y, paint);
            canvas.drawText("مبلغ کل (تومان)", 455, y, paint);
            y += 24;

            // Table Rows
            paint.setTypeface(Typeface.DEFAULT);
            for (int i = 0; i < items.size(); i++) {
                if (y > 700) break;
                LineInfo ln = (LineInfo) items.get(i);
                paint.setColor(i % 2 == 0 ? Color.WHITE : Color.rgb(249, 250, 251));
                canvas.drawRect(30, y - 13, 565, y + 9, paint);

                paint.setColor(Color.BLACK);
                canvas.drawText(U.dig((i + 1) + ""), 40, y, paint);
                canvas.drawText(ln.title, 90, y, paint);
                canvas.drawText(U.karatName(ln.karat), 280, y, paint);
                canvas.drawText(U.mw(ln.wmw), 350, y, paint);
                canvas.drawText(U.money(ln.total), 455, y, paint);
                y += 24;
            }

            y += 12;
            paint.setColor(Color.rgb(210, 215, 220));
            canvas.drawLine(30, y, 565, y, paint);
            y += 25;

            // Payment / Summary section
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextSize(13);
            paint.setColor(Color.rgb(200, 130, 20));
            canvas.drawText("جمع کل فاکتور: " + U.money(total) + " تومان", 45, y, paint);
            y += 24;

            paint.setTypeface(Typeface.DEFAULT);
            paint.setTextSize(11);
            paint.setColor(Color.BLACK);
            canvas.drawText("پرداخت نقدی: " + U.money(pcash) + " تومان", 45, y, paint);
            y += 20;

            if (pgoldMw > 0) {
                canvas.drawText("طلا دریافتی از مشتری: " + U.mw((int) pgoldMw) + " گرم " + U.karatName((int) pgoldKarat) + " ≈ " + U.money(pgoldVal) + " تومان", 45, y, paint);
                y += 20;
            }

            if (debt > 0) {
                canvas.drawText("مانده (بدهی مشتری): " + U.money(debt) + " تومان", 45, y, paint);
                y += 20;
            } else if (debt < 0) {
                canvas.drawText("اضافه‌پرداخت (بستانکاری): " + U.money(-debt) + " تومان", 45, y, paint);
                y += 20;
            } else {
                canvas.drawText("وضعیت تسویه: تسویه کامل ✓", 45, y, paint);
                y += 20;
            }

            if (note != null && note.length() > 0) {
                y += 5;
                canvas.drawText("یادداشت: " + note, 45, y, paint);
                y += 20;
            }

            // Footer
            y = 810;
            paint.setColor(Color.rgb(150, 150, 150));
            paint.setTextSize(9);
            canvas.drawText("طلایار — نرم‌افزار تخصصی حسابداری و مدیریت طلافروشی", 45, y, paint);

            document.finishPage(page);

            // Save file
            File dir = new File(context.getFilesDir(), "invoices");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, (isPreInvoice ? "preinvoice.pdf" : ("invoice_" + invoiceId + ".pdf")));
            FileOutputStream fos = new FileOutputStream(file);
            document.writeTo(fos);
            document.close();
            fos.close();

            // Share / View intent via ShareProvider
            Uri uri = ShareProvider.uriFor(context, "invoices/" + file.getName());
            Intent it = new Intent(Intent.ACTION_VIEW);
            it.setDataAndType(uri, "application/pdf");
            it.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent chooser = Intent.createChooser(it, isPreInvoice ? "مشاهده پیش‌فاکتور PDF" : "مشاهده فاکتور PDF");
            context.startActivity(chooser);

        } catch (Exception e) {
            U.toast(context, "خطا در تولید PDF: " + (e.getMessage() != null ? e.getMessage() : "ناشناخته"));
        }
    }
}

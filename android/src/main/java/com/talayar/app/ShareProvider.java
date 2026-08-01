package com.talayar.app;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/** پرووایدر سبک برای اشتراک‌گذاری امن فایل‌های داخلی برنامه (عکس اتیکت، کارت بارکد، فاکتور PDF) */
public class ShareProvider extends ContentProvider {

    @Override public boolean onCreate() { return true; }

    private File fileFor(Uri uri) throws FileNotFoundException {
        File base = getContext().getFilesDir();
        String rel = uri.getPath();
        if (rel == null) throw new FileNotFoundException("bad uri");
        if (rel.startsWith("/")) rel = rel.substring(1);
        File f = new File(base, rel);
        try {
            String canon = f.getCanonicalPath();
            if (!canon.startsWith(base.getCanonicalPath() + File.separator)
                    && !canon.equals(base.getCanonicalPath()))
                throw new FileNotFoundException("denied");
            if (!f.exists()) throw new FileNotFoundException("missing");
            return f;
        } catch (FileNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new FileNotFoundException("io");
        }
    }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return ParcelFileDescriptor.open(fileFor(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override public Cursor query(Uri uri, String[] proj, String sel, String[] args, String ord) {
        String[] cols = {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        MatrixCursor mc = new MatrixCursor(cols, 1);
        try {
            File f = fileFor(uri);
            mc.addRow(new Object[]{f.getName(), Long.valueOf(f.length())});
        } catch (Exception e) {
            mc.addRow(new Object[]{"file", Long.valueOf(0)});
        }
        return mc;
    }

    @Override public String getType(Uri uri) {
        String p = uri.getPath();
        if (p != null) {
            if (p.endsWith(".png")) return "image/png";
            if (p.endsWith(".jpg") || p.endsWith(".jpeg")) return "image/jpeg";
            if (p.endsWith(".pdf")) return "application/pdf";
        }
        return "application/octet-stream";
    }

    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String s, String[] a) { return 0; }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { return 0; }

    /** ساخت content Uri برای مسیر نسبیِ داخل filesDir (مثل "etiket/x.jpg" یا "invoices/preinvoice.pdf") */
    public static Uri uriFor(Context c, String relPath) {
        return Uri.parse("content://com.talayar.app.share/" + Uri.encode(relPath));
    }
}

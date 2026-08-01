package com.talayar.app;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;

/** راهنمای فارسی استفاده از برنامه */
public class GuideActivity extends A {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        scaffold("راهنمای استفاده", true);
        WebView guide = new WebView(this);
        WebSettings settings = guide.getSettings();
        settings.setDefaultTextEncodingName("UTF-8");
        // جاوااسکریپت فقط برای جستجوی داخل فایل محلی راهنما فعال است (بدون دسترسی به اینترنت)
        settings.setJavaScriptEnabled(true);
        guide.loadUrl("file:///android_asset/usage_manual.html");
        body.addView(guide, new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT));
    }
}

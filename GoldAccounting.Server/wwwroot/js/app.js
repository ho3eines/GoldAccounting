/* طلایار — اسکریپت‌های PWA و رابط کاربری */
(function () {
  'use strict';

  // ── ثبت Service Worker ──
  if ('serviceWorker' in navigator) {
    window.addEventListener('load', function () {
      navigator.serviceWorker.register('/sw.js').catch(function (err) {
        console.warn('SW registration failed:', err);
      });
    });
  }

  // ── دکمهٔ نصب PWA (A2HS) ──
  var deferredPrompt = null;
  var installBtn = document.getElementById('pwa-install-btn');
  var installBar = document.getElementById('pwa-install-bar');

  window.addEventListener('beforeinstallprompt', function (e) {
    e.preventDefault();
    deferredPrompt = e;
    if (installBar) installBar.classList.remove('hidden');
  });

  if (installBtn) {
    installBtn.addEventListener('click', function () {
      if (!deferredPrompt) return;
      deferredPrompt.prompt();
      deferredPrompt.userChoice.then(function () {
        deferredPrompt = null;
        if (installBar) installBar.classList.add('hidden');
      });
    });
  }

  // نمایش حالت نصب‌شده (standalone) — مخفی کردن نوار نصب
  if (window.matchMedia('(display-mode: standalone)').matches) {
    if (installBar) installBar.classList.add('hidden');
  }

  // ── بستن سایدبار موبایل بعد از کلیک روی لینک ──
  document.addEventListener('click', function (e) {
    var link = e.target.closest ? e.target.closest('nav a') : null;
    if (link && window.innerWidth < 768) {
      var toggle = document.querySelector('[data-sidebar-toggle]');
      // سایدبار با کلاس hidden مدیریت می‌شود؛ لینک‌ها خودشان صفحه را عوض می‌کنند
      if (toggle) toggle.click();
    }
  });

  // ── نمایش سال شمسی در فوتر هدر (اختیاری) ──
  var jalaliEl = document.getElementById('jalali-date');
  if (jalaliEl) {
    try {
      var g = new Date();
      // تبدیل ساده میلادی به شمسی (الگوریتم jalaali ساده)
      var gy = g.getFullYear(), gm = g.getMonth() + 1, gd = g.getDate();
      var g_d_m = [0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334];
      var gy2 = (gm > 2) ? (gy + 1) : gy;
      var days = 355666 + (365 * gy) + ~~((gy2 + 3) / 4) - ~~((gy2 + 99) / 100) + ~~((gy2 + 399) / 400) + gd + g_d_m[gm - 1];
      var jy = 0, jm = 0, jd = 0;
      var jy_ = 979, days_ = days - 79;
      jy = jy_ + 33 * ~~(days_ / 12053); days_ %= 12053;
      jy += 4 * ~~(days_ / 1461); days_ %= 1461;
      if (days_ > 365) { jy += ~~((days_ - 1) / 365); days_ = (days_ - 1) % 365; }
      jm = days_ < 186 ? 1 + ~~(days_ / 31) : 7 + ~~((days_ - 186) / 30);
      jd = 1 + (days_ < 186 ? days_ % 31 : (days_ - 186) % 30);
      var months = ['فروردین','اردیبهشت','خرداد','تیر','مرداد','شهریور','مهر','آبان','آذر','دی','بهمن','اسفند'];
      jalaliEl.textContent = jd + ' ' + months[jm - 1] + ' ' + jy;
    } catch (err) { /* بی‌خیال */ }
  }
})();

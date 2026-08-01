/* طلایار — اسکریپت‌های PWA، بارکدخوان دوربین، دانلود/اشتراک */
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

  // ── نصب PWA (A2HS) ──
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
  if (window.matchMedia('(display-mode: standalone)').matches && installBar) {
    installBar.classList.add('hidden');
  }

  // ── دانلود فایل متنی (Blob) ──
  window.downloadText = function (filename, content, mime) {
    var blob = new Blob([content], { type: mime || 'text/plain;charset=utf-8' });
    var url = URL.createObjectURL(blob);
    var a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    setTimeout(function () { URL.revokeObjectURL(url); a.remove(); }, 500);
  };

  // ── اشتراک‌گذاری (Web Share API) با fallback کپی/دانلود ──
  window.shareText = function (title, text) {
    if (navigator.share) {
      navigator.share({ title: title, text: text }).catch(function () {});
    } else if (navigator.clipboard) {
      navigator.clipboard.writeText(text).then(function () {
        alert('متن کپی شد (مرورگر شما اشتراک‌گذاری را پشتیبانی نمی‌کند)');
      });
    }
  };
  window.shareFile = function (filename, content, mime) {
    var blob = new Blob([content], { type: mime || 'text/plain;charset=utf-8' });
    var file = new File([blob], filename, { type: mime || 'text/plain;charset=utf-8' });
    if (navigator.canShare && navigator.canShare({ files: [file] })) {
      navigator.share({ files: [file], title: filename }).catch(function () {});
    } else {
      window.downloadText(filename, content, mime);
    }
  };
  window.shareUrl = function (title, url) {
    if (navigator.share) {
      navigator.share({ title: title, url: url }).catch(function () {});
    } else if (navigator.clipboard) {
      navigator.clipboard.writeText(url).then(function () {
        alert('لینک کپی شد: ' + url);
      });
    }
  };

  // ═══════════════════════════════════════════════
  //  بارکدخوان دوربین (BarcodeDetector API)
  //  - در Chrome اندروید/دسکتاپ پشتیبانی می‌شود
  //  - fallback: ورود دستی
  // ═══════════════════════════════════════════════
  var scanState = {
    stream: null,
    raf: null,
    detector: null,
    running: false,
    lastCodes: {},
    onCode: null
  };

  function supportsBarcodeDetector() {
    return !!(window.BarcodeDetector && window.BarcodeDetector.getSupportedFormats);
  }

  window.scanInit = function (objRef) {
    scanState.onCode = function (val) {
      if (objRef && objRef.invokeMethodAsync) {
        objRef.invokeMethodAsync('OnScanned', val).catch(function () {});
      }
    };
    if (supportsBarcodeDetector()) {
      window.BarcodeDetector.getSupportedFormats().then(function (formats) {
        try {
          scanState.detector = new BarcodeDetector({
            formats: formats.filter(function (f) {
              return ['code_128', 'code_39', 'ean_13', 'ean_8', 'qr_code', 'upc_a', 'upc_e', 'codabar'].indexOf(f) >= 0;
            })
          });
        } catch (e) {
          scanState.detector = new BarcodeDetector();
        }
      });
    }
    return supportsBarcodeDetector();
  };

  window.scanStart = function (videoElId) {
    var video = document.getElementById(videoElId);
    if (!video || scanState.running) return Promise.resolve(false);
    return navigator.mediaDevices.getUserMedia({
      video: { facingMode: 'environment', width: { ideal: 1280 }, height: { ideal: 720 } }
    }).then(function (stream) {
      scanState.stream = stream;
      video.srcObject = stream;
      video.setAttribute('playsinline', '');
      video.play();
      scanState.running = true;
      scanState.lastCodes = {};
      if (scanState.detector) {
        scanState.raf = requestAnimationFrame(scanLoop);
      } else {
        // بدون BarcodeDetector فقط تصویر دوربین نمایش داده می‌شود (ورود دستی)
        return false;
      }
      return true;
    }).catch(function (err) {
      console.warn('camera error:', err);
      return false;
    });
  };

  function scanLoop() {
    if (!scanState.running || !scanState.detector) return;
    var video = document.querySelector('video[data-scan]');
    if (!video || video.readyState < 2) {
      scanState.raf = requestAnimationFrame(scanLoop);
      return;
    }
    scanState.detector.detect(video).then(function (codes) {
      codes.forEach(function (c) {
        var val = (c.rawValue || '').trim();
        if (!val) return;
        var now = Date.now();
        if (scanState.lastCodes[val] && now - scanState.lastCodes[val] < 2000) return;
        scanState.lastCodes[val] = now;
        if (scanState.onCode) scanState.onCode(val);
      });
    }).catch(function () {}).then(function () {
      if (scanState.running) scanState.raf = requestAnimationFrame(scanLoop);
    });
  }

  window.scanStop = function () {
    scanState.running = false;
    if (scanState.raf) cancelAnimationFrame(scanState.raf);
    if (scanState.stream) {
      scanState.stream.getTracks().forEach(function (t) { t.stop(); });
      scanState.stream = null;
    }
  };

  // ── بستن سایدبار موبایل بعد از کلیک روی لینک ──
  document.addEventListener('click', function (e) {
    var link = e.target.closest ? e.target.closest('nav a') : null;
    if (link && window.innerWidth < 768) {
      var toggle = document.querySelector('[data-sidebar-toggle]');
      if (toggle) toggle.click();
    }
  });
})();

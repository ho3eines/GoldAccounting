# 🔧 زنجیرهٔ ابزار ساخت (بدون اندروید استودیو / بدون Gradle)

این پروژه عمداً به‌گونه‌ای ساخته شده که هیچ وابستگی به Maven/Gradle نداشته باشد و با ابزارهای مستقل، بیلد شود. اسکریپت: `build.sh`

## اجزای موردنیاز (در `/home/user/toolchain`)

| ابزار | نقش | منبع |
|---|---|---|
| `jre/` | اجرای ابزارها (Temurin JRE 25، استخراج از wheel پکیج `jdk4py`) | PyPI |
| `aapt2` | کامپایل + لینک منابع اندروید | باینری گنجانده‌شده در پکیج npm `aaptjs3` |
| `jlaunch.jar` | لانچر کوتلینِ درایور janino (در این مخزن ساخته شده: `Launcher.kt`) | بیلد محلی با `kotlinc` (npm `kotlin-compiler`) |
| `janino-3.1.9.jar` + `commons-compiler-3.1.9.jar` | کامپایلر جاوا (به‌جای javac) | از داخل tarball `pyspark` روی PyPI |
| `r8.jar` | D8/R8 نسخهٔ AOSP (تولید dex + پاک‌سازی android.jar) | ریپوی `LineageOS/android_prebuilts_r8` |
| `platforms/android-21/android.jar` | پلتفرم کامپایل (مرجع برای aapt2/D8) | ریپوی `Sable/android-platforms` |
| `android-lite.jar` | نسخهٔ پردازش‌شدهٔ android.jar بدون انوتیشن (سازگار با parser جانینو، حاوی InnerClasses) | با `strip_annotations.py` (حذف attributeهای انوتیشن) یا R8 ساخته می‌شود |
| `apksigner.jar` | امضای APK (v2/v3) | پکیج npm `@postar/apktool-node` |
| `gold.keystore` | کلید امضا (با keytool همین JRE ساخته شده؛ پسورد: goldpass) | محلی در `toolchain/` — خارج از گیت نگه‌داری می‌شود تا نسخه‌های بعدی با همین کلید امضا شوند |

## مراحل build.sh

1. `aapt2 compile --dir res -o res.zip`
2. `aapt2 link -o talayar-raw.apk -I platforms/android-21/android.jar --manifest AndroidManifest.xml --java gen -A assets res.zip --min-sdk-version 24 --target-sdk-version 27`
3. `java -cp jlaunch.jar:janino.jar:commons.jar LauncherKt android-lite classes <src> <gen>` (کامپایل جاوا با janino)
4. `java -cp r8.jar com.android.tools.r8.D8 --output dexout --lib platforms/android-21/android.jar <classes>`
5. افزودن `classes.dex` به APK و امضا با `apksigner.jar`

## نکته‌ها

- چرا android-lite؟ janino 3.1.9 هنگام خواندن انوتیشن‌های کلاس‌های android.jar باگ NYI می‌خورد؛ انوتیشن‌ها حذف و `InnerClasses` حفظ می‌شود که هر دو پازل حل می‌شود (`strip_annotations.py` یا R8).
- نسخهٔ خروجی: `VERSION=4.0 bash build.sh` (پیش‌فرض 4.0) — نام فایل در `publish/` و `versionCode/versionName` در `android/AndroidManifest.xml` متناظرند.
- janino داری محدودیت‌های ژنریک است؛ به همین دلیل کد به سبک Java 7 (بدون لامبدا، با castهای صریح) نوشته شده است.

#!/usr/bin/env bash
# ساخت کامل APK طلایار — بدون اندروید استودیو و گریدل
set -e
T=${TOOLCHAIN:-/home/user/toolchain}
export PATH="$T/jre/bin:$PATH"
ROOT=$(cd "$(dirname "$0")" && pwd)
ANDROID_DIR=$ROOT/android
SRC=$ANDROID_DIR/src/main/java
GEN=$ROOT/gen
CLASSES=$ROOT/classes
DEX=$ROOT/dexout
RES=$ANDROID_DIR/res
ASSETS=$ANDROID_DIR/assets
MANIFEST=$ANDROID_DIR/AndroidManifest.xml
ANDROID_JAR_LITE=$T/android-lite.jar
ANDROID_JAR=$T/platforms/android-21/android.jar
APK_RAW=$ROOT/talayar-raw.apk
APK_FULL=$ROOT/talayar-full.apk
VERSION=${VERSION:-4.0}
APK_FINAL=$ROOT/publish/Talayar-GoldAccounting-v$VERSION.apk

echo "== 1/5 aapt2: کامپایل و لینک منابع"
cd "$ROOT"
rm -rf res.zip "$GEN" "$CLASSES" "$DEX" "$APK_RAW" "$APK_FULL"
$T/aapt2 compile --dir "$RES" -o res.zip
$T/aapt2 link -o "$APK_RAW" -I "$ANDROID_JAR" --manifest "$MANIFEST" --java "$GEN" -A "$ASSETS" res.zip \
    --min-sdk-version 24 --target-sdk-version 27

echo "== 2/5 janino: کامپایل جاوا"
mkdir -p "$CLASSES"
java -cp "$T/jlaunch.jar:$T/janino-3.1.9.jar:$T/commons-compiler-3.1.9.jar" LauncherKt \
    "$ANDROID_JAR_LITE" "$CLASSES" "$SRC" "$GEN"

echo "== 3/5 d8: تبدیل به dex"
mkdir -p "$DEX"
java -cp "$T/r8.jar" com.android.tools.r8.D8 --output "$DEX" --lib "$ANDROID_JAR" \
    $(find "$CLASSES" -name "*.class")

echo "== 4/5 افزودن dex به APK"
cp "$APK_RAW" "$APK_FULL"
zip -j "$APK_FULL" "$DEX/classes.dex" > /dev/null

echo "== 5/5 امضا (v2/v3)"
mkdir -p "$ROOT/publish"
java -jar "$T/apksigner.jar" sign \
    --ks "$T/gold.keystore" --ks-pass pass:goldpass --key-pass pass:goldpass \
    --out "$APK_FINAL" "$APK_FULL"
java -jar "$T/apksigner.jar" verify --verbose "$APK_FINAL"

echo "== تمام شد: $APK_FINAL"
ls -la "$APK_FINAL"

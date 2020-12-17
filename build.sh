#!/bin/bash
set -e

# TODO: get absolute path
MOTATOR=.
# TODO: auto-detect android.jar location
ANDROID_JAR=/usr/lib/android-sdk/platforms/android-23/android.jar
# TODO: auto-detect SDK build-tools location
BUILD_TOOLS=/usr/lib/android-sdk/build-tools/27.0.1
# TODO: stablize keystore w/o checking into GIT
# Create keystore: keytool -genkeypair -validity 365 -keystore mykey.keystore -keyalg RSA -keysize 2048
KEYSTORE=mykey.keystore

# Make sure important directories exist
mkdir -p $MOTATOR/res/drawable
mkdir -p $MOTATOR/bin
mkdir -p $MOTATOR/obj

# TODO: switch to aapt2
# TODO: res/drawable/ 
echo "Building R.java (resources file)"
$BUILD_TOOLS/aapt package -f -m -J src -M $MOTATOR/AndroidManifest.xml -S $MOTATOR/res -I $ANDROID_JAR

echo "Compiling Java Code"
javac -source 1.7 -target 1.7 -d $MOTATOR/obj -classpath $MOTATOR/src -bootclasspath $ANDROID_JAR $MOTATOR/src/life/nosk/motator/*.java

echo "Compiling Kotlin Code"
kotlinc -d $MOTATOR/obj -classpath $MOTATOR/obj:$ANDROID_JAR -include-runtime $MOTATOR/src/life/nosk/motator/*.kt

echo "Converting to Dalvik"
$BUILD_TOOLS/dx --dex --output=$MOTATOR/bin/classes.dex $MOTATOR/obj

echo "Creating APK (unaligned)"
aapt package -f -m -F $MOTATOR/bin/motator.unaligned.apk -M $MOTATOR/AndroidManifest.xml -S $MOTATOR/res -I $ANDROID_JAR

echo "Adding Code to APK"
pushd $MOTATOR/bin
$BUILD_TOOLS/aapt add motator.unaligned.apk classes.dex
popd

echo "Aligning APK"
$BUILD_TOOLS/zipalign -f 4 $MOTATOR/bin/motator.unaligned.apk $MOTATOR/bin/motator.apk

echo "Signing APK"
java -jar $BUILD_TOOLS/apksigner.jar sign --ks $KEYSTORE $MOTATOR/bin/motator.apk

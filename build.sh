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
KEYSTORE_PASSWORD=password

# Make sure important directories exist
mkdir -p $MOTATOR/res/drawable
rm -rf $MOTATOR/bin
mkdir -p $MOTATOR/bin
rm -rf $MOTATOR/obj
mkdir -p $MOTATOR/obj
rm -rf $MOTATOR/build_files
mkdir -p $MOTATOR/build_files

echo "Extracting AAR's"
unzip aar/osmdroid-android-6.1.8-release.aar -d build_files/

# TODO: switch to aapt2
# TODO: res/drawable/ 
echo "Building R.java (resources file)"
$BUILD_TOOLS/aapt package --auto-add-overlay -f -m -J src -M $MOTATOR/build_files/AndroidManifest.xml -S $MOTATOR/res -S $MOTATOR/build_files/res -I $ANDROID_JAR
$BUILD_TOOLS/aapt package --auto-add-overlay -f -m -J src -M $MOTATOR/AndroidManifest.xml -S $MOTATOR/res -S $MOTATOR/build_files/res -I $ANDROID_JAR

echo "Compiling Java Code"
javac -source 1.7 -target 1.8 -d $MOTATOR/obj -classpath $MOTATOR/src:$MOTATOR/build_files/classes.jar -bootclasspath $ANDROID_JAR $MOTATOR/src/life/nosk/motator/*.java
javac -source 1.7 -target 1.8 -d $MOTATOR/obj -classpath $MOTATOR/src:$MOTATOR/build_files/classes.jar -bootclasspath $ANDROID_JAR $MOTATOR/src/org/osmdroid/library/*.java

echo "Compiling Kotlin Code"
kotlinc -jvm-target 1.8 -include-runtime -d $MOTATOR/obj/k.jar -classpath $MOTATOR/obj:$ANDROID_JAR::$MOTATOR/build_files/classes.jar $MOTATOR/src/life/nosk/motator/*.kt

echo "Extracting Compiled Classes"
cp $MOTATOR/build_files/classes.jar $MOTATOR/obj
pushd $MOTATOR/obj
jar xf k.jar
jar xf classes.jar
popd
rm $MOTATOR/obj/k.jar
rm $MOTATOR/obj/classes.jar
# fixes the following:
# PARSE ERROR:
# unsupported class file version 53.0
# ...while parsing META-INF/versions/9/module-info.class
rm -rf $MOTATOR/obj/META-INF

# TODO: switch to d8 (requires build tools v 28+)
echo "Converting to Dalvik"
$BUILD_TOOLS/dx --dex --output=$MOTATOR/bin/classes.dex $MOTATOR/obj

echo "Creating APK (unaligned)"
aapt package --auto-add-overlay -f -m -F $MOTATOR/bin/motator.unaligned.apk -M $MOTATOR/AndroidManifest.xml -S $MOTATOR/res -S $MOTATOR/build_files/res -I $ANDROID_JAR

echo "Adding Code to APK"
pushd $MOTATOR/bin
$BUILD_TOOLS/aapt add motator.unaligned.apk classes.dex
popd

echo "Aligning APK"
$BUILD_TOOLS/zipalign -f 4 $MOTATOR/bin/motator.unaligned.apk $MOTATOR/bin/motator.apk

echo "Signing APK"
java -jar $BUILD_TOOLS/apksigner.jar sign --ks $KEYSTORE --ks-pass "pass:$KEYSTORE_PASSWORD" $MOTATOR/bin/motator.apk

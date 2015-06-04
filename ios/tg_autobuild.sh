#!/bin/bash

############################
# v0.1
# Automatically runs the scons scripts necessary to build the iotivity libraries
#   and combines their respective builds into universal libraries for iOS
# Target: iOS 8.3
# 
############################
cd ..

echo "Building Arm64"
scons TARGET_OS=ios TARGET_ARCH=arm64 SYS_VERSION=8.3

echo "Building Armv7"
scons TARGET_OS=ios TARGET_ARCH=armv7 SYS_VERSION=8.3

echo "Building Armv7s"
scons TARGET_OS=ios TARGET_ARCH=armv7s SYS_VERSION=8.3

echo "Building i386"
scons TARGET_OS=ios TARGET_ARCH=i386 SYS_VERSION=8.3

mkdir "out/ios/universal"

echo "Building Universal Libraries"
lipo -create "out/ios/arm64/release/libcoap.a" "out/ios/armv7/release/libcoap.a" "out/ios/armv7s/release/libcoap.a" "out/ios/i386/release/libcoap.a" -output "out/ios/universal/libcoap.a"
lipo -create "out/ios/arm64/release/liboctbstack.a" "out/ios/armv7/release/liboctbstack.a" "out/ios/armv7s/release/liboctbstack.a" "out/ios/i386/release/liboctbstack.a" -output "out/ios/universal/liboctbstack.a"
lipo -create "out/ios/arm64/release/libconnectivity_abstraction.a" "out/ios/armv7/release/libconnectivity_abstraction.a" "out/ios/armv7s/release/libconnectivity_abstraction.a" "out/ios/i386/release/libconnectivity_abstraction.a" -output "out/ios/universal/libconnectivity_abstraction.a"

cd -
echo "Finished"
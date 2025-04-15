#!/bin/bash
set -e

LIB_NAME=mylib
SRC_FILE=src/mylib.c
OUT_DIR=libs/ios

# Create output directories
mkdir -p ${OUT_DIR}/device
mkdir -p ${OUT_DIR}/simulator

echo "📱 Building for iOS Device (arm64)..."
xcrun --sdk iphoneos clang -arch arm64 \
    -isysroot $(xcrun --sdk iphoneos --show-sdk-path) \
    -c ${SRC_FILE} -o ${OUT_DIR}/device/${LIB_NAME}_arm64.o

ar rcs ${OUT_DIR}/device/lib${LIB_NAME}_ios_arm64.a ${OUT_DIR}/device/${LIB_NAME}_arm64.o

echo "🧪 Building for iOS Simulator (x86_64)..."
xcrun --sdk iphonesimulator clang -arch x86_64 \
    -isysroot $(xcrun --sdk iphonesimulator --show-sdk-path) \
    -c ${SRC_FILE} -o ${OUT_DIR}/simulator/${LIB_NAME}_x86_64.o

ar rcs ${OUT_DIR}/simulator/lib${LIB_NAME}_ios_x86_64.a ${OUT_DIR}/simulator/${LIB_NAME}_x86_64.o

echo "🧪 Building for iOS Simulator (arm64)..."
xcrun --sdk iphonesimulator clang -arch arm64 \
    -isysroot $(xcrun --sdk iphonesimulator --show-sdk-path) \
    -c ${SRC_FILE} -o ${OUT_DIR}/simulator/${LIB_NAME}_sim_arm64.o

ar rcs ${OUT_DIR}/simulator/lib${LIB_NAME}_ios_sim_arm64.a ${OUT_DIR}/simulator/${LIB_NAME}_sim_arm64.o

echo "🤝 Creating fat library for Simulator (x86_64 + arm64)..."
lipo -create -output ${OUT_DIR}/simulator/lib${LIB_NAME}_sim_universal.a \
    ${OUT_DIR}/simulator/lib${LIB_NAME}_ios_x86_64.a \
    ${OUT_DIR}/simulator/lib${LIB_NAME}_ios_sim_arm64.a

echo "✅ Done!"

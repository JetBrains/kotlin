/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use
 * of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// MODULE: cinterop
// FILE: signext_zeroext_interop_input.def
---
char char_id(char c) {
    return c;
}

unsigned char unsigned_char_id(unsigned char c) {
    return c;
}

short short_id(short s) {
    return s;
}

unsigned short unsigned_short_id(unsigned short s) {
    return s;
}

int callbackUser(int (*fn)(int, short)) {
    return fn(5,5);
}

// MODULE: main(cinterop)
// FILE: main.kt

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import signext_zeroext_interop_input.*
import kotlinx.cinterop.*

// CHECK-DEFAULTABI-CACHE_NO: declare zeroext i1 @Kotlin_Char_isHighSurrogate(i16 zeroext){{.*}}
// CHECK-AAPCS-CACHE_NO: declare i1 @Kotlin_Char_isHighSurrogate(i16)
// CHECK-WINDOWSX64-CACHE_NO: declare zeroext i1 @Kotlin_Char_isHighSurrogate(i16)

// Check that we pass attributes to functions imported from runtime.
// CHECK-LABEL: void @"kfun:#checkRuntimeFunctionImport(){}"()
fun checkRuntimeFunctionImport() {
    // CHECK-DEFAULTABI: call zeroext i1 @Kotlin_Char_isHighSurrogate(i16 zeroext {{.*}})
    // CHECK-AAPCS: call i1 @Kotlin_Char_isHighSurrogate(i16 {{.*}})
    // CHECK-WINDOWSX64: call zeroext i1 @Kotlin_Char_isHighSurrogate(i16 {{.*}})
    'c'.isHighSurrogate()
    // CHECK-DEFAULTABI: call zeroext i1 @Kotlin_Float_isNaN(float {{.*}}
    // CHECK-AAPCS: call i1 @Kotlin_Float_isNaN(float {{.*}}
    // CHECK-WINDOWSX64: call zeroext i1 @Kotlin_Float_isNaN(float {{.*}}
    0.0f.isNaN()
}

// CHECK-LABEL: void @"kfun:#checkDirectInterop(){}"()
// CHECK-LABEL-AAPCS: void @"kfun:#checkDirectInterop(){}"()
// CHECK-LABEL-WINDOWSX64: void @"kfun:#checkDirectInterop(){}"()
fun checkDirectInterop() {
    // compiler generates quite lovely names for bridges
    // (e.g. `_66696c65636865636b5f7369676e6578745f7a65726f6578745f696e7465726f70_knbridge0`),
    // so we don't check exact function names here.
    // CHECK-DEFAULTABI: invoke signext i8 [[CHAR_ID_BRIDGE:@_.*_knbridge[0-9]+]](i8 signext {{.*}})
    // CHECK-AAPCS: invoke i8 [[CHAR_ID_BRIDGE:@_.*_knbridge[0-9]+]](i8 {{.*}})
    // CHECK-WINDOWSX64: invoke i8 [[CHAR_ID_BRIDGE:@_.*_knbridge[0-9]+]](i8 {{.*}})
    char_id(0.toByte())
    // CHECK-DEFAULTABI: invoke zeroext i8 [[UNSIGNED_CHAR_ID_BRIDGE:@_.*_knbridge[0-9]+]](i8 zeroext {{.*}})
    // CHECK-AAPCS: invoke i8 [[UNSIGNED_CHAR_ID_BRIDGE:@_.*_knbridge[0-9]+]](i8 {{.*}})
    // CHECK-WINDOWSX64: invoke i8 [[UNSIGNED_CHAR_ID_BRIDGE:@_.*_knbridge[0-9]+]](i8 {{.*}})
    unsigned_char_id(0.toUByte())
    // CHECK-DEFAULTABI: invoke signext i16 [[SHORT_ID_BRIDGE:@_.*_knbridge[0-9]+]](i16 signext {{.*}})
    // CHECK-AAPCS: invoke i16 [[SHORT_ID_BRIDGE:@_.*_knbridge[0-9]+]](i16 {{.*}})
    // CHECK-WINDOWSX64: invoke i16 [[SHORT_ID_BRIDGE:@_.*_knbridge[0-9]+]](i16 {{.*}})
    short_id(0.toShort())
    // CHECK-DEFAULTABI: invoke zeroext i16 [[UNSIGNED_SHORT_ID_BRIDGE:@_.*_knbridge[0-9]+]](i16 zeroext {{.*}})
    // CHECK-AAPCS: invoke i16 [[UNSIGNED_SHORT_ID_BRIDGE:@_.*_knbridge[0-9]+]](i16 {{.*}})
    // CHECK-WINDOWSX64: invoke i16 [[UNSIGNED_SHORT_ID_BRIDGE:@_.*_knbridge[0-9]+]](i16 {{.*}})
    unsigned_short_id(0.toUShort())
    // CHECK-DEFAULTABI: invoke i32 [[CALLBACK_USER_BRIDGE:@_.*_knbridge[0-9]+]](i8* bitcast (i32 (i32, i16)* [[STATIC_C_FUNCTION_BRIDGE:@_.*_kncfun[0-9]+]] to i8*))
    // CHECK-AAPCS: invoke i32 [[CALLBACK_USER_BRIDGE:@_.*_knbridge[0-9]+]](i8* bitcast (i32 (i32, i16)* [[STATIC_C_FUNCTION_BRIDGE:@_.*_kncfun[0-9]+]] to i8*))
    // CHECK-WINDOWSX64: invoke i32 [[CALLBACK_USER_BRIDGE:@_.*_knbridge[0-9]+]](i8* bitcast (i32 (i32, i16)* [[STATIC_C_FUNCTION_BRIDGE:@_.*_kncfun[0-9]+]] to i8*))
    callbackUser(staticCFunction { int: Int, short: Short -> int + short })
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
fun box(): String {
    checkRuntimeFunctionImport()
    checkDirectInterop()
    return "OK"
}

// CHECK-DEFAULTABI-CACHE_STATIC_ONLY_DIST: declare zeroext i1 @Kotlin_Char_isHighSurrogate(i16 zeroext){{.*}}
// CHECK-AAPCS-CACHE_STATIC_ONLY_DIST: declare i1 @Kotlin_Char_isHighSurrogate(i16)
// CHECK-WINDOWSX64-CACHE_STATIC_ONLY_DIST: declare zeroext i1 @Kotlin_Char_isHighSurrogate(i16)

// CHECK-DEFAULTABI: signext i8 [[CHAR_ID_BRIDGE]](i8 signext %0)
// CHECK-DEFAULTABI: [[CHAR_ID_PTR:%[0-9]+]] = load i8 (i8)*, i8 (i8)** @{{.*char_id}}
// CHECK-DEFAULTABI: call signext i8 [[CHAR_ID_PTR]](i8 signext %0)

// CHECK-AAPCS: i8 [[CHAR_ID_BRIDGE]](i8 %0)
// CHECK-AAPCS: [[CHAR_ID_PTR:%[0-9]+]] = load i8 (i8)*, i8 (i8)** @{{.*char_id}}
// CHECK-AAPCS: call i8 [[CHAR_ID_PTR]](i8 %0)

// CHECK-WINDOWSX64: i8 [[CHAR_ID_BRIDGE]](i8 %0)
// CHECK-WINDOWSX64: [[CHAR_ID_PTR:%[0-9]+]] = load i8 (i8)*, i8 (i8)** @{{.*char_id}}
// CHECK-WINDOWSX64: call i8 [[CHAR_ID_PTR]](i8 %0)


// CHECK-DEFAULTABI: zeroext i8 [[UNSIGNED_CHAR_ID_BRIDGE]](i8 zeroext %0)
// CHECK-DEFAULTABI: [[UNSIGNED_CHAR_ID_PTR:%[0-9]+]] = load i8 (i8)*, i8 (i8)** @{{.*unsigned_char_id}}
// CHECK-DEFAULTABI: call zeroext i8 [[UNSIGNED_CHAR_ID_PTR]](i8 zeroext %0)

// CHECK-AAPCS: i8 [[UNSIGNED_CHAR_ID_BRIDGE]](i8 %0)
// CHECK-AAPCS: [[UNSIGNED_CHAR_ID_PTR:%[0-9]+]] = load i8 (i8)*, i8 (i8)** @{{.*unsigned_char_id}}
// CHECK-AAPCS: call i8 [[UNSIGNED_CHAR_ID_PTR]](i8 %0)

// CHECK-WINDOWSX64: i8 [[UNSIGNED_CHAR_ID_BRIDGE]](i8 %0)
// CHECK-WINDOWSX64: [[UNSIGNED_CHAR_ID_PTR:%[0-9]+]] = load i8 (i8)*, i8 (i8)** @{{.*unsigned_char_id}}
// CHECK-WINDOWSX64: call i8 [[UNSIGNED_CHAR_ID_PTR]](i8 %0)


// CHECK-DEFAULTABI: signext i16 [[SHORT_ID_BRIDGE]](i16 signext %0)
// CHECK-DEFAULTABI: [[SHORT_ID_PTR:%[0-9]+]] = load i16 (i16)*, i16 (i16)** @{{.*short_id}}
// CHECK-DEFAULTABI: call signext i16 [[SHORT_ID_PTR]](i16 signext %0)

// CHECK-AAPCS: i16 [[SHORT_ID_BRIDGE]](i16 %0)
// CHECK-AAPCS: [[SHORT_ID_PTR:%[0-9]+]] = load i16 (i16)*, i16 (i16)** @{{.*short_id}}
// CHECK-AAPCS: call i16 [[SHORT_ID_PTR]](i16 %0)

// CHECK-WINDOWSX64: i16 [[SHORT_ID_BRIDGE]](i16 %0)
// CHECK-WINDOWSX64: [[SHORT_ID_PTR:%[0-9]+]] = load i16 (i16)*, i16 (i16)** @{{.*short_id}}
// CHECK-WINDOWSX64: call i16 [[SHORT_ID_PTR]](i16 %0)


// CHECK-DEFAULTABI: zeroext i16 [[UNSIGNED_SHORT_ID_BRIDGE]](i16 zeroext %0)
// CHECK-DEFAULTABI: [[UNSIGNED_SHORT_ID_PTR:%[0-9]+]] = load i16 (i16)*, i16 (i16)** @{{.*unsigned_short_id}}
// CHECK-DEFAULTABI: call zeroext i16 [[UNSIGNED_SHORT_ID_PTR]](i16 zeroext %0)

// CHECK-AAPCS: i16 [[UNSIGNED_SHORT_ID_BRIDGE]](i16 %0)
// CHECK-AAPCS: [[UNSIGNED_SHORT_ID_PTR:%[0-9]+]] = load i16 (i16)*, i16 (i16)** @{{.*unsigned_short_id}}
// CHECK-AAPCS: call i16 [[UNSIGNED_SHORT_ID_PTR]](i16 %0)

// CHECK-WINDOWSX64: i16 [[UNSIGNED_SHORT_ID_BRIDGE]](i16 %0)
// CHECK-WINDOWSX64: [[UNSIGNED_SHORT_ID_PTR:%[0-9]+]] = load i16 (i16)*, i16 (i16)** @{{.*unsigned_short_id}}
// CHECK-WINDOWSX64: call i16 [[UNSIGNED_SHORT_ID_PTR]](i16 %0)


// CHECK-DEFAULTABI: i32 [[STATIC_C_FUNCTION_BRIDGE]](i32 %0, i16 signext %1)
// CHECK-DEFAULTABI: call i32 {{@_.*_knbridge[0-9]+}}(i32 %0, i16 signext %1)

// CHECK-AAPCS: i32 [[STATIC_C_FUNCTION_BRIDGE]](i32 %0, i16 %1)
// CHECK-AAPCS: call i32 {{@_.*_knbridge[0-9]+}}(i32 %0, i16 %1)

// CHECK-WINDOWSX64: i32 [[STATIC_C_FUNCTION_BRIDGE]](i32 %0, i16 %1)
// CHECK-WINDOWSX64: call i32 {{@_.*_knbridge[0-9]+}}(i32 %0, i16 %1)


// CHECK-DEFAULTABI: i32 [[CALLBACK_USER_BRIDGE]](i8* %0)
// CHECK-DEFAULTABI: [[CALLBACK_USER_PTR:%[0-9]+]] = load i32 (i8*)*, i32 (i8*)** @{{.*callbackUser}}
// CHECK-DEFAULTABI: call i32 [[CALLBACK_USER_PTR]](i8* %0)

// CHECK-AAPCS: i32 [[CALLBACK_USER_BRIDGE]](i8* %0)
// CHECK-AAPCS: [[CALLBACK_USER_PTR:%[0-9]+]] = load i32 (i8*)*, i32 (i8*)** @{{.*callbackUser}}
// CHECK-AAPCS: call i32 [[CALLBACK_USER_PTR]](i8* %0)

// CHECK-WINDOWSX64: i32 [[CALLBACK_USER_BRIDGE]](i8* %0)
// CHECK-WINDOWSX64: [[CALLBACK_USER_PTR:%[0-9]+]] = load i32 (i8*)*, i32 (i8*)** @{{.*callbackUser}}
// CHECK-WINDOWSX64: call i32 [[CALLBACK_USER_PTR]](i8* %0)

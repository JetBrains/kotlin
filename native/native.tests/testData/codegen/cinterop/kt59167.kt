/*
* Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
* Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
*/
// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false
//   There is no GameController on watchOS.
// DISABLE_NATIVE: targetFamily=WATCHOS
// MODULE: cinterop
// FILE: kt59167.def
language=Objective-C
---
#import <GameController/GCDevice.h>

// We only need to touch the problematic header to trigger the problem,
// so actual code does not matter.
id<GCDevice> dummy() {
    return nil;
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import kotlin.test.*
import kt59167.*

fun box(): String {
    assertNull(dummy())
    return "OK"
}
/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
// TARGET_BACKEND: NATIVE
// NATIVE_STANDALONE

// MODULE: objc
// FILE: objc.def
language=Objective-C
headers=objc/objc.h

// MODULE: lib(objc)
// FILE: lib.def
language=Objective-C
---
#include <objc/NSObject.h>
#include <objc/objc.h>
#include <objc/runtime.h>

static const char* getProtocolName(Protocol* p) {
    return p == NULL ? NULL : protocol_getName(p);
}

static Protocol* getNSObjectProtocol() {
    return @protocol(NSObject);
}
// MODULE: main(lib)
// FILE: main.kt
import lib.*
import kotlinx.cinterop.toKString
import objcnames.classes.Protocol

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun box(): String {
    // Executing any code is not really necessary. But let's keep it as a sanity check,
    // also making sure the code is compiled and not somehow skipped.
    if (getProtocolName(null) != null) return "FAIL 1"

    val nsObjectProtocol: Protocol? = getNSObjectProtocol()
    if (getProtocolName(nsObjectProtocol)?.toKString() != "NSObject") return "FAIL 2"

    return "OK"
}

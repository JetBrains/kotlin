/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// IGNORE_BACKEND: NATIVE

import kotlin.test.*

fun box(): String {
    try {
        val x = cast<String>(Any())
        return "FAIL: ${x.length}"
    } catch (e: Throwable) {
        return "OK"
    }
}

fun <T> cast(x: Any?) = x as T
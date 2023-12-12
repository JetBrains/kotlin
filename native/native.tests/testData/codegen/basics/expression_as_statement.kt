/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun foo() {
    Any() as String
}

fun box(): String {
    try {
        foo()
    } catch (e: Throwable) {
        return "OK"
    }

    return "Fail"
}
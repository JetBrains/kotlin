/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    try {
        foo()
    } catch (e: Throwable) {
        val message = e.message
        if (message != null) {
            sb.append(message)
        }
    }

    return sb.toString()
}

fun foo() {
    throw Error("OK")
}
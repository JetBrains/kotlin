/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

inline fun <R> call(block: ()->R): R {
    try {
        return block()
    } finally {
        sb.append("OK")
    }
}

fun box(): String {
    call { class Z(); Z() }
    return sb.toString()
}

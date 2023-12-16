/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    apply("OK") {
        sb.append(this)
    }
    return sb.toString()
}

fun apply(str: String, block: String.() -> Unit) {
    str.block()
}
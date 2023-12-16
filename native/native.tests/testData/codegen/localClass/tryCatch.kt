/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

private fun foo() {
    val local =
            object {
                fun bar() {
                    try {
                    } catch (t: Throwable) {
                        sb.appendLine(t)
                    }
                }
            }
    local.bar()
}

fun box(): String {
    sb.append("OK")
    foo()
    return sb.toString()
}
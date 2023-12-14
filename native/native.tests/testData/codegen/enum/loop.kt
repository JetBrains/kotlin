/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()


enum class Zzz {
    Z {
        init {
            sb.appendLine(Z.name)
        }
    }
}

fun box(): String {
    sb.appendLine(Zzz.Z)
    assertEquals("""
        Z
        Z

    """.trimIndent(), sb.toString())
    return "OK"
}
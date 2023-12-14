/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

val lazyValue: String by lazy {
    sb.appendLine("computed!")
    "Hello"
}

fun box(): String {
    sb.appendLine(lazyValue)
    sb.appendLine(lazyValue)

    assertEquals("""
        computed!
        Hello
        Hello

    """.trimIndent(), sb.toString())
    return "OK"
}
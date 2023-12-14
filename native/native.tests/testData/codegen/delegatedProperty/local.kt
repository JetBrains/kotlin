/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*
import kotlin.reflect.KProperty

val sb = StringBuilder()

fun foo(): Int {
   class Delegate {
        operator fun getValue(receiver: Any?, p: KProperty<*>): Int {
            sb.appendLine(p.name)
            return 42
        }
    }

    val x: Int by Delegate()

    return x
}

fun box(): String {
    sb.appendLine(foo())

    assertEquals("""
        x
        42

    """.trimIndent(), sb.toString())
    return "OK"
}
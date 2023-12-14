/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

import kotlin.reflect.KProperty

val sb = StringBuilder()

class Delegate {
    operator fun getValue(receiver: Any?, p: KProperty<*>): Int {
        sb.appendLine(p.name)
        return 42
    }
}

class C {
    val x: Int by Delegate()
}

fun box(): String {
    sb.appendLine(C().x)

    assertEquals("""
        x
        42

    """.trimIndent(), sb.toString())
    return "OK"
}
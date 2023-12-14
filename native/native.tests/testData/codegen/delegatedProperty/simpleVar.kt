/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

import kotlin.reflect.KProperty

val sb = StringBuilder()

class Delegate {
    var f: Int = 42

    operator fun getValue(receiver: Any?, p: KProperty<*>): Int {
        sb.appendLine("get ${p.name}")
        return f
    }

    operator fun setValue(receiver: Any?, p: KProperty<*>, value: Int) {
        sb.appendLine("set ${p.name}")
        f = value
    }
}

class C {
    var x: Int by Delegate()
}

fun box(): String {
    val c = C()
    sb.appendLine(c.x)
    c.x = 117
    sb.appendLine(c.x)

    assertEquals("""
        get x
        42
        set x
        get x
        117

    """.trimIndent(), sb.toString())
    return "OK"
}
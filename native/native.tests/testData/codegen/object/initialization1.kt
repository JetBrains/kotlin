/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

class TestClass {
    constructor() {
        sb.appendLine("constructor1")
    }

    constructor(x: Int) : this() {
        sb.appendLine("constructor2")
    }

    init {
        sb.appendLine("init")
    }

    val f = sb.appendLine("field")
}

fun box(): String {
    TestClass()
    TestClass(1)

    assertEquals("""
        init
        field
        constructor1
        init
        field
        constructor1
        constructor2

    """.trimIndent(), sb.toString())
    return "OK"
}
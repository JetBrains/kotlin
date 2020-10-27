/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.text.parse0

import kotlin.test.*

@Test fun runTest() {
    println("false".toBoolean())
    println("true".toBoolean())
    println("-1".toByte())
    println("a".toByte(16))
    println("aa".toShort(16))
    println("11110".toInt(2))
    println("ffffffff".toLong(16))
    try {
        val x = "ffffffff".toLong(10)
    } catch (ne: NumberFormatException) {
        println("bad format")
    }
    println("0.5".toFloat())
    println("2.39".toDouble())
}
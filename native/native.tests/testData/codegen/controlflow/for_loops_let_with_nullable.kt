/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

// Github issue #1012
fun testInt(left: Int?, right: Int?, step: Int?) {
    right?.let {
        for (i in 0..it) { sb.append(i) }
    }
    sb.appendLine()

    left?.let {
        for (i in it..5) { sb.append(i) }
    }
    sb.appendLine()

    step?.let {
        for (i in 0..5 step it) { sb.append(i) }
    }
    sb.appendLine()

    right?.let {
        for (i in 0 until it) { sb.append(i) }
    }
    sb.appendLine()

    left?.let {
        for (i in it until 5) { sb.append(i) }
    }
    sb.appendLine()

    step?.let {
        for (i in 0 until 5 step it) { sb.append(i) }
    }
    sb.appendLine()

    right?.let {
        for (i in it downTo 0) { sb.append(i) }
    }
    sb.appendLine()

    left?.let {
        for (i in 5 downTo it) { sb.append(i) }
    }
    sb.appendLine()

    step?.let {
        for (i in 5 downTo 0 step it) { sb.append(i) }
    }
    sb.appendLine()
}

fun testLong(left: Long?, right: Long?, step: Long?) {
    right?.let {
        for (i in 0..it) { sb.append(i) }
    }
    sb.appendLine()

    left?.let {
        for (i in it..5) { sb.append(i) }
    }
    sb.appendLine()

    step?.let {
        for (i in 0..5L step it) { sb.append(i) }
    }
    sb.appendLine()

    right?.let {
        for (i in 0 until it) { sb.append(i) }
    }
    sb.appendLine()

    left?.let {
        for (i in it until 5) { sb.append(i) }
    }
    sb.appendLine()

    step?.let {
        for (i in 0 until 5L step it) { sb.append(i) }
    }
    sb.appendLine()

    right?.let {
        for (i in it downTo 0) { sb.append(i) }
    }
    sb.appendLine()

    left?.let {
        for (i in 5 downTo it) { sb.append(i) }
    }
    sb.appendLine()

    step?.let {
        for (i in 5 downTo 0L step it) { sb.append(i) }
    }
    sb.appendLine()
}

fun testChar(left: Char?, right: Char?, step: Int?) {
    right?.let {
        for (i in 'a'..it) { sb.append(i) }
    }
    sb.appendLine()

    left?.let {
        for (i in it..'f') { sb.append(i) }
    }
    sb.appendLine()

    step?.let {
        for (i in 'a'..'f' step it) { sb.append(i) }
    }
    sb.appendLine()

    right?.let {
        for (i in 'a' until it) { sb.append(i) }
    }
    sb.appendLine()

    left?.let {
        for (i in it until 'f') { sb.append(i) }
    }
    sb.appendLine()

    step?.let {
        for (i in 'a' until 'f' step it) { sb.append(i) }
    }
    sb.appendLine()

    right?.let {
        for (i in it downTo 'a') { sb.append(i) }
    }
    sb.appendLine()

    left?.let {
        for (i in 'f' downTo it) { sb.append(i) }
    }
    sb.appendLine()

    step?.let {
        for (i in 'f' downTo 'a' step it) { sb.append(i) }
    }
    sb.appendLine()
}

fun box(): String {
    testInt(0, 5, 2)
    testLong(0, 5, 2)
    testChar('a', 'f', 2)

    assertEquals("""
        012345
        012345
        024
        01234
        01234
        024
        543210
        543210
        531
        012345
        012345
        024
        01234
        01234
        024
        543210
        543210
        531
        abcdef
        abcdef
        ace
        abcde
        abcde
        ace
        fedcba
        fedcba
        fdb

        """.trimIndent(), sb.toString())
    return "OK"
}
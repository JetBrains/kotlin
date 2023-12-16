/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {

    try {
        sb.appendLine("Try")
        throw Error("Error happens")
        sb.appendLine("After throw")
    } catch (e: Error) {
        sb.appendLine("Caught Error")
    } finally {
        sb.appendLine("Finally")
    }

    sb.appendLine("Done")

    assertEquals("""
        Try
        Caught Error
        Finally
        Done

    """.trimIndent(), sb.toString())
    return "OK"
}
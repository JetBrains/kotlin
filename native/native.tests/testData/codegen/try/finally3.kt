/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {

    try {
        try {
            sb.appendLine("Try")
            throw Error("Error happens")
            sb.appendLine("After throw")
        } finally {
            sb.appendLine("Finally")
        }

        sb.appendLine("After nested try")

    } catch (e: Error) {
        sb.appendLine("Caught Error")
    }

    sb.appendLine("Done")

    assertEquals("""
        Try
        Finally
        Caught Error
        Done

    """.trimIndent(), sb.toString())
    return "OK"
}
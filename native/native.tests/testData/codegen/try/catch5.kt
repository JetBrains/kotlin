/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    try {
        try {
            sb.appendLine("Before")
            foo()
            sb.appendLine("After")
        } catch (e: Exception) {
            sb.appendLine("Caught Exception")
        }

        sb.appendLine("After nested try")

    } catch (e: Error) {
        sb.appendLine("Caught Error")
    } catch (e: Throwable) {
        sb.appendLine("Caught Throwable")
    }

    sb.appendLine("Done")

    assertEquals("""
        Before
        Caught Error
        Done

    """.trimIndent(), sb.toString())
    return "OK"
}

fun foo() {
    throw Error("Error happens")
    sb.appendLine("After in foo()")
}
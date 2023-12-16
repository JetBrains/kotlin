/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    test()

    assertEquals("""
        Finally
        Catch 2
        Done

    """.trimIndent(), sb.toString())
    return "OK"
}

fun test() {
    try {
        try {
            return
        } catch (e: Error) {
            sb.appendLine("Catch 1")
        } finally {
            sb.appendLine("Finally")
            throw Error()
        }
    } catch (e: Error) {
        sb.appendLine("Catch 2")
    }

    sb.appendLine("Done")
}
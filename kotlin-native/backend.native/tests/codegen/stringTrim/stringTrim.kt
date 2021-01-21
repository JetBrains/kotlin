/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.stringTrim.stringTrim

import kotlin.test.*

// TODO: check IR
fun constantIndent(): String {
    return """
        Hello,
        World
    """.trimIndent()
}

fun constantMargin(): String {
    return """
        |Hello,
        |World
    """.trimMargin()
}

@Test
fun runTest() {
    assertTrue(constantIndent() === constantIndent())
    assertTrue(constantMargin() === constantMargin())
}


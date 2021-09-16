/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.text.string_builder1

import kotlin.test.*

@Test fun runTest() {
    val a = StringBuilder()
    a.append("Hello").appendLine("Kotlin").appendLine(42).appendLine(0.1).appendLine(true)
    println(a.toString())	
}

/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.boxing.boxing14

import kotlin.test.*

@Test fun runTest() {
    42.println()
}

fun <T> T.println() = println(this.toString())
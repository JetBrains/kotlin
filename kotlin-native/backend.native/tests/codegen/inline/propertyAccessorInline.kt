/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.inline.propertyAccessorInline

import kotlin.test.*

object C {
    const val x = 42
}

fun getC(): C {
    println(123)
    return C
}

@Test fun runTest() {
    println(getC().x)
}


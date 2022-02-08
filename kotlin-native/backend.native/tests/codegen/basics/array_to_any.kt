/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.basics.array_to_any

import kotlin.test.*

@Test
fun runTest() {
    foo().hashCode()
}

fun foo(): Any {
    return Array<Any?>(0, { i -> null })
}
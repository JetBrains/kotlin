/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.delegatedProperty.lazy

import kotlin.test.*

val lazyValue: String by lazy {
    println("computed!")
    "Hello"
}

@Test fun runTest() {
    println(lazyValue)
    println(lazyValue)
}
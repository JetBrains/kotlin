/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.basic.empty_substring

import kotlin.test.*

@Test fun runTest() {
    val hello = "Hello world"
    println(hello.subSequence(1, 1).toString())
}
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.collections.sort0

import kotlin.test.*

@Test fun runTest() {
    println(arrayOf("x", "a", "b").sorted().toString())
    println(intArrayOf(239, 42, -1, 100500, 0).sorted().toString());
}

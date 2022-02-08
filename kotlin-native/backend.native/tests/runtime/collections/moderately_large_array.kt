/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.collections.moderately_large_array

import kotlin.test.*

@Test fun runTest() {
    val a = ByteArray(1000000)

    var sum = 0
    for (b in a) {
        sum += b
    }

    println(sum)
}


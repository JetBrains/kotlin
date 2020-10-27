/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.collections.moderately_large_array1

import kotlin.test.*

@Test fun runTest() {
    val a = Array<Byte>(100000, { i -> i.toByte()})

    var sum = 0
    for (b in a) {
        sum += b
    }

    println(sum)
}


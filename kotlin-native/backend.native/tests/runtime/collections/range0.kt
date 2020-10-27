/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.collections.range0

import kotlin.test.*

@Test fun runTest() {
    for (i in 1..3) print(i)
    println()
    for (i in 'a'..'d') print(i)
    println()
}
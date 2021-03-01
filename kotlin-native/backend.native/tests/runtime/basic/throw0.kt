/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.basic.throw0

import kotlin.test.*

@Test fun runTest() {
    val cond = 1
    if (cond == 2) throw RuntimeException()
    if (cond == 3) throw NoSuchElementException("no such element")
    if (cond == 4) throw Error("error happens")

    println("Done")
}

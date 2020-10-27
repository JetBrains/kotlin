/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.exceptions.extend0

import kotlin.test.*

class C : Exception("OK")

@Test fun runTest() {
    try {
        throw C()
    } catch (e: Throwable) {
        println(e.message!!)
    }
}
/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test.tests

import kotlin.test.*

class BasicAssertionsJVMTest {

    @Test
    fun testFailsWithClassMessage() {
        @Suppress("UNCHECKED_CAST")
        (assertFailsWith((Class.forName("java.lang.IllegalArgumentException") as Class<Throwable>).kotlin) {
            throw IllegalArgumentException()
        })
    }
}

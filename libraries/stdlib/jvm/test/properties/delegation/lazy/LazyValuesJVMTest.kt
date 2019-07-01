/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.properties.delegation.lazy

import kotlin.test.*
import kotlin.jvm.Volatile

class SynchronizedLazyValTest {
    @Volatile
    var result = 0
    val a by lazy(this) {
        ++result
    }

    @Test fun doTest() {
        synchronized(this) {
            kotlin.concurrent.thread { a } // not available in js
            result = 1
            a
        }
        assertTrue(a == 2, "fail: initializer should be invoked only once")
        assertTrue(result == 2, "fail result should be incremented after test")
    }
}
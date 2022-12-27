/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jdk7.test

import test.platformNull
import java.io.*
import kotlin.test.*

class UseAutoCloseableResourceJVMTest {

    class Resource : AutoCloseable {
        override fun close() {
            error("Unreachable")
        }
    }

    @Test fun platformResourceOpFails() {
        val resource = platformNull<Resource>()
        val e = assertFails {
            resource.use { requireNotNull(it) }
        }
        assertTrue(e is IllegalArgumentException)
        assertTrue(e.suppressed.isEmpty())
    }
}
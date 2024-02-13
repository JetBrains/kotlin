/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.objcexport.KtObjCExportConfiguration
import org.jetbrains.kotlin.objcexport.KtObjCExportSession
import org.jetbrains.kotlin.objcexport.cached
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.fail

class KtObjCExportSessionCacheTest {

    data class Payload(val key: Any)

    @Test
    fun `test - cache with string key`() {

        KtObjCExportSession(KtObjCExportConfiguration()) {
            val cachedInstance1 = cached("instance1") { Payload("1") }
            val cachedInstance2 = cached("instance2") { Payload("2") }

            assertSame(cachedInstance1, cached("instance1") { fail("Expected instance1 to be cached") })
            assertSame(cachedInstance2, cached("instance2") { fail("Expected instance2 to be cached") })

            assertEquals(cachedInstance1, Payload("1"))
            assertEquals(cachedInstance2, Payload("2"))
        }
    }
}

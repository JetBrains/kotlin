/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.jetbrains.kotlin.util.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class BuildFinishedListenerServiceTest {
    private val service = object : BuildFinishedListenerService() {
        override fun getParameters() = fail("The service has no parameters")
    }

    @Test
    fun testOnCloseActions() {
        val actionResults = arrayListOf<Int>()
        service.onClose {
            actionResults.add(1)
        }
        service.onClose {
            actionResults.add(2)
        }

        service.close()

        assertEquals(arrayListOf(1, 2), actionResults)
    }

    @Test
    fun testOnCloseOnceByKey() {
        val actionResults = arrayListOf<Int>()
        service.onCloseOnceByKey("key1") {
            actionResults.add(1)
        }
        service.onCloseOnceByKey("key1") {
            fail("Should not be executed")
        }

        service.onCloseOnceByKey("key2") {
            actionResults.add(3)
        }
        service.onCloseOnceByKey("key2") {
            fail("Should not be executed")
        }

        service.close()

        assertEquals(arrayListOf(1, 3), actionResults)
    }

    @Test
    fun testServiceCanBeClosedOnlyOnce() {
        service.close()
        val exceptionOnClose = assertThrows<IllegalStateException> { service.close() }
        assertEquals("BuildFinishedListenerService is already closed", exceptionOnClose.message)
    }

    @Test
    fun testNoActionsExecutedWithoutClose() {
        service.onClose {
            fail("Should not be executed")
        }

        service.onCloseOnceByKey("key") {
            fail("Should not be executed")
        }
    }

    @Test
    fun testCannotRegisterActionsAfterClose() {
        service.close()

        val exception1 = assertThrows<IllegalStateException> {
            service.onClose {
                fail("Should not be executed")
            }
        }
        assertEquals("BuildFinishedListenerService is already closed, cannot register new actions", exception1.message)

        val exception2 = assertThrows<IllegalStateException> {
            service.onCloseOnceByKey("key") {
                fail("Should not be executed")
            }
        }
        assertEquals("BuildFinishedListenerService is already closed, cannot register new actions", exception2.message)
    }
}
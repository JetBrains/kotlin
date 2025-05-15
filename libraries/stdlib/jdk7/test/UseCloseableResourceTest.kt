/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jdk7.test

import test.platformNull
import java.io.*
import kotlin.test.*

class UseCloseableResourceTest {

    class Resource(val faultyClose: Boolean = false) : Closeable {

        var isClosed = false
            private set

        override fun close() {
            if (faultyClose)
                throw IOException("Close failed")
            isClosed = true
        }
    }

    @Test fun success() {
        val resource = Resource()
        val result = resource.use { "ok" }
        assertEquals("ok", result)
        assertTrue(resource.isClosed)
    }

    @Test fun closeFails() {
        val e = assertFails {
            val _ = Resource(faultyClose = true).use { it.isClosed }
        }
        assertTrue(e is IOException)
    }

    @Test fun opFailsCloseSuccess() {
        val e = assertFails {
            Resource().use { error("op fail") }
        }
        assertTrue(e is IllegalStateException)
        assertTrue(e.suppressed.isEmpty())
    }

    @Test fun opFailsCloseFails() {
        val e = assertFails {
            Resource(faultyClose = true).use { error("op fail") }
        }
        assertTrue(e is IllegalStateException)
        assertTrue(e.suppressed.single() is IOException)
    }

    @Test fun opFailsCloseFailsTwice() {
        val e = assertFails {
            Resource(faultyClose = true).use { _ ->
                Resource(faultyClose = true).use { _ ->
                    error("op fail")
                }
            }
        }
        assertTrue(e is IllegalStateException)
        val suppressed = e.suppressed
        assertEquals(2, suppressed.size)
        assertTrue(suppressed.all { it is IOException })
    }

    @Test fun nonLocalReturnInBlock() {
        fun Resource.operation(nonLocal: Boolean): String {
            return use { if (nonLocal) return "nonLocal" else "local" }
        }

        Resource().let { resource ->
            val result = resource.operation(nonLocal = false)
            assertEquals("local", result)
            assertTrue(resource.isClosed)
        }

        Resource().let { resource ->
            val result = resource.operation(nonLocal = true)
            assertEquals("nonLocal", result)
            assertTrue(resource.isClosed)
        }

    }

    @Test fun nullableResourceSuccess() {
        val resource: Resource? = null
        val result = resource.use { "ok" }
        assertEquals("ok", result)
    }

    @Test fun nullableResourceOpFails() {
        val resource: Resource? = null
        val e = assertFails {
            resource.use { requireNotNull(it) }
        }
        assertTrue(e is IllegalArgumentException)
        assertTrue(e.suppressed.isEmpty())
    }

    @Test fun platformResourceOpFails() {
        val resource = platformNull<Resource>()
        val e = assertFails {
            resource.use { requireNotNull(it) }
        }
        assertTrue(e is IllegalArgumentException)
        assertTrue(e.suppressed.isEmpty())
    }

    @Test
    fun contractCallsInPlace() {
        val i: Int
        Resource().use { _ ->
            Resource().use { _ ->
                i = 1
            }
        }
        assertEquals(1, i)
    }
}

/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.jdk7.test

import java.io.*
import org.junit.Test
import java.util.*
import kotlin.test.*

class TryWithResourcesAutoCloseableTest {

    @Suppress("HasPlatformType") fun <T> platformNull() = Collections.singletonList(null as T).first()

    class Resource(val faultyClose: Boolean = false) : AutoCloseable {

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
            Resource(faultyClose = true).use { "" }
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

}

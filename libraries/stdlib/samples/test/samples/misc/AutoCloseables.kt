/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.misc

import samples.*
import kotlin.test.*

class AutoCloseables {
    @Sample
    fun naive() {
        class Resource {
            var isReleased = false
                private set

            fun release() = if (!isReleased) isReleased = true else throw IllegalStateException("Already released")
        }

        class NaiveCloseable(private val resource: Resource) : AutoCloseable {
            override fun close() {
                resource.release()
            }
        }

        val resource = Resource()
        val closeable = NaiveCloseable(resource)

        closeable.use { /* do something */ }
        assertTrue(resource.isReleased)

        // close again explicitly
        assertFailsWith<IllegalStateException> { closeable.close() }
    }

    @Sample
    fun idempotent() {
        class Resource {
            var isReleased = false
                private set

            fun release() = if (!isReleased) isReleased = true else throw IllegalStateException("Already released")
        }

        class IdempotentCloseable(private val resource: Resource) : AutoCloseable {
            private var isClosed = false

            override fun close() {
                if (isClosed) return
                isClosed = true
                resource.release()
            }
        }

        val resource = Resource()
        val closeable = IdempotentCloseable(resource)

        closeable.use { /* do something */ }
        assertTrue(resource.isReleased)

        closeable.close() // close again explicitly
    }
}

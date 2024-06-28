/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.autoCloseable

import kotlin.test.*

class AutoCloseableConstructorFunctionTest {
    class ResourceCloseException : Exception()

    @Test
    fun success() {
        var isClosed = false
        val resource = AutoCloseable(closeAction = { isClosed = true })
        val result = resource.use { "ok" }
        assertEquals("ok", result)
        assertTrue(isClosed)
    }

    @Test
    fun closeFails() {
        assertFailsWith<ResourceCloseException> {
            AutoCloseable { throw ResourceCloseException() }.close()
        }
    }

    @Test
    fun multipleCloseInvocations() {
        var counter = 0
        val resource = AutoCloseable { counter++ }
        resource.use {}
        assertEquals(1, counter)
        resource.close()
        resource.close()
        assertEquals(3, counter)
    }
}

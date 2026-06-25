/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.targets.js.testing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PrefixLocatorTest {

    // region add()

    @Test
    fun `add returns null for a freshly added key`() {
        val locator = SimpleJdkHttpServer.PrefixLocator<String>()
        assertNull(locator.add("key", "value"))
    }

    @Test
    fun `add returns previous value when key already exists`() {
        val locator = SimpleJdkHttpServer.PrefixLocator<String>()
        locator.add("key", "first")
        assertEquals("first", locator.add("key", "second"))
    }

    // endregion

    // region findClosestGreaterPrefix()

    @Test
    fun `findClosestGreaterPrefix returns null for empty locator`() {
        val locator = SimpleJdkHttpServer.PrefixLocator<String>()
        assertNull(locator.findClosestGreaterPrefix("anything"))
    }

    @Test
    fun `findClosestGreaterPrefix returns null when no key is contained in query`() {
        val locator = SimpleJdkHttpServer.PrefixLocator<String>()
        locator.add("ab", "1")
        locator.add("abc", "2")
        locator.add("cba", "3")
        assertNull(locator.findClosestGreaterPrefix("a"))
    }

    @Test
    fun `findClosestGreaterPrefix returns longest matching key`() {
        val locator = SimpleJdkHttpServer.PrefixLocator<String>()
        locator.add("ab", "1")
        locator.add("abc", "2")
        locator.add("cba", "3")
        assertEquals("abc" to "2", locator.findClosestGreaterPrefix("abc11111"))
    }

    @Test
    fun `findClosestGreaterPrefix returns exact match`() {
        val locator = SimpleJdkHttpServer.PrefixLocator<String>()
        locator.add("abc", "value")
        assertEquals("abc" to "value", locator.findClosestGreaterPrefix("abc"))
    }

    @Test
    fun `findClosestGreaterPrefix matches task path prefix in URL path`() {
        val locator = SimpleJdkHttpServer.PrefixLocator<String>()
        locator.add(":jsTest", "root")
    }

    @Test
    fun `findClosestGreaterPrefix matches by prefix not by substring`() {
        val locator = SimpleJdkHttpServer.PrefixLocator<String>()
        locator.add(":jsTest", "root")
        // Verify that the key must be at the start of the query string
        // When ":jsTest" appears in the middle, it should not match
        assertNull(locator.findClosestGreaterPrefix("prefix:jsTest/test.html"))
    }

    // endregion

}

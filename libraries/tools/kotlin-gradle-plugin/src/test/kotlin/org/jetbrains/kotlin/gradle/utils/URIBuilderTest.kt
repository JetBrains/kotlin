/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.test.*

class URIBuilderTest {

    @Test
    fun `sanity check - URI passed through builder without modification matches original`() {
        val original = URI("https://user@example.com:8080/some/path?foo=bar&baz=qux#frag")
        val rebuilt = original.withBuilder { }
        assertEquals(original, rebuilt)
    }

    @Test
    fun `sanity check - URI without query and fragment is preserved`() {
        val original = URI("https://example.com/some/path")
        val rebuilt = original.withBuilder { }
        assertEquals(original, rebuilt)
    }

    @Test
    fun `sanity check - URI with only query is preserved`() {
        val original = URI("https://example.com/path?a=1&b=2")
        val rebuilt = original.withBuilder { }
        assertEquals(original, rebuilt)
    }

    @Test
    fun `addQueryParam adds a new query parameter to URI without query`() {
        val base = URI("https://example.com/path")
        val result = base.withBuilder {
            addQueryParam("key", "value")
        }
        assertEquals("https://example.com/path?key=value", result.toString())
    }

    @Test
    fun `addQueryParam appends to existing query parameters`() {
        val base = URI("https://example.com/path?existing=1")
        val result = base.withBuilder {
            addQueryParam("added", "2")
        }
        val query = result.query.split("&").toSet()
        assertEquals(setOf("existing=1", "added=2"), query)
    }

    @Test
    fun `addQueryParam preserves multiple values for the same key`() {
        val base = URI("https://example.com/path?key=v1")
        val result = base.withBuilder {
            addQueryParam("key", "v2")
        }
        val values = result.query.split("&").filter { it.startsWith("key=") }.map { it.substringAfter("=") }
        assertEquals(listOf("v1", "v2"), values)
    }

    @Test
    fun `addQueryParam adds multiple values for a new key`() {
        val base = URI("https://example.com/path")
        val result = base.withBuilder {
            addQueryParam("k", "a")
            addQueryParam("k", "b")
        }
        val values = result.query.split("&").map { it.substringAfter("=") }
        assertEquals(listOf("a", "b"), values)
    }

    @Test
    fun `addQueryParam URL-encodes keys and values`() {
        val base = URI("https://example.com/path")
        val result = base.withBuilder {
            addQueryParam("key with space", "value&special=chars")
        }
        // The raw query preserves the URL-encoded form; URI.getQuery() returns the decoded form.
        assertEquals("key+with+space=value%26special%3Dchars", result.rawQuery)
        // URI.getQuery() decodes percent-escapes but keeps '+' literal (it is not '%20').
        assertEquals("key+with+space=value&special=chars", result.query)
    }

    @Test
    fun `addQueryParam encodes special characters question mark pipe and space in keys and values`() {
        val base = URI("https://example.com/path")
        val result = base.withBuilder {
            addQueryParam("key ?|name", "val ?|ue")
        }
        // The raw query keeps the URL-encoded form; URI.getQuery() returns the decoded form.
        assertEquals("key+%3F%7Cname=val+%3F%7Cue", result.rawQuery)
        // URI.getQuery() decodes percent-escapes but keeps '+' literal (it is not '%20').
        assertEquals("key+?|name=val+?|ue", result.query)
    }

    @Test
    fun `addQueryParam preserves scheme authority path and fragment`() {
        val base = URI("https://user@example.com:8080/some/path?foo=bar#frag")
        val result = base.withBuilder {
            addQueryParam("added", "x")
        }
        assertEquals(base.scheme, result.scheme)
        assertEquals(base.authority, result.authority)
        assertEquals(base.path, result.path)
        assertEquals(base.fragment, result.fragment)
    }

    @Test
    fun `addQueryParam works for file scheme URI`() {
        val baseUri = Path("/foo").toUri()
        val result = baseUri.withBuilder {
            addQueryParam("key", "value")
            addQueryParam("someJson", """["'", "", {}]""")
        }
        assertEquals("file", result.scheme)
        assertEquals("""key=value&someJson=["'",+"",+{}]""", result.query)
        assertEquals("key=value&someJson=%5B%22%27%22%2C+%22%22%2C+%7B%7D%5D", result.rawQuery)
        assertEquals("${baseUri}?key=value&someJson=%5B%22%27%22%2C+%22%22%2C+%7B%7D%5D", result.toString())
    }

    @Test
    fun `addQueryParam supports chaining`() {
        val base = URI("https://example.com/")
        val result = base.withBuilder {
            addQueryParam("a", "1").addQueryParam("b", "2")
        }
        val query = result.query.split("&").toSet()
        assertEquals(setOf("a=1", "b=2"), query)
    }
}

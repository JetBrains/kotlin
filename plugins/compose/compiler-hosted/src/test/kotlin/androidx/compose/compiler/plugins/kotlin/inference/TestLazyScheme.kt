/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin.inference

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TestLazyScheme {

    @Test
    fun canCreateALazyScheme() {
        val scheme = schemeOf("[a, [b]]")
        val lazyScheme = LazyScheme(scheme)
        val schemeCopy = lazyScheme.toScheme()
        assertEquals(scheme, schemeCopy)
    }

    @Test
    fun canCreateALazySchemeWithOpenParameters() {
        val scheme = schemeOf("[0, [0]]")
        val lazyScheme = LazyScheme(scheme)
        val schemeCopy = lazyScheme.toScheme()
        assertEquals(scheme, schemeCopy)
    }

    @Test
    fun canCreateALazySchemeWithAnonymousParameters() {
        val scheme = schemeOf("[_, [_]]")
        val lazyScheme = LazyScheme(scheme)
        val schemeCopy = lazyScheme.toScheme()
        assertEquals(scheme, schemeCopy)
    }

    @Test
    fun canUpdateLazySchemeThroughBindings() {
        val lazyScheme = LazyScheme(schemeOf("[_, [_]]"))
        val bindings = lazyScheme.bindings
        bindings.unify(lazyScheme.target, lazyScheme.parameters.first().target)
        assertEquals(schemeOf("[0, [0]]"), lazyScheme.toScheme())
    }

    @Test
    fun canUpdateResult() {
        val lazyScheme = LazyScheme(schemeOf("[0, [1], [2]:[0, [2], [1]]"))
        val bindings = lazyScheme.bindings
        val a = bindings.closed("a")
        val b = bindings.closed("b")
        val c = bindings.closed("c")
        bindings.unify(lazyScheme.target, a)
        bindings.unify(lazyScheme.parameters[0].target, b)
        bindings.unify(lazyScheme.parameters[1].target, c)
        assertEquals(schemeOf("[a, [b], [c]:[a, [c], [b]]]"), lazyScheme.toScheme())
    }

    @Test
    fun canCreateAnyParameterScheme() {
        val lazyScheme = LazyScheme(schemeOf("[A*]"))
        assertTrue(lazyScheme.anyParameters)
        assertTrue(lazyScheme.parameters.isEmpty())
    }
}

internal fun schemeOf(text: String): Scheme {
    val eos = '\u0000'
    var current = 0

    fun skipWhiteSpace() {
        while (current < text.length) {
            when (text[current]) {
                ' ', '\t', '\n', '\r' -> {
                    current++
                    continue
                }
            }
            break
        }
    }

    fun expect(c: Char) {
        if (c == eos) return
        if (current < text.length && text[current] != c) error("Expected '$c' at $current")
        current++
        skipWhiteSpace()
    }

    fun isChar(c: Char) =
        if (current < text.length && c == text[current]) {
            current++
            true
        } else false

    fun expectToken(): String = buildString {
        var charSeen = false
        while (current < text.length) {
            val ch = text[current]
            if ((ch in 'a'..'z') || (ch in 'A'..'Z')) {
                append(ch)
                current++
                charSeen = true
                continue
            }
            break
        }
        if (!charSeen) error("Expected a token at $current")
        skipWhiteSpace()
    }

    fun expectNumber(): Int {
        var numberSeen = false
        var result = 0
        while (current < text.length) {
            val ch = text[current]
            if (ch in '0'..'9') {
                result = result * 10 + (ch - '0')
                current++
                numberSeen = true
                continue
            }
            break
        }
        if (!numberSeen) error("Expected a number at $current")
        skipWhiteSpace()
        return result
    }

    fun <T> delimited(start: Char, end: Char, block: () -> T): T {
        skipWhiteSpace()
        expect(start)
        return block().also { expect(end) }
    }

    fun <T> optional(start: Char, end: Char = '\u0000', block: () -> T): T? = run {
        skipWhiteSpace()
        if (text[current] == start) {
            delimited(start, end, block)
        } else null
    }

    fun isVariableStart() = current < text.length && when (text[current]) {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '_', '\\' -> true
        else -> false
    }

    fun item(): Item = if (isVariableStart()) {
        if (text[current] == '\\') expect('\\')
        if (text[current] == '_') {
            expect('_')
            Open(-1)
        } else {
            Open(expectNumber())
        }
    } else Token(expectToken())

    fun <T> list(continueBlock: (first: Boolean) -> Boolean, block: () -> T): List<T> =
        if (continueBlock(true)) {
            skipWhiteSpace()
            val result = mutableListOf<T>()
            while (true) {
                result.add(block())
                if (!continueBlock(false)) break
                skipWhiteSpace()
            }
            result
        } else emptyList()

    fun scheme(): Scheme = delimited('[', ']') {
        val target = item()
        val anyParameters = isChar('*')
        val parameters = if (anyParameters) emptyList() else list({
            (text[current] == ',').also { if (it) expect(',') }
        }) { scheme() }
        val result = optional(':') { scheme() }
        Scheme(target, parameters, result, anyParameters)
    }

    return scheme()
}
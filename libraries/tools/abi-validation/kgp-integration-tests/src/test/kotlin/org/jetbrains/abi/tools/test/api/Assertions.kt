/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.abi.tools.test.api

import kotlin.test.assertContains
import kotlin.test.assertEquals

internal object Assertions {
    fun assertThat(text: String): Asserter {
        return Asserter(text)
    }
}

internal class Asserter(val value: String) {
    fun contains(other: String): Asserter {
        assertContains(value, other)
        return this
    }

    fun isEqualToIgnoringNewLines(text: String): Asserter {
        val actual = value.removeNewLines()
        val expected = text.removeNewLines()
        assertEquals(expected, actual)
        return this
    }

    fun isEqualTo(other: String): Asserter {
        assertEquals(other, value)
        return this
    }
}

internal fun CharSequence.removeNewLines(): String {
    val normalizedText: String = normalizeNewlines(this)
    return normalizedText.replace("\n", "")
}

internal fun normalizeNewlines(actual: CharSequence): String {
    return actual.toString().replace("\r\n", "\n")
}
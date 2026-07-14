/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.test

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.dependencies.impl.SimpleExternalDependenciesResolverOptionsParser
import kotlin.script.experimental.dependencies.impl.makeExternalDependenciesResolverOptions

class ResolverOptionsTest {
    @Test
    fun testValueInMapAppearsIfPresent() {
        val map = mapOf("option" to "value")
        val options = makeExternalDependenciesResolverOptions(map)

        assertEquals("value", options.value("option"))
    }

    @Test
    fun testFlagInMapAppearsIfPresent() {
        val map = mapOf("option" to "true")
        val options = makeExternalDependenciesResolverOptions(map)

        assertEquals("true", options.value("option"))
        assertEquals(true, options.flag("option"))
    }

    @Test
    fun testValueInMapDoesNotAppearsIfPresent() {
        val options = makeExternalDependenciesResolverOptions(emptyMap())
        assertNull(options.value("option"))
    }

    @Test
    fun testFlagInMapDoesNotAppearsIfPresent() {
        val options = makeExternalDependenciesResolverOptions(emptyMap())
        assertNull(options.flag("option"))
    }

    @Test
    fun testParserReturnsSingleValue() {
        val parser = SimpleExternalDependenciesResolverOptionsParser
        val options = parser("option1 = hello").valueOrThrow()
        assertEquals("hello", options.value("option1"))
    }

    @Test
    fun testParserReturnsMultipleValue() {
        val parser = SimpleExternalDependenciesResolverOptionsParser
        val options = parser("option1 = hello option2 = 42").valueOrThrow()
        assertEquals("hello", options.value("option1"))
        assertEquals("42", options.value("option2"))
    }


    @Test
    fun testParserReturnsSingleFlag() {
        val parser = SimpleExternalDependenciesResolverOptionsParser
        val options = parser("option1 = hello").valueOrThrow()
        assertEquals("hello", options.value("option1"))
    }

    @Test
    fun testParserReturnsMultipleFlags() {
        val parser = SimpleExternalDependenciesResolverOptionsParser
        val options = parser("option1 option2=false option3").valueOrThrow()
        assertEquals(true, options.flag("option1"))
        assertEquals(false, options.flag("option2"))
        assertEquals(true, options.flag("option3"))
    }

    @Test
    fun testParserAcceptsSpecialSymbols() {
        val parser = SimpleExternalDependenciesResolverOptionsParser
        val options = parser("option1=/User/path/file.kt option2=C:\\\\User\\\\file.pem option3=\$MY_ENV").valueOrThrow()
        assertEquals("/User/path/file.kt", options.value("option1"))
        assertEquals("C:\\User\\file.pem", options.value("option2"))
        assertEquals("\$MY_ENV", options.value("option3"))
    }

    @Test
    fun testParserAcceptsValuesWithSpaces() {
        val parser = SimpleExternalDependenciesResolverOptionsParser
        val options = parser("option1= spaced\\ \\ value\\  option2=\\ x option3=line1\\nline2").valueOrThrow()
        assertEquals("spaced  value ", options.value("option1"))
        assertEquals(" x", options.value("option2"))
        assertEquals("line1\nline2", options.value("option3"))
    }

    @Test
    fun testParserReturnsMixOfValuesAndFlags() {
        val parser = SimpleExternalDependenciesResolverOptionsParser
        val options = parser("option1 = hello option2 option3=world option4 option5 = false").valueOrThrow()
        assertEquals("hello", options.value("option1"))
        assertEquals(true, options.flag("option2"))
        assertEquals("world", options.value("option3"))
        assertEquals(true, options.flag("option4"))
        assertEquals(false, options.flag("option5"))
    }

    @Test
    fun testParserReportsClashWithConflictingOptions() {
        val parser = SimpleExternalDependenciesResolverOptionsParser
        when (val result = parser("option1 = hello option1 = world")) {
            is ResultWithDiagnostics.Success -> fail("Managed to parse options despite conflicting options: ${result.value}")
            is ResultWithDiagnostics.Failure -> {
                assertEquals(1, result.reports.count())
                assertEquals("Conflicting values for option option1: hello and world", result.reports.first().message)
            }
        }
    }

    @Test
    fun testParserDoesNotClashWithTheSameOptionTwice() {
        val parser = SimpleExternalDependenciesResolverOptionsParser
        val options = parser("option1 = hello option1 = hello").valueOrThrow()
        assertEquals("hello", options.value("option1"))
    }
}

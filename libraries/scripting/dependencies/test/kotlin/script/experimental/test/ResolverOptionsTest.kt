/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.test

import junit.framework.TestCase
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.dependencies.impl.SimpleExternalDependenciesResolverOptionsParser
import kotlin.script.experimental.dependencies.impl.makeExternalDependenciesResolverOptions

class ResolverOptionsTest : TestCase() {
    fun testValueInMapAppearsIfPresent() {
        val map = mapOf("option" to "value")
        val options = makeExternalDependenciesResolverOptions(map)

        assertEquals(options.value("option"), "value")
    }

    fun testFlagInMapAppearsIfPresent() {
        val map = mapOf("option" to "true")
        val options = makeExternalDependenciesResolverOptions(map)

        assertEquals(options.value("option"), "true")
        assertEquals(options.flag("option"), true)
    }

    fun testValueInMapDoesNotAppearsIfPresent() {
        val options = makeExternalDependenciesResolverOptions(emptyMap())
        assertNull(options.value("option"))
    }

    fun testFlagInMapDoesNotAppearsIfPresent() {
        val options = makeExternalDependenciesResolverOptions(emptyMap())
        assertNull(options.flag("option"))
    }

    fun testParserReturnsSingleValue() {
        val parser = SimpleExternalDependenciesResolverOptionsParser
        val options = parser("option1 = hello").valueOrThrow()
        assertEquals(options.value("option1"), "hello")
    }

    fun testParserReturnsMultipleValue() {
        val parser = SimpleExternalDependenciesResolverOptionsParser
        val options = parser("option1 = hello option2 = 42").valueOrThrow()
        assertEquals(options.value("option1"), "hello")
        assertEquals(options.value("option2"), "42")
    }


    fun testParserReturnsSingleFlag() {
        val parser = SimpleExternalDependenciesResolverOptionsParser
        val options = parser("option1 = hello").valueOrThrow()
        assertEquals(options.value("option1"), "hello")
    }

    fun testParserReturnsMultipleFlags() {
        val parser = SimpleExternalDependenciesResolverOptionsParser
        val options = parser("option1 option2=false option3").valueOrThrow()
        assertEquals(options.flag("option1"), true)
        assertEquals(options.flag("option2"), false)
        assertEquals(options.flag("option3"), true)
    }

    fun testParserAcceptsSpecialSymbols() {
        val parser = SimpleExternalDependenciesResolverOptionsParser
        val options = parser("option1=/User/path/file.kt option2=C:\\\\User\\\\file.pem option3=\$MY_ENV").valueOrThrow()
        assertEquals(options.value("option1"), "/User/path/file.kt")
        assertEquals(options.value("option2"), "C:\\User\\file.pem")
        assertEquals(options.value("option3"), "\$MY_ENV")
    }

    fun testParserAcceptsValuesWithSpaces() {
        val parser = SimpleExternalDependenciesResolverOptionsParser
        val options = parser("option1= spaced\\ \\ value\\  option2=\\ x option3=line1\\nline2").valueOrThrow()
        assertEquals(options.value("option1"), "spaced  value ")
        assertEquals(options.value("option2"), " x")
        assertEquals(options.value("option3"), "line1\nline2")
    }

    fun testParserReturnsMixOfValuesAndFlags() {
        val parser = SimpleExternalDependenciesResolverOptionsParser
        val options = parser("option1 = hello option2 option3=world option4 option5 = false").valueOrThrow()
        assertEquals(options.value("option1"), "hello")
        assertEquals(options.flag("option2"), true)
        assertEquals(options.value("option3"), "world")
        assertEquals(options.flag("option4"), true)
        assertEquals(options.flag("option5"), false)
    }

    fun testParserReportsClashWithConflictingOptions() {
        val parser = SimpleExternalDependenciesResolverOptionsParser
        when (val result = parser("option1 = hello option1 = world")) {
            is ResultWithDiagnostics.Success -> fail("Managed to parse options despite conflicting options: ${result.value}")
            is ResultWithDiagnostics.Failure -> {
                assertEquals(result.reports.count(), 1)
                assertEquals(result.reports.first().message, "Conflicting values for option option1: hello and world")
            }
        }
    }

    fun testParserDoesNotClashWithTheSameOptionTwice() {
        val parser = SimpleExternalDependenciesResolverOptionsParser
        val options = parser("option1 = hello option1 = hello").valueOrThrow()
        assertEquals(options.value("option1"), "hello")
    }
}
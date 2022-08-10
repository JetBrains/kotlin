/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model.infra

import org.jetbrains.kotlin.project.model.coreCases.KpmTestCaseDescriptor
import org.jetbrains.kotlin.project.model.infra.generate.generateTestMethodsTemplateForCases
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Base class for testing KPM Core cases
 *
 * All core cases are listed here as abstract tests and will be required,
 * to be overridden introducing a compile-time check that as soon as new
 * case is added, it will be covered in all inheritors.
 *
 * For situation when a new Core Case is added, but is mistakenly not added
 * to this interface, there's a [checkAllCoreCasesCovered], which will enforce
 * that all Core Cases have a respective method in this interface
 *
 * Additionally, this interface uses [KpmCoreCasesJunitParameterResolver], which
 * will inject a corresponding [KpmTestCaseDescriptor] into a test-method based on the
 * method's name
 */
@ExtendWith(KpmCoreCasesJunitParameterResolver::class)
interface KpmCoreCasesTestRunner {
    @Test
    @Throws(Exception::class)
    fun testSimpleProjectToProject(case: KpmTestCase)

    @Test
    @Throws(Exception::class)
    fun testSimpleTwoLevel(case: KpmTestCase)

    @Test
    @Throws(Exception::class)
    fun checkAllCoreCasesCovered() {
        val testRunnerClass = this::class.java

        val testCasesNames = testRunnerClass.methods.asSequence()
            .map { it.name }
            .filter { it.startsWith("test") }
            .map { it.substringAfter("test") }
            .toSet()

        val uncoveredCases = KpmTestCaseDescriptor.allCasesNames - testCasesNames
        if (uncoveredCases.isNotEmpty()) {
            Assertions.fail<Nothing>(
                """
                    Test runner '${testRunnerClass.canonicalName}'
                    has some KPM Core Test Cases uncovered:
    
                    ${uncoveredCases.joinToString()}
                """.trimIndent() + "\n\n" + fixSuggestion(uncoveredCases)
            )
        }
    }
}

private fun fixSuggestion(missingCases: Set<String>): String =
    "Please re-run \"Generate KPM Tests\"-task to generate missing methods,\n" +
            "or insert the following scaffold if the test runner uses custom assertions:\n\n" +
            generateTestMethodsTemplateForCases(missingCases)

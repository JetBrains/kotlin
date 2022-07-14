/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model.infra

import org.jetbrains.kotlin.project.model.coreCases.KpmTestCaseWrapper
import org.jetbrains.kotlin.project.model.infra.generate.generateTestMethodsTemplateForCases
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(KpmCoreCasesJunitParameterResolver::class)
interface KpmCoreCasesTestRunner {
    fun runTest(kpmTestCase: KpmTestCase)

    @Test
    fun checkAllCoreCasesCovered() {
        val testRunnerClass = this::class.java

        val testCasesNames = testRunnerClass.methods.asSequence()
            .map { it.name }
            .filter { it.startsWith("test") }
            .map { it.substringAfter("test") }
            .toSet()

        val uncoveredCases = KpmTestCaseWrapper.allCasesNames - testCasesNames
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

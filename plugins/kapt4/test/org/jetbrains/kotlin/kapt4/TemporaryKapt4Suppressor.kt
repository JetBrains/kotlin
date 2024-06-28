/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4

import org.jetbrains.kotlin.kapt4.Kapt4Directives.FIR_BLOCKED
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.directives.model.Directive
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure

internal class TemporaryKapt4Suppressor(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(Kapt4Directives)

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        val hasFailures = failedAssertions.isNotEmpty()
        if (suppressedByDirective(FIR_BLOCKED, hasFailures)) return emptyList()
        return failedAssertions
    }

    private fun suppressedByDirective(directive: Directive, hasFailures: Boolean): Boolean {
        val hasDirective = testServices.moduleStructure.modules.any { directive in it.directives }
        if (hasDirective && !hasFailures) {
            testServices.assertions.fail { "Test passes, remove $directive directive" }
        }
        return hasDirective
    }
}

object Kapt4Directives : SimpleDirectivesContainer() {
    val FIR_BLOCKED by stringDirective("Blocked by light classes")
}

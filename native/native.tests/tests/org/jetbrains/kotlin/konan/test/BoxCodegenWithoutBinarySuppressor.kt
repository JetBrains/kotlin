/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor.SuppressionChecker
import org.jetbrains.kotlin.test.backend.codegenSuppressionChecker
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.ServiceRegistrationData
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.service
import org.jetbrains.kotlin.utils.bind

/**
 * The regular [BlackBoxCodegenSuppressor] fails the test with [CodegenTestDirectives.IGNORE_BACKEND],
 * [CodegenTestDirectives.IGNORE_BACKEND_K1] or [CodegenTestDirectives.IGNORE_BACKEND_K2] directive if the test
 * has passed all facade and handler stages without failed assertions, and thus needs to be "unmuted". This works well
 * for Kotlin/JVM backend, because the result of compilation is always a JAR file.
 *
 * However, for KLIB-based compilers such as Kotlin/Native, there can be two types of resulting artifacts:
 *   1. KLIB artifacts
 *   2. Binaries (produced out of previously generated KLIB artifacts)
 * and some assertions may fail only at the stage where the compiler produces binaries.
 *
 * If we would like to have two tests based on the same testData file, one that produces only KLIBs and another one
 * that produces binaries, then we might end up in a situation where the test that produces KLIBs fails
 * due to [BlackBoxCodegenSuppressor] and the test that produces binaries is "green".
 *
 * A new [BoxCodegenWithoutBinarySuppressor] is an alternative to [BlackBoxCodegenSuppressor] which makes it
 * possible to not fail a test in the absence of failed assertions.
 *
 * Important:
 *   1. [BoxCodegenWithoutBinarySuppressor] should be only used for tests that produce KLIBs but do not produce binaries.
 *   2. There should be a paired (generated) test class that compiles the same testData files and produces binaries.
 */
class BoxCodegenWithoutBinarySuppressor(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives)

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::SuppressionChecker.bind(null)))

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        val suppressionChecker = testServices.codegenSuppressionChecker
        val modules = testServices.moduleStructure.modules

        val ignoreDirective = suppressionChecker.extractIgnoreDirective(modules.first()) ?: return failedAssertions
        return if (modules.any { suppressionChecker.failuresInModuleAreIgnored(it, ignoreDirective).testMuted })
            emptyList()
        else
            failedAssertions
    }
}

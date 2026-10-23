/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.konan.test.blackbox.support.AssertionsMode
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.FREE_COMPILER_ARGS
import org.jetbrains.kotlin.konan.test.blackbox.support.TestKind
import org.jetbrains.kotlin.konan.test.blackbox.support.group.collectToggledCheckers
import org.jetbrains.kotlin.konan.test.blackbox.support.group.isDisabledNative
import org.jetbrains.kotlin.konan.test.blackbox.support.group.isIgnoredTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.testKind
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.KlibAbiDumpDirectives
import org.jetbrains.kotlin.test.directives.model.Directive
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerTestDirectives
import org.jetbrains.kotlin.test.model.GroupingTestIsolator
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

/**
 * @param customCompilerStage the stage compiled by a custom (previously released) compiler in KLIB compatibility tests, or `null`
 *   otherwise: the tests expecting its failure are isolated (see [CUSTOM_FIRST_STAGE_IGNORE_DIRECTIVES] and
 *   [CUSTOM_SECOND_STAGE_IGNORE_DIRECTIVES]).
 *   In forward-compatibility tests ([CompilationStage.SECOND]), [KlibAbiDumpDirectives.DUMP_KLIB_ABI] isolates a test too:
 *   their first stage checks the KLIB ABI dump, which a test renamed for a batch would not match.
 */
class NativeGroupingTestIsolator(
    testServices: TestServices,
    private val customCompilerStage: CompilationStage? = null,
) : GroupingTestIsolator(testServices, affectsFileGenerators = true) {
    companion object {
        private val assertionTokens = AssertionsMode.entries.associateWith {
            BatchToken.Custom("AssertionMode: ${it.name}")
        }

        // Some test failures can bring down an entire batch, so where a failure is expected, the test runs in isolation.
        // The lists mirror the ones of `JsGroupingTestIsolator`.
        private val CUSTOM_FIRST_STAGE_IGNORE_DIRECTIVES: List<Directive> = listOf(
            CustomKlibCompilerTestDirectives.IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE,
            CustomKlibCompilerTestDirectives.IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_FIRST_STAGE,
        )

        private val CUSTOM_SECOND_STAGE_IGNORE_DIRECTIVES: List<Directive> = listOf(
            CustomKlibCompilerTestDirectives.IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE,
            CustomKlibCompilerTestDirectives.IGNORE_KLIB_FRONTEND_ERRORS_WITH_CUSTOM_SECOND_STAGE,
            CustomKlibCompilerTestDirectives.IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE,
        )
    }

    private val customCompilerExpectedFailureDirectives: List<Directive> = when (customCompilerStage) {
        CompilationStage.FIRST -> CUSTOM_FIRST_STAGE_IGNORE_DIRECTIVES
        CompilationStage.SECOND -> CUSTOM_SECOND_STAGE_IGNORE_DIRECTIVES
        null -> emptyList()
    }

    override val directiveContainers: List<DirectivesContainer>
        get() = listOfNotNull(
            CodegenTestDirectives,
            TestDirectives,
            CustomKlibCompilerTestDirectives,
            KlibAbiDumpDirectives.takeIf { customCompilerStage == CompilationStage.SECOND },
        )

    override fun computeBatchToken(moduleStructure: TestModuleStructure): BatchToken {
        val isolationDirectives = listOfNotNull(
            TestDirectives.NATIVE_STANDALONE,
            TestDirectives.FILECHECK_STAGE,
            TestDirectives.EXPECTED_TIMEOUT_FAILURE,
            TestDirectives.MUTED,
            KlibAbiDumpDirectives.DUMP_KLIB_ABI.takeIf { customCompilerStage == CompilationStage.SECOND },
        ) + customCompilerExpectedFailureDirectives

        // KT-84713: Migrate here full grouping logic from TestRunProvider.withTestExecutable(): respect ignores, difference of compiler args, etc.
        val registeredDirectives = moduleStructure.allDirectives
        val testRunSettings = testServices.testRunSettings
        val shouldBeIsolated = testRunSettings.testKind(moduleStructure.modules.firstOrNull()?.directives) != TestKind.REGULAR
                || isolationDirectives.any { it in registeredDirectives }
                || moduleStructure.testSourceContains(packageKotlinInternalRegex)
                || moduleStructure.modules.any { module -> module.files.any { it.name.endsWith(".def") } }
                || testRunSettings.isIgnoredTarget(registeredDirectives) // considers IGNORE_BACKEND* and IGNORE_NATIVE directives
                || testRunSettings.isDisabledNative(registeredDirectives) // considers DISABLE_NATIVE directive
        if (shouldBeIsolated) return BatchToken.Isolated

        val specificTokens = listOfNotNull(
            computeAssertionsModeToken(moduleStructure),
            computeFreeCompilerArgsToken(moduleStructure),
            computeToggledCheckersToken(registeredDirectives),
        )

        return when (specificTokens.size) {
            0 -> BatchToken.Regular
            1 -> specificTokens.single()
            else -> BatchToken.Isolated
        }
    }

    private fun computeAssertionsModeToken(moduleStructure: TestModuleStructure): BatchToken? {
        val assertionsMode = moduleStructure.allDirectives.singleOrZeroValue(TestDirectives.ASSERTIONS_MODE) ?: return null
        return assertionTokens.getValue(assertionsMode)
    }

    private fun computeFreeCompilerArgsToken(moduleStructure: TestModuleStructure): FreeCompilerArgsToken? {
        val freeArgs = moduleStructure.allDirectives[FREE_COMPILER_ARGS].takeIf { it.isNotEmpty() } ?: return null
        return FreeCompilerArgsToken(freeArgs.toSet())
    }

    private fun computeToggledCheckersToken(registeredDirectives: RegisteredDirectives): ToggledCheckersToken? =
        registeredDirectives.collectToggledCheckers().let { [additional, disabled] -> additional + disabled }
            .ifNotEmpty { ToggledCheckersToken(this) }


    /**
     * Scans the test files only. Additional files are supplied by the test infrastructure and do not differ between tests in a way
     * that should isolate one: for example, the `kotlin.internal.ReflectionPackageName` helper of KLIB backward-compatibility tests
     * is declared in the package `kotlin.internal` for the old stdlibs missing it, while the test itself does not touch that package.
     * Scanning such a file would also make the answer depend on the moment it's asked: the additional source providers ask while
     * the helper is not yet added, and `BatchingPackageInserter` asks after that, and the answers must agree.
     */
    private fun TestModuleStructure.testSourceContains(regex: Regex): Boolean =
        modules.any { module -> module.files.any { !it.isAdditional && it.originalContent.contains(regex) } }

    private val packageKotlinInternalRegex = Regex("package\\s${StandardNames.KOTLIN_INTERNAL_FQ_NAME}")

    private data class FreeCompilerArgsToken(val freeArgs: Set<String>) : BatchToken()
}

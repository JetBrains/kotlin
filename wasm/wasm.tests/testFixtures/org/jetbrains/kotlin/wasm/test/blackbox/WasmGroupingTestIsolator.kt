/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.blackbox

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives.CHECK_STATE_MACHINE
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives.CHECK_TAIL_CALL_OPTIMIZATION
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.DISABLE_WASM_EXCEPTION_HANDLING
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.USE_NEW_EXCEPTION_HANDLING_PROPOSAL
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.USE_OLD_EXCEPTION_HANDLING_PROPOSAL
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerTestDirectives
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.GroupingTestIsolator
import org.jetbrains.kotlin.test.model.GroupingTestIsolator.BatchToken.Custom
import org.jetbrains.kotlin.test.services.IrCheckersDisabledByTestDirectives
import org.jetbrains.kotlin.test.services.IrCheckersEnabledByTestDirectives
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

class WasmGroupingTestIsolator(testServices: TestServices) : GroupingTestIsolator(testServices, affectsFileGenerators = true) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(
            WasmEnvironmentConfigurationDirectives,
            JvmEnvironmentConfigurationDirectives, // for directive WITH_REFLECT
            JsEnvironmentConfigurationDirectives, // for directive CALL_MAIN
            CodegenTestDirectives,
            LanguageSettingsDirectives,
            CustomKlibCompilerTestDirectives,
            AdditionalFilesDirectives, // for directives WITH_COROUTINES, CHECK_STATE_MACHINE, CHECK_TAIL_CALL_OPTIMIZATION
        )

    override fun computeBatchToken(moduleStructure: TestModuleStructure): BatchToken {
        val isolationDirectives = listOf(
            // some test failures can bring down an entire batch, so where those failures are expected, tests need to be run in isolated mode
            CodegenTestDirectives.IGNORE_BACKEND,
            CodegenTestDirectives.IGNORE_BACKEND_K2,
            CustomKlibCompilerTestDirectives.IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE,
            CustomKlibCompilerTestDirectives.IGNORE_KLIB_FRONTEND_ERRORS_WITH_CUSTOM_SECOND_STAGE,
            CustomKlibCompilerTestDirectives.IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE,
            WasmEnvironmentConfigurationDirectives.WASM_IGNORE_FOR,
            // other isolation reasons
            WasmEnvironmentConfigurationDirectives.WASM_STANDALONE,
            WasmEnvironmentConfigurationDirectives.RUN_THIRD_PARTY_OPTIMIZER,
            WasmEnvironmentConfigurationDirectives.RUN_UNIT_TESTS,
            JvmEnvironmentConfigurationDirectives.WITH_REFLECT,
            JsEnvironmentConfigurationDirectives.CALL_MAIN,
        )
        if (isolationDirectives.any { it in moduleStructure.allDirectives })
            return BatchToken.Isolated

        if (moduleStructure.modules.any { module ->
                // Isolate tests having non-Kotlin files
                module.files.any { !it.name.endsWith(".kt") }
                        // Tests with friend dependencies between their own modules cannot be safely grouped with other tests
                        // (e.g. `// MODULE: lib1` + `// MODULE: main()(lib1)`).
                        // The Wasm grouping facade synthesizes a single `-Xinclude` launcher KLIB and passes all per-test KLIBs
                        // as ordinary `-libraries`. The JS/Wasm compiler can express friend-module relationships only for the included main module:
                        // there is no CLI to declare friendship between two `-libraries`. As a result, friend visibility between `main` and `lib1`
                        // of the same test is lost at IR link time, which manifests as e.g. `kotlin.internal.IrLinkageError`
                        // or wrong override resolution for `internal open` declarations crossing module boundaries.
                        // Isolating such tests routes them through the isolated-batch path which preserves per-test friend dependencies.
                        || module.allDependencies.any { it.relation == DependencyRelation.FriendDependency }
            })
            return BatchToken.Isolated

        // Tests with companion .js/.mjs files on disk are highly likely to break in grouped execution
        val hasCompanionJsFile = moduleStructure.originalTestDataFiles.any { file ->
            file.parentFile.resolve(file.nameWithoutExtension + ".js").exists() ||
            file.parentFile.resolve(file.nameWithoutExtension + ".mjs").exists() ||
            file.parentFile.resolve(file.nameWithoutExtension + "__main.js").exists()
        }
        if (hasCompanionJsFile)
            return BatchToken.Isolated

        val specificTokens = listOfNotNull(
            computeEHToken(moduleStructure),
            computeLanguageSettingsToken(moduleStructure),
            computeToggledCheckersToken(moduleStructure.allDirectives),
            computeCoroutineHelpersToken(moduleStructure.allDirectives),
        )
        return when (specificTokens.size) {
            0 -> BatchToken.Regular
            1 -> specificTokens.single()
            // Every token states something which has to be uniform within a batch, so tests agreeing on *all*
            // of their tokens are exactly as groupable as tests agreeing on a single one. Combining the tokens
            // keeps such tests batched together instead of giving each of them a batch of its own.
            else -> Custom(specificTokens.joinToString(separator = " & "))
        }
    }

    private fun computeEHToken(moduleStructure: TestModuleStructure): BatchToken? =
        mapOf(
            DISABLE_WASM_EXCEPTION_HANDLING to Custom("disabled EH"),
            USE_NEW_EXCEPTION_HANDLING_PROPOSAL to Custom("new EH"),
            USE_OLD_EXCEPTION_HANDLING_PROPOSAL to Custom("old EH"),
        ).firstNotNullOfOrNull { [directive, token] ->
            token.takeIf { directive in moduleStructure.allDirectives }
        }

    /**
     * All `// WITH_COROUTINES` tests of a batch share a single `helpers.klib`: their helper files are extracted
     * into one `helpers` module by [org.jetbrains.kotlin.wasm.test.WasmCoroutineHelpersModuleTransformer], and
     * the grouping facade keeps only the first of the resulting per-test KLIBs
     * (`AbstractWasmSecondStageGroupingFacade.deduplicateHelperKlibPaths`), as they all declare
     * `unique_name = "helpers"`.
     *
     * That is only sound while the helper files are the same for every test of the batch, and they are not:
     * [org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider] adds
     * `CoroutineUtil.kt` + `StateMachineChecker.kt` only for [CHECK_STATE_MACHINE], and
     * `TailCallOptimizationChecker.kt` only for [CHECK_TAIL_CALL_OPTIMIZATION].
     * Mixing tests with different helper sets makes the batch link against whichever `helpers.klib` came first,
     * and if that one is the smaller set, every test needing the extra declarations fails with linkage errors
     * from the partial linkage engine. So tests requesting extra helper files are grouped separately.
     */
    private fun computeCoroutineHelpersToken(registeredDirectives: RegisteredDirectives): BatchToken? {
        if (AdditionalFilesDirectives.WITH_COROUTINES !in registeredDirectives) return null
        val extraHelperDirectives = listOf(
            CHECK_STATE_MACHINE,
            CHECK_TAIL_CALL_OPTIMIZATION,
        ).filter { it in registeredDirectives }
        // The default set of helper files is the same for all such tests, so they may be grouped as usual.
        return extraHelperDirectives.ifNotEmpty { Custom("coroutine helpers: ${joinToString { it.name }}") }
    }

    private fun computeLanguageSettingsToken(moduleStructure: TestModuleStructure): BatchToken? {
        // `allDirectives` reports the values of every module, so a directive shared by all modules of a test is
        // repeated once per module. Without deduplication two tests requesting exactly the same settings end up
        // with different tokens as soon as they have a different number of modules.
        val languageFeatures = moduleStructure.allDirectives[LanguageSettingsDirectives.LANGUAGE].distinct().sorted()
        val optIns = moduleStructure.allDirectives[LanguageSettingsDirectives.OPT_IN].distinct().sorted()
        val apiVersion = moduleStructure.allDirectives[LanguageSettingsDirectives.API_VERSION].distinct()
        val languageVersion = moduleStructure.allDirectives[LanguageSettingsDirectives.LANGUAGE_VERSION].distinct()
        val returnValueCheckerMode = moduleStructure.allDirectives[LanguageSettingsDirectives.RETURN_VALUE_CHECKER_MODE].distinct()
        val progressiveMode = LanguageSettingsDirectives.PROGRESSIVE_MODE in moduleStructure.allDirectives

        if (languageFeatures.isEmpty()
            && optIns.isEmpty()
            && apiVersion.isEmpty()
            && languageVersion.isEmpty()
            && returnValueCheckerMode.isEmpty()
            && !progressiveMode
        ) {
            return null
        }

        return BatchToken.Custom("Lang settings: $languageFeatures, $optIns, $apiVersion, $languageVersion, $returnValueCheckerMode, progressive=$progressiveMode")
    }

    private fun computeToggledCheckersToken(registeredDirectives: RegisteredDirectives): ToggledCheckersToken? =
        registeredDirectives.collectToggledCheckers().let { [additional, disabled] -> additional + disabled }
            .ifNotEmpty { ToggledCheckersToken(this) }

    private fun RegisteredDirectives.collectToggledCheckers(): Pair<Set<String>, Set<String>> {
        val additionalCheckers = IrCheckersEnabledByTestDirectives.filter { it.key in this }.values.toSet()
        val disabledIrCheckers = IrCheckersDisabledByTestDirectives.filter { dir ->
            this[dir.key].any { it == TargetBackend.ANY || it.isTransitivelyCompatibleWith(TargetBackend.WASM) }
        }.values.toSet()

        return additionalCheckers to disabledIrCheckers
    }
}


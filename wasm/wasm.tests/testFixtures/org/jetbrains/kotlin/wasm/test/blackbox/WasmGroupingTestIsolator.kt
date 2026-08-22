/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.blackbox

import org.jetbrains.kotlin.builtins.StandardNames
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
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerTestDirectives
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.GroupingTestIsolator
import org.jetbrains.kotlin.test.model.GroupingTestIsolator.BatchToken.Custom
import org.jetbrains.kotlin.test.services.IrCheckersDisabledByTestDirectives
import org.jetbrains.kotlin.test.services.IrCheckersEnabledByTestDirectives
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

/**
 * @param considerCustomSecondStageDirectives whether `IGNORE_KLIB_*_WITH_CUSTOM_SECOND_STAGE` directives are
 *   meaningful for this test runner. They mark failures which are expected only when a custom second-stage
 *   compiler is used, so for the regular Wasm runners such tests are expected to pass and need no isolation.
 */
class WasmGroupingTestIsolator(
    testServices: TestServices,
    private val considerCustomSecondStageDirectives: Boolean = false,
) : GroupingTestIsolator(testServices, affectsFileGenerators = true) {
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
        // A failing test can bring down its entire batch, so tests whose failure is expected run in isolation.
        // `IGNORE_BACKEND`/`IGNORE_BACKEND_K2` only mute the backends they name though: a test muted for, say,
        // the JVM backend is still expected to pass here, and isolating it would only fragment the batches.
        if (moduleStructure.allDirectives.mutesCurrentBackend(CodegenTestDirectives.IGNORE_BACKEND) ||
            moduleStructure.allDirectives.mutesCurrentBackend(CodegenTestDirectives.IGNORE_BACKEND_K2)
        ) return BatchToken.Isolated

        if (considerCustomSecondStageDirectives) {
            val customSecondStageIgnoreDirectives = listOf(
                CustomKlibCompilerTestDirectives.IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE,
                CustomKlibCompilerTestDirectives.IGNORE_KLIB_FRONTEND_ERRORS_WITH_CUSTOM_SECOND_STAGE,
                CustomKlibCompilerTestDirectives.IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE,
            )
            if (customSecondStageIgnoreDirectives.any { it in moduleStructure.allDirectives })
                return BatchToken.Isolated
        }

        val isolationDirectives = listOf(
            // some test failures can bring down an entire batch, so where those failures are expected, tests need to be run in isolated mode
            WasmEnvironmentConfigurationDirectives.WASM_IGNORE_FOR,
            // other isolation reasons
            WasmEnvironmentConfigurationDirectives.WASM_STANDALONE,
            WasmEnvironmentConfigurationDirectives.RUN_THIRD_PARTY_OPTIMIZER,
            WasmEnvironmentConfigurationDirectives.RUN_UNIT_TESTS,
            JsEnvironmentConfigurationDirectives.CALL_MAIN,
        )
        if (isolationDirectives.any { it in moduleStructure.allDirectives })
            return BatchToken.Isolated

        if (moduleStructure.modules.any { module ->
                // Isolate tests having non-Kotlin files
                module.files.any { !it.name.endsWith(".kt") }
            })
            return BatchToken.Isolated

        // The splitting test transformer deliberately creates a `lib` module and a `main` module with a friend
        // dependency between them. This is needed to preserve the visibility of internal declarations after a
        // single-module test is split, but it must not turn every splitting test into a singleton batch. Explicit
        // friend-module test data still takes the isolated path below.
        if (moduleStructure.modules.any { module ->
                module.allDependencies.any { it.relation == DependencyRelation.FriendDependency }
            } && !isSyntheticSplitTest(moduleStructure)
        )
            return BatchToken.Isolated

        // Tests with companion .js/.mjs files on disk are highly likely to break in grouped execution
        val hasCompanionJsFile = moduleStructure.originalTestDataFiles.any { file ->
            file.parentFile.resolve(file.nameWithoutExtension + ".js").exists() ||
            file.parentFile.resolve(file.nameWithoutExtension + ".mjs").exists() ||
            file.parentFile.resolve(file.nameWithoutExtension + "__main.js").exists()
        }
        if (hasCompanionJsFile)
            return BatchToken.Isolated

        // `BatchingPackageInserter` deliberately leaves the `kotlin` and `kotlin.internal` packages unpatched, as
        // the compiler recognizes some of their declarations by exact fully qualified name. Declarations which
        // tests put there therefore keep colliding across the batch: two tests declaring, say,
        // `kotlin.internal.ImplicitIntegerCoercion` make the deserializer fail with `IrClassSymbolImpl is already
        // bound`. `NativeGroupingTestIsolator` isolates such tests for the same reason.
        // Additional source providers may deliberately add shared support files in the `kotlin` package (for
        // example, Wasm's common assertion helpers). They are not declarations from the testdata and therefore
        // must not make every test look like a non-patched-package collision.
        if (moduleStructure.modules.any { module ->
                module.files.any { !it.isAdditional && it.originalContent.contains(nonPatchedPackageRegex) }
            })
            return BatchToken.Isolated

        val specificTokens = listOfNotNull(
            computeWasmCodegenSettingsToken(moduleStructure.allDirectives),
            computeLanguageSettingsToken(moduleStructure),
            computeToggledCheckersToken(moduleStructure.allDirectives),
            computeCoroutineHelpersToken(moduleStructure.allDirectives),
            computeWithReflectToken(moduleStructure.allDirectives),
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

    /**
     * `true` if [directive] mutes the test for the backend which is being tested. A directive listing no backend
     * at all, or a test which does not declare its target backend, is treated as a catch-all mute.
     */
    private fun RegisteredDirectives.mutesCurrentBackend(directive: ValueDirective<TargetBackend>): Boolean {
        val mutedBackends = this[directive]
        if (mutedBackends.isEmpty()) return directive in this
        val targetBackend = testServices.defaultsProvider.targetBackend ?: return true
        return mutedBackends.any { it == TargetBackend.ANY || targetBackend.isTransitivelyCompatibleWith(it) }
    }

    /**
     * `WITH_REFLECT` only means something to the JVM runners (it adds `kotlin-reflect` to the classpath, see
     * `JvmEnvironmentConfigurator`), so it changes nothing about how a Wasm test is compiled or run. It is used
     * here purely as a marker of tests which are likely to inspect names of their own declarations — the thing
     * that the package patching of grouped runs changes. Such tests are grouped with each other rather than
     * isolated one by one, so that a name-dependent test can only ever affect other name-dependent ones.
     */
    private fun computeWithReflectToken(registeredDirectives: RegisteredDirectives): BatchToken? =
        Custom("WITH_REFLECT").takeIf { JvmEnvironmentConfigurationDirectives.WITH_REFLECT in registeredDirectives }

    /**
     * A whole batch is compiled by a single invocation of the backend, so every directive which changes *how* the
     * code is generated has to be uniform within the batch — otherwise the tests requesting a setting would be
     * compiled without it. For example, `wasmIrCheckForTailCalls.kt` relies on [ENABLE_TAIL_CALLS] to emit
     * `return_call`, and without it its mutually recursive functions exhaust the VM stack, which fails the run of
     * the whole batch. Note that the remaining directives of `WasmEnvironmentConfigurationDirectives` are
     * [org.jetbrains.kotlin.test.directives.model.DirectiveApplicability.Global], i.e. equal for every test of a
     * run, so they cannot differ within a batch.
     */
    private fun computeWasmCodegenSettingsToken(registeredDirectives: RegisteredDirectives): BatchToken? {
        val codegenDirectives = listOf(
            DISABLE_WASM_EXCEPTION_HANDLING,
            USE_NEW_EXCEPTION_HANDLING_PROPOSAL,
            USE_OLD_EXCEPTION_HANDLING_PROPOSAL,
            WasmEnvironmentConfigurationDirectives.USE_STACK_SWITCHING_PROPOSAL,
            WasmEnvironmentConfigurationDirectives.ENABLE_TAIL_CALLS,
            WasmEnvironmentConfigurationDirectives.WASM_NO_JS_TAG,
            WasmEnvironmentConfigurationDirectives.WASM_DISABLE_FQNAME_IN_KCLASS,
            WasmEnvironmentConfigurationDirectives.WASM_DISABLE_ARRAY_RANGE_CHECKS,
            WasmEnvironmentConfigurationDirectives.WASM_DISABLE_ARRAY_RANGE_CHECKS_SAFE_ELIMINATION,
        ).filter { it in registeredDirectives }
        val localVariablePrefix =
            registeredDirectives[WasmEnvironmentConfigurationDirectives.WASM_INTERNAL_LOCAL_VARIABLE_PREFIX].distinct()

        if (codegenDirectives.isEmpty() && localVariablePrefix.isEmpty()) return null
        return Custom("Wasm codegen settings: ${codegenDirectives.map { it.name }}, $localVariablePrefix")
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

    private val nonPatchedPackageRegex = Regex(
        "^\\s*package\\s+(${StandardNames.KOTLIN_INTERNAL_FQ_NAME}|${StandardNames.BUILT_INS_PACKAGE_FQ_NAME})\\s*$",
        RegexOption.MULTILINE,
    )

    private val explicitModuleDirectiveRegex = Regex("^\\s*//\\s*MODULE\\s*:", RegexOption.MULTILINE)

    private fun isSyntheticSplitTest(moduleStructure: TestModuleStructure): Boolean {
        val modules = moduleStructure.modules
        if (modules.size != 2) return false
        val [lib, main] = modules
        val friendDependency = main.allDependencies.singleOrNull()
            ?.takeIf { it.relation == DependencyRelation.FriendDependency }
        return friendDependency?.dependencyModule == lib &&
                lib.allDependencies.isEmpty() &&
                lib.name.substringAfterLast('.') == "lib" &&
                main.name.substringAfterLast('.') == "main" &&
                modules.flatMap { it.files }.none { it.originalContent.contains(explicitModuleDirectiveRegex) }
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

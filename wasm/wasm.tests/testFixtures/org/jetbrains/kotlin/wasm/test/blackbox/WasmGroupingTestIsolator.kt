/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.blackbox

import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.DISABLE_WASM_EXCEPTION_HANDLING
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.USE_NEW_EXCEPTION_HANDLING_PROPOSAL
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.USE_OLD_EXCEPTION_HANDLING_PROPOSAL
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerTestDirectives
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.GroupingTestIsolator
import org.jetbrains.kotlin.test.model.GroupingTestIsolator.BatchToken.Custom
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

class WasmGroupingTestIsolator(testServices: TestServices) : GroupingTestIsolator(testServices, affectsFileGenerators = true) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(
            WasmEnvironmentConfigurationDirectives,
            JvmEnvironmentConfigurationDirectives, // for directive WITH_REFLECT
            JsEnvironmentConfigurationDirectives, // for directive CALL_MAIN
            CodegenTestDirectives,
            LanguageSettingsDirectives,
            CustomKlibCompilerTestDirectives,
        )

    override fun computeBatchToken(moduleStructure: TestModuleStructure): BatchToken {
        val isolationDirectives = listOf(
            // some test failures can bring down an entire batch, so where those failures are expected, tests need to be run in isolated mode"
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
        )
        return when (specificTokens.size) {
            0 -> BatchToken.Regular
            1 -> specificTokens.single()
            else -> BatchToken.Isolated
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

    private fun computeLanguageSettingsToken(moduleStructure: TestModuleStructure): BatchToken? {
        val languageFeatures = moduleStructure.allDirectives[LanguageSettingsDirectives.LANGUAGE].sorted()
        val optIns = moduleStructure.allDirectives[LanguageSettingsDirectives.OPT_IN].sorted()
        val apiVersion = moduleStructure.allDirectives[LanguageSettingsDirectives.API_VERSION]
        val languageVersion = moduleStructure.allDirectives[LanguageSettingsDirectives.LANGUAGE_VERSION]
        val returnValueCheckerMode = moduleStructure.allDirectives[LanguageSettingsDirectives.RETURN_VALUE_CHECKER_MODE]
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
}


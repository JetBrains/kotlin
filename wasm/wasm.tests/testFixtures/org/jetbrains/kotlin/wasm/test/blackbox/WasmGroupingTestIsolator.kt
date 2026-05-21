/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.blackbox

import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.DISABLE_WASM_EXCEPTION_HANDLING
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.USE_NEW_EXCEPTION_HANDLING_PROPOSAL
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.USE_OLD_EXCEPTION_HANDLING_PROPOSAL
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.GroupingTestIsolator
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

class WasmGroupingTestIsolator(testServices: TestServices) : GroupingTestIsolator(testServices, affectsFileGenerators = true) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(
            WasmEnvironmentConfigurationDirectives,
            JvmEnvironmentConfigurationDirectives, // for directive WITH_REFLECT
            CodegenTestDirectives,
            LanguageSettingsDirectives,
        )

    companion object {
        private val EHTokens = mapOf(
            DISABLE_WASM_EXCEPTION_HANDLING to BatchToken.Custom("disabled EH"),
            USE_NEW_EXCEPTION_HANDLING_PROPOSAL to BatchToken.Custom("new EH"),
            USE_OLD_EXCEPTION_HANDLING_PROPOSAL to BatchToken.Custom("old EH"),
        )
    }

    override fun computeBatchToken(moduleStructure: TestModuleStructure): BatchToken {
        val isolationDirectives = listOf(
            CodegenTestDirectives.IGNORE_BACKEND,
            CodegenTestDirectives.IGNORE_BACKEND_K2,
            WasmEnvironmentConfigurationDirectives.RUN_THIRD_PARTY_OPTIMIZER,
            WasmEnvironmentConfigurationDirectives.WASM_FAILS_IN_SINGLE_MODULE_MODE,
            WasmEnvironmentConfigurationDirectives.WASM_FAILS_IN_MULTI_MODULE_MODE,
            WasmEnvironmentConfigurationDirectives.WASM_FAILS_IN_MULTI_MODULE_MODE_WINDOWS,
            JvmEnvironmentConfigurationDirectives.WITH_REFLECT,
        )
        if (isolationDirectives.any { it in moduleStructure.allDirectives })
            return BatchToken.Isolated

        val hasNonKotlinFiles = moduleStructure.modules.any { module ->
            module.files.any { !it.name.endsWith(".kt") }
        }
        if (hasNonKotlinFiles)
            return BatchToken.Isolated

        if (ISOLATION_SOURCE_REGEXES.any { moduleStructure.sourceContains(it) })
            return BatchToken.Isolated

        if ("+MultiPlatformProjects" in moduleStructure.allDirectives[LanguageSettingsDirectives.LANGUAGE])
            return BatchToken.Isolated

        // Multi-module tests with friend dependencies between their own modules (e.g.
        // `// MODULE: lib1` + `// MODULE: main()(lib1)`) cannot be safely grouped with other
        // tests. The Wasm grouping facade synthesizes a single `-Xinclude` launcher KLIB and
        // passes all per-test KLIBs as ordinary `-libraries`. The JS/Wasm compiler can express
        // friend-module relationships only for the included main module — there is no CLI to
        // declare friendship between two `-libraries`. As a result, friend visibility between
        // `main` and `lib1` of the same test is lost at IR link time, which manifests as e.g.
        // `kotlin.internal.IrLinkageError` or wrong override resolution for `internal open`
        // declarations crossing module boundaries. Isolating such tests routes them through
        // the isolated-batch path which preserves per-test friend dependencies.
        if (moduleStructure.modules.any { module ->
                module.allDependencies.any { it.relation == DependencyRelation.FriendDependency }
            }
        ) {
            return BatchToken.Isolated
        }

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
        EHTokens.firstNotNullOfOrNull { (directive, token) ->
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


/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.blackbox

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.DISABLE_WASM_EXCEPTION_HANDLING
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.USE_NEW_EXCEPTION_HANDLING_PROPOSAL
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.USE_OLD_EXCEPTION_HANDLING_PROPOSAL
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.GroupingTestIsolator
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

class WasmGroupingTestIsolator(testServices: TestServices) : GroupingTestIsolator(testServices, affectsFileGenerators = true) {
    private val packageKotlinInternalRegex = Regex("package\\s${StandardNames.KOTLIN_INTERNAL_FQ_NAME}")
    private val classQualifiedNameRegex = Regex("::class.qualifiedName")
    private val classToStringRegex = Regex("::class.toString\\(\\)")
    private val wasmFailsInRegex = Regex("// WASM_FAILS_IN: ")

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(WasmEnvironmentConfigurationDirectives, CodegenTestDirectives)

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
        )
        if (isolationDirectives.any { it in moduleStructure.allDirectives })
            return BatchToken.Isolated

        val isolationSourceRegexes = listOf(
            packageKotlinInternalRegex,
            classQualifiedNameRegex,
            classToStringRegex,
            wasmFailsInRegex,
        )
        if (isolationSourceRegexes.any { moduleStructure.sourceContains(it) })
            return BatchToken.Isolated

        val specificTokens = listOfNotNull(
            computeEHToken(moduleStructure),
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
}


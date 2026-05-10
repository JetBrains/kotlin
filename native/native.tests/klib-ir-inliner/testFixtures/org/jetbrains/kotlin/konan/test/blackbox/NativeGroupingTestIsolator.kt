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
import org.jetbrains.kotlin.konan.test.blackbox.support.testKind
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.model.GroupingTestIsolator
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

class NativeGroupingTestIsolator(testServices: TestServices) : GroupingTestIsolator(testServices, affectsFileGenerators = true) {
    companion object {
        private val assertionTokens = AssertionsMode.entries.associateWith {
            BatchToken.Custom("AssertionMode: ${it.name}")
        }
    }

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(TestDirectives)

    override fun computeBatchToken(moduleStructure: TestModuleStructure): BatchToken {
        // KT-84713: Migrate here full grouping logic from TestRunProvider.withTestExecutable(): respect ignores, difference of compiler args, etc.
        val shouldBeIsolated = testServices.testRunSettings.testKind(moduleStructure.modules.firstOrNull()?.directives) != TestKind.REGULAR
                || moduleStructure.allDirectives.contains(TestDirectives.NATIVE_STANDALONE)
                || moduleStructure.allDirectives[TestDirectives.FILECHECK_STAGE].isNotEmpty()
                || moduleStructure.sourceContains(packageKotlinInternalRegex)
                || moduleStructure.modules.any { module -> module.files.any { it.name.endsWith(".def") } }
        if (shouldBeIsolated) return BatchToken.Isolated

        val specificTokens = listOfNotNull(
            computeAssertionsModeToken(moduleStructure),
            computeFreeCompilerArgsToken(moduleStructure),
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

    private val packageKotlinInternalRegex = Regex("package\\s${StandardNames.KOTLIN_INTERNAL_FQ_NAME}")
    private val sourceContainsCache = HashMap<Pair<TestModuleStructure, Regex>, Boolean>()

    private fun TestModuleStructure.sourceContains(regex: Regex): Boolean {
        return sourceContainsCache.getOrPut(this to regex) { modules.any { it.files.any { it.originalContent.contains(regex) } } }
    }

    private data class FreeCompilerArgsToken(val freeArgs: Set<String>) : BatchToken()
}

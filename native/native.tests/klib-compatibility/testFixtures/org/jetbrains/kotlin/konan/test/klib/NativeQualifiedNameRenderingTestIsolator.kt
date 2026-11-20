/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.test.klib.ReflectionPackageNameDependentTestIsolator
import org.jetbrains.kotlin.test.model.GroupingTestIsolator
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

/**
 * Isolates the tests rendering qualified names when the second stage of a KLIB forward-compatibility test is linked by a compiler
 * older than 2.5, which does not know `kotlin.internal.ReflectionPackageName`: a test renamed for a batch cannot match the expected
 * names then. [ReflectionPackageNameDependentTestIsolator] recognizes `KClass.qualifiedName` and `::class.toString()`. Besides
 * them, Kotlin/Native renders the qualified names in `KClass.toString()` and `KType.toString()`, which this isolator recognizes.
 *
 * The patterns are a heuristic: they isolate about 1.5% of the box tests, a few of them without need, and a test missed by them
 * (e.g. comparing `Throwable.toString()`) fails in its batch, and is then retried in isolation.
 */
class NativeQualifiedNameRenderingTestIsolator(
    testServices: TestServices,
) : GroupingTestIsolator(testServices, affectsFileGenerators = true) {
    override fun computeBatchToken(moduleStructure: TestModuleStructure): BatchToken =
        if (QUALIFIED_NAME_RENDERINGS.any { moduleStructure.testSourceContains(it) }) BatchToken.Isolated
        else BatchToken.Regular

    /** Scans the test files only: the additional files come from the test infrastructure and are the same for every test. */
    private fun TestModuleStructure.testSourceContains(regex: Regex): Boolean =
        modules.any { module -> module.files.any { !it.isAdditional && it.originalContent.contains(regex) } }

    companion object {
        private val QUALIFIED_NAME_RENDERINGS = listOf(
            Regex(""""class """), // an expected result of `KClass.toString()`
            Regex("""\$\{[^}]*::class\b"""), // `KClass.toString()` in a string template
            Regex("""\btypeOf\b"""),
            Regex("""\bKType\b"""),
            Regex("""\.returnType\b"""),
            Regex("""\.type\b"""),
        )
    }
}

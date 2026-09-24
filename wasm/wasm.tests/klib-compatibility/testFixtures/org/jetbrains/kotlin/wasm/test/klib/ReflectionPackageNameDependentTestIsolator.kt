/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.klib

import org.jetbrains.kotlin.test.model.GroupingTestIsolator
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

class ReflectionPackageNameDependentTestIsolator(
    testServices: TestServices,
) : GroupingTestIsolator(testServices, affectsFileGenerators = true) {
    override fun computeBatchToken(moduleStructure: TestModuleStructure): BatchToken =
        if (REFLECTIVE_PACKAGE_NAME_ACCESSES.any { moduleStructure.sourceContains(it) }) BatchToken.Isolated
        else BatchToken.Regular

    companion object {
        private val REFLECTIVE_PACKAGE_NAME_ACCESSES = listOf(
            Regex("""\.qualifiedName\b"""),
            Regex("""::class\.toString\(\)"""),
        )
    }
}

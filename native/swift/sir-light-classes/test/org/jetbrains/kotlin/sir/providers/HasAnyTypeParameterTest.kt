/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers

import org.jetbrains.kotlin.analysis.api.export.utilities.hasTypeParameter
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysisExtension
import org.jetbrains.kotlin.export.test.getClassOrFail
import org.jetbrains.kotlin.sir.providers.support.withAnalysisSession
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(InlineSourceCodeAnalysisExtension::class)
class HasAnyTypeParameterTest(val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis) {
    @Test
    fun `test - has any type parameter`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                open class ParentGeneric<T>
                class ChildGeneric: ParentGeneric<String>

                open class ParentNoGeneric
                class ChildNoGeneric: ParentNoGeneric

            """.trimIndent()
        )

        withAnalysisSession(file) { ktFile ->
            val parentGeneric = ktFile.getClassOrFail("ParentGeneric", this)
            val childGeneric = ktFile.getClassOrFail("ChildGeneric", this)

            val parentNoGeneric = ktFile.getClassOrFail("ParentNoGeneric", this)
            val childNoGeneric = ktFile.getClassOrFail("ChildNoGeneric", this)

            assertTrue(parentGeneric.hasTypeParameter(this))
            assertTrue(childGeneric.hasTypeParameter(this))

            assertFalse(parentNoGeneric.hasTypeParameter(this))
            assertFalse(childNoGeneric.hasTypeParameter(this))
        }
    }
}
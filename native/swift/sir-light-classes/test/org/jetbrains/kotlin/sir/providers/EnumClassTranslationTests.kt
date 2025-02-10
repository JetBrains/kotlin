/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers

import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.SirVariable
import org.jetbrains.kotlin.sir.providers.source.kotlinOriginOrNull
import org.jetbrains.kotlin.sir.providers.support.SirTranslationTest
import org.jetbrains.kotlin.sir.providers.support.classNamed
import org.jetbrains.kotlin.sir.providers.support.translate
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class EnumClassTranslationTests : SirTranslationTest() {
    @Test
    fun `simple enum class`(inlineSourceCodeAnalysis: InlineSourceCodeAnalysis) {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                enum class Foo { A, B, C }
            """.trimIndent()
        )
        translate(file) {
            val enumClass = it.classNamed("Foo")
            assertContains(enumClass.protocols, SirSwiftModule.caseIterable)
            val cases = enumClass.declarations
                .filterIsInstance<SirVariable>()
                .filter { it.kotlinOriginOrNull<KaEnumEntrySymbol>() != null }
            val caseA = cases.find { it.name == "A" }
            val caseB = cases.find { it.name == "B" }
            val caseC = cases.find { it.name == "C" }
            assertNotNull(caseA)
            assertNotNull(caseB)
            assertNotNull(caseC)
            val sirEnumType = SirNominalType(enumClass)
            assertEquals(sirEnumType, caseA.type)
            assertEquals(sirEnumType, caseB.type)
            assertEquals(sirEnumType, caseB.type)
        }
    }
}
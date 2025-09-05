/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers

import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.sir.SirEnumCase
import org.jetbrains.kotlin.sir.providers.source.kaSymbolOrNull
import org.jetbrains.kotlin.sir.providers.support.SirTranslationTest
import org.jetbrains.kotlin.sir.providers.support.enumNamed
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
            val enumeration = it.enumNamed("Foo")
            assertContains(enumeration.protocols, SirSwiftModule.caseIterable)
            val cases = enumeration.declarations
                .filterIsInstance<SirEnumCase>()
                .filter { it.kaSymbolOrNull<KaEnumEntrySymbol>() != null }
            val caseA = cases.find { it.name == "A" }
            val caseB = cases.find { it.name == "B" }
            val caseC = cases.find { it.name == "C" }
            assertNotNull(caseA)
            assertNotNull(caseB)
            assertNotNull(caseC)
            assertEquals(enumeration, caseA.parent)
            assertEquals(enumeration, caseB.parent)
            assertEquals(enumeration, caseB.parent)
        }
    }
}
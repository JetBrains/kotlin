/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers

import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.sir.SirArrayType
import org.jetbrains.kotlin.sir.SirDictionaryType
import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.providers.support.SirTranslationTest
import org.jetbrains.kotlin.sir.providers.support.translate
import org.jetbrains.kotlin.sir.providers.support.variableNamed
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CollectionTranslationTests : SirTranslationTest() {
    @Test
    fun `list property is translated to Swift array`(inlineSourceCodeAnalysis: InlineSourceCodeAnalysis) {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                val list: List<String> = listOf("a", "b", "c")
            """.trimIndent()
        )
        translate(file) {
            val list = it.variableNamed("list")
            assertEquals(SirArrayType(SirNominalType(SirSwiftModule.string)), list.type)
        }
    }

    @Test
    fun `map property is translated to Swift dictionary`(inlineSourceCodeAnalysis: InlineSourceCodeAnalysis) {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                val map: Map<String, List<String>> = listOf("a", "b", "c")
            """.trimIndent()
        )
        translate(file) {
            val map = it.variableNamed("map")
            val keyType = SirNominalType(SirSwiftModule.string)
            val valueType = SirArrayType(SirNominalType(SirSwiftModule.string))
            assertEquals(SirDictionaryType(keyType, valueType), map.type)
        }
    }
}
/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers

import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.providers.support.SirTranslationTest
import org.jetbrains.kotlin.sir.providers.support.translate
import org.jetbrains.kotlin.sir.providers.support.functionsNamed
import org.jetbrains.kotlin.sir.providers.support.variableNamed
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class StringTranslationTest : SirTranslationTest() {
    @Test
    fun `constant string function is translated correctly`(inlineSourceCodeAnalysis: InlineSourceCodeAnalysis) {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                fun getConstantString(): String = "Hello, World!"
            """.trimIndent()
        )
        translate(file) {
            val function = it.functionsNamed("getConstantString").first()
            assertEquals(SirNominalType(SirSwiftModule.string), function.returnType)
        }
    }

    @Test
    fun `string variable is translated correctly`(inlineSourceCodeAnalysis: InlineSourceCodeAnalysis) {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                var string: String = "Hello, World!"
            """.trimIndent()
        )
        translate(file) {
            val variable = it.variableNamed("string")
            assertEquals(SirNominalType(SirSwiftModule.string), variable.type)
        }
    }

    @Test
    fun `string getter function is translated correctly`(inlineSourceCodeAnalysis: InlineSourceCodeAnalysis) {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                var string: String = "Hello, World!"
                fun getString(): String = string
            """.trimIndent()
        )
        translate(file) {
            val function = it.functionsNamed("getString").first()
            assertEquals(SirNominalType(SirSwiftModule.string), function.returnType)
        }
    }

    @Test
    fun `string setter function is translated correctly`(inlineSourceCodeAnalysis: InlineSourceCodeAnalysis) {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                var string: String = "Hello, World!"
                fun setString(value: String) { string = value }
            """.trimIndent()
        )
        translate(file) {
            val function = it.functionsNamed("setString").first()
            val parameter = function.parameters.first()
            assertEquals(SirNominalType(SirSwiftModule.string), parameter.type)
        }
    }
}
/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers

import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.sir.SirExistentialType
import org.jetbrains.kotlin.sir.providers.support.SirTranslationTest
import org.jetbrains.kotlin.sir.providers.support.functionsNamed
import org.jetbrains.kotlin.sir.providers.support.translate
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeSupportModule
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AnyTypeTranslationTest : SirTranslationTest() {
    @Test
    fun `Any type in return and parameter positions`(inlineSourceCodeAnalysis: InlineSourceCodeAnalysis) {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                object MyObject
                
                fun getMainObject(): Any = MyObject
                fun isMainObject(obj: Any): Boolean = obj == MyObject
            """.trimIndent()
        )
        translate(file) {
            val getMainObject = it.functionsNamed("getMainObject").first()
            val isMainObject = it.functionsNamed("isMainObject").first()

            // Check that the return type of getMainObject() is mapped to KotlinBase
            assertEquals(SirExistentialType(KotlinRuntimeSupportModule.kotlinBridgeable), getMainObject.returnType)

            // Check that the parameter type of isMainObject(obj: Any) is mapped to KotlinBase
            val objParam = isMainObject.parameters.first()
            assertEquals(SirExistentialType(KotlinRuntimeSupportModule.kotlinBridgeable), objParam.type)
        }
    }
}
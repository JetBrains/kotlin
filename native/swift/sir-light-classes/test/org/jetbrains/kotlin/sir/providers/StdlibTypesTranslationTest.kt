/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers

import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.sir.SirUnsupportedType
import org.jetbrains.kotlin.sir.providers.support.SirTranslationTest
import org.jetbrains.kotlin.sir.providers.support.functionsNamed
import org.jetbrains.kotlin.sir.providers.support.translate
import org.junit.jupiter.api.Test
import kotlin.test.assertNotEquals

class StdlibTypesTranslationTest : SirTranslationTest() {

    @Test
    fun `ByteArray-returning function`(inlineSourceCodeAnalysis: InlineSourceCodeAnalysis) {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                fun foo(): ByteArray = byteArrayOf()
            """.trimIndent()
        )
        translate(file) {
            val foo = it.functionsNamed("foo").first()
            assertNotEquals(SirUnsupportedType, foo.returnType)

        }
    }
}
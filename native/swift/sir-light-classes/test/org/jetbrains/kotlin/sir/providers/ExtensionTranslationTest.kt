/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers

import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.sir.SirExistentialType
import org.jetbrains.kotlin.sir.providers.support.SirTranslationTest
import org.jetbrains.kotlin.sir.providers.support.functionsNamed
import org.jetbrains.kotlin.sir.providers.support.protocolNamed
import org.jetbrains.kotlin.sir.providers.support.translate
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ExtensionTranslationTest : SirTranslationTest() {
    @Test
    fun `extension to interface`(inlineSourceCodeAnalysis: InlineSourceCodeAnalysis) {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                interface IFace {}

                fun IFace.foo() {}
            """.trimIndent()
        )
        translate(file) {
            val iface = it.protocolNamed("IFace")
            val fooFunction = it.functionsNamed("foo").single()
            val receiverParameter = fooFunction.extensionReceiverParameter
            assertNotNull(receiverParameter)
            val sirProtocol = (receiverParameter.type as SirExistentialType).protocols.single()
            assertEquals(iface, sirProtocol)
        }
    }
}
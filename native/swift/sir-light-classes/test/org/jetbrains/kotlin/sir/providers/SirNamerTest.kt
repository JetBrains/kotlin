/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers

import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysisExtension
import org.jetbrains.kotlin.export.test.getClassOrFail
import org.jetbrains.kotlin.export.test.getFunctionOrFail
import org.jetbrains.kotlin.sir.providers.impl.SirDeclarationNamerImpl
import org.jetbrains.kotlin.sir.providers.support.withAnalysisSession
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals

@ExtendWith(InlineSourceCodeAnalysisExtension::class)
class SirNamerTest(val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis) {
    @Test
    fun `test - JvmName annotation renaming`() {

        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                import kotlin.jvm.JvmName                

                public class Foo {
                    @JvmName("barRenamed")
                    fun bar(param: Collection<Int>) = Unit
                }
            """.trimIndent()
        )

        withAnalysisSession(file) { ktFile ->
            val foo = ktFile.getClassOrFail("Foo", this)
            val bar = foo.getFunctionOrFail("bar", this)
            val namer = SirDeclarationNamerImpl()

            with(namer) {
                assertEquals("barRenamed", bar.sirDeclarationName())
            }
        }
    }
}
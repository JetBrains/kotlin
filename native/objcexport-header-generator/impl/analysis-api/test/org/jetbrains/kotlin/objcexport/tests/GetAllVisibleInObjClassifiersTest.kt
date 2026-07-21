/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.scopes.memberScope
import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.analysis.api.session.useSiteSession
import org.jetbrains.kotlin.analysis.api.symbols.symbol
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.export.test.getClassOrFail
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getAllVisibleInObjClassifiers
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GetAllVisibleInObjClassifiersTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {

    @Test
    fun `test - no classifiers in file`() {
        val file = inlineSourceCodeAnalysis.createKtFile("val foo = 42")
        analyze(file) {
            val session = useSiteSession
            assertEquals(emptyList(), session.getAllVisibleInObjClassifiers(file.symbol))
        }
    }

    @Test
    fun `test - single class in file`() {
        val file = inlineSourceCodeAnalysis.createKtFile("class Foo")
        analyze(file) {
            val session = useSiteSession
            assertEquals(listOf(session.getClassOrFail(file, "Foo")), session.getAllVisibleInObjClassifiers(file.symbol))
        }
    }

    @Test
    fun `test - multiple nested classes in file`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                class A {
                    class B {
                        class C
                    }
                }
                    
                class D {
                    class E
                }
            """.trimIndent()
        )

        analyze(file) {
            val session = useSiteSession
            assertEquals(
                listOf(
                    session.getClassOrFail(file, "A"),
                    session.getClassOrFail(file, "D"),
                    session.getClassOrFail(file, "A").memberScope.getClassOrFail("B"),
                    session.getClassOrFail(file, "D").memberScope.getClassOrFail("E"),
                    session.getClassOrFail(file, "A").memberScope.getClassOrFail("B").memberScope.getClassOrFail("C")
                ),
                session.getAllVisibleInObjClassifiers(file.symbol)
            )
        }
    }
}

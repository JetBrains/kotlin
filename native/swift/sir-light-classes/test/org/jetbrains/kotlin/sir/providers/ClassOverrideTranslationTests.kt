/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers

import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.sir.SirFunction
import org.jetbrains.kotlin.sir.providers.support.classNamed
import org.jetbrains.kotlin.sir.providers.support.translate
import org.jetbrains.sir.lightclasses.utils.OverrideStatus
import org.jetbrains.sir.lightclasses.utils.computeIsOverride
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class ClassOverrideTranslationTests(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {
    @Test
    fun `super class members are available in derived`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                open class Base {
                    open fun foo() {}
                }

                class Derived : Base() {
                    override fun foo() {}
                }
            """.trimIndent()
        )
        translate(file) { declarations ->
            val baseClass = declarations.classNamed("Base")
            val derivedClass = declarations.classNamed("Derived")

            val fooInDerived = derivedClass.declarations
                .filterIsInstance<SirFunction>()
                .first { it.name == "foo" }

            val fooInBase = baseClass.declarations
                .filterIsInstance<SirFunction>()
                .first { it.name == "foo" }

            val overrideStatus = fooInDerived.computeIsOverride()!!
            assertTrue(overrideStatus is OverrideStatus.Overrides && overrideStatus.declaration == fooInBase)
        }
    }
}
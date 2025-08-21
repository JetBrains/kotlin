/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers

import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysisExtension
import org.jetbrains.kotlin.sir.SirClass
import org.jetbrains.kotlin.sir.SirInit
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.providers.support.SirTranslationTest
import org.jetbrains.kotlin.sir.providers.support.classNamed
import org.jetbrains.kotlin.sir.providers.support.superClassDeclaration
import org.jetbrains.kotlin.sir.providers.support.translate
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeModule
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ClassInheritanceTranslationTests : SirTranslationTest() {
    @Test
    fun `translation of the simplest class`(inlineSourceCodeAnalysis: InlineSourceCodeAnalysis) {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                class Foo
            """.trimIndent()
        )
        translate(file) {
            val sirClass = it.single() as SirClass
            assertEquals("Foo", sirClass.name)
            assertEquals(KotlinRuntimeModule.kotlinBase, sirClass.superClassDeclaration)
        }
    }

    @Test
    fun `open class inheritance`(inlineSourceCodeAnalysis: InlineSourceCodeAnalysis) {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                open class Base

                class Derived : Base()
            """.trimIndent()
        )
        translate(file) { declarations ->
            val baseClass = declarations.first() as SirClass
            val derivedClass = declarations[1] as SirClass
            assertEquals("Base", baseClass.name)
            assertEquals("Derived", derivedClass.name)
            assertEquals(baseClass, derivedClass.superClassDeclaration)
        }
    }

    @Test
    fun `abstract class inheritance`(inlineSourceCodeAnalysis: InlineSourceCodeAnalysis) {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                abstract class Abstract

                class Derived : Abstract()
            """.trimIndent()
        )
        translate(file) { declarations ->
            val abstractClass = declarations.classNamed("Abstract")
            val derivedClass = declarations.classNamed("Derived")
            assertEquals(abstractClass, derivedClass.superClassDeclaration)
        }
    }

    @Test
    fun `abstract class constructors are not public`(inlineSourceCodeAnalysis: InlineSourceCodeAnalysis) {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                abstract class Abstract
            """.trimIndent()
        )
        translate(file) { declarations ->
            val abstractClass = declarations.first() as SirClass
            abstractClass.declarations
                .filterIsInstance<SirInit>()
                .forEach {
                    assertNotEquals(SirVisibility.PUBLIC, it.visibility)
                }
        }
    }
}
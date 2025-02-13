/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers

import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirUnsupportedType
import org.jetbrains.kotlin.sir.providers.support.SirTranslationTest
import org.jetbrains.kotlin.sir.providers.support.TestSirSession
import org.jetbrains.kotlin.sir.providers.support.classNamed
import org.jetbrains.kotlin.sir.providers.support.functionsNamed
import org.jetbrains.kotlin.sir.providers.support.translate
import org.jetbrains.kotlin.sir.providers.support.variableNamed
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertNotEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SirKaClassReferenceHandlerTests : SirTranslationTest() {

    @Test
    fun `simple function`(inlineSourceCodeAnalysis: InlineSourceCodeAnalysis) {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                fun foo(sb: StringBuilder): ByteArray = byteArrayOf()
            """.trimIndent()
        )
        doTest(file) { declarations, referencedTypes ->
            val foo = declarations.functionsNamed("foo").first()
            // Trigger type access.
            assertNotEquals(SirUnsupportedType, foo.returnType)
            assertContains(referencedTypes, fqName("kotlin", "ByteArray"))
            assertNotEquals(SirUnsupportedType, foo.parameters.first().type)
            assertContains(referencedTypes, fqName("kotlin.text", "StringBuilder"))
        }
    }

    @Test
    fun `class properties references`(inlineSourceCodeAnalysis: InlineSourceCodeAnalysis) {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                class Foo(val sb: StringBuilder) {
                    val field: IntArray? = null
                }
            """.trimIndent()
        )
        doTest(file) { declarations, referencedTypes ->
            val foo = declarations.classNamed("Foo")
            val sb = foo.declarations.variableNamed("sb")
            val field = foo.declarations.variableNamed("field")
            assertNotEquals(SirUnsupportedType, sb.type)
            assertContains(referencedTypes, fqName("kotlin.text", "StringBuilder"))
            assertNotEquals(SirUnsupportedType, field.type)
            assertContains(referencedTypes, fqName("kotlin", "IntArray"))
        }
    }

    @Test
    fun `supertype references`(inlineSourceCodeAnalysis: InlineSourceCodeAnalysis) {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                class A : CharSequence {}

                class MyNumber : Number {}
            """.trimIndent()
        )
        doTest(file) { declarations, referencedTypes ->
            val A = declarations.classNamed("A")
            val MyNumber = declarations.classNamed("MyNumber")
            assertEquals(1, A.protocols.size)
            assertContains(referencedTypes, fqName("kotlin", "CharSequence"))
            assertNotEquals(SirUnsupportedType, MyNumber.superClass)
            assertContains(referencedTypes, fqName("kotlin", "Number"))
        }
    }

    @Test
    fun `custom translated types do not trigger type references`(inlineSourceCodeAnalysis: InlineSourceCodeAnalysis) {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                val a: List<String> = emptyList()
            """.trimIndent()
        )
        doTest(file) { declarations, referencedTypes ->
            val a = declarations.variableNamed("a")
            assertNotEquals(SirUnsupportedType, a.type)
            assertTrue(referencedTypes.isEmpty())
        }
    }

    private inline fun doTest(file: KtFile, testBody: (List<SirDeclaration>, Set<FqName>) -> Unit) {
        val referencedDeclarations = mutableSetOf<FqName>()
        val sirSessionBuilder = { kaModule: KaModule ->
            TestSirSession(kaModule) {
                it.classId?.asSingleFqName()?.let(referencedDeclarations::add)
            }
        }
        translate(file, sirSessionBuilder) { declarations ->
            testBody(declarations, referencedDeclarations)
        }
    }

    private fun fqName(vararg segments: String): FqName = FqName.fromSegments(segments.toList())
}
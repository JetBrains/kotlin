/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers

import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.sir.SirExistentialType
import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.SirUnsupportedType
import org.jetbrains.kotlin.sir.optional
import org.jetbrains.kotlin.sir.providers.support.SirTranslationTest
import org.jetbrains.kotlin.sir.providers.support.classNamed
import org.jetbrains.kotlin.sir.providers.support.functionsNamed
import org.jetbrains.kotlin.sir.providers.support.translate
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeSupportModule
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GenericsErasure : SirTranslationTest() {
    @Test
    fun `simple function generic erasure`(codeAnalysis: InlineSourceCodeAnalysis) {
        val code = codeAnalysis.createKtFile(
            """
            fun <T> foo(t: T) {}
            
            fun <T: Any> id(a: T): T {
                return a
            }     

            open class MyClass {
                fun <T> method(): T = error("")
            }
            """.trimIndent()
        )
        translate(code) {
            val foo = it.functionsNamed("foo").first()
            val tParam = foo.parameters.first()
            val optionalAny = SirExistentialType(KotlinRuntimeSupportModule.kotlinBridgeable).optional()
            assertEquals(optionalAny, tParam.type)

            val id = it.functionsNamed("id").first()
            val aParam = id.parameters.first()
            assertEquals(SirExistentialType(KotlinRuntimeSupportModule.kotlinBridgeable), aParam.type)

            val myClassMethod = it.classNamed("MyClass").declarations.functionsNamed("method").first()
            assertEquals(optionalAny, myClassMethod.returnType)
        }
    }

    @Test
    fun `with a single upper bound`(codeAnalysis: InlineSourceCodeAnalysis) {
        val code = codeAnalysis.createKtFile(
            """
            open class MyClass {
                fun <T> method(): T = error("")
            }

            fun <T> foo(t: T) where T : Any {}

            fun <T: MyClass> bar(t: T?): T = error("")
            """.trimIndent()
        )
        translate(code) {
            val foo = it.functionsNamed("foo").first()
            val tParam = foo.parameters.first()
            assertEquals(SirExistentialType(KotlinRuntimeSupportModule.kotlinBridgeable), tParam.type)

            val bar = it.functionsNamed("bar").first()
            val myClass = it.classNamed("MyClass")
            assertEquals(SirNominalType(myClass), bar.returnType)
            assertEquals(SirNominalType(myClass).optional(), bar.parameters.first().type)
        }
    }

    @Test
    fun `generics with 2 and more upper bounds are not supported`(codeAnalysis: InlineSourceCodeAnalysis) {
        val code = codeAnalysis.createKtFile(
            """
            fun <T> foo(t: T) where T : Any, T : Comparable<T> {}
            """.trimIndent()
        )
        translate(code) {
            val foo = it.functionsNamed("foo").first()
            val tParam = foo.parameters.first()
            assertEquals(SirUnsupportedType, tParam.type)
        }
    }
}
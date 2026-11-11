/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers

import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.SirProtocol
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.SirTypeConstraint
import org.jetbrains.kotlin.sir.SirTypeParameterType
import org.jetbrains.kotlin.sir.optional
import org.jetbrains.kotlin.sir.providers.support.SirTranslationTest
import org.jetbrains.kotlin.sir.providers.support.TestSirSession
import org.jetbrains.kotlin.sir.providers.support.classNamed
import org.jetbrains.kotlin.sir.providers.support.functionsNamed
import org.jetbrains.kotlin.sir.providers.support.translate
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeSupportModule
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Translation of type parameters when [SirSession.enableGenericsSupport] is on.
 * [GenericsErasure] covers the default mode.
 */
class GenericsSupport : SirTranslationTest() {
    private fun translateWithGenerics(file: KtFile, action: (List<SirDeclaration>) -> Unit) {
        translate(file, sirSessionBuilder = { TestSirSession(it, enableGenericsSupport = true) }, action)
    }

    @Test
    fun `simple function generic parameters`(codeAnalysis: InlineSourceCodeAnalysis) {
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
        translateWithGenerics(code) {
            val bridgeableBound: List<SirType> = listOf(SirNominalType(KotlinRuntimeSupportModule.kotlinBridgeable))

            val foo = it.functionsNamed("foo").first()
            val fooTp = foo.typeParameters.single()
            assertEquals("T", fooTp.name)
            assertEquals(bridgeableBound, fooTp.bounds)
            assertEquals(SirTypeParameterType(fooTp), foo.parameters.first().type)

            val id = it.functionsNamed("id").first()
            val idTp = id.typeParameters.single()
            assertEquals(bridgeableBound, idTp.bounds)
            assertEquals(SirTypeParameterType(idTp), id.parameters.first().type)
            assertEquals(SirTypeParameterType(idTp), id.returnType)

            val myClassMethod = it.classNamed("MyClass").declarations.functionsNamed("method").first()
            val methodTp = myClassMethod.typeParameters.single()
            assertEquals(bridgeableBound, methodTp.bounds)
            assertEquals(SirTypeParameterType(methodTp), myClassMethod.returnType)
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
        translateWithGenerics(code) {
            val foo = it.functionsNamed("foo").first()
            val fooTp = foo.typeParameters.single()
            assertEquals(SirTypeParameterType(fooTp), foo.parameters.first().type)

            val bar = it.functionsNamed("bar").first()
            val barTp = bar.typeParameters.single()
            val myClass = it.classNamed("MyClass")
            assertEquals<List<SirType>>(listOf(SirNominalType(myClass)), barTp.bounds)
            assertEquals(SirTypeParameterType(barTp), bar.returnType)
            assertEquals(SirTypeParameterType(barTp).optional(), bar.parameters.first().type)
        }
    }

    @Test
    fun `generics with multiple upper bounds and PAT constraints`(codeAnalysis: InlineSourceCodeAnalysis) {
        val code = codeAnalysis.createKtFile(
            """
            open class MyClass
            interface MyComparable<T> {
                fun compare(other: T): Int
            }
            fun <T> foo(t: T) where T : MyClass, T : MyComparable<T> {}
            """.trimIndent()
        )
        translateWithGenerics(code) {
            val myClass = it.classNamed("MyClass")
            val myComp = it.filterIsInstance<SirProtocol>().first { p -> p.name == "MyComparable" }
            val foo = it.functionsNamed("foo").first()
            val fooTp = foo.typeParameters.single()
            assertEquals<List<SirType>>(listOf(SirNominalType(myClass), SirNominalType(myComp)), fooTp.bounds)
            assertEquals(
                listOf(SirTypeConstraint.Equality(SirTypeParameterType(fooTp), listOf("T", "T"))),
                foo.constraints,
            )
            assertEquals(SirTypeParameterType(fooTp), foo.parameters.first().type)
        }
    }
}

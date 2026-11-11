/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir.providers

import org.jetbrains.kotlin.kir.KirClass
import org.jetbrains.kotlin.kir.KirConstructor
import org.jetbrains.kotlin.kir.KirFunction
import org.jetbrains.kotlin.kir.KirModality
import org.jetbrains.kotlin.kir.providers.source.SirSource
import org.jetbrains.kotlin.kir.providers.support.classesNamed
import org.jetbrains.kotlin.kir.providers.support.translate
import org.jetbrains.kotlin.sir.SirClass
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirModality
import org.jetbrains.kotlin.sir.SirUnsupportedType
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.builder.buildClass
import org.jetbrains.kotlin.sir.builder.buildFunction
import org.jetbrains.kotlin.sir.builder.buildInit
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.sir.util.addChild
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ClassTranslationTest {
    private fun sirClass(className: String, vararg members: SirDeclaration, classVisibility: SirVisibility = SirVisibility.PUBLIC): SirClass =
        buildClass {
            name = className
            modality = SirModality.FINAL
            visibility = classVisibility
            declarations += members
        }.also { sirClass -> members.forEach { it.parent = sirClass } }

    private fun sirMethod(methodName: String, methodVisibility: SirVisibility = SirVisibility.PUBLIC) = buildFunction {
        name = methodName
        returnType = SirUnsupportedType
        visibility = methodVisibility
    }

    private fun sirInit(initVisibility: SirVisibility = SirVisibility.PUBLIC) = buildInit {
        isFailable = false
        visibility = initVisibility
    }

    @Test
    fun `class is translated with its members in order`() {
        val sirModule = buildModule { name = "Lib" }.apply {
            addChild { sirClass("Foo", sirInit(), sirMethod("bar"), sirMethod("baz")) }
        }

        sirModule.translate { kirModule ->
            val kirClass = kirModule.classesNamed("Foo").single()
            val members = kirClass.declarations
            assertIs<KirConstructor>(members[0])
            assertEquals(listOf("bar", "baz"), members.drop(1).map { (it as KirFunction).name })
        }
    }

    @Test
    fun `class has the module as a parent, members have the class as a parent`() {
        val sirClass = sirClass("Foo", sirInit(), sirMethod("bar"))
        val sirModule = buildModule { name = "Lib" }.apply { addChild { sirClass } }

        sirModule.translate { kirModule ->
            val kirClass = kirModule.classesNamed("Foo").single()
            assertSame(kirModule, kirClass.parent)
            assertSame(sirClass, (kirClass.origin as SirSource).declaration)
            kirClass.declarations.forEach { assertSame(kirClass, it.parent) }
        }
    }

    @Test
    fun `swift modality is translated to kotlin one`() {
        val sirModule = buildModule { name = "Lib" }.apply {
            addChild { sirClass("Foo") }
        }

        sirModule.translate { kirModule ->
            assertEquals(KirModality.FINAL, kirModule.classesNamed("Foo").single().modality)
        }
    }

    @Test
    fun `members not visible outside of the module are not translated`() {
        val sirModule = buildModule { name = "Lib" }.apply {
            addChild {
                sirClass(
                    "Foo",
                    sirInit(SirVisibility.INTERNAL),
                    sirMethod("visible"),
                    sirMethod("hidden", SirVisibility.INTERNAL),
                    sirMethod("secret", SirVisibility.PRIVATE),
                )
            }
        }

        sirModule.translate { kirModule ->
            val members = kirModule.classesNamed("Foo").single().declarations
            assertEquals(listOf("visible"), members.map { (it as KirFunction).name })
        }
    }

    @Test
    fun `classes not visible outside of the module are not translated`() {
        val sirModule = buildModule { name = "Lib" }.apply {
            addChild { sirClass("Visible") }
            addChild { sirClass("Hidden", classVisibility = SirVisibility.INTERNAL) }
        }

        sirModule.translate { kirModule ->
            assertEquals(listOf("Visible"), kirModule.declarations.map { (it as KirClass).name })
        }
    }

    @Test
    fun `translated members are instance members of a class`() {
        val sirModule = buildModule { name = "Lib" }.apply {
            addChild { sirClass("Foo", sirMethod("bar")) }
        }

        sirModule.translate { kirModule ->
            val method = kirModule.classesNamed("Foo").single().declarations.single() as KirFunction
            assertTrue(method.isInstance)
        }
    }

    @Test
    fun `translating the same class twice returns the same node`() {
        val sirClass = sirClass("Foo")
        val sirModule = buildModule { name = "Lib" }.apply { addChild { sirClass } }

        sirModule.translate {
            val first = with(sirClass) { toKir() }
            val second = with(sirClass) { toKir() }
            assertSame(first, second)
        }
    }
}

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.tree.deserializer

import org.jetbrains.kotlin.commonizer.cir.CirClassType
import org.jetbrains.kotlin.commonizer.cir.CirName
import org.jetbrains.kotlin.commonizer.cir.CirTypeParameterType
import org.jetbrains.kotlin.commonizer.mergedtree.PropertyApproximationKey
import org.jetbrains.kotlin.descriptors.Visibilities
import kotlin.test.*


class CirTreePropertyDeserializerTest : AbstractCirTreeDeserializerTest() {

    fun `test simple val property`() {
        val module = createCirTreeFromSourceCode("val x: Int = 42")

        val property = module.assertSingleProperty()

        assertEquals("x", property.name.toStrippedString())
        assertFalse(property.isConst, "Expected property to is *no* const")
        assertNotNull(property.getter, "Expected property to have getter")
        assertNull(property.setter, "Expected property to have *no* setter")
        assertFalse(property.isLateInit, "Expected property to be *not* lateinit")
        assertFalse(property.isVar, "Expected property to be *not* var")
        assertEquals(Visibilities.Public, property.visibility, "Expected property to be public")
        assertNull(property.extensionReceiver, "Expected property to *not* have extension receiver")
    }

    fun `test simple var property`() {
        val module = createCirTreeFromSourceCode("var x: Int = 42")
        val property = module.assertSingleProperty()
        assertNotNull(property.getter, "Expected property has getter")
        assertNotNull(property.setter, "Expected property has setter")
        assertFalse(property.isLateInit, "Expected property to be not lateinit")
        assertTrue(property.isVar, "Expected property to be var")
    }

    fun `test lateinit var property`() {
        val module = createCirTreeFromSourceCode("lateinit var x: Int")
        val property = module.assertSingleProperty()

        assertNotNull(property.getter, "Expected property has getter")
        assertNotNull(property.setter, "Expected property has setter")
        assertTrue(property.isLateInit, "Expected property to be lateinit")
        assertTrue(property.isVar, "Expected property to be var")
    }

    fun `test generic var property`() {
        val module = createCirTreeFromSourceCode("var <T> T.x: T get() = this")
        val property = module.assertSingleProperty()

        assertNotNull(property.extensionReceiver, "Expected property has extension receiver")
        assertTrue(
            property.extensionReceiver?.type is CirTypeParameterType,
            "Expected extension receiver being type of ${CirTypeParameterType::class.simpleName}"
        )
    }

    fun `test multiple properties`() {
        val module = createCirTreeFromSourceCode(
            """
            val x: Int = 42
            val y: Float = 42f
            var z: String = "42"
            var Any?.answer get() = if(this != null) 42 else null
            """.trimIndent()
        )

        val pkg = module.assertSinglePackage()
        assertEquals(4, pkg.properties.size, "Expected exactly 4 properties in package")

        val answerProperty = pkg.properties.single { it.name.toStrippedString() == "answer" }
        val answerReturnType = answerProperty.returnType as? CirClassType
            ?: kotlin.test.fail("Expected answer return type is class")
        assertEquals("kotlin/Int", answerReturnType.classifierId.toString())
        assertTrue(answerReturnType.isMarkedNullable, "Expected answer return type being marked nullable")
    }
}

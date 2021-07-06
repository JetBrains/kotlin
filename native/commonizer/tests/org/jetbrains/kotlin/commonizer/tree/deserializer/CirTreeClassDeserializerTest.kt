/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.tree.deserializer

import org.jetbrains.kotlin.commonizer.cir.CirClassType
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import kotlin.test.*

class CirTreeClassDeserializerTest : AbstractCirTreeDeserializerTest() {

    fun `test simple class`() {
        val module = createCirTreeFromSourceCode("class X")
        val clazz = module.assertSingleClass().clazz
        assertEquals(Visibilities.Public, clazz.visibility, "Expected class to be public")
        assertNull(clazz.companion, "Expected class *not* having a companion")
        assertFalse(clazz.isInner, "Expected class *not* being inner")
        assertFalse(clazz.isCompanion, "Expected class *not* being companion")
        assertFalse(clazz.isExternal, "Expected class *not* being external")
        assertFalse(clazz.isValue, "Expected class *not* being value")
        assertFalse(clazz.isData, "Expected class *not* being data class")
        assertTrue(clazz.supertypes.isEmpty(), "Expected class not having any explicit supertypes")
        assertEquals(Modality.FINAL, clazz.modality, "Expected class to be final")
        assertEquals(ClassKind.CLASS, clazz.kind)
    }

    fun `test nested class`() {
        val module = createCirTreeFromSourceCode(
            """
            class Outer {
                class Inner
            }
            """
        )

        val pkg = module.assertSinglePackage()
        val outerClass = pkg.classes.singleOrNull() ?: kotlin.test.fail("Expected single class in package")
        assertEquals("Outer", outerClass.clazz.name.toStrippedString())

        val innerClass = outerClass.classes.singleOrNull() ?: kotlin.test.fail("Expected single nested class 'Inner'")
        assertFalse(innerClass.clazz.isInner, "Expected nested class to *not* be inner")
    }

    fun `test inner class`() {
        val module = createCirTreeFromSourceCode(
            """
            class Outer {
                inner class Inner
            }
            """
        )

        val pkg = module.assertSinglePackage()
        val outerClass = pkg.classes.singleOrNull() ?: kotlin.test.fail("Expected single class in package")
        assertEquals("Outer", outerClass.clazz.name.toStrippedString())

        val innerClass = outerClass.classes.singleOrNull() ?: kotlin.test.fail("Expected single nested class 'Inner'")
        assertTrue(innerClass.clazz.isInner, "Expected nested class to be inner class")
    }


    fun `test data class`() {
        val module = createCirTreeFromSourceCode("data class X(val x: String)")
        val clazz = module.assertSingleClass()
        assertTrue(clazz.clazz.isData, "Expected is data class")
        assertEquals(1, clazz.constructors.size, "Expected single constructor")
        assertEquals(1, clazz.properties.size, "Expected single property")
    }

    fun `test companion`() {
        val module = createCirTreeFromSourceCode(
            """
            class Outer {
                companion object {
                    val x: Int = 42
                }
            }
        """.trimIndent()
        )
        val clazz = module.assertSingleClass()
        val companion = clazz.classes.singleOrNull() ?: kotlin.test.fail("Expected single class in 'Outer'")
        assertTrue(companion.clazz.isCompanion, "Expected companion being marked as companion")
        assertEquals(1, companion.properties.size, "Expected exactly one property in companion")
    }

    fun `test object`() {
        val module = createCirTreeFromSourceCode("object X")
        val clazz = module.assertSingleClass()
        assertEquals(ClassKind.OBJECT, clazz.clazz.kind, "Expected object class kind")
    }

    fun `test interface`() {
        val module = createCirTreeFromSourceCode("interface X")
        val clazz = module.assertSingleClass()
        assertEquals(ClassKind.INTERFACE, clazz.clazz.kind)
    }

    fun `test supertypes`() {
        val module = createCirTreeFromSourceCode(
            """
            interface A
            interface B: A 
            interface C: B
            class X: C
            """.trimIndent()
        )

        val pkg = module.assertSinglePackage()
        val xClass = pkg.classes.singleOrNull { it.clazz.name.toStrippedString() == "X" } ?: kotlin.test.fail("Missing class 'X'")
        val xSuperType = xClass.clazz.supertypes.singleOrNull() ?: kotlin.test.fail("Expected single supertype for 'X'")
        val xClassSuperType = assertIs<CirClassType>(xSuperType, "Expected xSuperType to be class type")
        assertEquals("/C", xClassSuperType.classifierId.toString())

    }

    fun `test abstract class`() {
        val module = createCirTreeFromSourceCode("abstract class X")
        val clazz = module.assertSingleClass()
        assertEquals(Modality.ABSTRACT, clazz.clazz.modality)
    }

    fun `test open class`() {
        val module = createCirTreeFromSourceCode("open class X")
        val clazz = module.assertSingleClass()
        assertEquals(Modality.OPEN, clazz.clazz.modality)
    }

    fun `test class with properties functions and nested classes`() {
        val module = createCirTreeFromSourceCode(
            """
                class X {
                    val myInt: Int = 42
                    val myFloat: Float = 42f
                    val myDouble: Double = 42.0
                    val myString = "hello"
                    
                    fun myIntFunction(int: Int) = int
                    fun myStringFunction(string: String) = string
                    
                    class MyInnerClass1
                    class MyInnerClass2
                }
            """.trimIndent()
        )

        val clazz = module.assertSingleClass()

        fun assertContainsProperty(name: String) {
            clazz.properties.singleOrNull { it.name.toStrippedString() == name }
                ?: kotlin.test.fail("Missing property '$name'")
        }

        fun assertContainsFunction(name: String) {
            clazz.functions.singleOrNull { it.name.toStrippedString() == name }
                ?: kotlin.test.fail("Missing function '$name'")
        }

        fun assertContainsClass(name: String) {
            clazz.classes.singleOrNull { it.clazz.name.toStrippedString() == name }
                ?: kotlin.test.fail("Missing class '$name'")
        }

        assertContainsProperty("myInt")
        assertContainsProperty("myFloat")
        assertContainsProperty("myDouble")
        assertContainsProperty("myString")
        assertContainsFunction("myIntFunction")
        assertContainsFunction("myStringFunction")
        assertContainsClass("MyInnerClass1")
        assertContainsClass("MyInnerClass2")
    }
}

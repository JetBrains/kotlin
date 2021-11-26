/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cir

import org.jetbrains.kotlin.commonizer.mergedtree.CirClassifierIndex
import org.jetbrains.kotlin.commonizer.mergedtree.CirProvidedClassifiers
import org.jetbrains.kotlin.commonizer.mergedtree.CirProvidedClassifiersByModules
import org.jetbrains.kotlin.commonizer.tree.CirTreeRoot
import org.jetbrains.kotlin.commonizer.utils.KtInlineSourceCommonizerTestCase
import org.jetbrains.kotlin.commonizer.utils.createCirTree
import org.jetbrains.kotlin.commonizer.utils.mockClassType
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class SimpleCirSupertypesResolverTest : KtInlineSourceCommonizerTestCase() {

    fun `test single supertype from module`() {
        val module = createCirTreeFromSourceCode(
            """
                interface A
                class X: A
            """.trimIndent()
        )

        val root = CirTreeRoot(listOf(module))
        val index = CirClassifierIndex(root)
        val resolver = SimpleCirSupertypesResolver(index, CirProvidedClassifiers.EMPTY)
        val supertypes = resolver.supertypes(mockClassType("X"))
        kotlin.test.assertEquals(1, supertypes.size, "Expected single supertype. Found $supertypes")
        val supertype = supertypes.single()
        kotlin.test.assertEquals(CirEntityId.create("A"), supertype.classifierId)
        kotlin.test.assertNull(supertype.outerType, "Expected no outerType")
        kotlin.test.assertTrue(supertype.arguments.isEmpty(), "Expected no arguments. Found ${supertype.arguments}")
        kotlin.test.assertFalse(supertype.isMarkedNullable, "Expected supertype to be marked *not* nullable")
    }

    fun `test outerType`() {
        val module = createCirTreeFromSourceCode(
            """
                class Outer {
                    open inner class Inner
                    inner class X: Inner()
                }
            """.trimIndent()
        )

        val root = CirTreeRoot(listOf(module))
        val index = CirClassifierIndex(root)
        val resolver = SimpleCirSupertypesResolver(index, CirProvidedClassifiers.EMPTY)
        val supertypes = resolver.supertypes(mockClassType("/Outer.X"))
        kotlin.test.assertEquals(1, supertypes.size, "Expected single supertype. Found $supertypes")
        val supertype = supertypes.single()
        kotlin.test.assertEquals(CirEntityId.create("Outer.Inner"), supertype.classifierId)
        val outerType = kotlin.test.assertNotNull(supertype.outerType, "Expected outerType")
        kotlin.test.assertEquals(CirEntityId.create("Outer"), outerType.classifierId)
    }

    fun `test hierarchy supertype from module`() {
        val module = createCirTreeFromSourceCode(
            """
                interface A
                interface B: A
                interface C: A
                class X: B, C
            """.trimIndent()
        )

        val root = CirTreeRoot(listOf(module))
        val index = CirClassifierIndex(root)
        val resolver = SimpleCirSupertypesResolver(index, CirProvidedClassifiers.EMPTY)
        val supertypes = resolver.supertypes(mockClassType("X"))

        assertEquals(
            setOf("/B", "/C"), supertypes.map { it.safeAs<CirClassType>()?.classifierId.toString() }.toSet()
        )

        supertypes.forEach { supertype ->
            kotlin.test.assertNull(supertype.outerType, "Expected no outerType")
            kotlin.test.assertTrue(supertype.arguments.isEmpty(), "Expected no arguments. Found ${supertype.arguments}")
            kotlin.test.assertFalse(supertype.isMarkedNullable, "Expected supertype to be marked *not* nullable")
        }
    }

    fun `test single nullable supertype from module`() {
        val module = createCirTreeFromSourceCode(
            """
                interface A
                class X: A
            """.trimIndent()
        )

        val root = CirTreeRoot(listOf(module))
        val index = CirClassifierIndex(root)
        val resolver = SimpleCirSupertypesResolver(index, CirProvidedClassifiers.EMPTY)
        val supertypes = resolver.supertypes(mockClassType("X", nullable = true))
        kotlin.test.assertEquals(1, supertypes.size, "Expected single supertype. Found $supertypes")
        val supertype = supertypes.single()
        kotlin.test.assertEquals(CirEntityId.create("A"), supertype.classifierId)
        kotlin.test.assertTrue(supertype.isMarkedNullable, "Expected supertype to be marked nullable")
    }

    fun `test single supertype from dependencies`() {
        val module = createCirTree {
            dependency { source("""interface A""") }
            source("""class X: A""")
        }

        val root = CirTreeRoot(listOf(module))
        val index = CirClassifierIndex(root)
        val resolver = SimpleCirSupertypesResolver(
            index, CirProvidedClassifiersByModules(
                false, mapOf(
                    CirEntityId.create("A") to CirProvided.RegularClass(
                        typeParameters = emptyList(),
                        supertypes = emptyList(),
                        visibility = Visibilities.Public,
                        kind = ClassKind.INTERFACE
                    )
                )
            )
        )
        val supertypes = resolver.supertypes(mockClassType("X"))
        kotlin.test.assertEquals(1, supertypes.size, "Expected single supertype. Found $supertypes")
        val supertype = supertypes.single()
        kotlin.test.assertEquals(CirEntityId.create("A"), supertype.classifierId)
        kotlin.test.assertNull(supertype.outerType, "Expected no outerType")
        kotlin.test.assertTrue(supertype.arguments.isEmpty(), "Expected no arguments. Found ${supertype.arguments}")
        kotlin.test.assertFalse(supertype.isMarkedNullable, "Expected supertype to be marked *not* nullable")
    }

    fun `test single supertype from module and dependencies`() {
        val module = createCirTree {
            dependency { source("""interface A""") }
            source(
                """
                interface B
                class X: A, B
                """.trimIndent()
            )
        }

        val root = CirTreeRoot(listOf(module))
        val index = CirClassifierIndex(root)
        val resolver = SimpleCirSupertypesResolver(
            index, CirProvidedClassifiersByModules(
                false, mapOf(
                    CirEntityId.create("A") to CirProvided.RegularClass(
                        typeParameters = emptyList(),
                        supertypes = emptyList(),
                        visibility = Visibilities.Public,
                        kind = ClassKind.INTERFACE
                    )
                )
            )
        )
        val supertypes = resolver.supertypes(mockClassType("X"))
        kotlin.test.assertEquals(setOf("/A", "/B"), supertypes.map { it.safeAs<CirClassType>()?.classifierId.toString() }.toSet())

        supertypes.forEach { supertype ->
            kotlin.test.assertNull(supertype.outerType, "Expected no outerType")
            kotlin.test.assertTrue(supertype.arguments.isEmpty(), "Expected no arguments. Found ${supertype.arguments}")
            kotlin.test.assertFalse(supertype.isMarkedNullable, "Expected supertype to be marked *not* nullable")
        }
    }
}

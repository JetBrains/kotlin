/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("RemoveRedundantQualifierName")

package org.jetbrains.kotlin.commonizer.tree.merge

import org.jetbrains.kotlin.commonizer.cir.CirName.Companion.create
import org.jetbrains.kotlin.commonizer.mergedtree.PropertyApproximationKey
import kotlin.test.assertNotNull

class MergeCirTreePropertyTest : AbstractMergeCirTreeTest() {

    fun `test simple property`() {
        val aTree = createCirTreeFromSourceCode("""val a: Int = 42""")
        val bTree = createCirTreeFromSourceCode("""val a: Int = 42""")
        val merged = mergeCirTree("a" to aTree, "b" to bTree)

        val property = merged.assertSingleModule().assertSinglePackage().assertSingleProperty()
        property.targetDeclarations.forEachIndexed { index, cirProperty ->
            assertNotNull(cirProperty, "Expected not-null property at index $index")
            kotlin.test.assertEquals("a", cirProperty.name.toStrippedString(), "Expected correct property name at index $index")
            kotlin.test.assertEquals("kotlin/Int", cirProperty.returnType.toString(), "Expected correct return type at index $index")
        }
    }

    fun `test multiple properties`() {
        val aTree = createCirTreeFromSourceCode(
            """
                val a: Int = 42
                val b: Int = 42
                val c: String = "hello"
            """.trimIndent()
        )

        val bTree = createCirTreeFromSourceCode(
            """
                val a: Int = 42
                val b: Int = 42
                val c: String = "hello"
            """.trimIndent()
        )

        val merged = mergeCirTree("a" to aTree, "b" to bTree)
        val pkg = merged.assertSingleModule().assertSinglePackage()
        kotlin.test.assertEquals(3, pkg.properties.size, "Expected exactly three properties")

        val a = pkg.properties[PropertyApproximationKey(create("a"), null)] ?: kotlin.test.fail("Missing a property")
        val b = pkg.properties[PropertyApproximationKey(create("b"), null)] ?: kotlin.test.fail("Missing a property")
        val c = pkg.properties[PropertyApproximationKey(create("c"), null)] ?: kotlin.test.fail("Missing a property")

        a.assertNoMissingTargetDeclaration()
        b.assertNoMissingTargetDeclaration()
        c.assertNoMissingTargetDeclaration()
    }

    fun `test missing target declarations`() {
        val aTree = createCirTreeFromSourceCode("val a: Int = 42")
        val bTree = createCirTreeFromSourceCode("val b: Int = 42")
        val merged = mergeCirTree("a" to aTree, "b" to bTree)
        val pkg = merged.assertSingleModule().assertSinglePackage()

        val a = pkg.properties[PropertyApproximationKey(create("a"), null)] ?: kotlin.test.fail("Missing a property")
        val b = pkg.properties[PropertyApproximationKey(create("b"), null)] ?: kotlin.test.fail("Missing a property")

        kotlin.test.assertNotNull(a.targetDeclarations[0], "Expected *non* missing target declaration at index 0")
        kotlin.test.assertNull(a.targetDeclarations[1], "Expected missing target declaration for a at index 1")

        kotlin.test.assertNull(b.targetDeclarations[0], "Expected missing target declaration at index 0")
        kotlin.test.assertNotNull(b.targetDeclarations[1], "Expected *non* missing target declaration for a at index 1")
    }
}


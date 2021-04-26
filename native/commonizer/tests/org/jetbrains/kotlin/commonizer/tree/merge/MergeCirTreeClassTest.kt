/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.tree.merge

import org.jetbrains.kotlin.commonizer.cir.CirName
import org.jetbrains.kotlin.commonizer.mergedtree.PropertyApproximationKey

class MergeCirTreeClassTest : AbstractMergeCirTreeTest() {
    fun `test simple class`() {
        val aTree = createCirTreeFromSourceCode("class X")
        val bTree = createCirTreeFromSourceCode("class X")
        val merged = mergeCirTree("a" to aTree, "b" to bTree)
        val clazz = merged.assertSingleModule().assertSinglePackage().assertSingleClass()
        clazz.assertNoMissingTargetDeclaration()
    }

    fun `test missing target declarations`() {
        val aTree = createCirTreeFromSourceCode("class A")
        val bTree = createCirTreeFromSourceCode("class B")
        val merged = mergeCirTree("a" to aTree, "b" to bTree)
        val pkg = merged.assertSingleModule().assertSinglePackage()
        kotlin.test.assertEquals(2, pkg.classes.size, "Expected two classes (A, B)")
        val a = pkg.classes[CirName.create("A")] ?: kotlin.test.fail("Missing class 'A'")
        val b = pkg.classes[CirName.create("B")] ?: kotlin.test.fail("Missing class 'B'")

        a.assertOnlyTargetDeclarationAtIndex(0)
        b.assertOnlyTargetDeclarationAtIndex(1)
    }

    fun `test with children`() {
        val aTree = createCirTreeFromSourceCode(
            """
                class X {
                    val x: Int = 42
                    val a: Int = 42
                }
            """.trimIndent()
        )

        val bTree = createCirTreeFromSourceCode(
            """
                class X {
                    val x: Int = 42
                    val b: Int = 42
                }
            """.trimIndent()
        )

        val merged = mergeCirTree("a" to aTree, "b" to bTree)
        val clazz = merged.assertSingleModule().assertSinglePackage().assertSingleClass()
        kotlin.test.assertEquals(3, clazz.properties.size, "Expected three properties (x, a, b)")
        val x = clazz.properties[PropertyApproximationKey(CirName.create("x"), null)] ?: kotlin.test.fail("Missing property 'x'")
        val a = clazz.properties[PropertyApproximationKey(CirName.create("a"), null)] ?: kotlin.test.fail("Missing property 'a'")
        val b = clazz.properties[PropertyApproximationKey(CirName.create("b"), null)] ?: kotlin.test.fail("Missing property 'b'")

        x.assertNoMissingTargetDeclaration()
        a.assertOnlyTargetDeclarationAtIndex(0)
        b.assertOnlyTargetDeclarationAtIndex(1)
    }
}

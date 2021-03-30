/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.tree.merge

class MergeCirTreeClassConstructorTest : AbstractMergeCirTreeTest() {

    fun `test simple constructors`() {
        val aTree = createCirTreeFromSourceCode("class X(val x: Int)")
        val bTree = createCirTreeFromSourceCode("class X(val x: Int)")
        val merged = mergeCirTree("a" to aTree, "b" to bTree)
        val constructor = merged.assertSingleModule().assertSinglePackage().assertSingleClass().assertSingleConstructor()
        constructor.assertNoMissingTargetDeclaration()
    }

    fun `test missing target declaration`() {
        val aTree = createCirTreeFromSourceCode("class X(val a: Int)")
        val bTree = createCirTreeFromSourceCode("class X(val b: Short)")
        val merged = mergeCirTree("a" to aTree, "b" to bTree)
        val clazz = merged.assertSingleModule().assertSinglePackage().assertSingleClass()
        kotlin.test.assertEquals(2, clazz.constructors.size, "Expected two constructors")
    }
}

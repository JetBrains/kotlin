/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.tree.merge

import org.jetbrains.kotlin.commonizer.cir.CirName
import kotlin.test.assertEquals

class MergeCirTreeTypeAliasTest : AbstractMergeCirTreeTest() {

    fun `test simple type alias`() {
        val aTree = createCirTreeFromSourceCode("typealias a = Int")
        val bTree = createCirTreeFromSourceCode("typealias a = Int")
        val merged = mergeCirTree("a" to aTree, "b" to bTree)
        val typeAlias = merged.assertSingleModule().assertSinglePackage().assertSingleTypeAlias()
        typeAlias.assertNoMissingTargetDeclaration()
    }

    fun `test missing target declarations`() {
        val aTree = createCirTreeFromSourceCode("typealias a = Int")
        val bTree = createCirTreeFromSourceCode("typealias b = Int")
        val merged = mergeCirTree("a" to aTree, "b" to bTree)
        val pkg = merged.assertSingleModule().assertSinglePackage()

        assertEquals(2, pkg.typeAliases.size, "Expected 2 type aliases (a, b)")
        val a = pkg.typeAliases[CirName.create("a")] ?: kotlin.test.fail("Missing type alias 'a'")
        val b = pkg.typeAliases[CirName.create("b")] ?: kotlin.test.fail("Missing type alias 'b'")

        kotlin.test.assertNotNull(a.targetDeclarations[0], "Expected target declaration for 'a' at index 0")
        kotlin.test.assertNotNull(b.targetDeclarations[1], "Expected target declaration for 'b' at index 1")
        kotlin.test.assertNull(a.targetDeclarations[1], "Expected *no* target declaration for 'a' at index 1")
        kotlin.test.assertNull(b.targetDeclarations[0], "Expected *no* target declaration for 'b' at index 0")
    }
}

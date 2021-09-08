/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.jetbrains.kotlin.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.commonizer.mergedtree.CirClassifierIndex
import org.jetbrains.kotlin.commonizer.mergedtree.CirCommonClassifierIdResolver
import org.jetbrains.kotlin.commonizer.tree.CirTreeRoot
import org.jetbrains.kotlin.commonizer.utils.createCirTreeRootFromSourceCode

class CirCommonClassifierIdResolverTest : AbstractInlineSourcesCommonizationTest() {

    /*
    Platform A:    X -> A
    Platform B:    X -> B

    Expected:     X
     */
    fun `test sample 0`() {
        val resolver = createCommonClassifierIdResolver(
            createCirTreeRootFromSourceCode(
                """
                class A
                typealias X = A
            """.trimIndent()
            ),
            createCirTreeRootFromSourceCode(
                """
                class B
                typealias X = B
            """.trimIndent()
            )
        )

        assertEquals(setOf("X"), resolver.findCommonId("A"))
        assertEquals(setOf("X"), resolver.findCommonId("B"))
        assertEquals(setOf("X"), resolver.findCommonId("X"))
    }

    /*
    Platform A:    A -> B -> C
    Platform B:    C -> D -> E

    Expected:    C
     */
    fun `test sample 1`() {
        val resolver = createCommonClassifierIdResolver(
            createCirTreeRootFromSourceCode(
                """
                class C
                typealias B = C
                typealias A = B
            """.trimIndent()
            ),
            createCirTreeRootFromSourceCode(
                """
                class E
                typealias D = E
                typealias C = D
            """.trimIndent()
            )
        )

        //assertEquals(setOf("C"), resolver.findCommonId("A"))
        //assertEquals(setOf("C"), resolver.findCommonId("B"))
        //assertEquals(setOf("C"), resolver.findCommonId("C"))
        assertEquals(setOf("C"), resolver.findCommonId("D"))
        //assertEquals(setOf("C"), resolver.findCommonId("E"))
    }

    /*
    Platform A:    A -> B -> C
    Platform B:    A -> X -> B -> C

    Expected: A, B, C
     */
    fun `test sample 2`() {
        val resolver = createCommonClassifierIdResolver(
            createCirTreeRootFromSourceCode(
                """
                class C
                typealias B = C
                typealias A = B
            """.trimIndent()
            ),
            createCirTreeRootFromSourceCode(
                """
                class C
                typealias B = C
                typealias X = B
                typealias A = X
            """.trimIndent()
            )
        )

        assertEquals(setOf("A", "B", "C"), resolver.findCommonId("A"))
        assertEquals(setOf("A", "B", "C"), resolver.findCommonId("B"))
        assertEquals(setOf("A", "B", "C"), resolver.findCommonId("C"))
        assertEquals(setOf("A", "B", "C"), resolver.findCommonId("X"))
    }

    fun `test sample 3`() {
        val resolver = createCommonClassifierIdResolver(
            createCirTreeRootFromSourceCode("class A"),
            createCirTreeRootFromSourceCode("class B")
        )

        assertEquals(emptySet<String>(), resolver.findCommonId("A"))
        assertEquals(emptySet<String>(), resolver.findCommonId("B"))
    }


    /*
    Platform A:    A -> B -> C
    Platform B:    A -> B -> C
    Platform C:    A -> B

    Expected:    A, B
     */
    fun `test sample 4`() {
        val resolver = createCommonClassifierIdResolver(
            createCirTreeRootFromSourceCode(
                """
                class C
                typealias B = C
                typealias A = B
            """.trimIndent()
            ),
            createCirTreeRootFromSourceCode(
                """
                class C
                typealias B = C
                typealias A = B
            """.trimIndent()
            ),
            createCirTreeRootFromSourceCode(
                """
                class B 
                typealias A = B
                """.trimIndent()
            )
        )

        assertEquals(setOf("A", "B"), resolver.findCommonId("A"))
        assertEquals(setOf("A", "B"), resolver.findCommonId("B"))
        assertEquals(setOf("A", "B"), resolver.findCommonId("C"))
    }

    /*
    Rather esoteric case!
    Platform A:    A -> B -> C    D -> E
    Platform B:    B -> C - D

    Expected:    B, C, D
     */
    fun `test sample 5`() {
        val rootA = createCirTreeRootFromSourceCode(
            """
                class C
                typealias B = C
                typealias A = B
                
                class E
                typealias D = E
            """.trimIndent()
        )

        val rootB = createCirTreeRootFromSourceCode(
            """
                class D
                typealias C = D
                typealias B = C
            """.trimIndent()
        )

        val resolver = createCommonClassifierIdResolver(rootB, rootA)
        assertEquals(setOf("B", "C", "D"), resolver.findCommonId("B"))
        assertEquals(setOf("B", "C", "D"), resolver.findCommonId("C"))
        assertEquals(setOf("B", "C", "D"), resolver.findCommonId("D"))
        assertEquals(setOf("B", "C", "D"), resolver.findCommonId("A"))
        assertEquals(setOf("B", "C", "D"), resolver.findCommonId("E"))
    }

    /*
    Platform A:    A -> B -> C    Z -> Y -> X -> C
    Platform B:    A -> B -> C    Z -> Y -> X -> C

    Expected: A, B, C, X, Y, Z
     */
    fun `test sample 6`() {
        val resolver = createCommonClassifierIdResolver(
            createCirTreeRootFromSourceCode(
                """
                class C
                typealias B = C
                typealias A = B
            
                typealias X = C
                typealias Y = X
                typealias Z = Y
            """.trimIndent()
            ),
            createCirTreeRootFromSourceCode(
                """
                class C
                typealias B = C
                typealias A = B
                
                typealias X = C
                typealias Y = X
                typealias Z = Y
            """.trimIndent()
            )
        )

        assertEquals(setOf("A", "B", "C", "X", "Y", "Z"), resolver.findCommonId("A"))
        assertEquals(setOf("A", "B", "C", "X", "Y", "Z"), resolver.findCommonId("B"))
        assertEquals(setOf("A", "B", "C", "X", "Y", "Z"), resolver.findCommonId("C"))
        assertEquals(setOf("A", "B", "C", "X", "Y", "Z"), resolver.findCommonId("X"))
        assertEquals(setOf("A", "B", "C", "X", "Y", "Z"), resolver.findCommonId("Y"))
        assertEquals(setOf("A", "B", "C", "X", "Y", "Z"), resolver.findCommonId("Z"))
    }
}

private fun createCommonClassifierIdResolver(vararg root: CirTreeRoot): CirCommonClassifierIdResolver {
    return CirCommonClassifierIdResolver(
        TargetDependent(root.withIndex().associate { (index, root) -> LeafCommonizerTarget(index.toString()) to root })
            .mapValue(::CirClassifierIndex)
    )
}

private fun CirCommonClassifierIdResolver.findCommonId(id: String): Set<String> =
    findCommonId(CirEntityId.create(id))?.aliases.orEmpty().map { it.toQualifiedNameString() }.toSet()


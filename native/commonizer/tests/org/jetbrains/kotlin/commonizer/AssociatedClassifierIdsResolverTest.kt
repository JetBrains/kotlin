/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.jetbrains.kotlin.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.commonizer.mergedtree.CirClassifierIndex
import org.jetbrains.kotlin.commonizer.mergedtree.AssociatedClassifierIdsResolver
import org.jetbrains.kotlin.commonizer.mergedtree.AssociatedClassifierIdsResolverCache
import org.jetbrains.kotlin.commonizer.mergedtree.CirProvidedClassifiers
import org.jetbrains.kotlin.commonizer.tree.CirTreeRoot
import org.jetbrains.kotlin.commonizer.utils.createCirProvidedClassifiers
import org.jetbrains.kotlin.commonizer.utils.createCirTreeRoot
import org.jetbrains.kotlin.commonizer.utils.createCirTreeRootFromSourceCode

class AssociatedClassifierIdsResolverTest : AbstractInlineSourcesCommonizationTest() {

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

        assertEquals(setOf("X"), resolver.resolveAssociatedIds("A"))
        assertEquals(setOf("X"), resolver.resolveAssociatedIds("B"))
        assertEquals(setOf("X"), resolver.resolveAssociatedIds("X"))
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

        assertEquals(setOf("C"), resolver.resolveAssociatedIds("A"))
        assertEquals(setOf("C"), resolver.resolveAssociatedIds("B"))
        assertEquals(setOf("C"), resolver.resolveAssociatedIds("C"))
        assertEquals(setOf("C"), resolver.resolveAssociatedIds("D"))
        assertEquals(setOf("C"), resolver.resolveAssociatedIds("E"))
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

        assertEquals(setOf("A", "B", "C"), resolver.resolveAssociatedIds("A"))
        assertEquals(setOf("A", "B", "C"), resolver.resolveAssociatedIds("B"))
        assertEquals(setOf("A", "B", "C"), resolver.resolveAssociatedIds("C"))
        assertEquals(setOf("A", "B", "C"), resolver.resolveAssociatedIds("X"))
    }

    fun `test sample 3`() {
        val resolver = createCommonClassifierIdResolver(
            createCirTreeRootFromSourceCode("class A"),
            createCirTreeRootFromSourceCode("class B")
        )

        assertEquals(emptySet<String>(), resolver.resolveAssociatedIds("A"))
        assertEquals(emptySet<String>(), resolver.resolveAssociatedIds("B"))
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

        assertEquals(setOf("A", "B"), resolver.resolveAssociatedIds("A"))
        assertEquals(setOf("A", "B"), resolver.resolveAssociatedIds("B"))
        assertEquals(setOf("A", "B"), resolver.resolveAssociatedIds("C"))
    }

    /*
    Rather esoteric case!
    Platform A:    A -> B -> C
                   D -> E
    Platform B:    B -> C -> D

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
        assertEquals(setOf("B", "C", "D"), resolver.resolveAssociatedIds("A"))
        assertEquals(setOf("B", "C", "D"), resolver.resolveAssociatedIds("B"))
        assertEquals(setOf("B", "C", "D"), resolver.resolveAssociatedIds("C"))
        assertEquals(setOf("B", "C", "D"), resolver.resolveAssociatedIds("D"))
        assertEquals(setOf("B", "C", "D"), resolver.resolveAssociatedIds("E"))
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

        assertEquals(setOf("A", "B", "C", "X", "Y", "Z"), resolver.resolveAssociatedIds("A"))
        assertEquals(setOf("A", "B", "C", "X", "Y", "Z"), resolver.resolveAssociatedIds("B"))
        assertEquals(setOf("A", "B", "C", "X", "Y", "Z"), resolver.resolveAssociatedIds("C"))
        assertEquals(setOf("A", "B", "C", "X", "Y", "Z"), resolver.resolveAssociatedIds("X"))
        assertEquals(setOf("A", "B", "C", "X", "Y", "Z"), resolver.resolveAssociatedIds("Y"))
        assertEquals(setOf("A", "B", "C", "X", "Y", "Z"), resolver.resolveAssociatedIds("Z"))
    }

    fun `test sample 7 - with dependencies`() {
        val dependenciesModule = createModule {
            source(
                """
                class D_X
                typealias D_TA = D_X
                """.trimIndent()
            )
        }

        val rootA = createCirTreeRoot {
            dependency(dependenciesModule)
            source(
                """
                typealias A = D_TA
                typealias B = D_X
                typealias C = B
            """.trimIndent()
            )
        }

        val rootB = createCirTreeRoot {
            dependency(dependenciesModule)
            source(
                """
                typealias A = D_TA
                typealias C = D_TA
                """.trimIndent()
            )
        }

        val resolver = createCommonClassifierIdResolver(
            rootA, rootB, dependencies = createCirProvidedClassifiers(dependenciesModule)
        )

        assertEquals(setOf("D_X", "D_TA", "A", "C"), resolver.resolveAssociatedIds("D_X"))
        assertEquals(setOf("D_X", "D_TA", "A", "C"), resolver.resolveAssociatedIds("D_TA"))
        assertEquals(setOf("D_X", "D_TA", "A", "C"), resolver.resolveAssociatedIds("A"))
        assertEquals(setOf("D_X", "D_TA", "A", "C"), resolver.resolveAssociatedIds("B"))
        assertEquals(setOf("D_X", "D_TA", "A", "C"), resolver.resolveAssociatedIds("C"))
    }

    /*
    Platform A:    A1 -> A -> B -> C
    Platform B:    A2 -> A -> D -> E

    Expected: A
    */
    fun `test sample 8`() {
        val resolver = createCommonClassifierIdResolver(
            createCirTreeRootFromSourceCode(
                """
                    class C
                    typealias B = C
                    typealias A = B
                    typealias A1 = A
                """.trimIndent()
            ),
            createCirTreeRootFromSourceCode(
                """
                    class E
                    typealias D = E
                    typealias A = D
                    typealias A2 = A
                """.trimIndent()
            )
        )

        assertEquals(setOf("A"), resolver.resolveAssociatedIds("A"))
        assertEquals(setOf("A"), resolver.resolveAssociatedIds("A1"))
        assertEquals(setOf("A"), resolver.resolveAssociatedIds("A2"))
        assertEquals(setOf("A"), resolver.resolveAssociatedIds("B"))
        assertEquals(setOf("A"), resolver.resolveAssociatedIds("C"))
        assertEquals(setOf("A"), resolver.resolveAssociatedIds("E"))
        assertEquals(setOf("A"), resolver.resolveAssociatedIds("D"))
    }

    /*
    Platform A:    A -> B -> C
    Platform B:    A -> D -> E
                   F -> G -> C

    Expected:  A, C
    */
    fun `test sample 9`() {
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
                    typealias A = D
            
                    class C
                    typealias G = C
                    typealias F = G
                """.trimIndent()
            )
        )

        assertEquals(setOf("A", "C"), resolver.resolveAssociatedIds("A"))
        assertEquals(setOf("A", "C"), resolver.resolveAssociatedIds("B"))
        assertEquals(setOf("A", "C"), resolver.resolveAssociatedIds("C"))
        assertEquals(setOf("A", "C"), resolver.resolveAssociatedIds("D"))
        assertEquals(setOf("A", "C"), resolver.resolveAssociatedIds("E"))
        assertEquals(setOf("A", "C"), resolver.resolveAssociatedIds("F"))
        assertEquals(setOf("A", "C"), resolver.resolveAssociatedIds("G"))
    }
}

private fun createCommonClassifierIdResolver(
    vararg root: CirTreeRoot,
    dependencies: CirProvidedClassifiers = CirProvidedClassifiers.EMPTY
): AssociatedClassifierIdsResolver {
    return AssociatedClassifierIdsResolver(
        TargetDependent(root.withIndex().associate { (index, root) -> LeafCommonizerTarget(index.toString()) to root })
            .mapValue(::CirClassifierIndex),
        targetDependencies = root.withIndex()
            .associate { (index, _) -> LeafCommonizerTarget(index.toString()) to CirProvidedClassifiers.EMPTY }.toTargetDependent(),
        commonDependencies = dependencies,
        cache = AssociatedClassifierIdsResolverCache.None
    )
}

private fun AssociatedClassifierIdsResolver.resolveAssociatedIds(id: String): Set<String> =
    resolveAssociatedIds(CirEntityId.create(id))?.ids.orEmpty().map { it.toQualifiedNameString() }.toSet()


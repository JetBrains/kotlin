/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.uklibs

import org.jetbrains.kotlin.gradle.artifacts.uklibsPublication.*
import org.jetbrains.kotlin.util.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.reflect.KProperty

class UklibSourceSetStructureCheckerTests {

    @Test
    fun `empty graph`() {
        assertThrows<Exception> { checkFGMap() }
    }

    @Test
    fun `empty attributes`() {
        assertEquals(
            Violations(
                fragmentsWithEmptyAttributes = setOf(VFragment("a", emptySet()))
            ),
            checkSourceSetStructure(
                mapOf(
                    VFragment("a", emptySet()) to emptySet()
                )
            )
        )
    }

    @Test
    fun `missing fragments`() {
        val a by FG
        assertEquals(
            Violations(
                missingFragments = setOf("b")
            ),
            checkSourceSetStructure(
                mapOf(
                    a to setOf("b")
                )
            )
        )
    }

    @Test
    fun `self cycle`() {
        val a by FG
        assertEquals(
            Violations(
                cycles = listOf(
                    listOf(a, a)
                )
            ),
            checkFGMap(
                a to setOf(a)
            )
        )
    }

    @Test
    fun `other cycles`() {
        val a by FG
        val b by FG
        val c by FG
        assertEquals(
            Violations(
                cycles = listOf(
                    listOf(a, b, c, a),
                    listOf(b, c, a, b),
                    listOf(c, a, b, c),
                )
            ).pp(),
            checkFGMap(
                a to setOf(b),
                b to setOf(c),
                c to setOf(a),
            ).pp()
        )
    }

    @Test
    fun `single attribute frament - has no violations`() {
        val a by FG
        assertEquals(
            Violations(),
            checkFGMap(
                a to emptySet(),
            )
        )
        val b by FG
        assertEquals(
            Violations(),
            checkFGMap(
                a to emptySet(),
                b to emptySet(),
            )
        )
    }

    @Test
    fun `orphaned intermediate fragment`() {
        val abc by FG
        assertEquals(
            Violations(
                orphanedIntermediateFragments = setOf(abc)
            ),
            checkFGMap(
                abc to emptySet()
            )
        )
        val ab by FG
        assertEquals(
            Violations(
                orphanedIntermediateFragments = setOf(ab)
            ),
            checkFGMap(
                abc to emptySet(),
                ab to setOf(abc),
            )
        )
    }

    @Test
    fun `intermediate fragment - without all refiners`() {
        val android by FG
        val a by FG
        assertEquals(
            Violations(),
            checkFGMap(
                android to emptySet(),
                a to setOf(android),
            )
        )
    }

    @Test
    fun `multirooted graphs`() {
        val ab by FG
        val bc by FG
        val a by FG
        val b by FG
        val c by FG
        assertEquals(
            Violations(),
            checkFGMap(
                ab to emptySet(),
                bc to emptySet(),
                a to setOf(ab),
                b to setOf(ab, bc),
                c to setOf(bc),
            )
        )

        assertEquals(
            Violations(
                underRefinementViolations=setOf(
                    UnderRefinementViolation(
                        fragment = b,
                        underRefinedFragments = setOf(bc),
                        actuallyRefinedFragments = setOf(ab),
                    )
                )
            ),
            checkFGMap(
                ab to emptySet(),
                bc to emptySet(),
                a to setOf(ab),
                b to setOf(ab),
                c to setOf(bc),
            )
        )
    }

    @Test
    fun `unexpected refinemenet structure`() {
        val abc by FG
        val ab by FG
        val a by FG
        val b by FG
        val c by FG

        assertEquals(
            Violations(),
            checkFGMap(
                abc to emptySet(),
                ab to setOf(abc),
                a to setOf(ab),
                b to setOf(ab),
                c to setOf(abc),
            )
        )
        // Also accept non-reduced version of this graph
        assertEquals(
            Violations(),
            checkFGMap(
                abc to emptySet(),
                ab to setOf(abc),
                a to setOf(ab, abc),
                b to setOf(ab, abc),
                c to setOf(abc),
            )
        )

        assertEquals(
            Violations(
                underRefinementViolations = setOf(
                    UnderRefinementViolation(
                        fragment = ab,
                        underRefinedFragments = setOf(abc),
                        actuallyRefinedFragments = emptySet(),
                    ),
                    UnderRefinementViolation(
                        fragment = a,
                        underRefinedFragments = setOf(abc),
                        actuallyRefinedFragments = setOf(ab),
                    ),
                    UnderRefinementViolation(
                        fragment = b,
                        underRefinedFragments = setOf(abc),
                        actuallyRefinedFragments = setOf(ab),
                    ),
                )
            ),
            checkFGMap(
                abc to emptySet(),
                ab to emptySet(),
                a to setOf(ab),
                b to setOf(ab),
                c to setOf(abc),
            )
        )
    }

    @Test
    fun `isolated components`() {
        val abc by FG
        val a by FG
        val b by FG

        val de by FG
        val d by FG

        assertEquals(
            Violations(),
            checkFGMap(
                abc to emptySet(),
                a to setOf(abc),
                b to setOf(abc),
                de to emptySet(),
                d to setOf(de),
            )
        )
    }

    @Test
    fun `incompatible fragment refinement`() {
        val abc by FG
        val e by FG
        val d by FG

        assertEquals(
            Violations(
                incompatibleRefinementViolations = setOf(
                    RefinesIncompatibleFragmentViolation(
                        fragment = d,
                        incompatibleFragments = setOf(abc)
                    ),
                    RefinesIncompatibleFragmentViolation(
                        fragment = e,
                        incompatibleFragments = setOf(d, abc)
                    )
                )
            ),
            checkFGMap(
                abc to emptySet(),
                d to setOf(abc),
                e to setOf(d),
            )
        )

        val ab by FG
        val a by FG
        assertEquals(
            Violations(
                underRefinementViolations = setOf(
                    UnderRefinementViolation(
                        fragment = ab,
                        underRefinedFragments = setOf(abc),
                        actuallyRefinedFragments = emptySet(),
                    ),
                ),
                incompatibleRefinementViolations = setOf(
                    RefinesIncompatibleFragmentViolation(
                        fragment = abc,
                        incompatibleFragments = setOf(ab)
                    ),
                )
            ),
            checkFGMap(
                abc to setOf(ab),
                ab to emptySet(),
                a to setOf(ab, abc),
            )
        )
    }

    @Test
    fun `diamond refinement`() {
        val abcd by FG
        val abc by FG
        val bcd by FG
        val ab by FG
        val bc by FG
        val cd by FG
        val a by FG
        val b by FG
        val c by FG
        val d by FG

        // Whether the graph is reduced or not doesn't matter
        assertEquals(
            Violations(),
            checkFGMap(
                abcd to emptySet(),
                abc to setOf(abcd),
                bcd to setOf(abcd),
                ab to setOf(abc),
                bc to setOf(abc, bcd),
                cd to setOf(bcd),
                a to setOf(ab),
                b to setOf(ab, bc),
                c to setOf(bc, cd),
                d to setOf(cd),
            )
        )
        assertEquals(
            Violations(),
            checkFGMap(
                abcd to emptySet(),
                abc to setOf(abcd),
                bcd to setOf(abcd),
                ab to setOf(abc, abcd),
                bc to setOf(abc, bcd, abcd),
                cd to setOf(bcd, abcd),
                a to setOf(ab, abcd),
                b to setOf(ab, bc, abcd),
                c to setOf(bc, cd, bcd),
                d to setOf(cd),
            )
        )

        assertEquals(
            Violations(
                underRefinementViolations = setOf(
                    UnderRefinementViolation(
                        fragment = bc,
                        underRefinedFragments = setOf(abc, bcd, abcd),
                        actuallyRefinedFragments = emptySet(),
                    ),
                    UnderRefinementViolation(
                        fragment = b,
                        underRefinedFragments = setOf(bcd),
                        actuallyRefinedFragments = setOf(ab, bc, abc, abcd),
                    ),
                    UnderRefinementViolation(
                        fragment = c,
                        underRefinedFragments = setOf(abc),
                        actuallyRefinedFragments = setOf(bc, cd, bcd, abcd),
                    ),
                )
            ).pp(),
            checkFGMap(
                abcd to emptySet(),
                abc to setOf(abcd),
                bcd to setOf(abcd),
                ab to setOf(abc),

                bc to emptySet(),

                cd to setOf(bcd),
                a to setOf(ab),
                b to setOf(ab, bc),
                c to setOf(bc, cd),
                d to setOf(cd),
            ).pp()
        )
    }

    fun checkFGMap(vararg refinementEdges: Pair<VFragment, Set<VFragment>>): Violations {
        return checkSourceSetStructure(
            refinementEdges.toMap().mapValues { it.value.map { it.identifier }.toHashSet() }
        )
    }

    object FG {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): VFragment {
            return VFragment(
                // "abc"
                property.name,
                // setOf("a", "b", "c")
                property.name.map { it.toString() }.toHashSet(),
            )
        }
    }
}
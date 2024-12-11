/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.uklibs

import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.diagnostics.UklibFragmentsChecker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.reflect.KProperty

internal class UklibFragmentsCheckerTests {

    @Test
    fun `empty graph`() {
        assertEquals(
            setOf(
                UklibFragmentsChecker.Violation.EmptyRefinementGraph,
            ),
            check()
        )
    }

    @Test
    fun `empty attributes`() {
        assertEquals(
            setOf(
                UklibFragmentsChecker.Violation.FragmentWithEmptyAttributes(FragmentToCheck("a", emptySet()))
            ),
            checkSourceSetStructure(
                mapOf(
                    FragmentToCheck("a", emptySet()) to emptySet()
                )
            )
        )
    }

    @Test
    fun `missing fragments`() {
        val a by name
        assertEquals(
            setOf(
                UklibFragmentsChecker.Violation.MissingFragment("b")
            ),
            checkSourceSetStructure(
                mapOf(
                    a to setOf("b")
                )
            )
        )
    }

    @Test
    fun `cycles`() {
        val a by name
        assertEquals(
            setOf(
                UklibFragmentsChecker.Violation.FirstEncounteredCycle(listOf(a, a))
            ),
            check(
                a to setOf(a)
            )
        )

        val b by name
        assertEquals(
            setOf(
                UklibFragmentsChecker.Violation.FirstEncounteredCycle(listOf(a, b, a))
            ),
            check(
                a to setOf(b),
                b to setOf(a),
            )
        )

        val entry by name
        val c by name
        assertEquals(
            setOf(
                UklibFragmentsChecker.Violation.FirstEncounteredCycle(
                    listOf(entry, a, b, c, a),
                )
            ),
            check(
                entry to setOf(a),
                a to setOf(b),
                b to setOf(c),
                c to setOf(a),
            )
        )
    }

    @Test
    fun `single attribute framents`() {
        val a by name
        assertEquals(
            emptySet(),
            check(
                a to emptySet(),
            )
        )
        val b by name
        assertEquals(
            emptySet(),
            check(
                a to emptySet(),
                b to emptySet(),
            )
        )
    }

    @Test
    fun `orphaned intermediate fragment`() {
        val abc by name
        assertEquals(
            setOf(
                UklibFragmentsChecker.Violation.OrphanedIntermediateFragment(abc),
            ),
            check(
                abc to emptySet()
            )
        )
        val ab by name
        assertEquals(
            setOf(
                UklibFragmentsChecker.Violation.OrphanedIntermediateFragment(ab),
            ),
            check(
                abc to emptySet(),
                ab to setOf(abc),
            )
        )
    }

    @Test
    fun `multiple-same fragment`() {
        val jvm8 = FragmentToCheck("jvm-8", setOf("jvm"))
        val jvm11 = FragmentToCheck("jvm-11", setOf("jvm"))
        assertEquals(
            setOf(
                UklibFragmentsChecker.Violation.DuplicateAttributesFragments(
                    setOf("jvm"),
                    setOf(jvm8, jvm11)
                )
            ),
            checkSourceSetStructure(
                mapOf(
                    jvm8 to emptySet(),
                    jvm11 to emptySet(),
                )
            )
        )

        val iosArm64 = FragmentToCheck("iosArm64", setOf("iosArm64"))
        val iosX64 = FragmentToCheck("iosX64", setOf("iosX64"))
        val iosMain = FragmentToCheck("iosMain", setOf("iosArm64", "iosX64"))
        val appleMain = FragmentToCheck("appleMain", setOf("iosArm64", "iosX64"))
        val commonMain = FragmentToCheck("commonMain", setOf("iosArm64", "iosX64", "jvm"))
        assertEquals(
            setOf(
                UklibFragmentsChecker.Violation.DuplicateAttributesFragments(
                    setOf("iosArm64", "iosX64"),
                    setOf(iosMain, appleMain)
                )
            ),
            checkSourceSetStructure(
                mapOf(
                    iosArm64 to setOf(iosMain.identifier),
                    iosX64 to setOf(iosMain.identifier),
                    iosMain to setOf(appleMain.identifier),
                    appleMain to setOf(commonMain.identifier),
                    commonMain to emptySet(),
                )
            )
        )
    }

    @Test
    fun `intermediate fragment - without all refiners`() {
        val android by name
        val a by name
        assertEquals(
            emptySet(),
            check(
                android to emptySet(),
                a to setOf(android),
            )
        )
    }

    @Test
    fun `multirooted graphs`() {
        val ab by name
        val bc by name
        val a by name
        val b by name
        val c by name
        assertEquals(
            emptySet(),
            check(
                ab to emptySet(),
                bc to emptySet(),
                a to setOf(ab),
                b to setOf(ab, bc),
                c to setOf(bc),
            )
        )

        assertEquals(
            setOf(
                UklibFragmentsChecker.Violation.UnderRefinementViolation(
                    fragment = b,
                    underRefinedFragments = setOf(bc),
                    actuallyRefinedFragments = setOf(ab),
                )
            ),
            check(
                ab to emptySet(),
                bc to emptySet(),
                a to setOf(ab),
                b to setOf(ab),
                c to setOf(bc),
            )
        )
    }

    @Test
    fun `under refined fragments`() {
        val abc by name
        val ab by name
        val a by name
        val b by name
        val c by name

        assertEquals(
            emptySet(),
            check(
                abc to emptySet(),
                ab to setOf(abc),
                a to setOf(ab),
            )
        )

        assertEquals(
            emptySet(),
            check(
                abc to emptySet(),
                ab to setOf(abc),
                a to setOf(ab),
                b to setOf(ab),
                c to setOf(abc),
            )
        )

        // Also accept transitive version of this graph
        assertEquals(
            emptySet(),
            check(
                abc to emptySet(),
                ab to setOf(abc),
                a to setOf(ab, abc),
                b to setOf(ab, abc),
                c to setOf(abc),
            )
        )

        assertEquals(
            setOf(
                UklibFragmentsChecker.Violation.UnderRefinementViolation(
                    fragment = ab,
                    underRefinedFragments = setOf(abc),
                    actuallyRefinedFragments = emptySet(),
                ),
                UklibFragmentsChecker.Violation.UnderRefinementViolation(
                    fragment = a,
                    underRefinedFragments = setOf(abc),
                    actuallyRefinedFragments = setOf(ab),
                ),
                UklibFragmentsChecker.Violation.UnderRefinementViolation(
                    fragment = b,
                    underRefinedFragments = setOf(abc),
                    actuallyRefinedFragments = setOf(ab),
                )
            ),
            check(
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
        val abc by name
        val a by name
        val b by name
        val c by name

        val de by name
        val d by name

        assertEquals(
            emptySet(),
            check(
                abc to emptySet(),
                a to setOf(abc),
                b to setOf(abc),
                c to setOf(abc),
                de to emptySet(),
                d to setOf(de),
            )
        )
    }

    @Test
    fun `incompatible fragment refinement`() {
        val abc by name
        val e by name
        val d by name

        assertEquals(
            setOf(
                UklibFragmentsChecker.Violation.IncompatibleRefinementViolation(
                    fragment = d,
                    incompatibleFragments = setOf(abc)
                ),
                UklibFragmentsChecker.Violation.IncompatibleRefinementViolation(
                    fragment = e,
                    incompatibleFragments = setOf(d, abc)
                )
            ),
            check(
                abc to emptySet(),
                d to setOf(abc),
                e to setOf(d),
            )
        )

        val ab by name
        val a by name
        assertEquals(
            setOf(
                UklibFragmentsChecker.Violation.UnderRefinementViolation(
                    fragment = ab,
                    underRefinedFragments = setOf(abc),
                    actuallyRefinedFragments = emptySet(),
                ),
                UklibFragmentsChecker.Violation.IncompatibleRefinementViolation(
                    fragment = abc,
                    incompatibleFragments = setOf(ab),
                )
            ),
            check(
                abc to setOf(ab),
                ab to emptySet(),
                a to setOf(ab, abc),
            )
        )
    }

    @Test
    fun `diamond refinement`() {
        val abcd by name
        val abc by name
        val bcd by name
        val ab by name
        val bc by name
        val cd by name
        val a by name
        val b by name
        val c by name
        val d by name

        // Whether the graph is reduced or not shouldn't matter
        assertEquals(
            emptySet(),
            check(
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
            emptySet(),
            check(
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
            setOf(
                UklibFragmentsChecker.Violation.UnderRefinementViolation(
                    fragment = bc,
                    underRefinedFragments = setOf(abc, bcd, abcd),
                    actuallyRefinedFragments = emptySet(),
                ),
                UklibFragmentsChecker.Violation.UnderRefinementViolation(
                    fragment = b,
                    underRefinedFragments = setOf(bcd),
                    actuallyRefinedFragments = setOf(ab, bc, abc, abcd),
                ),
                UklibFragmentsChecker.Violation.UnderRefinementViolation(
                    fragment = c,
                    underRefinedFragments = setOf(abc),
                    actuallyRefinedFragments = setOf(bc, cd, bcd, abcd),
                )
            ),
            check(
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
            )
        )
    }

    fun check(vararg refinementEdges: Pair<FragmentToCheck, Set<FragmentToCheck>>): Set<UklibFragmentsChecker.Violation> {
        return UklibFragmentsChecker.findViolationsInSourceSetGraph(
            refinementEdges.toMap().mapValues { it.value.map { it.identifier }.toHashSet() }
        )
    }

    fun checkSourceSetStructure(
        refinementEdges: Map<FragmentToCheck, Set<String>>,
    ): Set<UklibFragmentsChecker.Violation> = UklibFragmentsChecker.findViolationsInSourceSetGraph(refinementEdges)

    object name {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): FragmentToCheck {
            return FragmentToCheck(
                // "abc"
                property.name,
                // setOf("a", "b", "c")
                property.name.map { it.toString() }.toHashSet(),
            )
        }
    }
}

internal typealias FragmentToCheck = UklibFragmentsChecker.FragmentToCheck
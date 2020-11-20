/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import kotlin.test.Test
import kotlin.test.assertTrue


class NpmRangeTest {
    @Test
    fun intersectTest() {
        fun assertIntersect(range1: NpmRange, range2: NpmRange, expected: NpmRange?) {
            val intersection = range1 intersect range2
            assertTrue("Range $range1 and $range2 expected to have intersection $expected, but actual is $intersection") {
                intersection == expected
            }

            val symIntersection = range2 intersect range1
            assertTrue("Range $range2 and $range1 expected to have union $expected, but actual is $symIntersection") {
                symIntersection == expected
            }
        }

        val range1 = npmRange(startMajor = 1, endMajor = 2)
        val range2 = npmRange(startMajor = 3, endMajor = 4)
        assertIntersect(range1, range2, null)

        assertIntersect(
            npmRange(startMajor = 1, endMajor = 3),
            npmRange(startMajor = 2, endMajor = 4),
            npmRange(
                startMajor = 2,
                endMajor = 3
            )
        )

        assertIntersect(
            npmRange(startMajor = 1, endMajor = 3),
            npmRange(startMajor = 2, endMajor = 4, startInclusive = true, endInclusive = true),
            npmRange(
                startMajor = 2,
                endMajor = 3,
                startInclusive = true
            )
        )

        assertIntersect(
            npmRange(startMajor = 1, endMajor = 3, startInclusive = true, endInclusive = true),
            npmRange(startMajor = 2, endMajor = 4, startInclusive = true, endInclusive = true),
            npmRange(
                startMajor = 2,
                endMajor = 3,
                startInclusive = true,
                endInclusive = true
            )
        )

        assertIntersect(
            npmRange(startMajor = 1, endMajor = 4, startInclusive = true, endInclusive = true),
            npmRange(startMajor = 2, endMajor = 3, startInclusive = true, endInclusive = true),
            npmRange(
                startMajor = 2,
                endMajor = 3,
                startInclusive = true,
                endInclusive = true
            )
        )

        assertIntersect(
            npmRange(startMajor = 1, endMajor = 2),
            npmRange(startMajor = 2, endMajor = 3),
            null
        )

        assertIntersect(
            npmRange(startMajor = 1, endMajor = 2, endInclusive = true),
            npmRange(startMajor = 2, endMajor = 3, startInclusive = true),
            npmRange(
                startMajor = 2,
                endMajor = 2,
                startInclusive = true,
                endInclusive = true
            )
        )
    }

    @Test
    fun unionTest() {
        fun assertUnion(range1: NpmRange, range2: NpmRange, expected: Set<NpmRange>) {
            val union = (range1 union range2)
            assertTrue("Range $range1 and $range2 expected to have union $expected, but actual is $union") {
                union == expected
            }

            val symUnion = range2 union range1
            assertTrue("Range $range2 and $range1 expected to have union $expected, but actual is $symUnion") {
                symUnion == expected
            }
        }

        val range1 = npmRange(startMajor = 1, endMajor = 2)
        val range2 = npmRange(startMajor = 3, endMajor = 4)
        assertUnion(range1, range2, setOf(range1, range2))

        assertUnion(
            npmRange(startMajor = 1, endMajor = 3),
            npmRange(startMajor = 2, endMajor = 4),
            setOf(
                npmRange(
                    startMajor = 1,
                    endMajor = 4
                )
            )
        )

        assertUnion(
            npmRange(startMajor = 1, endMajor = 3),
            npmRange(startMajor = 2, endMajor = 4, startInclusive = true, endInclusive = true),
            setOf(
                npmRange(
                    startMajor = 1,
                    endMajor = 4,
                    endInclusive = true
                )
            )
        )

        assertUnion(
            npmRange(startMajor = 1, endMajor = 3, startInclusive = true, endInclusive = true),
            npmRange(startMajor = 2, endMajor = 4, startInclusive = true, endInclusive = true),
            setOf(
                npmRange(
                    startMajor = 1,
                    endMajor = 4,
                    startInclusive = true,
                    endInclusive = true
                )
            )
        )

        assertUnion(
            npmRange(startMajor = 1, endMajor = 4, startInclusive = true, endInclusive = true),
            npmRange(startMajor = 2, endMajor = 3, startInclusive = true, endInclusive = true),
            setOf(
                npmRange(
                    startMajor = 1,
                    endMajor = 4,
                    startInclusive = true,
                    endInclusive = true
                )
            )
        )

        assertUnion(
            npmRange(startMajor = 1, endMajor = 2),
            npmRange(startMajor = 2, endMajor = 3),
            setOf(
                npmRange(
                    startMajor = 1,
                    endMajor = 2
                ),
                npmRange(
                    startMajor = 2,
                    endMajor = 3
                )
            )
        )

        assertUnion(
            npmRange(startMajor = 1, endMajor = 2, endInclusive = true),
            npmRange(startMajor = 2, endMajor = 3, startInclusive = true),
            setOf(
                npmRange(
                    startMajor = 1,
                    endMajor = 3
                )
            )
        )
    }

    @Test
    fun rangeInvertTest() {
        fun assertInvert(invertible: NpmRange, expected: Set<NpmRange>) {
            val invert = invertible.invert()
            assertTrue("Inverted $invertible should be $expected but found $invert") {
                invert == expected
            }
        }

        assertInvert(npmRange(), emptySet())

        assertInvert(npmRange(endMajor = 1), setOf(npmRange(startMajor = 1, startInclusive = true)))
        assertInvert(npmRange(startMajor = 1), setOf(npmRange(endMajor = 1, endInclusive = true)))
        assertInvert(npmRange(endMajor = 1, endInclusive = true), setOf(npmRange(startMajor = 1)))
        assertInvert(npmRange(startMajor = 1, startInclusive = true), setOf(npmRange(endMajor = 1)))

        assertInvert(
            npmRange(startMajor = 1, endMajor = 2),
            setOf(
                npmRange(endMajor = 1, endInclusive = true),
                npmRange(startMajor = 2, startInclusive = true)
            )
        )
    }

    @Test
    fun hasIntersectionTest() {
        fun assertHasIntersection(range1: NpmRange, range2: NpmRange) =
            assertTrue("Range $range1 and $range2 expected to have intersection, but actual is not") {
                (range1 hasIntersection range2) && (range2 hasIntersection range1)
            }

        assertHasIntersection(npmRange(endMajor = 1), npmRange(endMajor = 2))
        assertHasIntersection(npmRange(startMajor = 1), npmRange(startMajor = 2))
        assertHasIntersection(npmRange(startMajor = 1, endMajor = 4), npmRange(startMajor = 2, endMajor = 3))
        assertHasIntersection(npmRange(startMajor = 1, endMajor = 3), npmRange(startMajor = 2, endMajor = 4))
        assertHasIntersection(
            npmRange(
                startMajor = 1,
                endMajor = 2,
                endInclusive = true
            ),
            npmRange(
                startMajor = 2,
                endMajor = 3,
                startInclusive = true
            )
        )
    }

    @Test
    fun hasNoIntersectionTest() {
        fun assertHasNoIntersection(range1: NpmRange, range2: NpmRange) =
            assertTrue("Range $range1 and $range2 expected to not have intersection, but actual is") {
                !(range1 hasIntersection range2) && !(range2 hasIntersection range1)
            }

        assertHasNoIntersection(npmRange(endMajor = 1), npmRange(startMajor = 2))
        assertHasNoIntersection(npmRange(startMajor = 1, endMajor = 2), npmRange(startMajor = 3, endMajor = 4))
        assertHasNoIntersection(
            npmRange(
                startMajor = 1,
                endMajor = 2,
                endInclusive = false
            ),
            npmRange(
                startMajor = 2,
                endMajor = 3,
                startInclusive = true
            )
        )

        assertHasNoIntersection(
            npmRange(
                startMajor = 1,
                endMajor = 2,
                endInclusive = true
            ),
            npmRange(
                startMajor = 2,
                endMajor = 3,
                startInclusive = false
            )
        )

        assertHasNoIntersection(
            npmRange(
                startMajor = 1,
                endMajor = 2,
                endInclusive = false
            ),
            npmRange(
                startMajor = 2,
                endMajor = 3,
                startInclusive = false
            )
        )
    }

    @Test
    fun maxStartTest() {
        val nullRange1 = npmRange()
        val nullRange2 = npmRange()
        val maxStart1 = maxStart(
            nullRange1,
            nullRange2
        )
        assertTrue("Max start should be ${nullRange2.startVersion} but $maxStart1 found") {
            maxStart1 == nullRange2.startVersion
        }

        val startRange1 = npmRange(
            startMajor = 1
        )
        val maxStart2 = maxStart(
            startRange1,
            npmRange()
        )
        assertTrue("Max start should be ${startRange1.startVersion} but $maxStart2 found") {
            maxStart2 == startRange1.startVersion
        }

        val startRange2 = npmRange(startMajor = 2)
        val maxStart3 = maxStart(
            startRange1,
            startRange2
        )
        assertTrue("Max start should be ${startRange2.startVersion} but $maxStart3 found") {
            maxStart3 == startRange2.startVersion
        }
    }

    @Test
    fun minStartTest() {
        val nullRange1 = npmRange()
        val nullRange2 = npmRange()
        val minStart1 = minStart(
            nullRange1,
            nullRange2
        )
        assertTrue("Min start should be ${null} but $minStart1 found") {
            minStart1 == null
        }

        val startRange1 = npmRange(
            startMajor = 1
        )
        val minStart2 = minStart(
            startRange1,
            npmRange()
        )
        assertTrue("Min start should be ${null} but $minStart2 found") {
            minStart2 == null
        }

        val startRange2 = npmRange(startMajor = 2)
        val minStart3 = minStart(
            startRange1,
            startRange2
        )
        assertTrue("Min start should be ${startRange1.startVersion} but $minStart3 found") {
            minStart3 == startRange1.startVersion
        }
    }

    @Test
    fun maxEndTest() {
        val nullRange1 = npmRange()
        val nullRange2 = npmRange()
        val maxEnd1 = maxEnd(
            nullRange1,
            nullRange2
        )
        assertTrue("Max end should be ${null} but $maxEnd1 found") {
            maxEnd1 == null
        }

        val endRange1 = npmRange(
            endMajor = 1
        )
        val maxEnd2 = maxEnd(
            endRange1,
            npmRange()
        )
        assertTrue("Max end should be ${null} but $maxEnd2 found") {
            maxEnd2 == null
        }

        val endRange2 = npmRange(endMajor = 2)
        val maxEnd3 = maxEnd(
            endRange1,
            endRange2
        )
        assertTrue("Max end should be ${endRange2.endVersion} but $maxEnd3 found") {
            maxEnd3 == endRange2.endVersion
        }
    }

    @Test
    fun minEndTest() {
        val nullRange1 = npmRange()
        val nullRange2 = npmRange()
        val minEnd1 = minEnd(
            nullRange1,
            nullRange2
        )
        assertTrue("Min end should be ${nullRange1.endVersion} but $minEnd1 found") {
            minEnd1 == nullRange1.endVersion
        }

        val endRange1 = npmRange(
            endMajor = 1
        )
        val minEnd2 = minEnd(
            endRange1,
            npmRange()
        )
        assertTrue("Min end should be ${endRange1.endVersion} but $minEnd2 found") {
            minEnd2 == endRange1.endVersion
        }

        val endRange2 = npmRange(endMajor = 2)
        val minEnd3 = minEnd(
            endRange1,
            endRange2
        )
        assertTrue("Min end should be ${endRange1.endVersion} but $minEnd3 found") {
            minEnd3 == endRange1.endVersion
        }
    }
}

private fun npmRange(
    startMajor: Int? = null,
    startMinor: Int? = null,
    startPatch: Int? = null,
    endMajor: Int? = null,
    endMinor: Int? = null,
    endPatch: Int? = null,
    startInclusive: Boolean = false,
    endInclusive: Boolean = false
): NpmRange =
    NpmRange(
        startVersion = semVer(startMajor, startMinor, startPatch),
        endVersion = semVer(endMajor, endMinor, endPatch),
        startInclusive = startInclusive,
        endInclusive = endInclusive
    )

private fun semVer(
    major: Int? = null,
    minor: Int? = null,
    patch: Int? = null
): SemVer? =
    if (major == null && minor == null && patch == null)
        null
    else {
        SemVer(
            (major ?: 0).toBigInteger(),
            (minor ?: 0).toBigInteger(),
            (patch ?: 0).toBigInteger()
        )
    }
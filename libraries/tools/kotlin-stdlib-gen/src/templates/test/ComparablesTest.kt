/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package templates.test

import templates.*
import templates.Family.*

object ComparablesTest : TestTemplateGroupBase() {

    private val Family.sourceFileComparisons: SourceFile
        get() = when (this) {
            Generic, Primitives -> SourceFile.Comparisons
            Unsigned -> SourceFile.UComparisons
            else -> error(this)
        }


    val f_minOf_2 = test("minOf_2()") {
        include(Generic)
        include(Primitives, PrimitiveType.numericPrimitives)
        include(Unsigned)
    } builder {
        sourceFile(f.sourceFileComparisons)

        val p = primitive ?: PrimitiveType.Int
        val minOf = if (f == Generic) "minOf<${p.name}>" else "minOf"
        body {
            """
            expect(${toP(1)}) { $minOf(${toP(2)}, ${toP(1)}) }
            expect(${toP(58)}) { $minOf(${toP(58)}, ${toP(126)}) }
            expect(${toP(23)}) { $minOf(${p.randomNextFrom(23)}, ${toP(23)}) }
            expect(MIN_VALUE) { $minOf(MIN_VALUE, MAX_VALUE) }
            expect(MAX_VALUE) { $minOf(MAX_VALUE, MAX_VALUE) }
            """
        }
        if (p.isFloatingPoint()) {
            bodyAppend {
                """
                assertEquals(-ZERO, $minOf(ZERO, -ZERO))
                assertEquals(-ZERO, $minOf(-ZERO, ZERO))
                assertEquals(NEGATIVE_INFINITY, $minOf(NEGATIVE_INFINITY, POSITIVE_INFINITY))
                """
            }
        }
    }

    val f_minOf_3 = test("minOf_3()") {
        include(Generic)
        include(Primitives, PrimitiveType.numericPrimitives)
        include(Unsigned)
    } builder {
        sourceFile(f.sourceFileComparisons)

        val p = primitive ?: PrimitiveType.Int
        val minOf = if (f == Generic) "minOf<${p.name}>" else "minOf"
        body {
            """
            expect(${toP(1)}) { $minOf(${toP(2)}, ${toP(1)}, ${toP(3)}) }
            expect(${toP(55)}) { $minOf(${toP(58)}, ${toP(126)}, ${toP(55)}) }
            expect(${toP(23)}) { $minOf(${p.randomNextFrom(23)}, ${toP(23)}, ${p.randomNextFrom(23)}) }
            expect(MAX_VALUE) { $minOf(MAX_VALUE, MAX_VALUE, MAX_VALUE) }
            """
        }
        if (p.isFloatingPoint()) {
            bodyAppend {
                """
                expect(ZERO) { $minOf(MIN_VALUE, MAX_VALUE, ZERO) }
                assertEquals(-ZERO, $minOf(ZERO, -ZERO, -ZERO))
                assertEquals(-ZERO, $minOf(-ZERO, ZERO, ZERO))
                assertEquals(MIN_VALUE, $minOf(POSITIVE_INFINITY, MAX_VALUE, MIN_VALUE))
                """
            }
        } else {
            bodyAppend {
                """
                expect(MIN_VALUE) { $minOf(MIN_VALUE, MAX_VALUE, ZERO) }
                """
            }
        }
    }

    val f_minOf_vararg = test("minOf_vararg()") {
        include(Generic)
        include(Primitives, PrimitiveType.numericPrimitives)
        include(Unsigned)
    } builder {
        sourceFile(f.sourceFileComparisons)

        val p = primitive ?: PrimitiveType.Int
        val minOf = if (f == Generic) "minOf<${p.name}>" else "minOf"
        body {
            """
            expect(${toP(1)}) { $minOf(${toP(2)}, ${toP(1)}, ${toP(3)}, ${toP(10)}) }
            expect(${toP(55)}) { $minOf(${toP(58)}, ${toP(126)}, ${toP(55)}, ${toP(87)}) }
            expect(${toP(21)}) { $minOf(${p.randomNextFrom(23)}, ${toP(23)}, ${p.randomNextFrom(23)}, ${toP(21)}) }
            expect(MAX_VALUE) { $minOf(MAX_VALUE, MAX_VALUE, MAX_VALUE, MAX_VALUE) }
            """
        }
        if (p.isFloatingPoint()) {
            bodyAppend {
                """
                assertEquals(ZERO, $minOf(MIN_VALUE, MAX_VALUE, ${toP(0)}, ${toP(1)}))
                assertEquals(-ZERO, $minOf(ZERO, -ZERO, -ZERO, ZERO))
                assertEquals(-ZERO, $minOf(-ZERO, ZERO, ZERO, -ZERO))
                assertEquals(NEGATIVE_INFINITY, $minOf(POSITIVE_INFINITY, NEGATIVE_INFINITY, MAX_VALUE, MIN_VALUE))
                """
            }
        } else {
            bodyAppend {
                """
                assertEquals(MIN_VALUE, $minOf(MIN_VALUE, MAX_VALUE, ${toP(0)}, ${toP(1)}))
                """
            }
        }
    }

    val f_maxOf_2 = test("maxOf_2()") {
        include(Generic)
        include(Primitives, PrimitiveType.numericPrimitives)
        include(Unsigned)
    } builder {
        sourceFile(f.sourceFileComparisons)

        val p = primitive ?: PrimitiveType.Int
        val maxOf = if (f == Generic) "maxOf<${p.name}>" else "maxOf"
        body {
            """
            expect(${toP(2)}) { $maxOf(${toP(2)}, ${toP(1)}) }
            expect(${toP(126)}) { $maxOf(${toP(58)}, ${toP(126)}) }
            expect(${toP(23)}) { $maxOf(${p.randomNextUntil(23)}, ${toP(23)}) }
            expect(MAX_VALUE) { $maxOf(MIN_VALUE, MAX_VALUE) }
            expect(MIN_VALUE) { $maxOf(MIN_VALUE, MIN_VALUE) }
            """
        }
        if (p.isFloatingPoint()) {
            bodyAppend {
                """
                assertEquals(ZERO, $maxOf(ZERO, -ZERO))
                assertEquals(ZERO, $maxOf(-ZERO, ZERO))
                assertEquals(POSITIVE_INFINITY, $maxOf(NEGATIVE_INFINITY, POSITIVE_INFINITY))
                """
            }
        }
    }

    val f_maxOf_3 = test("maxOf_3()") {
        include(Generic)
        include(Primitives, PrimitiveType.numericPrimitives)
        include(Unsigned)
    } builder {
        sourceFile(f.sourceFileComparisons)

        val p = primitive ?: PrimitiveType.Int
        val maxOf = if (f == Generic) "maxOf<${p.name}>" else "maxOf"
        body {
            """
            expect(${toP(3)}) { $maxOf(${toP(2)}, ${toP(1)}, ${toP(3)}) }
            expect(${toP(126)}) { $maxOf(${toP(58)}, ${toP(126)}, ${toP(55)}) }
            expect(${toP(23)}) { $maxOf(${p.randomNextUntil(23)}, ${toP(23)}, ${p.randomNextUntil(23)}) }
            expect(MIN_VALUE) { $maxOf(MIN_VALUE, MIN_VALUE, MIN_VALUE) }
            """
        }
        if (p.isFloatingPoint()) {
            bodyAppend {
                """
                expect(MAX_VALUE) { $maxOf(MIN_VALUE, MAX_VALUE, ${toP(0)}) }
                assertEquals(ZERO, $maxOf(ZERO, -ZERO, -ZERO))
                assertEquals(ZERO, $maxOf(-ZERO, ZERO, ZERO))
                assertEquals(POSITIVE_INFINITY, $maxOf(POSITIVE_INFINITY, MAX_VALUE, MIN_VALUE))
                """
            }
        } else {
            bodyAppend {
                """
                expect(MAX_VALUE) { $maxOf(MIN_VALUE, MAX_VALUE, ${toP(0)}) }
                """
            }
        }
    }

    val f_maxOf_vararg = test("maxOf_vararg()") {
        include(Generic)
        include(Primitives, PrimitiveType.numericPrimitives)
        include(Unsigned)
    } builder {
        sourceFile(f.sourceFileComparisons)

        val p = primitive ?: PrimitiveType.Int
        val maxOf = if (f == Generic) "maxOf<${p.name}>" else "maxOf"
        body {
            """
            expect(${toP(10)}) { $maxOf(${toP(2)}, ${toP(1)}, ${toP(3)}, ${toP(10)}) }
            expect(${toP(126)}) { $maxOf(${toP(58)}, ${toP(126)}, ${toP(55)}, ${toP(87)}) }
            expect(${toP(23)}) { $maxOf(${p.randomNextUntil(23)}, ${toP(23)}, ${p.randomNextUntil(23)}, ${toP(21)}) }
            expect(MIN_VALUE) { $maxOf(MIN_VALUE, MIN_VALUE, MIN_VALUE, MIN_VALUE) }
            """
        }
        if (p.isFloatingPoint()) {
            bodyAppend {
                """
                expect(MAX_VALUE) { $maxOf(MIN_VALUE, MAX_VALUE, ${toP(0)}, ${toP(1)}) }
                assertEquals(ZERO, $maxOf(ZERO, -ZERO, -ZERO, ZERO))
                assertEquals(ZERO, $maxOf(-ZERO, ZERO, ZERO, -ZERO))
                assertEquals(POSITIVE_INFINITY, $maxOf(POSITIVE_INFINITY, NEGATIVE_INFINITY, MAX_VALUE, MIN_VALUE))
                """
            }
        } else {
            bodyAppend {
                """
                expect(MAX_VALUE) { $maxOf(MIN_VALUE, MAX_VALUE, ${toP(0)}, ${toP(1)}) }
                """
            }
        }
    }
}
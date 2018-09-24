/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package templates

import templates.Family.*

object ComparableOps : TemplateGroupBase() {

    private val numericPrimitives = PrimitiveType.numericPrimitives.sortedBy { it.capacity }.toSet()
    private val intPrimitives = setOf(PrimitiveType.Int, PrimitiveType.Long)
    private val shortIntPrimitives = setOf(PrimitiveType.Byte, PrimitiveType.Short)

    val f_coerceAtLeast = fn("coerceAtLeast(minimumValue: SELF)") {
        include(Generic)
        include(Primitives, numericPrimitives)
    } builder {
        sourceFile(SourceFile.Ranges)
        returns("SELF")
        typeParam("T : Comparable<T>")
        doc {
            """
            Ensures that this value is not less than the specified [minimumValue].

            @return this value if it's greater than or equal to the [minimumValue] or the [minimumValue] otherwise.
            """
        }
        sample("samples.comparisons.ComparableOps.coerceAtLeast${if (f == Generic) "Comparable" else ""}")
        body {
            """
            return if (this < minimumValue) minimumValue else this
            """
        }
    }

    val f_coerceAtMost = fn("coerceAtMost(maximumValue: SELF)") {
        include(Generic)
        include(Primitives, numericPrimitives)
    } builder {
        sourceFile(SourceFile.Ranges)
        returns("SELF")
        typeParam("T : Comparable<T>")
        doc {
            """
            Ensures that this value is not greater than the specified [maximumValue].

            @return this value if it's less than or equal to the [maximumValue] or the [maximumValue] otherwise.
            """
        }
        sample("samples.comparisons.ComparableOps.coerceAtMost${if (f == Generic) "Comparable" else ""}")
        body {
            """
            return if (this > maximumValue) maximumValue else this
            """
        }
    }

    val f_coerceIn_range_primitive = fn("coerceIn(range: ClosedRange<T>)") {
        include(Generic)
        include(Primitives, intPrimitives)
    } builder {
        sourceFile(SourceFile.Ranges)
        returns("SELF")
        typeParam("T : Comparable<T>")
        doc {
            """
            Ensures that this value lies in the specified [range].

            @return this value if it's in the [range], or `range.start` if this value is less than `range.start`, or `range.endInclusive` if this value is greater than `range.endInclusive`.
            """
        }
        sample("samples.comparisons.ComparableOps.coerceIn${if (f == Generic) "Comparable" else ""}")
        body(Generic) {
            """
            if (range is ClosedFloatingPointRange) {
                return this.coerceIn<T>(range)
            }
            if (range.isEmpty()) throw IllegalArgumentException("Cannot coerce value to an empty range: ${'$'}range.")
            return when {
                this < range.start -> range.start
                this > range.endInclusive -> range.endInclusive
                else -> this
            }
            """
        }
        body(Primitives) {
            """
            if (range is ClosedFloatingPointRange) {
                return this.coerceIn<T>(range)
            }
            if (range.isEmpty()) throw IllegalArgumentException("Cannot coerce value to an empty range: ${'$'}range.")
            return when {
                this < range.start -> range.start
                this > range.endInclusive -> range.endInclusive
                else -> this
            }
            """
        }
    }

    val f_coerceIn_fpRange = fn("coerceIn(range: ClosedFloatingPointRange<T>)") {
        include(Generic)
    } builder {
        sourceFile(SourceFile.Ranges)
        since("1.1")
        returns("SELF")
        typeParam("T : Comparable<T>")
        doc {
            """
            Ensures that this value lies in the specified [range].

            @return this value if it's in the [range], or `range.start` if this value is less than `range.start`, or `range.endInclusive` if this value is greater than `range.endInclusive`.
            """
        }
        sample("samples.comparisons.ComparableOps.coerceInFloatingPointRange")
        body(Generic) {
            """
            if (range.isEmpty()) throw IllegalArgumentException("Cannot coerce value to an empty range: ${'$'}range.")
            return when {
                // this < start equiv to this <= start && !(this >= start)
                range.lessThanOrEquals(this, range.start) && !range.lessThanOrEquals(range.start, this) -> range.start
                // this > end equiv to this >= end && !(this <= end)
                range.lessThanOrEquals(range.endInclusive, this) && !range.lessThanOrEquals(this, range.endInclusive) -> range.endInclusive
                else -> this
            }
            """
        }
    }


    val f_minOf_2 = fn("minOf(a: T, b: T)") {
        include(Generic)
        include(Primitives, numericPrimitives)
    } builder {
        sourceFile(SourceFile.Comparisons)
        since("1.1")
        typeParam("T : Comparable<T>")
        returns("T")
        receiver("")
        doc {
            """
            Returns the smaller of two values.
            If values are equal, returns the first one.
            """
        }
        // TODO: Add a note about NaN propagation for floats.
        specialFor(Primitives) {
            inlineOnly()
            doc {
                """Returns the smaller of two values."""
            }
            var convertBack = "to$primitive()"
            on(Platform.JS) {
                suppress("DEPRECATION_ERROR")
                convertBack = "unsafeCast<$primitive>()"
            }
            body {
                "return Math.min(a, b)"
            }
            if (primitive in shortIntPrimitives) {
                body { "return Math.min(a.toInt(), b.toInt()).$convertBack" }
            }
            on(Platform.JS) {
                if (primitive == PrimitiveType.Long) {
                    inline(suppressWarning = true)
                    body { "return if (a <= b) a else b" }
                }
            }
        }
        on(Platform.JS) { /* just to make expect, KT-22520 */ }
        body(Generic) {
            "return if (a <= b) a else b"
        }
    }

    val f_minOf = fn("minOf(a: T, b: T, c: T)") {
        include(Generic)
        include(Primitives, numericPrimitives)
    } builder {
        sourceFile(SourceFile.Comparisons)
        since("1.1")
        typeParam("T : Comparable<T>")
        returns("T")
        receiver("")
        specialFor(Primitives) { inlineOnly() }
        // TODO: Add a note about NaN propagation for floats.
        doc {
            """
            Returns the smaller of three values.
            """
        }
        body {
            "return minOf(a, minOf(b, c))"
        }
        on(Platform.JS) { /* just to make expect, KT-22520 */ }
        specialFor(Primitives) {
            if (primitive in shortIntPrimitives) {
                body { "return Math.min(a.toInt(), Math.min(b.toInt(), c.toInt())).to$primitive()" }
                on(Platform.JS) {
                    suppress("DEPRECATION_ERROR")
                    body { "return Math.min(a.toInt(), b.toInt(), c.toInt()).unsafeCast<$primitive>()" }
                }
            }
            else if (primitive != PrimitiveType.Long) {
                on(Platform.JS) {
                    suppress("DEPRECATION_ERROR")
                    body { "return Math.min(a, b, c)" }
                }
            }
        }
    }

    val f_minOf_2_comparator = fn("minOf(a: T, b: T, comparator: Comparator<in T>)") {
        include(Generic)
    } builder {
        sourceFile(SourceFile.Comparisons)
        since("1.1")
        returns("T")
        receiver("")
        doc {
            """
            Returns the smaller of two values according to the order specified by the given [comparator].
            If values are equal, returns the first one.
            """
        }
        body {
            "return if (comparator.compare(a, b) <= 0) a else b"
        }
    }

    val f_minOf_3_comparator = fn("minOf(a: T, b: T, c: T, comparator: Comparator<in T>)") {
        include(Generic)
    } builder {
        sourceFile(SourceFile.Comparisons)
        since("1.1")
        returns("T")
        receiver("")
        doc {
            """
            Returns the smaller of three values according to the order specified by the given [comparator].
            """
        }
        body {
            "return minOf(a, minOf(b, c, comparator), comparator)"
        }
    }

    val f_maxOf_2 = fn("maxOf(a: T, b: T)") {
        include(Generic)
        include(Primitives, numericPrimitives)
    } builder {
        sourceFile(SourceFile.Comparisons)
        since("1.1")
        typeParam("T : Comparable<T>")
        returns("T")
        receiver("")
        doc {
            """
            Returns the greater of two values.
            If values are equal, returns the first one.
            """
        }
        // TODO: Add a note about NaN propagation for floats.
        specialFor(Primitives) {
            inlineOnly()
            doc {
                """Returns the greater of two values."""
            }
            var convertBack = "to$primitive()"
            on(Platform.JS) {
                suppress("DEPRECATION_ERROR")
                convertBack = "unsafeCast<$primitive>()"
            }
            body {
                "return Math.max(a, b)"
            }
            if (primitive in shortIntPrimitives) {
                body { "return Math.max(a.toInt(), b.toInt()).$convertBack" }
            }
            on(Platform.JS) {
                if (primitive == PrimitiveType.Long) {
                    inline(suppressWarning = true)
                    body { "return if (a >= b) a else b" }
                }
            }
        }
        on(Platform.JS) { /* just to make expect, KT-22520 */ }
        body(Generic) {
            "return if (a >= b) a else b"
        }
    }

    val f_maxOf_3 = fn("maxOf(a: T, b: T, c: T)") {
        include(Generic)
        include(Primitives, numericPrimitives)
    } builder {
        sourceFile(SourceFile.Comparisons)
        since("1.1")
        typeParam("T : Comparable<T>")
        returns("T")
        receiver("")
        specialFor(Primitives) { inlineOnly() }
        // TODO: Add a note about NaN propagation for floats.
        doc {
            """
            Returns the greater of three values.
            """
        }
        body {
            "return maxOf(a, maxOf(b, c))"
        }
        on(Platform.JS) { /* just to make expect, KT-22520 */ }
        specialFor(Primitives) {
            if (primitive in shortIntPrimitives) {
                body { "return Math.max(a.toInt(), Math.max(b.toInt(), c.toInt())).to$primitive()" }
                on(Platform.JS) {
                    suppress("DEPRECATION_ERROR")
                    body { "return Math.max(a.toInt(), b.toInt(), c.toInt()).unsafeCast<$primitive>()" }
                }
            }
            else if (primitive != PrimitiveType.Long) {
                on(Platform.JS) {
                    suppress("DEPRECATION_ERROR")
                    body { "return Math.max(a, b, c)" }
                }
            }
        }
    }

    val f_maxOf_2_comparator = fn("maxOf(a: T, b: T, comparator: Comparator<in T>)") {
        include(Generic)
    } builder {
        sourceFile(SourceFile.Comparisons)
        since("1.1")
        returns("T")
        receiver("")
        doc {
            """
            Returns the greater of two values according to the order specified by the given [comparator].
            If values are equal, returns the first one.
            """
        }
        body {
            "return if (comparator.compare(a, b) >= 0) a else b"
        }
    }

    val f_maxOf_3_comparator = fn("maxOf(a: T, b: T, c: T, comparator: Comparator<in T>)") {
        include(Generic)
    } builder {
        sourceFile(SourceFile.Comparisons)
        since("1.1")
        returns("T")
        receiver("")
        doc {
            """
            Returns the greater of three values according to the order specified by the given [comparator].
            """
        }
        body {
            "return maxOf(a, maxOf(b, c, comparator), comparator)"
        }
    }


    val f_coerceIn_min_max = fn("coerceIn(minimumValue: SELF, maximumValue: SELF)") {
        include(Generic)
        include(Primitives, numericPrimitives)
    } builder {
        sourceFile(SourceFile.Ranges)

        specialFor(Generic) { signature("coerceIn(minimumValue: SELF?, maximumValue: SELF?)", notForSorting = true) }
        typeParam("T : Comparable<T>")
        returns("SELF")
        doc {
            """
            Ensures that this value lies in the specified range [minimumValue]..[maximumValue].

            @return this value if it's in the range, or [minimumValue] if this value is less than [minimumValue], or [maximumValue] if this value is greater than [maximumValue].
            """
        }
        sample("samples.comparisons.ComparableOps.coerceIn${if (f == Generic) "Comparable" else ""}")
        body(Primitives) {
            """
            if (minimumValue > maximumValue) throw IllegalArgumentException("Cannot coerce value to an empty range: maximum ${'$'}maximumValue is less than minimum ${'$'}minimumValue.")
            if (this < minimumValue) return minimumValue
            if (this > maximumValue) return maximumValue
            return this
            """
        }
        body(Generic) {
            """
            if (minimumValue !== null && maximumValue !== null) {
                if (minimumValue > maximumValue) throw IllegalArgumentException("Cannot coerce value to an empty range: maximum ${'$'}maximumValue is less than minimum ${'$'}minimumValue.")
                if (this < minimumValue) return minimumValue
                if (this > maximumValue) return maximumValue
            }
            else {
                if (minimumValue !== null && this < minimumValue) return minimumValue
                if (maximumValue !== null && this > maximumValue) return maximumValue
            }
            return this
            """
        }
    }
}

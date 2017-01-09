package templates

import templates.Family.*

fun comparables(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("coerceAtLeast(minimumValue: SELF)") {
        sourceFile(SourceFile.Ranges)
        only(Primitives, Generic)
        only(numericPrimitives)
        returns("SELF")
        typeParam("T: Comparable<T>")
        doc {
            """
            Ensures that this value is not less than the specified [minimumValue].

            @return this value if it's greater than or equal to the [minimumValue] or the [minimumValue] otherwise.
            """
        }
        body {
            """
            return if (this < minimumValue) minimumValue else this
            """
        }

    }

    templates add f("coerceAtMost(maximumValue: SELF)") {
        sourceFile(SourceFile.Ranges)
        only(Primitives, Generic)
        only(numericPrimitives)
        returns("SELF")
        typeParam("T: Comparable<T>")
        doc {
            """
            Ensures that this value is not greater than the specified [maximumValue].

            @return this value if it's less than or equal to the [maximumValue] or the [maximumValue] otherwise.
            """
        }
        body {
            """
            return if (this > maximumValue) maximumValue else this
            """
        }
    }

    templates add f("coerceIn(range: ClosedRange<T>)") {
        sourceFile(SourceFile.Ranges)
        only(Primitives, Generic)
        only(PrimitiveType.Int, PrimitiveType.Long)
        returns("SELF")
        typeParam("T: Comparable<T>")
        doc {
            """
            Ensures that this value lies in the specified [range].

            @return this value if it's in the [range], or `range.start` if this value is less than `range.start`, or `range.endInclusive` if this value is greater than `range.endInclusive`.
            """
        }
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

    templates add f("coerceIn(range: ClosedFloatingPointRange<T>)") {
        sourceFile(SourceFile.Ranges)
        only(Generic)
        since("1.1")
        returns("SELF")
        typeParam("T: Comparable<T>")
        doc {
            """
            Ensures that this value lies in the specified [range].

            @return this value if it's in the [range], or `range.start` if this value is less than `range.start`, or `range.endInclusive` if this value is greater than `range.endInclusive`.
            """
        }
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


    templates add f("minOf(a: T, b: T)") {
        sourceFile(SourceFile.Comparisons)
        only(Primitives, Generic)
        only(numericPrimitives)
        since("1.1")
        typeParam("T: Comparable<T>")
        returns("T")
        customReceiver("")
        inline(Primitives) { Inline.Only }
        doc {
            """
            Returns the smaller of two values.
            If values are equal, returns the first one.
            """
        }
        // TODO: Add a note about NaN propagation for floats.
        doc(Primitives) {
            """Returns the smaller of two values."""
        }
        bodyForTypes(Primitives, PrimitiveType.Byte, PrimitiveType.Short) { p ->
            "return Math.min(a.toInt(), b.toInt()).to$p()"
        }
        // TODO: custom body for JS minOf(Long, Long)
        body(Primitives) {
            "return Math.min(a, b)"
        }
        body(Generic) {
            "return if (a <= b) a else b"
        }
    }

    templates add f("minOf(a: T, b: T, c: T)") {
        sourceFile(SourceFile.Comparisons)
        only(Primitives, Generic)
        only(numericPrimitives)
        since("1.1")
        typeParam("T: Comparable<T>")
        returns("T")
        customReceiver("")
        inline(Primitives) { Inline.Only }
        // TODO: Add a note about NaN propagation for floats.
        doc {
            """
            Returns the smaller of three values.
            """
        }
        bodyForTypes(Primitives, PrimitiveType.Byte, PrimitiveType.Short) { p ->
            "return Math.min(a.toInt(), Math.min(b.toInt(), c.toInt())).to$p()"
        }
        body {
            "return minOf(a, minOf(b, c))"
        }
    }

    templates add f("minOf(a: T, b: T, comparator: Comparator<in T>)") {
        sourceFile(SourceFile.Comparisons)
        only(Generic)
        since("1.1")
        returns("T")
        customReceiver("")
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

    templates add f("minOf(a: T, b: T, c: T, comparator: Comparator<in T>)") {
        sourceFile(SourceFile.Comparisons)
        only(Generic)
        since("1.1")
        returns("T")
        customReceiver("")
        doc {
            """
            Returns the smaller of three values according to the order specified by the given [comparator].
            """
        }
        body {
            "return minOf(a, minOf(b, c, comparator), comparator)"
        }
    }
    
    templates add f("maxOf(a: T, b: T)") {
        sourceFile(SourceFile.Comparisons)
        only(Primitives, Generic)
        only(numericPrimitives)
        since("1.1")
        typeParam("T: Comparable<T>")
        returns("T")
        customReceiver("")
        inline(Primitives) { Inline.Only }
        doc {
            """
            Returns the greater of two values.
            If values are equal, returns the first one.
            """
        }
        // TODO: Add a note about NaN propagation for floats.
        doc(Primitives) {
            """Returns the greater of two values."""
        }
        bodyForTypes(Primitives, PrimitiveType.Byte, PrimitiveType.Short) { p ->
            "return Math.max(a.toInt(), b.toInt()).to$p()"
        }
        body(Primitives) {
            "return Math.max(a, b)"
        }
        body(Generic) {
            "return if (a >= b) a else b"
        }
    }

    templates add f("maxOf(a: T, b: T, c: T)") {
        sourceFile(SourceFile.Comparisons)
        only(Primitives, Generic)
        only(numericPrimitives)
        since("1.1")
        typeParam("T: Comparable<T>")
        returns("T")
        customReceiver("")
        inline(Primitives) { Inline.Only }
        // TODO: Add a note about NaN propagation for floats.
        doc {
            """
            Returns the greater of three values.
            """
        }
        bodyForTypes(Primitives, PrimitiveType.Byte, PrimitiveType.Short) { p ->
            "return Math.max(a.toInt(), Math.max(b.toInt(), c.toInt())).to$p()"
        }
        body {
            "return maxOf(a, maxOf(b, c))"
        }
    }

    templates add f("maxOf(a: T, b: T, comparator: Comparator<in T>)") {
        sourceFile(SourceFile.Comparisons)
        only(Generic)
        since("1.1")
        returns("T")
        customReceiver("")
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

    templates add f("maxOf(a: T, b: T, c: T, comparator: Comparator<in T>)") {
        sourceFile(SourceFile.Comparisons)
        only(Generic)
        since("1.1")
        returns("T")
        customReceiver("")
        doc {
            """
            Returns the greater of three values according to the order specified by the given [comparator].
            """
        }
        body {
            "return maxOf(a, maxOf(b, c, comparator), comparator)"
        }
    }


    templates add f("coerceIn(minimumValue: SELF, maximumValue: SELF)") {
        sourceFile(SourceFile.Ranges)
        only(Primitives, Generic)
        only(numericPrimitives)
        customSignature(Generic) { "coerceIn(minimumValue: SELF?, maximumValue: SELF?)" }
        typeParam("T: Comparable<T>")
        returns("SELF")
        doc {
            """
            Ensures that this value lies in the specified range [minimumValue]..[maximumValue].

            @return this value if it's in the range, or [minimumValue] if this value is less than [minimumValue], or [maximumValue] if this value is greater than [maximumValue].
            """
        }
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

    return templates
}
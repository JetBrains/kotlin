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

            @return this value if it's greater than or equal to the [maximumValue] or the [maximumValue] otherwise.
            """
        }
        body {
            """
            return if (this > maximumValue) maximumValue else this
            """
        }
    }

    templates add f("coerceIn(range: Range<T>)") {
        sourceFile(SourceFile.Ranges)
        only(Generic, Primitives)
        only(numericPrimitives.filter { it.isIntegral() })
        typeParam("T: Comparable<T>")
        returns("SELF")
        deprecate("Range<T> is deprecated. Use ClosedRange<T> instead.")
        deprecate(Generic) { forBinaryCompatibility }
        doc {
            """
            Ensures that this value lies in the specified [range].

            @return this value if it's in the [range], or range.start if this value is less than range.start, or range.end if this value is greater than range.end.
            """
        }
        body {
            """
            if (range.isEmpty()) throw IllegalArgumentException("Cannot coerce value to an empty range: ${'$'}range.")
            return if (this < range.start) range.start else if (this > range.end) range.end else this
            """
        }
    }

    templates add f("coerceIn(range: Range<T>)") {
        sourceFile(SourceFile.Ranges)
        only(Primitives)
        only(numericPrimitives.filterNot { it.isIntegral() })
        returns("SELF")
        deprecate { forBinaryCompatibility } // force use generic overload instead
        doc {
            """
            Ensures that this value lies in the specified [range].

            @return this value if it's in the [range], or range.start if this value is less than range.start, or range.end if this value is greater than range.end.
            """
        }
        body {
            """
            if (range.isEmpty()) throw IllegalArgumentException("Cannot coerce value to an empty range: ${'$'}range.")
            return if (this < range.start) range.start else if (this > range.end) range.end else this
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

            @return this value if it's in the [range], or range.start if this value is less than range.start, or range.end if this value is greater than range.end.
            """
        }
        body {
            """
            if (range.isEmpty()) throw IllegalArgumentException("Cannot coerce value to an empty range: ${'$'}range.")
            return if (this < range.start) range.start else if (this > range.endInclusive) range.endInclusive else this
            """
        }
    }

    templates add f("coerceIn(minimumValue: SELF?, maximumValue: SELF?)") {
        sourceFile(SourceFile.Ranges)
        only(Primitives, Generic)
        only(numericPrimitives)
        returns("SELF")
        typeParam("T: Comparable<T>")
        doc {
            """
            Ensures that this value lies in the specified range [minimumValue]..[maximumValue].

            @return this value if it's in the range, or [minimumValue] if this value is less than [minimumValue], or [maximumValue] if this value is greater than [maximumValue].
            """
        }
        body {
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
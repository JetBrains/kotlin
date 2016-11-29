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
            if (range is ClosedComparableRange) {
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
            if (range is ClosedComparableRange) {
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

    templates add f("coerceIn(range: ClosedComparableRange<T>)") {
        sourceFile(SourceFile.Ranges)
        only(Generic)
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
                !range.lessThanOrEquals(range.start, this) -> range.start
                !range.lessThanOrEquals(this, range.endInclusive) -> range.endInclusive
                else -> this
            }
            """
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
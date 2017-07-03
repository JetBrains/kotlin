package templates

import templates.Family.*
import templates.PrimitiveType.Companion.maxByCapacity

fun ranges(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    val rangePrimitives = listOf(PrimitiveType.Int, PrimitiveType.Long, PrimitiveType.Char)
    fun rangeElementType(fromType: PrimitiveType, toType: PrimitiveType)
            = maxByCapacity(fromType, toType).let { if (it == PrimitiveType.Char) it else maxByCapacity(it, PrimitiveType.Int) }

    fun <T> Collection<T>.permutations(): List<Pair<T, T>> = flatMap { a -> map { b -> a to b } }

    templates add f("reversed()") {
        only(ProgressionsOfPrimitives)
        only(rangePrimitives)
        doc(ProgressionsOfPrimitives) { "Returns a progression that goes over the same range in the opposite direction with the same step." }
        returns("TProgression")
        body(ProgressionsOfPrimitives) {
            "return TProgression.fromClosedRange(last, first, -step)"
        }
    }

    templates add f("step(step: SUM)") {
        infix(true)

        only(ProgressionsOfPrimitives)
        only(rangePrimitives)
        doc(ProgressionsOfPrimitives) { "Returns a progression that goes over the same range with the given step." }
        returns("TProgression")
        body(ProgressionsOfPrimitives) {
            """
            checkStepIsPositive(step > 0, step)
            return TProgression.fromClosedRange(first, last, if (this.step > 0) step else -step)
            """
        }
    }

    fun downTo(fromType: PrimitiveType, toType: PrimitiveType) = f("downTo(to: $toType)") {
        infix(true)

        sourceFile(SourceFile.Ranges)
        only(Primitives)
        only(fromType)
        val elementType = rangeElementType(fromType, toType)
        val progressionType = elementType.name + "Progression"
        returns(progressionType)

        doc {
            """
            Returns a progression from this value down to the specified [to] value with the step -1.

            The [to] value has to be less than this value.
            """
        }

        val fromExpr = if (elementType == fromType) "this" else "this.to$elementType()"
        val toExpr = if (elementType == toType) "to" else "to.to$elementType()"
        val incrementExpr = when (elementType) {
            PrimitiveType.Long -> "-1L"
            PrimitiveType.Float -> "-1.0F"
            PrimitiveType.Double -> "-1.0"
            else -> "-1"
        }

        body { "return $progressionType.fromClosedRange($fromExpr, $toExpr, $incrementExpr)" }
    }

    val numericPrimitives = PrimitiveType.numericPrimitives
    val numericPermutations = numericPrimitives.permutations()
    val primitivePermutations = numericPermutations + (PrimitiveType.Char to PrimitiveType.Char)
    val integralPermutations = primitivePermutations.filter { it.first.isIntegral() && it.second.isIntegral() }

    templates addAll integralPermutations.map { downTo(it.first, it.second) }

    fun until(fromType: PrimitiveType, toType: PrimitiveType) = f("until(to: $toType)") {
        infix(true)

        sourceFile(SourceFile.Ranges)
        only(Primitives)
        only(fromType)
        val elementType = rangeElementType(fromType, toType)
        val progressionType = elementType.name + "Range"
        returns(progressionType)
        val minValue = if (elementType == PrimitiveType.Char) "'\\u0000'" else "$elementType.MIN_VALUE"

        doc {
            """
            Returns a range from this value up to but excluding the specified [to] value.

            ${textWhen(elementType == toType) {
                "If the [to] value is less than or equal to [$minValue] the returned range is empty."
            }}
            """
        }

        val fromExpr = if (elementType == fromType) "this" else "this.to$elementType()"

        if (elementType == toType) {
            // hack to work around incorrect char overflow behavior in JVM and int overflow behavior in JS
            val toExpr = when (toType) {
                PrimitiveType.Char -> "to.toInt()"
                PrimitiveType.Int -> "to.toLong()"
                else -> "to"
            }
            body {
                // <= instead of == for JS
                """
                if (to <= $minValue) return $progressionType.EMPTY
                return $fromExpr .. (to - 1).to$elementType()
                """
            }
        } else {
            body { "return $fromExpr .. (to.to$elementType() - 1).to$elementType()" }
        }
    }

    templates addAll integralPermutations.map { until(it.first, it.second) }

    fun contains(rangeType: PrimitiveType, itemType: PrimitiveType) = f("contains(value: $itemType)") {
        operator(true)

        check(rangeType.isNumeric() == itemType.isNumeric()) { "Required rangeType and itemType both to be numeric or both not, got: $rangeType, $itemType" }
        only(Ranges)
        onlyPrimitives(Ranges, rangeType)
        platformName("${rangeType.name.decapitalize()}RangeContains")
        returns("Boolean")
        doc { "Checks if the specified [value] belongs to this range." }
        body {
            if (rangeType.capacity > itemType.capacity || !rangeType.isIntegral())
                "return contains(value.to$rangeType())"
            else
                "return value.to${rangeType}ExactOrNull().let { if (it != null) contains(it) else false }"
        }
    }


    templates addAll numericPermutations.filter { it.first != it.second }.map { contains(it.first, it.second) }

    fun narrowingExactOrNull(fromType: PrimitiveType, toType: PrimitiveType) = f("to${toType}ExactOrNull()") {
        visibility("internal")
        sourceFile(SourceFile.Ranges)
        check(toType.isIntegral())

        returns("$toType?")
        only(Primitives)
        only(fromType)
        body {
            "return if (this in $toType.MIN_VALUE.to$fromType()..$toType.MAX_VALUE.to$fromType()) this.to$toType() else null"
        }
    }

    templates addAll numericPermutations.filter { it.first.capacity > it.second.capacity && it.second.isIntegral() }.map { narrowingExactOrNull(it.first, it.second) }

    return templates
}
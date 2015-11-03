package templates

import templates.Family.*
import templates.PrimitiveType.Companion.maxByCapacity

fun ranges(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    val rangePrimitives = listOf(PrimitiveType.Int, PrimitiveType.Long, PrimitiveType.Char)
    fun rangeElementType(fromType: PrimitiveType, toType: PrimitiveType)
            = maxByCapacity(fromType, toType).let { if (it == PrimitiveType.Char) it else maxByCapacity(it, PrimitiveType.Int) }


    templates add f("reversed()") {
        only(RangesOfPrimitives, ProgressionsOfPrimitives)
        only(rangePrimitives)
        doc(ProgressionsOfPrimitives) { "Returns a progression that goes over the same range in the opposite direction with the same step." }
        doc(RangesOfPrimitives) { "Returns a progression that goes over this range in reverse direction." }
        returns("TProgression")
        body(RangesOfPrimitives) {
            "return TProgression.fromClosedRange(last, first, -ONE)"
        }
        body(ProgressionsOfPrimitives) {
            "return TProgression.fromClosedRange(last, first, -increment)"
        }
    }

    templates add f("reversed()") {
        only(RangesOfPrimitives, ProgressionsOfPrimitives)
        only(defaultPrimitives - PrimitiveType.Boolean - rangePrimitives)
        doc(ProgressionsOfPrimitives) { "Returns a progression that goes over the same range in the opposite direction with the same step." }
        doc(RangesOfPrimitives) { "Returns a progression that goes over this range in reverse direction." }
        returns("TProgression")
        deprecate(Deprecation("This range implementation has unclear semantics and will be removed soon.", level = DeprecationLevel.WARNING))
        annotations("""@Suppress("DEPRECATION_ERROR")""")
        body(RangesOfPrimitives) {
            "return TProgression.fromClosedRange(last, first, -ONE)"
        }
        body(ProgressionsOfPrimitives) {
            "return TProgression.fromClosedRange(last, first, -increment)"
        }
    }

    templates add f("step(step: SUM)") {
        infix(true)

        only(RangesOfPrimitives, ProgressionsOfPrimitives)
        only(rangePrimitives)
        doc(ProgressionsOfPrimitives) { "Returns a progression that goes over the same range with the given step." }
        doc(RangesOfPrimitives) { "Returns a progression that goes over this range with given step." }
        returns("TProgression")
        body(RangesOfPrimitives) {
            """
            checkStepIsPositive(step > 0, step)
            return TProgression.fromClosedRange(first, last, step)
            """
        }
        body(ProgressionsOfPrimitives) {
            """
            checkStepIsPositive(step > 0, step)
            return TProgression.fromClosedRange(first, last, if (increment > 0) step else -step)
            """
        }
    }

    templates add f("step(step: SUM)") {
        infix(true)

        only(RangesOfPrimitives, ProgressionsOfPrimitives)
        only(defaultPrimitives - PrimitiveType.Boolean - rangePrimitives)
        doc(ProgressionsOfPrimitives) { "Returns a progression that goes over the same range with the given step." }
        doc(RangesOfPrimitives) { "Returns a progression that goes over this range with given step." }
        returns("TProgression")
        deprecate(Deprecation("This range implementation has unclear semantics and will be removed soon.", level = DeprecationLevel.WARNING))
        annotations("""@Suppress("DEPRECATION_ERROR")""")
        body(RangesOfPrimitives) {
            """
            checkStepIsPositive(step > 0, step)
            return TProgression.fromClosedRange(first, last, step)
            """
        }
        bodyForTypes(RangesOfPrimitives, PrimitiveType.Float, PrimitiveType.Double) {
            """
            if (step.isNaN()) throw IllegalArgumentException("Step must not be NaN.")
            checkStepIsPositive(step > 0, step)
            return TProgression.fromClosedRange(start, end, step)
            """
        }
        body(ProgressionsOfPrimitives) {
            """
            checkStepIsPositive(step > 0, step)
            return TProgression.fromClosedRange(first, last, if (increment > 0) step else -step)
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
            Returns a progression from this value down to the specified [to] value with the increment -1.
            The [to] value has to be less than this value.
            """
        }

        if (!fromType.isIntegral() || !toType.isIntegral()) {
            deprecate(Deprecation("This range implementation has unclear semantics and will be removed soon.", level = DeprecationLevel.WARNING))
            annotations("""@Suppress("DEPRECATION_ERROR")""")
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
    val numericPermutations = numericPrimitives.flatMap { fromType -> numericPrimitives.map { toType -> fromType to toType }}
    val primitivePermutations = numericPermutations + (PrimitiveType.Char to PrimitiveType.Char)

    templates addAll primitivePermutations.map { downTo(it.first, it.second) }

    fun until(fromType: PrimitiveType, toType: PrimitiveType) = f("until(to: $toType)") {
        infix(true)

        sourceFile(SourceFile.Ranges)
        only(Primitives)
        only(fromType)
        val elementType = rangeElementType(fromType, toType)
        val progressionType = elementType.name + "Range"
        returns(progressionType)

        doc {
            """
            Returns a range from this value up to but excluding the specified [to] value.
            ${ if (elementType == toType) "The [to] value must be greater than [$elementType.MIN_VALUE]." else "" }
            """
        }

        if (!fromType.isIntegral() || !toType.isIntegral()) {
            deprecate(Deprecation("This range implementation has unclear semantics and will be removed soon.", level = DeprecationLevel.WARNING))
            annotations("""@Suppress("DEPRECATION_ERROR")""")
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
                """
                val to_  = ($toExpr - 1).to$elementType()
                if (to_ > to) throw IllegalArgumentException("The to argument value '${'$'}to' was too small.")
                return $fromExpr .. to_
                """
            }
        } else {
            body { "return $fromExpr .. (to.to$elementType() - 1).to$elementType()" }
        }
    }

    templates addAll primitivePermutations
            .filter { it.first.isIntegral() && it.second.isIntegral() }
            .map { until(it.first, it.second) }

    fun contains(rangeType: PrimitiveType, itemType: PrimitiveType) = f("contains(item: $itemType)") {
        operator(true)

        if (!rangeType.isIntegral()) {
            deprecate(Deprecation("This range implementation has unclear semantics and will be removed soon.", level = DeprecationLevel.WARNING))
            annotations("""@Suppress("DEPRECATION_ERROR")""")
        }
        else if (rangeType !in rangePrimitives) {
            deprecate(Deprecation("This range will be removed soon.", level = DeprecationLevel.WARNING))
        }

        check(rangeType.isNumeric() == itemType.isNumeric()) { "Required rangeType and itemType both to be numeric or both not, got: $rangeType, $itemType" }
        only(Ranges)
        onlyPrimitives(Ranges, rangeType)
        platformName("${rangeType.name.decapitalize()}RangeContains")
        returns("Boolean")
        doc { "Checks if the specified [item] belongs to this range." }
        body { "return start <= item && item <= end" }
    }


    templates addAll numericPermutations.filter { it.first != it.second }.map { contains(it.first, it.second) }

    return templates
}
package templates

import generators.SourceFile
import templates.Family.*

fun ranges(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("reversed()") {
        only(RangesOfPrimitives, ProgressionsOfPrimitives)
        exclude(PrimitiveType.Boolean)
        doc(ProgressionsOfPrimitives) { "Returns a progression that goes over the same range in the opposite direction with the same step." }
        doc(RangesOfPrimitives) { "Returns a progression that goes over this range in reverse direction." }
        returns("TProgression")
        body(RangesOfPrimitives) {
            "return TProgression(end, start, -ONE)"
        }
        body(ProgressionsOfPrimitives) {
            "return TProgression(end, start, -increment)"
        }
    }

    templates add f("step(step: SUM)") {
        only(RangesOfPrimitives, ProgressionsOfPrimitives)
        exclude(PrimitiveType.Boolean)
        doc(ProgressionsOfPrimitives) { "Returns a progression that goes over the same range with the given step." }
        doc(RangesOfPrimitives) { "Returns a progression that goes over this range with given step." }
        returns("TProgression")
        body(RangesOfPrimitives) {
            """
            checkStepIsPositive(step > 0, step)
            return TProgression(start, end, step)
            """
        }
        bodyForTypes(RangesOfPrimitives, PrimitiveType.Float, PrimitiveType.Double) {
            """
            if (step.isNaN()) throw IllegalArgumentException("Step must not be NaN.")
            checkStepIsPositive(step > 0, step)
            return TProgression(start, end, step)
            """
        }
        body(ProgressionsOfPrimitives) {
            """
            checkStepIsPositive(step > 0, step)
            return TProgression(start, end, if (increment > 0) step else -step)
            """
        }
    }

    fun downTo(fromType: PrimitiveType, toType: PrimitiveType) = f("downTo(to: $toType)") {
        sourceFile(SourceFile.Ranges)
        only(Primitives)
        only(fromType)
        val elementType = PrimitiveType.maxByCapacity(fromType, toType)
        val progressionType = elementType.name + "Progression"
        returns(progressionType)

        doc {
            """
            Returns a progression from this value down to the specified [to] value with the increment -1.
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

        body { "return $progressionType($fromExpr, $toExpr, $incrementExpr)" }
    }

    val numericPrimitives = PrimitiveType.numericPrimitives
    val numericPermutations = numericPrimitives flatMap { fromType -> numericPrimitives map { toType -> fromType to toType }}
    val primitivePermutations = numericPermutations + (PrimitiveType.Char to PrimitiveType.Char)

    templates addAll primitivePermutations.map { downTo(it.first, it.second) }

    fun until(fromType: PrimitiveType, toType: PrimitiveType) = f("until(to: $toType)") {
        sourceFile(SourceFile.Ranges)
        only(Primitives)
        only(fromType)
        val elementType = PrimitiveType.maxByCapacity(fromType, toType)
        val progressionType = elementType.name + "Range"
        returns(progressionType)

        doc {
            """
            Returns a range from this value up to but excluding the specified [to] value.
            ${ if (elementType == toType) "The [to] value must be greater than [$elementType.MIN_VALUE]." else "" }
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

        val meaningless = (rangeType.isNumeric() != itemType.isNumeric())
        if (!meaningless) {
            only(Ranges)
            onlyPrimitives(Ranges, rangeType)
            platformName("${rangeType.name.decapitalize()}RangeContains")
            returns("Boolean")
            doc { "Checks if the specified [item] belongs to this range." }
            body { "return start <= item && item <= end" }
        }
        else {
            only(RangesOfPrimitives)
            only(rangeType)
            returns("Nothing")
            annotations("""@Deprecated("The 'contains' operation for a range of $rangeType and $itemType item is not supported and should not be used.")""")
            body { """throw UnsupportedOperationException()""" }
        }
    }

    val allPermutations = (numericPrimitives + PrimitiveType.Char).let { it.flatMap { from -> it.map { to -> from to to } }}

    templates addAll allPermutations.filter { it.first != it.second }.map { contains(it.first, it.second) }

    return templates
}
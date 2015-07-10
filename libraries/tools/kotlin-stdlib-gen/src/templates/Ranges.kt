package templates

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
        only(Primitives)
        only(fromType)
        val elementType = PrimitiveType.getMaxCapacityType(fromType, toType)
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

    templates addAll PrimitiveType.primitivePermutations.map { downTo(it.first, it.second) }

    return templates
}
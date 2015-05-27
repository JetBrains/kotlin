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

    return templates
}

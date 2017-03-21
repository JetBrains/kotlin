package templates

import templates.Family.*

fun numeric(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("sum()") {
        exclude(Strings)
        buildFamilies.default!!.forEach { family -> onlyPrimitives(family, numericPrimitives) }
        doc { f -> "Returns the sum of all elements in the ${f.collection}." }
        returns("SUM")
        platformName("sumOf<T>")
        body {
            """
            var sum: SUM = ZERO
            for (element in this) {
                sum += element
            }
            return sum
            """
        }
    }

    templates add f("average()") {
        exclude(Strings)
        buildFamilies.default!!.forEach { family -> onlyPrimitives(family, numericPrimitives) }
        doc { f -> "Returns an average value of elements in the ${f.collection}."}
        returns("Double")
        platformName("averageOf<T>")
        body {
            """
            var sum: Double = 0.0
            var count: Int = 0
            for (element in this) {
                sum += element
                count += 1
            }
            return if (count == 0) Double.NaN else sum / count
            """
        }
    }

    templates.forEach { it.sequenceClassification(SequenceClass.terminal) }

    return templates
}

package templates

import templates.Family.*

fun numeric(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("sum()") {
        exclude(Strings)
        doc { "Returns the sum of all elements in the collection." }
        returns("SUM")
        body {
            """
            val iterator = iterator()
            var sum: SUM = ZERO
            while (iterator.hasNext()) {
                sum += iterator.next()
            }
            return sum
            """
        }
    }

    return templates
}

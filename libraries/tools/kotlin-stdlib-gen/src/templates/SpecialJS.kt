package templates

import templates.Family.*

fun specialJS(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("asList()") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns a list that wraps the original array" }
        returns("List<T>")
        body(ArraysOfObjects) {
            """
            val al = ArrayList<T>()
            (al: dynamic).array = this    // black dynamic magic
            return al
            """
        }

        inline(true, ArraysOfPrimitives)
        body(ArraysOfPrimitives) {"""return (this as Array<T>).asList()"""}
    }

    return templates
}
package templates

import templates.Family.*

fun arrays(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("isEmpty()") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns true if the array is empty" }
        returns("Boolean")
        body {
            "return size() == 0"
        }
    }

    templates add f("isNotEmpty()") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns true if the array is not empty" }
        returns("Boolean")
        body {
            "return !isEmpty()"
        }
    }

    return templates
}
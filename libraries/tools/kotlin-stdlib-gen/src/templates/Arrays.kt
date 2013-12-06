package templates

import templates.Family.*

fun arrays(): List<GenericFunction> {
    val templates = iterables()

    templates add f("isEmpty()") {
        absentFor(Arrays)
        isInline = false
        doc = "Returns true if the array is empty"
        returns("Boolean")
        body {
            "return size == 0"
        }
    }

    templates add f("isNotEmpty()") {
        absentFor(Arrays)
        isInline = false
        doc = "Returns true if the array is empty"
        returns("Boolean")
        body {
            "return !isEmpty()"
        }
    }

    return templates.sort()
}
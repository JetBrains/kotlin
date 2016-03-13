package templates

import templates.Family.*

fun guards(): List<GenericFunction> {
    val THIS = "\$this"

    val templates = arrayListOf<GenericFunction>()

    templates add f("requireNoNulls()") {
        only(Iterables, Sequences, InvariantArraysOfObjects, Lists)
        doc { "Returns an original collection containing all the non-`null` elements, throwing an [IllegalArgumentException] if there are any `null` elements." }
        typeParam("T : Any")
        toNullableT = true
        returns("SELF")
        body { f ->
            """
            for (element in this) {
                if (element == null) {
                    throw IllegalArgumentException("null element found in $THIS.")
                }
            }
            @Suppress("${if (f == InvariantArraysOfObjects) "CAST_NEVER_SUCCEEDS" else "UNCHECKED_CAST"}")
            return this as SELF
            """
        }
        body(Sequences) {
            """
            return map { it ?: throw IllegalArgumentException("null element found in $THIS.") }
            """
        }
    }

    return templates
}
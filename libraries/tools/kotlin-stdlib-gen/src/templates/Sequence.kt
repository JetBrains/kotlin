package templates

import templates.Family.*

fun sequences(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("sequence()") {
        include(Maps)
        exclude(Sequences)
        deprecate { "Use asSequence() instead" }
        deprecateReplacement { "asSequence()" }
        doc { "Returns a sequence from the given collection" }
        returns("Sequence<T>")
        body {
            """
            return asSequence()
            """
        }

        body(Sequences) {
            """
            return this
            """
        }
    }
    templates add f("asSequence()") {
        include(Maps)
        doc { "Returns a sequence from the given collection." }
        returns("Sequence<T>")
        body {
            """
            return object : Sequence<T> {
                override fun iterator(): Iterator<T> {
                    return this@asSequence.iterator()
                }
            }
            """
        }

        body(ArraysOfObjects, ArraysOfPrimitives, Strings) {
            """
            if (isEmpty()) return emptySequence()
            return object : Sequence<T> {
                override fun iterator(): Iterator<T> {
                    return this@asSequence.iterator()
                }
            }
            """
        }

        body(Sequences) {
            """
            return this
            """
        }
    }

    return templates
}


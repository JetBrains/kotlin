package templates

import templates.Family.*

fun sequences(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("stream()") {
        include(Maps)
        exclude(Sequences)
        deprecate { "Use sequence() instead" }
        doc { "Returns a sequence from the given collection" }
        returns("Stream<T>")
        body {
            """
            val sequence = sequence()
            return object : Stream<T> {
                override fun iterator(): Iterator<T> {
                    return sequence.iterator()
                }
            }
            """
        }

    }

    templates add f("sequence()") {
        include(Maps)
        exclude(Sequences)
        doc { "Returns a sequence from the given collection" }
        returns("Sequence<T>")
        body {
            """
            return object : Sequence<T> {
                override fun iterator(): Iterator<T> {
                    return this@sequence.iterator()
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
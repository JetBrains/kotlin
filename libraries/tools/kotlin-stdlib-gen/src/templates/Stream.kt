package templates

import templates.Family.*

fun streams(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("stream()") {
        include(Maps)
        doc { "Returns a stream from the given collection" }
        returns("Stream<T>")
        body {
            """
            return object : Stream<T> {
                override fun iterator(): Iterator<T> {
                    return this@stream.iterator()
                }
            }
            """
        }

        body(Streams) {
            """
            return this
            """
        }
    }

    return templates
}
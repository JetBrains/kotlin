package templates

import templates.Family.*

fun sequences(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("asIterable()") {
        only(Iterables, ArraysOfObjects, ArraysOfPrimitives, Sequences, CharSequences, Maps)
        doc { f -> "Creates an [Iterable] instance that wraps the original ${f.collection} returning its ${f.element.pluralize()} when being iterated." }
        returns("Iterable<T>")
        body { f ->
            """
            ${ when(f) {
                ArraysOfObjects, ArraysOfPrimitives -> "if (isEmpty()) return emptyList()"
                CharSequences -> "if (this is String && isEmpty()) return emptyList()"
                else -> ""
            }}
            return Iterable { this.iterator() }
            """
        }

        inline(Iterables, Maps) { Inline.Only }

        doc(Iterables) { "Returns this collection as an [Iterable]." }
        body(Iterables) { "return this" }
        body(Maps) { "return entries" }
    }

    templates add f("asSequence()") {
        include(CharSequences, Maps)
        doc { f ->
            """
            Creates a [Sequence] instance that wraps the original ${f.collection} returning its ${f.element.pluralize()} when being iterated.

            ${if (f in listOf(ArraysOfPrimitives, ArraysOfObjects, Iterables)) "@sample samples.collections.Sequences.Building.sequenceFrom${f.doc.collection.capitalize()}" else ""}
            """
        }
        returns("Sequence<T>")
        body { f ->
            """
            ${ when(f) {
                ArraysOfObjects, ArraysOfPrimitives -> "if (isEmpty()) return emptySequence()"
                CharSequences -> "if (this is String && isEmpty()) return emptySequence()"
                else -> ""
            }}
            return Sequence { this.iterator() }
            """
        }

        body(Maps) { "return entries.asSequence()" }

        doc(Sequences) { "Returns this sequence as a [Sequence]."}
        inline(Sequences) { Inline.Only }
        body(Sequences) { "return this" }
    }

    return templates
}


package templates

import templates.Family.*
import templates.DocExtensions.element
import templates.DocExtensions.collection
import templates.DocExtensions.mapResult

fun mapping(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("withIndex()") {
        deprecate(Strings) { forBinaryCompatibility }
        include(CharSequences, Strings)
        doc {  f -> "Returns a ${if (f == Sequences) f.mapResult else "lazy [Iterable]"} of [IndexedValue] for each ${f.element} of the original ${f.collection}." }
        returns("Iterable<IndexedValue<T>>")
        body {
            """
            return IndexingIterable { iterator() }
            """
        }

        returns(Sequences) { "Sequence<IndexedValue<T>>" }
        body(Sequences) {
            """
            return IndexingSequence(this)
            """
        }
    }

    templates add f("mapIndexed(transform: (Int, T) -> R)") {
        inline(true)

        doc { f ->
            """
            Returns a ${f.mapResult} containing the results of applying the given [transform] function
            to each ${f.element} and its index in the original ${f.collection}.
            """
        }
        typeParam("R")
        returns("List<R>")
        body {
            "return mapIndexedTo(ArrayList<R>(collectionSizeOrDefault(10)), transform)"
        }
        body(ArraysOfObjects, ArraysOfPrimitives) {
            "return mapIndexedTo(ArrayList<R>(size()), transform)"
        }
        deprecate(Strings) { forBinaryCompatibility }
        body(CharSequences, Strings) {
            "return mapIndexedTo(ArrayList<R>(length()), transform)"
        }
        inline(false, Sequences)
        returns(Sequences) { "Sequence<R>" }
        body(Sequences) {
            "return TransformingIndexedSequence(this, transform)"
        }
    }

    templates add f("map(transform: (T) -> R)") {
        inline(true)

        doc { f ->
            """
            Returns a ${f.mapResult} containing the results of applying the given [transform] function
            to each ${f.element} in the original ${f.collection}.
            """
        }
        typeParam("R")
        returns("List<R>")
        body {
            "return mapTo(ArrayList<R>(collectionSizeOrDefault(10)), transform)"
        }
        body(ArraysOfObjects, ArraysOfPrimitives, Maps) {
            "return mapTo(ArrayList<R>(size()), transform)"
        }
        deprecate(Strings) { forBinaryCompatibility }
        body(CharSequences, Strings) {
            "return mapTo(ArrayList<R>(length()), transform)"
        }

        inline(false, Sequences)
        returns(Sequences) { "Sequence<R>" }
        body(Sequences) {
            "return TransformingSequence(this, transform)"
        }
        include(Maps)
    }

    templates add f("mapNotNull(transform: (T) -> R?)") {
        inline(true)
        include(Maps, CharSequences)
        exclude(ArraysOfPrimitives)
        typeParam("R : Any")
        returns("List<R>")
        doc { f ->
            """
            Returns a ${f.mapResult} containing only the non-null results of applying the given [transform] function
            to each ${f.element} in the original ${f.collection}.
            """
        }
        body {
            "return mapNotNullTo(ArrayList<R>(), transform)"
        }

        inline(false, Sequences)
        returns(Sequences) { "Sequence<R>" }
        body(Sequences) {
            "return TransformingSequence(this, transform).filterNotNull()"
        }

    }

    templates add f("mapIndexedNotNull(transform: (Int, T) -> R?)") {
        inline(true)
        include(CharSequences)
        exclude(ArraysOfPrimitives)
        typeParam("R : Any")
        returns("List<R>")
        doc { f ->
            """
            Returns a ${f.mapResult} containing only the non-null results of applying the given [transform] function
            to each ${f.element} and its index in the original ${f.collection}.
            """
        }
        body {
            "return mapIndexedNotNullTo(ArrayList<R>(), transform)"
        }

        inline(false, Sequences)
        returns(Sequences) { "Sequence<R>" }
        body(Sequences) {
            "return TransformingIndexedSequence(this, transform).filterNotNull()"
        }
    }

    templates add f("mapTo(destination: C, transform: (T) -> R)") {
        inline(true)

        doc { f ->
            """
            Applies the given [transform] function to each ${f.element} of the original ${f.collection}
            and appends the results to the given [destination].
            """
        }
        typeParam("R")
        typeParam("C : MutableCollection<in R>")
        returns("C")

        body {
            """
                for (item in this)
                    destination.add(transform(item))
                return destination
            """
        }
        deprecate(Strings) { forBinaryCompatibility }
        include(Maps, CharSequences, Strings)
    }

    templates add f("mapIndexedTo(destination: C, transform: (Int, T) -> R)") {
        inline(true)

        doc { f ->
            """
            Applies the given [transform] function to each ${f.element} and its index in the original ${f.collection}
            and appends the results to the given [destination].
            """
        }
        typeParam("R")
        typeParam("C : MutableCollection<in R>")
        returns("C")

        body {
            """
                var index = 0
                for (item in this)
                    destination.add(transform(index++, item))
                return destination
            """
        }
        deprecate(Strings) { forBinaryCompatibility }
        deprecate(Maps) { Deprecation("Use entries.mapIndexedTo instead.", replaceWith = "this.entries.mapIndexedTo(destination, transform)") }
        include(Maps, CharSequences, Strings)
    }

    templates add f("mapNotNullTo(destination: C, transform: (T) -> R?)") {
        inline(true)
        include(Maps, CharSequences)
        exclude(ArraysOfPrimitives)
        typeParam("R : Any")
        typeParam("C : MutableCollection<in R>")
        returns("C")
        doc { f ->
            """
            Applies the given [transform] function to each ${f.element} in the original ${f.collection}
            and appends only the non-null results to the given [destination].
            """
        }
        body {
            """
            forEach { element -> transform(element)?.let { destination.add(it) } }
            return destination
            """
        }
    }

    templates add f("mapIndexedNotNullTo(destination: C, transform: (Int, T) -> R?)") {
        inline(true)
        include(CharSequences)
        exclude(ArraysOfPrimitives)
        typeParam("R : Any")
        typeParam("C : MutableCollection<in R>")
        returns("C")
        doc { f ->
            """
            Applies the given [transform] function to each ${f.element} and its index in the original ${f.collection}
            and appends only the non-null results to the given [destination].
            """
        }
        body {
            """
            forEachIndexed { index, element -> transform(index, element)?.let { destination.add(it) } }
            return destination
            """
        }
    }

    templates add f("flatMap(transform: (T) -> Iterable<R>)") {
        inline(true)

        exclude(Sequences)
        doc { f -> "Returns a single list of all elements yielded from results of [transform] function being invoked on each ${f.element} of original ${f.collection}." }
        typeParam("R")
        returns("List<R>")
        body {
            "return flatMapTo(ArrayList<R>(), transform)"
        }
        deprecate(Strings) { forBinaryCompatibility }
        include(Maps, CharSequences, Strings)
    }

    templates add f("flatMap(transform: (T) -> Sequence<R>)") {
        only(Sequences)
        doc { "Returns a single sequence of all elements from results of [transform] function being invoked on each element of original sequence." }
        typeParam("R")
        returns("Sequence<R>")
        body {
            "return FlatteningSequence(this, transform)"
        }
    }

    templates add f("flatMapTo(destination: C, transform: (T) -> Iterable<R>)") {
        inline(true)
        exclude(Sequences)
        doc { f -> "Appends all elements yielded from results of [transform] function being invoked on each ${f.element} of original ${f.collection}, to the given [destination]." }
        typeParam("R")
        typeParam("C : MutableCollection<in R>")
        returns("C")
        body {
            """
                for (element in this) {
                    val list = transform(element)
                    destination.addAll(list)
                }
                return destination
            """
        }
        deprecate(Strings) { forBinaryCompatibility }
        include(Maps, CharSequences, Strings)
    }

    templates add f("flatMapTo(destination: C, transform: (T) -> Sequence<R>)") {
        inline(true)

        only(Sequences)
        doc { "Appends all elements yielded from results of [transform] function being invoked on each element of original sequence, to the given [destination]." }
        typeParam("R")
        typeParam("C : MutableCollection<in R>")
        returns("C")
        body {
            """
                for (element in this) {
                    val list = transform(element)
                    destination.addAll(list)
                }
                return destination
            """
        }
    }

    templates add f("groupBy(toKey: (T) -> K)") {
        inline(true)

        deprecate(Strings) { forBinaryCompatibility }
        include(CharSequences, Strings)
        doc { f -> "Returns a map of the ${f.element}s in original ${f.collection} grouped by the result of given [toKey] function." }
        typeParam("K")
        returns("Map<K, List<T>>")
        body { "return groupByTo(LinkedHashMap<K, MutableList<T>>(), toKey)" }
    }

    templates add f("groupByTo(map: MutableMap<K, MutableList<T>>, toKey: (T) -> K)") {
        inline(true)

        deprecate(Strings) { forBinaryCompatibility }
        include(CharSequences, Strings)
        typeParam("K")
        doc { f -> "Appends ${f.element}s from original ${f.collection} grouped by the result of given [toKey] function to the given [map]." }
        returns("Map<K, MutableList<T>>")
        body {
            """
                for (element in this) {
                    val key = toKey(element)
                    val list = map.getOrPut(key) { ArrayList<T>() }
                    list.add(element)
                }
                return map
            """
        }
    }
    return templates
}
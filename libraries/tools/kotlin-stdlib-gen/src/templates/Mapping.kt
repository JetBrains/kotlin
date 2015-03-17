package templates

import templates.Family.*

fun mapping(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("withIndices()") {
        deprecate { "Use withIndex() instead." }
        doc { "Returns a list containing pairs of each element of the original collection and their index" }
        returns("List<Pair<Int, T>>")
        body {
            """
            var index = 0
            return mapTo(ArrayList<Pair<Int, T>>(), { index++ to it })
            """
        }

        returns(Sequences) { "Sequence<Pair<Int, T>>" }
        doc(Sequences) { "Returns a sequence containing pairs of each element of the original collection and their index" }
        body(Sequences) {
            """
            var index = 0
            return TransformingSequence(this, { index++ to it })
            """
        }
    }

    templates add f("withIndex()") {
        doc { "Returns a lazy [Iterable] of [IndexedValue] for each element of the original collection" }
        returns("Iterable<IndexedValue<T>>")
        body {
            """
            return IndexingIterable { iterator() }
            """
        }

        returns(Sequences) { "Sequence<IndexedValue<T>>" }
        doc(Sequences) { "Returns a sequence of [IndexedValue] for each element of the original sequence" }
        body(Sequences) {
            """
            return IndexingSequence(this)
            """
        }
    }

    templates add f("mapIndexed(transform: (Int, T) -> R)") {
        inline(true)

        doc { "Returns a list containing the results of applying the given *transform* function to each element and its index of the original collection" }
        typeParam("R")
        returns("List<R>")
        body {
            "return mapIndexedTo(ArrayList<R>(collectionSizeOrDefault(10)), transform)"
        }
        body(ArraysOfObjects, ArraysOfPrimitives) {
            "return mapIndexedTo(ArrayList<R>(size()), transform)"
        }
        body(Strings) {
            "return mapIndexedTo(ArrayList<R>(length()), transform)"
        }
        inline(false, Sequences)
        returns(Sequences) { "Sequence<R>" }
        doc(Sequences) { "Returns a sequence containing the results of applying the given *transform* function to each element and its index of the original sequence" }
        body(Sequences) {
            "return TransformingIndexedSequence(this, transform)"
        }
    }

    templates add f("map(transform: (T) -> R)") {
        inline(true)

        doc { "Returns a list containing the results of applying the given *transform* function to each element of the original collection" }
        typeParam("R")
        returns("List<R>")
        body {
            "return mapTo(ArrayList<R>(), transform)"
        }

        inline(false, Sequences)
        returns(Sequences) { "Sequence<R>" }
        doc(Sequences) { "Returns a sequence containing the results of applying the given *transform* function to each element of the original sequence" }
        body(Sequences) {
            "return TransformingSequence(this, transform)"
        }
        include(Maps)
    }

    templates add f("mapNotNull(transform: (T) -> R)") {
        inline(true)
        exclude(Strings, ArraysOfPrimitives)
        doc { "Returns a list containing the results of applying the given *transform* function to each non-null element of the original collection" }
        typeParam("T : Any")
        typeParam("R")
        returns("List<R>")
        toNullableT = true
        body {
            """
            return mapNotNullTo(ArrayList<R>(), transform)
            """
        }

        doc(Sequences) { "Returns a sequence containing the results of applying the given *transform* function to each non-null element of the original sequence" }
        returns(Sequences) { "Sequence<R>" }
        inline(false, Sequences)
        body(Sequences) {
            """
            return TransformingSequence(FilteringSequence(this, false, { it == null }) as Sequence<T>, transform)
            """
        }
    }

    templates add f("mapTo(destination: C, transform: (T) -> R)") {
        inline(true)

        doc {
            """
            Appends transformed elements of the original collection using the given *transform* function
            to the given *destination*
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
        include(Maps)
    }

    templates add f("mapIndexedTo(destination: C, transform: (Int, T) -> R)") {
        inline(true)

        doc {
            """
            Appends transformed elements and their indices of the original collection using the given *transform* function
            to the given *destination*
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
        include(Maps)
    }

    templates add f("mapNotNullTo(destination: C, transform: (T) -> R)") {
        inline(true)
        exclude(Strings, ArraysOfPrimitives)
        doc {
            """
            Appends transformed non-null elements of original collection using the given *transform* function
            to the given *destination*
            """
        }
        typeParam("T : Any")
        typeParam("R")
        typeParam("C : MutableCollection<in R>")
        returns("C")
        toNullableT = true
        body {
            """
            for (element in this) {
                if (element != null) {
                    destination.add(transform(element))
                }
            }
            return destination
            """
        }
    }

    templates add f("flatMap(transform: (T) -> Iterable<R>)") {
        inline(true)

        exclude(Sequences)
        doc { "Returns a single list of all elements yielded from results of *transform* function being invoked on each element of original collection" }
        typeParam("R")
        returns("List<R>")
        body {
            "return flatMapTo(ArrayList<R>(), transform)"
        }
        include(Maps)
    }

    templates add f("flatMap(transform: (T) -> Sequence<R>)") {
        only(Sequences)
        doc { "Returns a single sequence of all elements from results of *transform* function being invoked on each element of original sequence" }
        typeParam("R")
        returns("Sequence<R>")
        body {
            "return FlatteningSequence(this, transform)"
        }
    }

    templates add f("flatMapTo(destination: C, transform: (T) -> Iterable<R>)") {
        inline(true)
        exclude(Sequences)
        doc { "Appends all elements yielded from results of *transform* function being invoked on each element of original collection, to the given *destination*" }
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
        include(Maps)
    }

    templates add f("flatMapTo(destination: C, transform: (T) -> Sequence<R>)") {
        inline(true)

        only(Sequences)
        doc { "Appends all elements yielded from results of *transform* function being invoked on each element of original sequence, to the given *destination*" }
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

        doc { "Returns a map of the elements in original collection grouped by the result of given *toKey* function" }
        typeParam("K")
        returns("Map<K, List<T>>")
        body { "return groupByTo(LinkedHashMap<K, MutableList<T>>(), toKey)" }
    }

    templates add f("groupByTo(map: MutableMap<K, MutableList<T>>, toKey: (T) -> K)") {
        inline(true)

        typeParam("K")
        doc { "Appends elements from original collection grouped by the result of given *toKey* function to the given *map*" }
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
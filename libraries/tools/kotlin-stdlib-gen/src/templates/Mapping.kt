package templates

import templates.Family.*

fun mapping(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("withIndices()") {
        doc { "Returns a list containing pairs of each element of the original collection and their index" }
        returns("List<Pair<Int, T>>")
        body {
            """
            var index = 0
            return mapTo(ArrayList<Pair<Int, T>>(), { index++ to it })
            """
        }

        returns(Streams) { "Stream<Pair<Int, T>>" }
        doc(Streams) { "Returns a stream containing pairs of each element of the original collection and their index" }
        body(Streams) {
            """
            var index = 0
            return TransformingStream(this, { index++ to it })
            """
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

        inline(false, Streams)
        returns(Streams) { "Stream<R>" }
        doc(Streams) { "Returns a stream containing the results of applying the given *transform* function to each element of the original stream" }
        body(Streams) {
            "return TransformingStream(this, transform)"
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

        doc(Streams) { "Returns a stream containing the results of applying the given *transform* function to each non-null element of the original stream" }
        returns(Streams) { "Stream<R>" }
        inline(false, Streams)
        body(Streams) {
            """
            return TransformingStream(FilteringStream(this, false, { it == null }) as Stream<T>, transform)
            """
        }
    }

    templates add f("mapTo(destination: C, transform: (T) -> R)") {
        inline(true)

        doc {
            """
            Appends transformed elements of original collection using the given *transform* function
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

        exclude(Streams)
        doc { "Returns a single list of all elements yielded from results of *transform* function being invoked on each element of original collection" }
        typeParam("R")
        returns("List<R>")
        body {
            "return flatMapTo(ArrayList<R>(), transform)"
        }
        include(Maps)
    }

    templates add f("flatMap(transform: (T) -> Stream<R>)") {
        only(Streams)
        doc { "Returns a single stream of all elements streamed from results of *transform* function being invoked on each element of original stream" }
        typeParam("R")
        returns("Stream<R>")
        body {
            "return FlatteningStream(this, transform)"
        }
    }

    templates add f("flatMapTo(destination: C, transform: (T) -> Iterable<R>)") {
        inline(true)
        exclude(Streams)
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

    templates add f("flatMapTo(destination: C, transform: (T) -> Stream<R>)") {
        inline(true)

        only(Streams)
        doc { "Appends all elements yielded from results of *transform* function being invoked on each element of original stream, to the given *destination*" }
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
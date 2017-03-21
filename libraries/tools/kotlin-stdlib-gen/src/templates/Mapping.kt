package templates

import templates.Family.*
import templates.SequenceClass.*

fun mapping(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("withIndex()") {
        include(CharSequences)
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

    templates add f("mapIndexed(transform: (index: Int, T) -> R)") {
        inline(true)

        doc { f ->
            """
            Returns a ${f.mapResult} containing the results of applying the given [transform] function
            to each ${f.element} and its index in the original ${f.collection}.
            @param [transform] function that takes the index of ${f.element.prefixWithArticle()} and the ${f.element} itself
            and returns the result of the transform applied to the ${f.element}.
            """
        }
        typeParam("R")
        returns("List<R>")
        annotations(Iterables) { """@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")""" }
        body(Iterables) {
            "return mapIndexedTo(ArrayList<R>(collectionSizeOrDefault(10)), transform)"
        }
        body(ArraysOfObjects, ArraysOfPrimitives) {
            "return mapIndexedTo(ArrayList<R>(size), transform)"
        }
        body(CharSequences) {
            "return mapIndexedTo(ArrayList<R>(length), transform)"
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
        annotations(Iterables) { """@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")""" }
        body(Iterables) {
            "return mapTo(ArrayList<R>(collectionSizeOrDefault(10)), transform)"
        }
        body(ArraysOfObjects, ArraysOfPrimitives, Maps) {
            "return mapTo(ArrayList<R>(size), transform)"
        }
        body(CharSequences) {
            "return mapTo(ArrayList<R>(length), transform)"
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

    templates add f("mapIndexedNotNull(transform: (index: Int, T) -> R?)") {
        inline(true)
        include(CharSequences)
        exclude(ArraysOfPrimitives)
        typeParam("R : Any")
        returns("List<R>")
        doc { f ->
            """
            Returns a ${f.mapResult} containing only the non-null results of applying the given [transform] function
            to each ${f.element} and its index in the original ${f.collection}.
            @param [transform] function that takes the index of ${f.element.prefixWithArticle()} and the ${f.element} itself
            and returns the result of the transform applied to the ${f.element}.
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
        include(Maps, CharSequences)
    }

    templates add f("mapIndexedTo(destination: C, transform: (index: Int, T) -> R)") {
        inline(true)

        doc { f ->
            """
            Applies the given [transform] function to each ${f.element} and its index in the original ${f.collection}
            and appends the results to the given [destination].
            @param [transform] function that takes the index of ${f.element.prefixWithArticle()} and the ${f.element} itself
            and returns the result of the transform applied to the ${f.element}.
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
        include(CharSequences)
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

    templates add f("mapIndexedNotNullTo(destination: C, transform: (index: Int, T) -> R?)") {
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
            @param [transform] function that takes the index of ${f.element.prefixWithArticle()} and the ${f.element} itself
            and returns the result of the transform applied to the ${f.element}.
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
        include(Maps, CharSequences)
    }

    templates add f("flatMap(transform: (T) -> Sequence<R>)") {
        only(Sequences)
        doc { "Returns a single sequence of all elements from results of [transform] function being invoked on each element of original sequence." }
        typeParam("R")
        returns("Sequence<R>")
        body {
            "return FlatteningSequence(this, transform, { it.iterator() })"
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
        include(Maps, CharSequences)
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

    templates add f("groupBy(keySelector: (T) -> K)") {
        inline(true)

        include(CharSequences)
        doc { f ->
            """
            Groups ${f.element.pluralize()} of the original ${f.collection} by the key returned by the given [keySelector] function
            applied to each ${f.element} and returns a map where each group key is associated with a list of corresponding ${f.element.pluralize()}.

            The returned map preserves the entry iteration order of the keys produced from the original ${f.collection}.

            @sample samples.collections.Collections.Transformations.groupBy
            """
        }
        sequenceClassification(terminal, stateful)
        typeParam("K")
        returns("Map<K, List<T>>")
        body { "return groupByTo(LinkedHashMap<K, MutableList<T>>(), keySelector)" }
    }

    templates add f("groupByTo(destination: M, keySelector: (T) -> K)") {
        inline(true)

        include(CharSequences)
        typeParam("K")
        typeParam("M : MutableMap<in K, MutableList<T>>")
        doc { f ->
            """
            Groups ${f.element.pluralize()} of the original ${f.collection} by the key returned by the given [keySelector] function
            applied to each ${f.element} and puts to the [destination] map each group key associated with a list of corresponding ${f.element.pluralize()}.

            @return The [destination] map.

            @sample samples.collections.Collections.Transformations.groupBy
            """
        }
        sequenceClassification(terminal, stateful)
        returns("M")
        body {
            """
                for (element in this) {
                    val key = keySelector(element)
                    val list = destination.getOrPut(key) { ArrayList<T>() }
                    list.add(element)
                }
                return destination
            """
        }
    }

    templates add f("groupBy(keySelector: (T) -> K, valueTransform: (T) -> V)") {
        inline(true)

        include(CharSequences)
        doc { f ->
            """
            Groups values returned by the [valueTransform] function applied to each ${f.element} of the original ${f.collection}
            by the key returned by the given [keySelector] function applied to the ${f.element}
            and returns a map where each group key is associated with a list of corresponding values.

            The returned map preserves the entry iteration order of the keys produced from the original ${f.collection}.

            @sample samples.collections.Collections.Transformations.groupByKeysAndValues
            """
        }
        sequenceClassification(terminal, stateful)
        typeParam("K")
        typeParam("V")
        returns("Map<K, List<V>>")
        body { "return groupByTo(LinkedHashMap<K, MutableList<V>>(), keySelector, valueTransform)" }
    }


    templates add f("groupByTo(destination: M, keySelector: (T) -> K, valueTransform: (T) -> V)") {
        inline(true)

        include(CharSequences)
        typeParam("K")
        typeParam("V")
        typeParam("M : MutableMap<in K, MutableList<V>>")

        doc { f ->
            """
            Groups values returned by the [valueTransform] function applied to each ${f.element} of the original ${f.collection}
            by the key returned by the given [keySelector] function applied to the ${f.element}
            and puts to the [destination] map each group key associated with a list of corresponding values.

            @return The [destination] map.

            @sample samples.collections.Collections.Transformations.groupByKeysAndValues
            """
        }
        sequenceClassification(terminal, stateful)
        returns("M")
        body {
            """
                for (element in this) {
                    val key = keySelector(element)
                    val list = destination.getOrPut(key) { ArrayList<V>() }
                    list.add(valueTransform(element))
                }
                return destination
            """
        }
    }

    templates add f("groupingBy(crossinline keySelector: (T) -> K)") {
        since("1.1")
        inline(true)
        only(Iterables, Sequences, ArraysOfObjects, CharSequences)

        typeParam("T")
        typeParam("K")

        returns("Grouping<T, K>")

        doc { f ->
            """
            Creates a [Grouping] source from ${f.collection.prefixWithArticle()} to be used later with one of group-and-fold operations
            using the specified [keySelector] function to extract a key from each ${f.element}.

            @sample samples.collections.Collections.Transformations.groupingByEachCount
            """
        }

        body {
            """
            return object : Grouping<T, K> {
                override fun sourceIterator(): Iterator<T> = this@groupingBy.iterator()
                override fun keyOf(element: T): K = keySelector(element)
            }
            """
        }
    }

    val terminalOperationPattern = Regex("^\\w+To")
    templates.forEach { with (it) {
        if (sequenceClassification.isEmpty()) {
            sequenceClassification(if (terminalOperationPattern in signature) terminal else intermediate)
            sequenceClassification(if (signature.contains("index", ignoreCase = true)) nearly_stateless else stateless)
        }
    } }
    return templates
}
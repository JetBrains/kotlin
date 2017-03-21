package templates

import templates.Family.*

fun snapshots(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("toCollection(destination: C)") {
        include(CharSequences)
        doc { f -> "Appends all ${f.element.pluralize()} to the given [destination] collection." }
        returns("C")
        typeParam("C : MutableCollection<in T>")
        body {
            """
            for (item in this) {
                destination.add(item)
            }
            return destination
            """
        }
    }

    templates add f("toSet()") {
        doc { f ->
            """
            Returns a [Set] of all ${f.element.pluralize()}.

            The returned set preserves the element iteration order of the original ${f.collection}.
            """
        }
        returns("Set<T>")
        body(Iterables) {
            """
            if (this is Collection) {
                return when (size) {
                    0 -> emptySet()
                    1 -> setOf(if (this is List) this[0] else iterator().next())
                    else -> toCollection(LinkedHashSet<T>(mapCapacity(size)))
                }

            }
            return toCollection(LinkedHashSet<T>()).optimizeReadOnlySet()
            """
        }
        body(Sequences) { "return toCollection(LinkedHashSet<T>()).optimizeReadOnlySet()" }

        body(CharSequences, ArraysOfObjects, ArraysOfPrimitives) { f ->
            val size = if (f == CharSequences) "length" else "size"
            """
            return when ($size) {
                0 -> emptySet()
                1 -> setOf(this[0])
                else -> toCollection(LinkedHashSet<T>(mapCapacity($size)))
            }
            """
        }
    }

    templates add f("toHashSet()") {
        doc { f -> "Returns a [HashSet] of all ${f.element.pluralize()}." }
        returns("HashSet<T>")
        body { "return toCollection(HashSet<T>(mapCapacity(collectionSizeOrDefault(12))))" }
        body(Sequences) { "return toCollection(HashSet<T>())" }
        body(CharSequences) { "return toCollection(HashSet<T>(mapCapacity(length)))" }
        body(ArraysOfObjects, ArraysOfPrimitives) { "return toCollection(HashSet<T>(mapCapacity(size)))" }
    }

    templates add f("toSortedSet()") {
        include(CharSequences)
        jvmOnly(true)
        typeParam("T: Comparable<T>")
        doc { f -> "Returns a [SortedSet] of all ${f.element.pluralize()}." }
        returns("SortedSet<T>")
        body { "return toCollection(TreeSet<T>())" }
    }

    templates add f("toSortedSet(comparator: Comparator<in T>)") {
        only(Iterables, ArraysOfObjects, Sequences)
        jvmOnly(true)
        doc { f ->
            """
                Returns a [SortedSet] of all ${f.element.pluralize()}.

                Elements in the set returned are sorted according to the given [comparator].
            """
        }
        returns("SortedSet<T>")
        body { "return toCollection(TreeSet<T>(comparator))" }
    }

    templates add f("toMutableList()") {
        doc { f -> "Returns a [MutableList] filled with all ${f.element.pluralize()} of this ${f.collection}." }
        returns("MutableList<T>")
        body { "return toCollection(ArrayList<T>())" }
        body(Iterables) {
            """
            if (this is Collection<T>)
                return this.toMutableList()
            return toCollection(ArrayList<T>())
            """
        }
        body(Collections) { "return ArrayList(this)" }
        body(CharSequences) { "return toCollection(ArrayList<T>(length))" }
        body(ArraysOfObjects) { "return ArrayList(this.asCollection())" }
        body(ArraysOfPrimitives) {
            """
            val list = ArrayList<T>(size)
            for (item in this) list.add(item)
            return list
            """
        }
    }

    templates add f("toList()") {
        only(Maps)
        doc { "Returns a [List] containing all key-value pairs." }
        returns("List<Pair<K, V>>")
        body {
            """
            if (size == 0)
                return emptyList()
            val iterator = entries.iterator()
            if (!iterator.hasNext())
                return emptyList()
            val first = iterator.next()
            if (!iterator.hasNext())
                return listOf(first.toPair())
            val result = ArrayList<Pair<K, V>>(size)
            result.add(first.toPair())
            do {
                result.add(iterator.next().toPair())
            } while (iterator.hasNext())
            return result
            """
        }
    }

    templates add f("toList()") {
        include(CharSequences)
        doc { f -> "Returns a [List] containing all ${f.element.pluralize()}." }
        returns("List<T>")
        body { "return this.toMutableList().optimizeReadOnlyList()" }
        body(Iterables) {
            """
            if (this is Collection) {
                return when (size) {
                    0 -> emptyList()
                    1 -> listOf(if (this is List) get(0) else iterator().next())
                    else -> this.toMutableList()
                }
            }
            return this.toMutableList().optimizeReadOnlyList()
            """
        }
        body(CharSequences, ArraysOfPrimitives, ArraysOfObjects) { f ->
            """
            return when (${ if (f == CharSequences) "length" else "size" }) {
                0 -> emptyList()
                1 -> listOf(this[0])
                else -> this.toMutableList()
            }
            """
        }
    }

    templates add f("associate(transform: (T) -> Pair<K, V>)") {
        inline(true)
        include(CharSequences)
        typeParam("K")
        typeParam("V")
        returns("Map<K, V>")
        doc { f ->
            """
            Returns a [Map] containing key-value pairs provided by [transform] function
            applied to ${f.element.pluralize()} of the given ${f.collection}.

            If any of two pairs would have the same key the last one gets added to the map.

            The returned map preserves the entry iteration order of the original ${f.collection}.
            """
        }
        body {
            """
            val capacity = mapCapacity(collectionSizeOrDefault(10)).coerceAtLeast(16)
            return associateTo(LinkedHashMap<K, V>(capacity), transform)
            """
        }
        body(Sequences) {
            """
            return associateTo(LinkedHashMap<K, V>(), transform)
            """
        }
        body(CharSequences) {
            """
            val capacity = mapCapacity(length).coerceAtLeast(16)
            return associateTo(LinkedHashMap<K, V>(capacity), transform)
            """
        }
        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            val capacity = mapCapacity(size).coerceAtLeast(16)
            return associateTo(LinkedHashMap<K, V>(capacity), transform)
            """
        }
    }

    templates add f("associateTo(destination: M, transform: (T) -> Pair<K, V>)") {
        inline(true)
        include(CharSequences)
        typeParam("K")
        typeParam("V")
        typeParam("M : MutableMap<in K, in V>")
        returns("M")
        doc { f ->
            """
            Populates and returns the [destination] mutable map with key-value pairs
            provided by [transform] function applied to each ${f.element} of the given ${f.collection}.

            If any of two pairs would have the same key the last one gets added to the map.
            """
        }
        body {
            """
            for (element in this) {
                destination += transform(element)
            }
            return destination
            """
        }
    }

    templates add f("associateBy(keySelector: (T) -> K)") {
        inline(true)
        include(CharSequences)
        typeParam("K")
        doc { f ->
            """
            Returns a [Map] containing the ${f.element.pluralize()} from the given ${f.collection} indexed by the key
            returned from [keySelector] function applied to each ${f.element}.

            If any two ${f.element.pluralize()} would have the same key returned by [keySelector] the last one gets added to the map.

            The returned map preserves the entry iteration order of the original ${f.collection}.
            """
        }
        returns("Map<K, T>")

        /**
         * Collection size helper methods are private, so we fall back to the calculation from HashSet's Collection
         * constructor.
         */

        body {
            """
            val capacity = mapCapacity(collectionSizeOrDefault(10)).coerceAtLeast(16)
            return associateByTo(LinkedHashMap<K, T>(capacity), keySelector)
            """
        }
        body(Sequences) {
            """
            return associateByTo(LinkedHashMap<K, T>(), keySelector)
            """
        }
        body(CharSequences) {
            """
            val capacity = mapCapacity(length).coerceAtLeast(16)
            return associateByTo(LinkedHashMap<K, T>(capacity), keySelector)
            """
        }
        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            val capacity = mapCapacity(size).coerceAtLeast(16)
            return associateByTo(LinkedHashMap<K, T>(capacity), keySelector)
            """
        }
    }

    templates add f("associateByTo(destination: M, keySelector: (T) -> K)") {
        inline(true)
        include(CharSequences)
        typeParam("K")
        typeParam("M : MutableMap<in K, in T>")
        returns("M")
        doc { f ->
            """
            Populates and returns the [destination] mutable map with key-value pairs,
            where key is provided by the [keySelector] function applied to each ${f.element} of the given ${f.collection}
            and value is the ${f.element} itself.

            If any two ${f.element.pluralize()} would have the same key returned by [keySelector] the last one gets added to the map.
            """
        }
        body {
            """
            for (element in this) {
                destination.put(keySelector(element), element)
            }
            return destination
            """
        }
    }

    templates add f("associateBy(keySelector: (T) -> K, valueTransform: (T) -> V)") {
        inline(true)
        include(CharSequences)
        typeParam("K")
        typeParam("V")
        doc { f ->
            """
            Returns a [Map] containing the values provided by [valueTransform] and indexed by [keySelector] functions applied to ${f.element.pluralize()} of the given ${f.collection}.

            If any two ${f.element.pluralize()} would have the same key returned by [keySelector] the last one gets added to the map.

            The returned map preserves the entry iteration order of the original ${f.collection}.
            """
        }
        returns("Map<K, V>")

        /**
         * Collection size helper methods are private, so we fall back to the calculation from HashSet's Collection
         * constructor.
         */

        body {
            """
            val capacity = mapCapacity(collectionSizeOrDefault(10)).coerceAtLeast(16)
            return associateByTo(LinkedHashMap<K, V>(capacity), keySelector, valueTransform)
            """
        }
        body(Sequences) {
            """
            return associateByTo(LinkedHashMap<K, V>(), keySelector, valueTransform)
            """
        }
        body(CharSequences) {
            """
            val capacity = mapCapacity(length).coerceAtLeast(16)
            return associateByTo(LinkedHashMap<K, V>(capacity), keySelector, valueTransform)
            """
        }
        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            val capacity = mapCapacity(size).coerceAtLeast(16)
            return associateByTo(LinkedHashMap<K, V>(capacity), keySelector, valueTransform)
            """
        }
    }

    templates add f("associateByTo(destination: M, keySelector: (T) -> K, valueTransform: (T) -> V)") {
        inline(true)
        include(CharSequences)
        typeParam("K")
        typeParam("V")
        typeParam("M : MutableMap<in K, in V>")
        returns("M")

        doc { f ->
            """
            Populates and returns the [destination] mutable map with key-value pairs,
            where key is provided by the [keySelector] function and
            and value is provided by the [valueTransform] function applied to ${f.element.pluralize()} of the given ${f.collection}.

            If any two ${f.element.pluralize()} would have the same key returned by [keySelector] the last one gets added to the map.
            """
        }
        body {
            """
            for (element in this) {
                destination.put(keySelector(element), valueTransform(element))
            }
            return destination
            """
        }
    }

    templates.forEach { it.sequenceClassification(SequenceClass.terminal) }

    return templates
}
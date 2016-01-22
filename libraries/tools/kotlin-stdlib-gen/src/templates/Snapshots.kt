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
        doc { f -> "Returns a [Set] of all ${f.element.pluralize()}." }
        returns("Set<T>")
        body { "return toCollection(LinkedHashSet<T>(mapCapacity(collectionSizeOrDefault(12))))" }
        body(Sequences) { "return toCollection(LinkedHashSet<T>())" }
        body(CharSequences) { "return toCollection(LinkedHashSet<T>(mapCapacity(length)))" }
        body(ArraysOfObjects, ArraysOfPrimitives) { "return toCollection(LinkedHashSet<T>(mapCapacity(size)))" }
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

    templates add f("toArrayList()") {
        doc { f -> "Returns an [ArrayList] of all ${f.element.pluralize()}." }
        returns("ArrayList<T>")
        body { "return toCollection(ArrayList<T>())" }
        body(Iterables) {
            """
            if (this is Collection<T>)
                return this.toArrayList()
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
        deprecate(Deprecation("Use toMutableList instead or toCollection(ArrayList()) if you need ArrayList's ensureCapacity and trimToSize.", "toCollection(arrayListOf())"))
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
            val result = ArrayList<Pair<K, V>>(size)
            for (item in this)
                result.add(item.key to item.value)
            return result
            """
        }
    }

    templates add f("toList()") {
        include(CharSequences)
        doc { f -> "Returns a [List] containing all ${f.element.pluralize()}." }
        returns("List<T>")
        body { "return this.toMutableList()" }
    }

    templates add f("toMap(transform: (T) -> Pair<K, V>)") {
        inline(true)
        include(CharSequences)
        typeParam("K")
        typeParam("V")
        returns("Map<K, V>")
        annotations("""@kotlin.jvm.JvmName("toMapOfPairs")""")
        deprecate(Deprecation("Use associate instead.", replaceWith = "associate(transform)"))
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
            """
        }
        body {
            """
            val capacity = ((collectionSizeOrDefault(10)/.75f) + 1).toInt().coerceAtLeast(16)
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
            val capacity = ((length/.75f) + 1).toInt().coerceAtLeast(16)
            return associateTo(LinkedHashMap<K, V>(capacity), transform)
            """
        }
        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            val capacity = ((size/.75f) + 1).toInt().coerceAtLeast(16)
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

    templates add f("toMapBy(selector: (T) -> K)") {
        inline(true)
        typeParam("K")
        returns("Map<K, T>")
        include(CharSequences)
        deprecate(Deprecation("Use associateBy instead.", replaceWith = "associateBy(selector)"))
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
            """
        }
        returns("Map<K, T>")

        /**
         * Collection size helper methods are private, so we fall back to the calculation from HashSet's Collection
         * constructor.
         */

        body {
            """
            val capacity = ((collectionSizeOrDefault(10)/.75f) + 1).toInt().coerceAtLeast(16)
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
            val capacity = ((length/.75f) + 1).toInt().coerceAtLeast(16)
            return associateByTo(LinkedHashMap<K, T>(capacity), keySelector)
            """
        }
        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            val capacity = ((size/.75f) + 1).toInt().coerceAtLeast(16)
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

    templates add f("toMap(selector: (T) -> K, transform: (T) -> V)") {
        inline(true)
        include(CharSequences)
        typeParam("K")
        typeParam("V")
        doc { f ->
            """
            Returns a [Map] containing the values provided by [transform] and indexed by [selector] functions applied to ${f.element.pluralize()} of the given ${f.collection}.
            If any two ${f.element.pluralize()} would have the same key returned by [selector] the last one gets added to the map.
            """
        }
        returns("Map<K, V>")
        deprecate(Deprecation("Use associateBy instead.", "associateBy(selector, transform)"))
    }

    templates add f("toMapBy(selector: (T) -> K, transform: (T) -> V)") {
        inline(true)
        typeParam("K")
        typeParam("V")
        include(CharSequences)
        returns("Map<K, V>")
        deprecate(Deprecation("Use associateBy instead.", replaceWith = "associateBy(selector, transform)"))
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
            """
        }
        returns("Map<K, V>")

        /**
         * Collection size helper methods are private, so we fall back to the calculation from HashSet's Collection
         * constructor.
         */

        body {
            """
            val capacity = ((collectionSizeOrDefault(10)/.75f) + 1).toInt().coerceAtLeast(16)
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
            val capacity = ((length/.75f) + 1).toInt().coerceAtLeast(16)
            return associateByTo(LinkedHashMap<K, V>(capacity), keySelector, valueTransform)
            """
        }
        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            val capacity = ((size/.75f) + 1).toInt().coerceAtLeast(16)
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

    return templates
}
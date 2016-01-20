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
        body { "return this.toArrayList()" }
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
            Returns a [Map] containing key-value pairs provided by [transform] function applied to ${f.element}s of the given ${f.collection}.
            If any of two pairs would have the same key the last one gets added to the map.
            """
        }
        body {
            """
            val capacity = (collectionSizeOrDefault(10)/.75f) + 1
            val result = LinkedHashMap<K, V>(Math.max(capacity.toInt(), 16))
            for (element in this) {
                result += transform(element)
            }
            return result
            """
        }
        body(Sequences) {
            """
            val result = LinkedHashMap<K, V>()
            for (element in this) {
                result += transform(element)
            }
            return result
            """
        }
        body(CharSequences) {
            """
            val capacity = (length/.75f) + 1
            val result = LinkedHashMap<K, V>(Math.max(capacity.toInt(), 16))
            for (element in this) {
                result += transform(element)
            }
            return result
            """
        }
        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            val capacity = (size/.75f) + 1
            val result = LinkedHashMap<K, V>(Math.max(capacity.toInt(), 16))
            for (element in this) {
                result += transform(element)
            }
            return result
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
            val capacity = (collectionSizeOrDefault(10)/.75f) + 1
            val result = LinkedHashMap<K, T>(Math.max(capacity.toInt(), 16))
            for (element in this) {
                result.put(keySelector(element), element)
            }
            return result
            """
        }
        body(Sequences) {
            """
            val result = LinkedHashMap<K, T>()
            for (element in this) {
                result.put(keySelector(element), element)
            }
            return result
            """
        }
        body(CharSequences) {
            """
            val capacity = (length/.75f) + 1
            val result = LinkedHashMap<K, T>(Math.max(capacity.toInt(), 16))
            for (element in this) {
                result.put(keySelector(element), element)
            }
            return result
            """
        }
        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            val capacity = (size/.75f) + 1
            val result = LinkedHashMap<K, T>(Math.max(capacity.toInt(), 16))
            for (element in this) {
                result.put(keySelector(element), element)
            }
            return result
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
            val capacity = (collectionSizeOrDefault(10)/.75f) + 1
            val result = LinkedHashMap<K, V>(Math.max(capacity.toInt(), 16))
            for (element in this) {
                result.put(keySelector(element), valueTransform(element))
            }
            return result
            """
        }
        body(Sequences) {
            """
            val result = LinkedHashMap<K, V>()
            for (element in this) {
                result.put(keySelector(element), valueTransform(element))
            }
            return result
            """
        }
        body(CharSequences) {
            """
            val capacity = (length/.75f) + 1
            val result = LinkedHashMap<K, V>(Math.max(capacity.toInt(), 16))
            for (element in this) {
                result.put(keySelector(element), valueTransform(element))
            }
            return result
            """
        }
        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            val capacity = (size/.75f) + 1
            val result = LinkedHashMap<K, V>(Math.max(capacity.toInt(), 16))
            for (element in this) {
                result.put(keySelector(element), valueTransform(element))
            }
            return result
            """
        }
    }

    return templates
}
package templates

import templates.Family.*
import templates.DocExtensions.element
import templates.DocExtensions.collection

fun snapshots(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("toCollection(collection: C)") {
        deprecate(Strings) { forBinaryCompatibility }
        include(CharSequences, Strings)
        doc { f -> "Appends all ${f.element}s to the given [collection]." }
        returns("C")
        typeParam("C : MutableCollection<in T>")
        body {
            """
            for (item in this) {
                collection.add(item)
            }
            return collection
            """
        }
    }

    templates add f("toSet()") {
        doc { f -> "Returns a [Set] of all ${f.element}s." }
        returns("Set<T>")
        body { "return toCollection(LinkedHashSet<T>(mapCapacity(collectionSizeOrDefault(12))))" }
        body(Sequences) { "return toCollection(LinkedHashSet<T>())" }
        deprecate(Strings) { forBinaryCompatibility }
        body(CharSequences, Strings) { "return toCollection(LinkedHashSet<T>(mapCapacity(length)))" }
        body(ArraysOfObjects, ArraysOfPrimitives) { "return toCollection(LinkedHashSet<T>(mapCapacity(size)))" }
    }

    templates add f("toHashSet()") {
        doc { f -> "Returns a [HashSet] of all ${f.element}s." }
        returns("HashSet<T>")
        body { "return toCollection(HashSet<T>(mapCapacity(collectionSizeOrDefault(12))))" }
        body(Sequences) { "return toCollection(HashSet<T>())" }
        deprecate(Strings) { forBinaryCompatibility }
        body(CharSequences, Strings) { "return toCollection(HashSet<T>(mapCapacity(length)))" }
        body(ArraysOfObjects, ArraysOfPrimitives) { "return toCollection(HashSet<T>(mapCapacity(size)))" }
    }

    templates add f("toSortedSet()") {
        deprecate(Strings) { forBinaryCompatibility }
        include(CharSequences, Strings)
        doc { f -> "Returns a [SortedSet] of all ${f.element}s." }
        returns("SortedSet<T>")
        body { "return toCollection(TreeSet<T>())" }
    }

    templates add f("toArrayList()") {
        doc { f -> "Returns an [ArrayList] of all ${f.element}s." }
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
        deprecate(Strings) { forBinaryCompatibility }
        body(CharSequences, Strings) { "return toCollection(ArrayList<T>(length))" }
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
        deprecate(Strings) { forBinaryCompatibility }
        include(CharSequences, Strings)
        doc { f -> "Returns a [List] containing all ${f.element}s." }
        returns("List<T>")
        body { "return this.toArrayList()" }
    }

    templates add f("toLinkedList()") {
        include(Strings)
        doc { "Returns a [LinkedList] containing all elements." }
        returns("LinkedList<T>")
        deprecate { Deprecation("Use toCollection(LinkedList()) instead.", replaceWith = "toCollection(LinkedList())") }
    }

    templates add f("toMap(selector: (T) -> K)") {
        inline(true)
        deprecate(Strings) { forBinaryCompatibility }
        body(Strings) { "return toMapBy(selector)" }
        include(CharSequences, Strings)
        typeParam("K")
        returns("Map<K, T>")
        deprecate(Deprecation("Use toMapBy instead.", replaceWith = "toMapBy(selector)"))
    }

    templates add f("toMapBy(selector: (T) -> K)") {
        inline(true)
        typeParam("K")
        doc { f ->
            """
            Returns Map containing the ${f.element}s from the given ${f.collection} indexed by the key
            returned from [selector] function applied to each ${f.element}.
            If any two ${f.element}s would have the same key returned by [selector] the last one gets added to the map.
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
                result.put(selector(element), element)
            }
            return result
            """
        }
        body(Sequences) {
            """
            val result = LinkedHashMap<K, T>()
            for (element in this) {
                result.put(selector(element), element)
            }
            return result
            """
        }
        deprecate(Strings) { forBinaryCompatibility }
        body(CharSequences, Strings) {
            """
            val capacity = (length/.75f) + 1
            val result = LinkedHashMap<K, T>(Math.max(capacity.toInt(), 16))
            for (element in this) {
                result.put(selector(element), element)
            }
            return result
            """
        }
        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            val capacity = (size/.75f) + 1
            val result = LinkedHashMap<K, T>(Math.max(capacity.toInt(), 16))
            for (element in this) {
                result.put(selector(element), element)
            }
            return result
            """
        }
    }

    templates add f("toMap(selector: (T) -> K, transform: (T) -> V)") {
        inline(true)
        typeParam("K")
        typeParam("V")
        doc { f ->
            """
            Returns Map containing the values provided by [transform] and indexed by [selector] functions applied to ${f.element}s of the given ${f.collection}.
            If any two ${f.element}s would have the same key returned by [selector] the last one gets added to the map.
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
                result.put(selector(element), transform(element))
            }
            return result
            """
        }
        body(Sequences) {
            """
            val result = LinkedHashMap<K, V>()
            for (element in this) {
                result.put(selector(element), transform(element))
            }
            return result
            """
        }
        deprecate(Strings) { forBinaryCompatibility }
        body(CharSequences, Strings) {
            """
            val capacity = (length/.75f) + 1
            val result = LinkedHashMap<K, V>(Math.max(capacity.toInt(), 16))
            for (element in this) {
                result.put(selector(element), transform(element))
            }
            return result
            """
        }
        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            val capacity = (size/.75f) + 1
            val result = LinkedHashMap<K, V>(Math.max(capacity.toInt(), 16))
            for (element in this) {
                result.put(selector(element), transform(element))
            }
            return result
            """
        }
    }

    return templates
}
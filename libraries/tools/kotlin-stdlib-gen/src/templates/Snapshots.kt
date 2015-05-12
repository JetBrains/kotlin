package templates

import templates.Family.*
import java.util.ArrayList

fun snapshots(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("toCollection(collection: C)") {
        doc { "Appends all elements to the given *collection*" }
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
        doc { "Returns a Set of all elements" }
        returns("Set<T>")
        body { "return toCollection(LinkedHashSet<T>(mapCapacity(collectionSizeOrDefault(12))))" }
        body(Sequences) { "return toCollection(LinkedHashSet<T>())" }
        body(Strings) { "return toCollection(LinkedHashSet<T>(mapCapacity(length())))" }
        body(ArraysOfObjects, ArraysOfPrimitives) { "return toCollection(LinkedHashSet<T>(mapCapacity(size())))" }
    }

    templates add f("toHashSet()") {
        doc { "Returns a HashSet of all elements" }
        returns("HashSet<T>")
        body { "return toCollection(HashSet<T>(mapCapacity(collectionSizeOrDefault(12))))" }
        body(Sequences) { "return toCollection(HashSet<T>())" }
        body(Strings) { "return toCollection(HashSet<T>(mapCapacity(length())))" }
        body(ArraysOfObjects, ArraysOfPrimitives) { "return toCollection(HashSet<T>(mapCapacity(size())))" }
    }

    templates add f("toSortedSet()") {
        doc { "Returns a SortedSet of all elements" }
        returns("SortedSet<T>")
        body { "return toCollection(TreeSet<T>())" }
    }

    templates add f("toArrayList()") {
        doc { "Returns an ArrayList of all elements" }
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
        body(Strings) { "return toCollection(ArrayList<T>(length()))" }
        body(ArraysOfObjects) { "return this.asList().toArrayList()" }
        body(ArraysOfPrimitives) {
            """
            val list = ArrayList<T>(size())
            for (item in this) list.add(item)
            return list
            """
        }
    }

    templates add f("toList()") {
        only(Maps)
        doc { "Returns a List containing all key-value pairs" }
        returns("List<Pair<K, V>>")
        body {
            """
            val result = ArrayList<Pair<K, V>>(size())
            for (item in this)
                result.add(item.key to item.value)
            return result
            """
        }
    }

    templates add f("toList()") {
        doc { "Returns a List containing all elements" }
        returns("List<T>")
        body { "return this.toArrayList()" }
    }

    templates add f("toLinkedList()") {
        doc { "Returns a LinkedList containing all elements" }
        returns("LinkedList<T>")
        body { "return toCollection(LinkedList<T>())" }
    }

    templates add f("toMap(selector: (T) -> K)") {
        inline(true)
        typeParam("K")
        doc {
            """
            Returns Map containing all the values from the given collection indexed by *selector*
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
        body(Strings) {
            """
            val capacity = (length()/.75f) + 1
            val result = LinkedHashMap<K, T>(Math.max(capacity.toInt(), 16))
            for (element in this) {
                result.put(selector(element), element)
            }
            return result
            """
        }
        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            val capacity = (size()/.75f) + 1
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
        doc {
            """
            Returns Map containing all the values provided by *transform* and indexed by *selector* from the given collection
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
        body(Strings) {
            """
            val capacity = (length()/.75f) + 1
            val result = LinkedHashMap<K, V>(Math.max(capacity.toInt(), 16))
            for (element in this) {
                result.put(selector(element), transform(element))
            }
            return result
            """
        }
        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            val capacity = (size()/.75f) + 1
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
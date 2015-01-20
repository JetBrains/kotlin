package templates

import templates.Family.*

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
        body { "return toCollection(LinkedHashSet<T>())" }
    }

    templates add f("toHashSet()") {
        doc { "Returns a HashSet of all elements" }
        returns("HashSet<T>")
        body { "return toCollection(HashSet<T>())" }
    }

    templates add f("toSortedSet()") {
        doc { "Returns a SortedSet of all elements" }
        returns("SortedSet<T>")
        body { "return toCollection(TreeSet<T>())" }
    }

    templates add f("toArrayList()") {
        doc { "Returns an ArrayList of all elements" }
        returns("ArrayList<T>")
        body { "return toCollection(ArrayList<T>(collectionSizeOrDefault(10)))" }
        body(Streams) { "return toCollection(ArrayList<T>())" }
        body(Strings) { "return toCollection(ArrayList<T>(length()))" }
        body(ArraysOfObjects, ArraysOfPrimitives) {
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
        body { "return toCollection(ArrayList<T>(collectionSizeOrDefault(10)))" }
        body(Streams) { "return toCollection(ArrayList<T>())" }
        body(Strings) { "return toCollection(ArrayList<T>(length()))" }
        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            val list = ArrayList<T>(size())
            for (item in this) list.add(item)
            return list
            """
        }
    }

    templates add f("toLinkedList()") {
        doc { "Returns a LinkedList containing all elements" }
        returns("LinkedList<T>")
        body { "return toCollection(LinkedList<T>())" }
    }

    templates add f("asList()") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns a list that wraps the original array" }
        returns("List<T>")
        body(ArraysOfObjects) {
            """
            return Arrays.asList(*this)
            """
        }

        body(ArraysOfPrimitives) {
            """
            return object : AbstractList<T>(), RandomAccess {
                override fun size(): Int = this@asList.size()
                override fun isEmpty(): Boolean = this@asList.isEmpty()
                override fun contains(o: Any?): Boolean = this@asList.contains(o as T)
                override fun iterator(): Iterator<T> = this@asList.iterator()
                override fun get(index: Int): T = this@asList[index]
                override fun indexOf(o: Any?): Int = this@asList.indexOf(o as T)
                override fun lastIndexOf(o: Any?): Int = this@asList.lastIndexOf(o as T)
            }
            """
        }
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
        body {
            """
            val result = LinkedHashMap<K, T>()
            for (element in this) {
                result.put(selector(element), element)
            }
            return result
            """
        }
    }

    return templates
}

package templates

import templates.Family.*

fun sets(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("toMutableSet()") {
        exclude(Strings)
        doc { "Returns a mutable set containing all distinct elements from the given collection." }
        returns("MutableSet<T>")
        body {
            """
            return when (this) {
                is Collection<T> -> LinkedHashSet(this)
                else -> toCollection(LinkedHashSet<T>())
            }
            """
        }
        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            val set = LinkedHashSet<T>(mapCapacity(size()))
            for (item in this) set.add(item)
            return set
            """
        }
        doc(Sequences) { "Returns a mutable set containing all distinct elements from the given sequence." }
        body(Sequences) {
            """
            val set = LinkedHashSet<T>()
            for (item in this) set.add(item)
            return set
            """
        }
    }

    templates add f("distinct()") {
        exclude(Strings)
        val collectionDoc = """
            Returns a list containing only distinct elements from the given collection.

            The elements in the resulting list are in the same order as they were in the source collection.
            """
        doc { collectionDoc }

        returns("List<T>")
        body { "return this.toMutableSet().toList()" }
        doc(Sequences) { collectionDoc.replace("list", "sequence").replace("collection", "sequence") }
        returns(Sequences) { "Sequence<T>" }
        body(Sequences) { "return this.distinctBy { it }" }
    }

    templates add f("distinctBy(keySelector: (T) -> K)") {
        exclude(Strings)
        val collectionDoc = """
            Returns a list containing only distinct elements from the given collection according to the [keySelector].

            The elements in the resulting list are in the same order as they were in the source collection.
            """
        doc { collectionDoc }

        inline(true)
        typeParam("K")
        returns("List<T>")
        body {
            """
            val set = HashSet<K>()
            val list = ArrayList<T>()
            for (e in this) {
                val key = keySelector(e)
                if (set.add(key))
                    list.add(e)
            }
            return list
            """
        }

        inline(false, Sequences)
        doc(Sequences) { collectionDoc.replace("list", "sequence").replace("collection", "sequence") }
        returns(Sequences) { "Sequence<T>" }
        body(Sequences) {
            """
            return DistinctSequence(this, keySelector)
            """
        }

    }

    templates add f("union(other: Iterable<T>)") {
        exclude(Strings, Sequences)
        doc { "Returns a set containing all distinct elements from both collections." }
        returns("Set<T>")
        body {
            """
            val set = this.toMutableSet()
            set.addAll(other)
            return set
            """
        }
    }

    templates add f("intersect(other: Iterable<T>)") {
        exclude(Strings, Sequences)
        doc { "Returns a set containing all distinct elements from both collections." }
        returns("Set<T>")
        body {
            """
            val set = this.toMutableSet()
            set.retainAll(other)
            return set
            """
        }
    }

    templates add f("subtract(other: Iterable<T>)") {
        exclude(Strings, Sequences)
        doc { "Returns a set containing all distinct elements from both collections." }
        returns("Set<T>")
        body {
            """
            val set = this.toMutableSet()
            set.removeAll(other)
            return set
            """
        }
    }

    return templates
}
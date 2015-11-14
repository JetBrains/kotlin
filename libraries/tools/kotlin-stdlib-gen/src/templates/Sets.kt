package templates

import templates.Family.*
import templates.DocExtensions.element
import templates.DocExtensions.collection
import templates.DocExtensions.mapResult

fun sets(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("toMutableSet()") {
        exclude(Strings)
        doc { f -> "Returns a mutable set containing all distinct ${f.element}s from the given ${f.collection}." }
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
            val set = LinkedHashSet<T>(mapCapacity(size))
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
        doc { f ->
            """
                Returns a ${f.mapResult} containing only distinct ${f.element}s from the given ${f.collection}.

                The ${f.element}s in the resulting ${f.mapResult} are in the same order as they were in the source ${f.collection}.
                """
        }

        returns("List<T>")
        body { "return this.toMutableSet().toList()" }
        returns(Sequences) { "Sequence<T>" }
        body(Sequences) { "return this.distinctBy { it }" }
    }

    templates add f("distinctBy(keySelector: (T) -> K)") {
        exclude(Strings)
        doc { f ->
            """
                Returns a ${f.mapResult} containing only distinct ${f.element}s from the given ${f.collection} according to the [keySelector].

                The ${f.element}s in the resulting ${f.mapResult} are in the same order as they were in the source ${f.collection}.
                """
        }

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
        returns(Sequences) { "Sequence<T>" }
        body(Sequences) {
            """
            return DistinctSequence(this, keySelector)
            """
        }

    }

    templates add f("union(other: Iterable<T>)") {
        infix(true)
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
        infix(true)
        exclude(Strings, Sequences)
        doc { "Returns a set containing all elements that are contained by both this set and the specified collection." }
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
        infix(true)
        exclude(Strings, Sequences)
        doc { "Returns a set containing all elements that are contained by this set and not contained by the specified collection." }
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
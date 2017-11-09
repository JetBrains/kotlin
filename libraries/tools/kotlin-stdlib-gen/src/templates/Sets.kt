package templates

import templates.Family.*
import templates.SequenceClass.*

fun sets(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("toMutableSet()") {
        exclude(Strings)
        doc { f ->
            """
            Returns a mutable set containing all distinct ${f.element.pluralize()} from the given ${f.collection}.

            The returned set preserves the element iteration order of the original ${f.collection}.
            """
        }
        sequenceClassification(terminal)
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
                Returns a ${f.mapResult} containing only distinct ${f.element.pluralize()} from the given ${f.collection}.

                The ${f.element.pluralize()} in the resulting ${f.mapResult} are in the same order as they were in the source ${f.collection}.
                """
        }

        returns("List<T>")
        body { "return this.toMutableSet().toList()" }
        sequenceClassification(intermediate, stateful)
        returns(Sequences) { "Sequence<T>" }
        body(Sequences) { "return this.distinctBy { it }" }
    }

    templates add f("distinctBy(selector: (T) -> K)") {
        exclude(Strings)
        doc { f ->
            """
                Returns a ${f.mapResult} containing only ${f.element.pluralize()} from the given ${f.collection}
                having distinct keys returned by the given [selector] function.

                The ${f.element.pluralize()} in the resulting ${f.mapResult} are in the same order as they were in the source ${f.collection}.
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
                val key = selector(e)
                if (set.add(key))
                    list.add(e)
            }
            return list
            """
        }

        inline(false, Sequences)
        returns(Sequences) { "Sequence<T>" }
        sequenceClassification(intermediate, stateful)
        body(Sequences) {
            """
            return DistinctSequence(this, selector)
            """
        }

    }

    templates add f("union(other: Iterable<T>)") {
        infix(true)
        exclude(Strings, Sequences)
        doc { f ->
            """
            Returns a set containing all distinct elements from both collections.

            The returned set preserves the element iteration order of the original ${f.collection}.
            Those elements of the [other] collection that are unique are iterated in the end
            in the order of the [other] collection.
            """
        }
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
        doc { f ->
            """
            Returns a set containing all elements that are contained by both this set and the specified collection.

            The returned set preserves the element iteration order of the original ${f.collection}.
            """
        }
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
        doc { f ->
            """
            Returns a set containing all elements that are contained by this ${f.collection} and not contained by the specified collection.

            The returned set preserves the element iteration order of the original ${f.collection}.
            """
        }
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
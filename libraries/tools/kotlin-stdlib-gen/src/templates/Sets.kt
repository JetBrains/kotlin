package templates

import templates.Family.*

fun sets(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("toMutableSet()") {
        exclude(Strings, Sequences)
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
            val set = LinkedHashSet<T>(size())
            for (item in this) set.add(item)
            return set
            """
        }
    }

    templates add f("distinct()") {
        exclude(Strings, Sequences)
        doc { "Returns a set containing all distinct elements from the given collection." }

        returns("Set<T>")
        body {
            """
            return this.toMutableSet()
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
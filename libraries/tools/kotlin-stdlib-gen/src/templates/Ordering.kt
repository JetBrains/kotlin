package templates

import templates.Family.*

fun ordering(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("reverse()") {
        doc { "Returns a list with elements in reversed order" }
        returns { "List<T>" }
        body {
            """
            val list = toArrayList()
            Collections.reverse(list)
            return list
            """
        }

        doc(Strings) { "Returns a string with characters in reversed order" }
        returns(Strings) { "String" }
        body(Strings) {
            // TODO: Replace with StringBuilder(this) when JS can handle it
            """
            return StringBuilder().append(this).reverse().toString()
            """
        }

        exclude(Sequences)
    }

    templates add f("sort()") {
        doc {
            """
            Returns a sorted list of all elements
            """
        }
        returns("List<T>")
        typeParam("T : Comparable<T>")
        body {
            """
            val sortedList = toArrayList()
            java.util.Collections.sort(sortedList)
            return sortedList
            """
        }

        exclude(Sequences)
        exclude(ArraysOfPrimitives)
        exclude(ArraysOfObjects)
        exclude(Strings)
    }

    templates add f("toSortedList()") {
        doc {
            """
            Returns a sorted list of all elements
            """
        }
        returns("List<T>")
        typeParam("T : Comparable<T>")
        body {
            """
            val sortedList = toArrayList()
            java.util.Collections.sort(sortedList)
            return sortedList
            """
        }

        only(Sequences, ArraysOfObjects, ArraysOfPrimitives, Iterables)
    }

    templates add f("sortDescending()") {
        doc {
            """
            Returns a sorted list of all elements, in descending order
            """
        }
        returns("List<T>")
        typeParam("T : Comparable<T>")
        body {
            """
            val sortedList = toArrayList()
            java.util.Collections.sort(sortedList, comparator { x, y -> y.compareTo(x) })
            return sortedList
            """
        }

        exclude(Sequences)
        exclude(ArraysOfPrimitives)
        exclude(ArraysOfObjects)
        exclude(Strings)
    }

    templates add f("sortBy(inlineOptions(InlineOption.ONLY_LOCAL_RETURN) order: (T) -> R)") {
        inline(true)

        doc {
            """
            Returns a sorted list of all elements, ordered by results of specified *order* function.
            """
        }
        returns("List<T>")
        typeParam("R : Comparable<R>")
        body {
            """
            val sortedList = toArrayList()
            val sortBy: Comparator<T> = compareBy(order)
            java.util.Collections.sort(sortedList, sortBy)
            return sortedList
            """
        }

        exclude(Sequences)
        exclude(ArraysOfPrimitives)
        exclude(Strings)
    }

    templates add f("toSortedListBy(order: (T) -> V)") {
        doc {
            """
            Returns a sorted list of all elements, ordered by results of specified *order* function.
            """
        }
        returns("List<T>")
        typeParam("T")
        typeParam("V : Comparable<V>")
        body {
            """
            val sortedList = toArrayList()
            val sortBy: Comparator<T> = compareBy(order)
            java.util.Collections.sort(sortedList, sortBy)
            return sortedList
            """
        }

        only(Sequences, ArraysOfObjects, ArraysOfPrimitives, Iterables)
    }

    templates add f("sortDescendingBy(inlineOptions(InlineOption.ONLY_LOCAL_RETURN) order: (T) -> R)") {
        inline(true)

        doc {
            """
            Returns a sorted list of all elements, in descending order by results of specified *order* function.
            """
        }
        returns("List<T>")
        typeParam("R : Comparable<R>")
        body {
            """
            val sortedList = toArrayList()
            val sortBy: Comparator<T> = compareByDescending(order)
            java.util.Collections.sort(sortedList, sortBy)
            return sortedList
            """
        }

        exclude(Sequences)
        exclude(ArraysOfPrimitives)
        exclude(Strings)
    }

    templates add f("sortBy(comparator: Comparator<in T>)") {
        doc {
            """
            Returns a list of all elements, sorted by the specified *comparator*
            """
        }
        returns("List<T>")
        body {
            """
            val sortedList = toArrayList()
            java.util.Collections.sort(sortedList, comparator)
            return sortedList
            """
        }

        exclude(Sequences)
        exclude(ArraysOfPrimitives)
        exclude(Strings)
    }

    return templates
}
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

        exclude(Streams)
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

        exclude(Streams)
        exclude(ArraysOfPrimitives) // TODO: resolve collision between inplace sort and this function
        exclude(ArraysOfObjects)
        exclude(Strings)
    }

    templates add f("sortDescending()") {
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
            val sortBy: Comparator<T> = comparator<T> {(x: T, y: T) -> -x.compareTo(y) }
            java.util.Collections.sort(sortedList, sortBy)
            return sortedList
            """
        }

        exclude(Streams)
        exclude(ArraysOfPrimitives) // TODO: resolve collision between inplace sort and this function
        exclude(ArraysOfObjects)
        exclude(Strings)
    }

    templates add f("sortBy(order: (T) -> R)") {
        inline(true)

        doc {
            """
            Returns a list of all elements, sorted by results of specified *order* function.
            """
        }
        returns("List<T>")
        typeParam("R : Comparable<R>")
        body {
            """
            val sortedList = toArrayList()
            val sortBy: Comparator<T> = comparator<T> {(x: T, y: T) -> order(x).compareTo(order(y)) }
            java.util.Collections.sort(sortedList, sortBy)
            return sortedList
            """
        }

        exclude(Streams)
        exclude(ArraysOfPrimitives)
        exclude(Strings)
    }

    templates add f("sortDescendingBy(order: (T) -> R)") {
        inline(true)

        doc {
            """
            Returns a list of all elements, sorted by results of specified *order* function.
            """
        }
        returns("List<T>")
        typeParam("R : Comparable<R>")
        body {
            """
            val sortedList = toArrayList()
            val sortBy: Comparator<T> = comparator<T> {(x: T, y: T) -> -order(x).compareTo(order(y)) }
            java.util.Collections.sort(sortedList, sortBy)
            return sortedList
            """
        }

        exclude(Streams)
        exclude(ArraysOfPrimitives)
        exclude(Strings)
    }

    templates add f("sortBy(comparator: Comparator<T>)") {
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

        exclude(Streams)
        exclude(ArraysOfPrimitives)
        exclude(Strings)
    }

    return templates
}
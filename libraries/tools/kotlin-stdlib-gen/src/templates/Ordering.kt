package templates

import templates.Family.*

fun ordering(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("reverse()") {
        deprecate("reverse will change its behavior soon. Use reversed() instead.")
        deprecateReplacement("reversed()")
        doc { "Returns a list with elements in reversed order." }
        returns { "List<T>" }
        body { """return reversed()""" }

        include(Strings)
        returns(Strings) { "SELF" }

        exclude(Sequences)
    }

    templates add f("reversed()") {
        doc { "Returns a list with elements in reversed order." }
        returns { "List<T>" }
        body {
            """
            if (this is Collection && isEmpty()) return emptyList()
            val list = toArrayList()
            Collections.reverse(list)
            return list
            """
        }

        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            if (isEmpty()) return emptyList()
            val list = toArrayList()
            Collections.reverse(list)
            return list
            """
        }

        doc(Strings) { "Returns a string with characters in reversed order." }
        returns(Strings) { "SELF" }
        body(Strings) {
            // TODO: Replace with StringBuilder(this) when JS can handle it
            """
            return StringBuilder().append(this).reverse().toString()
            """
        }

        exclude(Sequences)
    }

    templates add f("reversedArray()") {
        doc { "Returns an array with elements of this array in reversed order." }
        only(ArraysOfObjectsSubtype, ArraysOfPrimitives)
        returns("SELF")
        body(ArraysOfObjectsSubtype) {
            """
            if (isEmpty()) return this
            val result = arrayOfNulls(this, size()) as Array<T>
            val lastIndex = lastIndex
            for (i in 0..lastIndex)
                result[lastIndex - i] = this[i]
            return result as A
            """
        }
        body(ArraysOfPrimitives) {
            """
            if (isEmpty()) return this
            val result = SELF(size())
            val lastIndex = lastIndex
            for (i in 0..lastIndex)
                result[lastIndex - i] = this[i]
            return result
            """
        }
    }

    templates add f("sorted()") {
        exclude(Strings)
        exclude(PrimitiveType.Boolean)

        doc {
            """
            Returns a list of all elements sorted according to their natural sort order.
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

        returns("SELF", Sequences)
        doc(Sequences) {
            "Returns a sequence that yields elements of this sequence sorted according to their natural sort order."
        }
        body(Sequences) {
            """
            return object : Sequence<T> {
                override fun iterator(): Iterator<T> {
                    val sortedList = this@sorted.toArrayList()
                    java.util.Collections.sort(sortedList)
                    return sortedList.iterator()
                }
            }
            """
        }
    }

    templates add f("sortedArray()") {
        only(ArraysOfObjectsSubtype, ArraysOfPrimitives)
        exclude(PrimitiveType.Boolean)
        doc {
            "Returns an array with all elements of this array sorted according to their natural sort order."
        }
        typeParam("T : Comparable<T>")
        returns("SELF")
        body() {
            """
            if (isEmpty()) return this
            return this.copyOf().apply { sort() } as SELF
            """
        }
    }

    templates add f("sortedDescending()") {
        exclude(Strings)
        exclude(PrimitiveType.Boolean)

        doc {
            """
            Returns a list of all elements sorted descending according to their natural sort order.
            """
        }
        returns("List<T>")
        typeParam("T : Comparable<T>")
        body {
            """
            return sortedWith(comparator { x, y -> y.compareTo(x) })
            """
        }
        body(ArraysOfPrimitives) {
            """
            return copyOf().apply { sort() }.reversed()
            """
        }

        returns("SELF", Sequences)
        doc(Sequences) {
            "Returns a sequence that yields elements of this sequence sorted descending according to their natural sort order."
        }
        body(Sequences) {
            """
            return sortedWith(comparator { x, y -> y.compareTo(x) })
            """
        }
    }

    templates add f("sortedArrayDescending()") {
        only(ArraysOfObjectsSubtype, ArraysOfPrimitives)
        exclude(PrimitiveType.Boolean)
        doc {
            "Returns an array with all elements of this array sorted descending according to their natural sort order."
        }
        typeParam("T : Comparable<T>")
        returns("SELF")
        body(ArraysOfObjectsSubtype) {
            """
            if (isEmpty()) return this
            // TODO: Use reverseOrder<T>()
            return this.copyOf().apply { sortWith(comparator { a, b -> b.compareTo(a) }) } as SELF
            """

        }
        body() {
            """
            if (isEmpty()) return this
            // TODO: Use in-place reverse
            return this.copyOf().apply { sort() }.reversedArray() as SELF
            """
        }
    }

    templates add f("sortedWith(comparator: Comparator<in T>)") {
        exclude(Strings)
        returns("List<T>")
        doc {
            """
            Returns a list of all elements sorted according to the specified [comparator].
            """
        }
        body {
            """
            val sortedList = toArrayList()
            java.util.Collections.sort(sortedList, comparator)
            return sortedList
            """
        }

        returns("SELF", Sequences)
        doc(Sequences) {
            "Returns a sequence that yields elements of this sequence sorted according to the specified [comparator]."
        }
        body(Sequences) {
            """
            return object : Sequence<T> {
                override fun iterator(): Iterator<T> {
                    val sortedList = this@sortedWith.toArrayList()
                    java.util.Collections.sort(sortedList, comparator)
                    return sortedList.iterator()
                }
            }
            """
        }
    }

    templates add f("sortedArrayWith(comparator: Comparator<in T>)") {
        only(ArraysOfObjectsSubtype)
        doc {
            "Returns an array with all elements of this array sorted according the specified [comparator]."
        }
        returns("SELF")
        body() {
            """
            if (isEmpty()) return this
            return this.copyOf().apply { sortWith(comparator) } as SELF
            """
        }
    }

    templates add f("sortedBy(inlineOptions(InlineOption.ONLY_LOCAL_RETURN) selector: (T) -> R?)") {
        exclude(Strings)
        inline(true)
        returns("List<T>")
        typeParam("R : Comparable<R>")

        doc {
            """
            Returns a list of all elements sorted according to natural sort order of the value returned by specified [selector] function.
            """
        }

        returns("SELF", Sequences)
        doc(Sequences) {
            "Returns a sequence that yields elements of this sequence sorted according to natural sort order of the value returned by specified [selector] function."
        }

        body {
            "return sortedWith(compareBy(selector))"
        }
    }

    templates add f("sortedByDescending(inlineOptions(InlineOption.ONLY_LOCAL_RETURN) selector: (T) -> R?)") {
        exclude(Strings)
        inline(true)
        returns("List<T>")
        typeParam("R : Comparable<R>")

        doc {
            """
            Returns a list of all elements sorted descending according to natural sort order of the value returned by specified [selector] function.
            """
        }

        returns("SELF", Sequences)
        doc(Sequences) {
            "Returns a sequence that yields elements of this sequence sorted descending according to natural sort order of the value returned by specified [selector] function."
        }

        body {
            "return sortedWith(compareByDescending(selector))"
        }
    }

    templates add f("sort()") {
        doc {
            """
            Returns a sorted list of all elements.
            """
        }
        returns("List<T>")
        typeParam("T : Comparable<T>")
        deprecate("This method may change its behavior soon. Use sorted() instead.")
        deprecateReplacement("sorted()")
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
            Returns a sorted list of all elements.
            """
        }
        returns("List<T>")
        typeParam("T : Comparable<T>")
        deprecate("Use sorted() instead.")
        deprecateReplacement("sorted()")

        deprecate("Use asIterable().sorted() instead.", Sequences)
        deprecateReplacement("asIterable().sorted()", Sequences)
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
            Returns a sorted list of all elements, in descending order.
            """
        }
        deprecate("This method may change its behavior soon. Use sortedDescending() instead.")
        deprecateReplacement("sortedDescending()")
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
            Returns a sorted list of all elements, ordered by results of specified [order] function.
            """
        }
        deprecate("This method may change its behavior soon. Use sortedBy() instead.")
        deprecateReplacement("sortedBy(order)")
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
            Returns a sorted list of all elements, ordered by results of specified [order] function.
            """
        }
        returns("List<T>")
        deprecate("Use sortedBy(order) instead.")
        deprecateReplacement("sortedBy(order)")

        deprecate("Use asIterable().sortedBy(order) instead.", Sequences)
        deprecateReplacement("asIterable().sortedBy(order)", Sequences)
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
            Returns a sorted list of all elements, in descending order by results of specified [order] function.
            """
        }
        deprecate("This method may change its behavior soon. Use sortedByDescending() instead.")
        deprecateReplacement("sortedByDescending(order)")
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
            Returns a list of all elements, sorted by the specified [comparator].
            """
        }
        returns("List<T>")
        deprecate("This method may change its behavior soon. Use sortedWith() instead.")
        deprecateReplacement("sortedWith(comparator)")
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
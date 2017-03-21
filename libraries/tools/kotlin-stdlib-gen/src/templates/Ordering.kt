package templates

import templates.Family.*
import templates.SequenceClass.intermediate
import templates.SequenceClass.stateful

fun ordering(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("reverse()") {
        doc { f -> "Reverses ${f.element.pluralize()} in the ${f.collection} in-place." }
        only(Lists, InvariantArraysOfObjects, ArraysOfPrimitives)
        customReceiver(Lists) { "MutableList<T>" }
        returns { "Unit" }
        body {
            """
            val midPoint = (size / 2) - 1
            if (midPoint < 0) return
            var reverseIndex = lastIndex
            for (index in 0..midPoint) {
                val tmp = this[index]
                this[index] = this[reverseIndex]
                this[reverseIndex] = tmp
                reverseIndex--
            }
            """
        }
        body(Platform.JVM, Lists) { """java.util.Collections.reverse(this)""" }
    }

    templates add f("reversed()") {
        doc { "Returns a list with elements in reversed order." }
        returns { "List<T>" }
        body {
            """
            if (this is Collection && size <= 1) return toList()
            val list = toMutableList()
            list.reverse()
            return list
            """
        }

        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            if (isEmpty()) return emptyList()
            val list = toMutableList()
            list.reverse()
            return list
            """
        }

        doc(CharSequences, Strings) { f -> "Returns a ${f.collection} with characters in reversed order." }
        returns(CharSequences, Strings) { "SELF" }
        body(CharSequences) { f ->
            """
            return StringBuilder(this).reverse()
            """
        }
        inline(Strings) { Inline.Only }
        body(Strings) {
            "return (this as CharSequence).reversed().toString()"
        }

        exclude(Sequences)
    }

    templates add f("reversedArray()") {
        doc { "Returns an array with elements of this array in reversed order." }
        only(InvariantArraysOfObjects, ArraysOfPrimitives)
        returns("SELF")
        body(InvariantArraysOfObjects) {
            """
            if (isEmpty()) return this
            val result = arrayOfNulls(this, size)
            val lastIndex = lastIndex
            for (i in 0..lastIndex)
                result[lastIndex - i] = this[i]
            return result
            """
        }
        body(ArraysOfPrimitives) {
            """
            if (isEmpty()) return this
            val result = SELF(size)
            val lastIndex = lastIndex
            for (i in 0..lastIndex)
                result[lastIndex - i] = this[i]
            return result
            """
        }
    }

    templates add f("sorted()") {
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
                if (this is Collection) {
                    if (size <= 1) return this.toList()
                    @Suppress("UNCHECKED_CAST")
                    return (toTypedArray<Comparable<T>>() as Array<T>).apply { sort() }.asList()
                }
                return toMutableList().apply { sort() }
            """
        }
        body(ArraysOfPrimitives) {
            """
            return toTypedArray().apply { sort() }.asList()
            """
        }
        body(ArraysOfObjects) {
            """
            return sortedArray().asList()
            """
        }

        returns("SELF", Sequences)
        doc(Sequences) {
            "Returns a sequence that yields elements of this sequence sorted according to their natural sort order."
        }
        sequenceClassification(intermediate, stateful)
        body(Sequences) {
            """
            return object : Sequence<T> {
                override fun iterator(): Iterator<T> {
                    val sortedList = this@sorted.toMutableList()
                    sortedList.sort()
                    return sortedList.iterator()
                }
            }
            """
        }
    }

    templates add f("sortedArray()") {
        only(InvariantArraysOfObjects, ArraysOfPrimitives)
        exclude(PrimitiveType.Boolean)
        doc {
            "Returns an array with all elements of this array sorted according to their natural sort order."
        }
        typeParam("T : Comparable<T>")
        returns("SELF")
        body() {
            """
            if (isEmpty()) return this
            return this.copyOf().apply { sort() }
            """
        }
    }

    templates add f("sortDescending()") {
        only(Lists, ArraysOfObjects, ArraysOfPrimitives)
        exclude(PrimitiveType.Boolean)
        doc { f -> """Sorts elements in the ${f.collection} in-place descending according to their natural sort order.""" }
        returns("Unit")
        typeParam("T : Comparable<T>")
        customReceiver(Lists) { "MutableList<T>" }

        body { """sortWith(reverseOrder())""" }
        body(ArraysOfPrimitives) {
            """
                if (size > 1) {
                    sort()
                    reverse()
                }
            """
        }
    }

    templates add f("sortedDescending()") {
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
            return sortedWith(reverseOrder())
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
        sequenceClassification(intermediate, stateful)
    }

    templates add f("sortedArrayDescending()") {
        only(InvariantArraysOfObjects, ArraysOfPrimitives)
        exclude(PrimitiveType.Boolean)
        doc {
            "Returns an array with all elements of this array sorted descending according to their natural sort order."
        }
        typeParam("T : Comparable<T>")
        returns("SELF")
        body(InvariantArraysOfObjects) {
            """
            if (isEmpty()) return this
            return this.copyOf().apply { sortWith(reverseOrder()) }
            """

        }
        body() {
            """
            if (isEmpty()) return this
            return this.copyOf().apply { sortDescending() }
            """
        }
    }

    templates add f("sortedWith(comparator: Comparator<in T>)") {
        returns("List<T>")
        doc {
            """
            Returns a list of all elements sorted according to the specified [comparator].
            """
        }
        body {
            """
             if (this is Collection) {
                if (size <= 1) return this.toList()
                @Suppress("UNCHECKED_CAST")
                return (toTypedArray<Any?>() as Array<T>).apply { sortWith(comparator) }.asList()
            }
            return toMutableList().apply { sortWith(comparator) }
            """
        }
        body(ArraysOfPrimitives) {
            """
            return toTypedArray().apply { sortWith(comparator) }.asList()
            """
        }
        body(ArraysOfObjects) {
            """
            return sortedArrayWith(comparator).asList()
            """
        }

        returns("SELF", Sequences)
        doc(Sequences) {
            "Returns a sequence that yields elements of this sequence sorted according to the specified [comparator]."
        }
        sequenceClassification(intermediate, stateful)
        body(Sequences) {
            """
            return object : Sequence<T> {
                override fun iterator(): Iterator<T> {
                    val sortedList = this@sortedWith.toMutableList()
                    sortedList.sortWith(comparator)
                    return sortedList.iterator()
                }
            }
            """
        }
    }

    templates add f("sortedArrayWith(comparator: Comparator<in T>)") {
        only(ArraysOfObjects)
        doc {
            "Returns an array with all elements of this array sorted according the specified [comparator]."
        }
        returns("SELF")
        body() {
            """
            if (isEmpty()) return this
            return this.copyOf().apply { sortWith(comparator) }
            """
        }
    }

    templates add f("sortBy(crossinline selector: (T) -> R?)") {
        inline(true)
        only(Lists, ArraysOfObjects)
        doc { f -> """Sorts elements in the ${f.collection} in-place according to natural sort order of the value returned by specified [selector] function.""" }
        returns("Unit")
        typeParam("R : Comparable<R>")
        customReceiver(Lists) { "MutableList<T>" }

        body { """if (size > 1) sortWith(compareBy(selector))""" }
    }

    templates add f("sortedBy(crossinline selector: (T) -> R?)") {
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
        sequenceClassification(intermediate, stateful)

        body {
            "return sortedWith(compareBy(selector))"
        }
    }

    templates add f("sortByDescending(crossinline selector: (T) -> R?)") {
        inline(true)
        only(Lists, ArraysOfObjects)
        doc { f -> """Sorts elements in the ${f.collection} in-place descending according to natural sort order of the value returned by specified [selector] function.""" }
        returns("Unit")
        typeParam("R : Comparable<R>")
        customReceiver(Lists) { "MutableList<T>" }

        body {
            """if (size > 1) sortWith(compareByDescending(selector))""" }
    }

    templates add f("sortedByDescending(crossinline selector: (T) -> R?)") {
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
        sequenceClassification(intermediate, stateful)

        body {
            "return sortedWith(compareByDescending(selector))"
        }
    }

    return templates
}
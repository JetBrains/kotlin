/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package templates

import templates.Family.*
import templates.SequenceClass.*

object Ordering : TemplateGroupBase() {

    init {
        defaultBuilder {
            specialFor(ArraysOfUnsigned) {
                since("1.3")
                annotation("@ExperimentalUnsignedTypes")
            }
        }
    }

    val f_reverse = fn("reverse()") {
        include(Lists, InvariantArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        doc { "Reverses ${f.element.pluralize()} in the ${f.collection} in-place." }
        returns("Unit")
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
        specialFor(ArraysOfUnsigned) {
            inlineOnly()
            body {
                """
                storage.reverse()
                """
            }
        }
        specialFor(Lists) {
            receiver("MutableList<T>")
            on(Platform.JVM) {
                body { """java.util.Collections.reverse(this)""" }
            }
        }
    }

    val f_reversed = fn("reversed()") {
        include(Iterables, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned, CharSequences, Strings)
    } builder {
        doc { "Returns a list with elements in reversed order." }
        returns("List<T>")
        body {
            """
            if (this is Collection && size <= 1) return toList()
            val list = toMutableList()
            list.reverse()
            return list
            """
        }

        body(ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned) {
            """
            if (isEmpty()) return emptyList()
            val list = toMutableList()
            list.reverse()
            return list
            """
        }

        specialFor(CharSequences, Strings) {
            returns("SELF")
            doc { "Returns a ${f.collection} with characters in reversed order." }
        }
        body(CharSequences) { "return StringBuilder(this).reverse()" }
        specialFor(Strings) { inlineOnly() }
        body(Strings) { "return (this as CharSequence).reversed().toString()" }

    }

    val f_reversedArray = fn("reversedArray()") {
        include(InvariantArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        doc { "Returns an array with elements of this array in reversed order." }
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
        specialFor(ArraysOfUnsigned) {
            inlineOnly()
            body {
                """
                return SELF(storage.reversedArray())
                """
            }
        }
    }

    val stableSortNote =
        "The sort is _stable_. It means that equal elements preserve their order relative to each other after sorting."

    fun MemberBuilder.appendStableSortNote() {
        doc {
            doc.orEmpty().trimIndent() + "\n\n" + stableSortNote
        }
    }

    val f_sorted = fn("sorted()") {
        includeDefault()
        exclude(PrimitiveType.Boolean)
        include(ArraysOfUnsigned)
    } builder {

        doc {
            """
            Returns a list of all elements sorted according to their natural sort order.
            """
        }
        if (f != ArraysOfPrimitives && f != ArraysOfUnsigned) {
            appendStableSortNote()
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
        body(ArraysOfUnsigned) {
            """
            return copyOf().apply { sort() }.asList()
            """
        }
        body(ArraysOfObjects) {
            """
            return sortedArray().asList()
            """
        }

        specialFor(Sequences) {
            returns("SELF")
            doc {
                "Returns a sequence that yields elements of this sequence sorted according to their natural sort order."
            }
            appendStableSortNote()
            sequenceClassification(intermediate, stateful)
        }
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

    val f_sortedArray = fn("sortedArray()") {
        include(InvariantArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
        exclude(PrimitiveType.Boolean)
    } builder {
        doc {
            "Returns an array with all elements of this array sorted according to their natural sort order."
        }
        specialFor(InvariantArraysOfObjects) {
            appendStableSortNote()
        }
        typeParam("T : Comparable<T>")
        returns("SELF")
        body {
            """
            if (isEmpty()) return this
            return this.copyOf().apply { sort() }
            """
        }
    }

    val f_sortDescending = fn("sortDescending()") {
        include(Lists, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
        exclude(PrimitiveType.Boolean)
    } builder {
        doc { """Sorts elements in the ${f.collection} in-place descending according to their natural sort order.""" }
        if (f != ArraysOfPrimitives && f != ArraysOfUnsigned) {
            appendStableSortNote()
        }
        returns("Unit")
        typeParam("T : Comparable<T>")
        specialFor(Lists) {
            receiver("MutableList<T>")
        }

        body { """sortWith(reverseOrder())""" }
        body(ArraysOfPrimitives, ArraysOfUnsigned) {
            """
                if (size > 1) {
                    sort()
                    reverse()
                }
            """
        }
    }

    val f_sortedDescending = fn("sortedDescending()") {
        includeDefault()
        exclude(PrimitiveType.Boolean)
        include(ArraysOfUnsigned)
    } builder {

        doc {
            """
            Returns a list of all elements sorted descending according to their natural sort order.
            """
        }
        if (f != ArraysOfPrimitives) {
            appendStableSortNote()
        }
        returns("List<T>")
        typeParam("T : Comparable<T>")
        body {
            """
            return sortedWith(reverseOrder())
            """
        }
        body(ArraysOfPrimitives, ArraysOfUnsigned) {
            """
            return copyOf().apply { sort() }.reversed()
            """
        }

        specialFor(Sequences) {
            returns("SELF")
            doc {
                "Returns a sequence that yields elements of this sequence sorted descending according to their natural sort order."
            }
            appendStableSortNote()
            sequenceClassification(intermediate, stateful)
        }
    }

    val f_sortedArrayDescending = fn("sortedArrayDescending()") {
        include(InvariantArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
        exclude(PrimitiveType.Boolean)
    } builder {
        doc {
            "Returns an array with all elements of this array sorted descending according to their natural sort order."
        }
        specialFor(InvariantArraysOfObjects) {
            appendStableSortNote()
        }
        typeParam("T : Comparable<T>")
        returns("SELF")
        body(InvariantArraysOfObjects) {
            """
            if (isEmpty()) return this
            return this.copyOf().apply { sortWith(reverseOrder()) }
            """
        }
        body(ArraysOfPrimitives, ArraysOfUnsigned) {
            """
            if (isEmpty()) return this
            return this.copyOf().apply { sortDescending() }
            """
        }
    }

    val f_sortedWith = fn("sortedWith(comparator: Comparator<in T>)") {
        includeDefault()
    } builder {
        returns("List<T>")
        doc {
            """
            Returns a list of all elements sorted according to the specified [comparator].
            """
        }
        if (f != ArraysOfPrimitives) {
            appendStableSortNote()
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

        specialFor(Sequences) {
            returns("SELF")
            doc {
                "Returns a sequence that yields elements of this sequence sorted according to the specified [comparator]."
            }
            appendStableSortNote()
            sequenceClassification(intermediate, stateful)
        }
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

    val f_sortedArrayWith = fn("sortedArrayWith(comparator: Comparator<in T>)") {
        include(ArraysOfObjects)
    } builder {
        doc {
            "Returns an array with all elements of this array sorted according the specified [comparator]."
        }
        appendStableSortNote()
        returns("SELF")
        body {
            """
            if (isEmpty()) return this
            return this.copyOf().apply { sortWith(comparator) }
            """
        }
    }

    val f_sortBy = fn("sortBy(crossinline selector: (T) -> R?)") {
        include(Lists, ArraysOfObjects)
    } builder {
        inline()
        doc { """Sorts elements in the ${f.collection} in-place according to natural sort order of the value returned by specified [selector] function.""" }
        appendStableSortNote()
        returns("Unit")
        typeParam("R : Comparable<R>")
        specialFor(Lists) { receiver("MutableList<T>") }

        body { """if (size > 1) sortWith(compareBy(selector))""" }
    }

    val f_sortedBy = fn("sortedBy(crossinline selector: (T) -> R?)") {
        includeDefault()
    } builder {
        inline()
        returns("List<T>")
        typeParam("R : Comparable<R>")

        doc {
            """
            Returns a list of all elements sorted according to natural sort order of the value returned by specified [selector] function.
            """
        }
        if (f != ArraysOfPrimitives) {
            appendStableSortNote()
        }

        specialFor(Sequences) {
            returns("SELF")
            doc {
                "Returns a sequence that yields elements of this sequence sorted according to natural sort order of the value returned by specified [selector] function."
            }
            appendStableSortNote()
            sequenceClassification(intermediate, stateful)
        }
        body {
            "return sortedWith(compareBy(selector))"
        }
    }

    val f_sortByDescending = fn("sortByDescending(crossinline selector: (T) -> R?)") {
        include(Lists, ArraysOfObjects)
    } builder {
        inline()
        doc { """Sorts elements in the ${f.collection} in-place descending according to natural sort order of the value returned by specified [selector] function.""" }
        appendStableSortNote()
        returns("Unit")
        typeParam("R : Comparable<R>")
        specialFor(Lists) { receiver("MutableList<T>") }

        body {
            """if (size > 1) sortWith(compareByDescending(selector))""" }
    }

    val f_sortedByDescending = fn("sortedByDescending(crossinline selector: (T) -> R?)") {
        includeDefault()
    } builder {
        inline()
        returns("List<T>")
        typeParam("R : Comparable<R>")

        doc {
            """
            Returns a list of all elements sorted descending according to natural sort order of the value returned by specified [selector] function.
            """
        }
        if (f != ArraysOfPrimitives) {
            appendStableSortNote()
        }

        specialFor(Sequences) {
            returns("SELF")
            doc {
                "Returns a sequence that yields elements of this sequence sorted descending according to natural sort order of the value returned by specified [selector] function."
            }
            appendStableSortNote()
            sequenceClassification(intermediate, stateful)
        }

        body {
            "return sortedWith(compareByDescending(selector))"
        }
    }

}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package templates

import templates.ArrayOps.rangeDoc
import templates.Family.*
import templates.SequenceClass.*

object Ordering : TemplateGroupBase() {

    init {
        defaultBuilder {
            specialFor(ArraysOfUnsigned) {
                sinceAtLeast("1.3")
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

    val f_reverse_range = fn("reverse(fromIndex: Int, toIndex: Int)") {
        include(InvariantArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        since("1.4")
        doc {
            """
            Reverses elements of the ${f.collection} in the specified range in-place.
            
            ${rangeDoc(hasDefault = false, action = "reverse")}
            """
        }
        returns("Unit")
        body {
            """
            AbstractList.checkRangeIndexes(fromIndex, toIndex, size)
            val midPoint = (fromIndex + toIndex) / 2
            if (fromIndex == midPoint) return
            var reverseIndex = toIndex - 1
            for (index in fromIndex until midPoint) {
                val tmp = this[index]
                this[index] = this[reverseIndex]
                this[reverseIndex] = tmp
                reverseIndex--
            }
            """
        }
        specialFor(ArraysOfUnsigned) {
            inlineOnly()
            body { """storage.reverse(fromIndex, toIndex)""" }
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

    val stableSortBySelectorNote = """
        The sort is _stable_. It means that elements for which [selector] returned equal values preserve their order 
        relative to each other after sorting.
    """.trimIndent()

    fun MemberBuilder.appendStableSortBySelectorNote() {
        doc {
            doc.orEmpty().trimIndent() + "\n\n" + stableSortBySelectorNote
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
        if (f != ArraysOfPrimitives && f != ArraysOfUnsigned) {
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
        appendStableSortBySelectorNote()
        returns("Unit")
        typeParam("R : Comparable<R>")
        specialFor(Lists) { receiver("MutableList<T>") }
        
        sample("samples.collections.Collections.Sorting.sortBy")

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
        appendStableSortBySelectorNote()

        when (f) {
            ArraysOfPrimitives -> {
                sample("samples.collections.Collections.Sorting.sortedPrimitiveArrayBy")
            }
            Sequences -> {
                sample("samples.collections.Sequences.Sorting.sortedBy")
            }
            else -> {
                sample("samples.collections.Collections.Sorting.sortedBy")
            }
        }

        specialFor(Sequences) {
            returns("SELF")
            doc {
                "Returns a sequence that yields elements of this sequence sorted according to natural sort order of the value returned by specified [selector] function."
            }
            appendStableSortBySelectorNote()
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
        appendStableSortBySelectorNote()
        returns("Unit")
        typeParam("R : Comparable<R>")
        specialFor(Lists) { receiver("MutableList<T>") }
        
        sample("samples.collections.Collections.Sorting.sortByDescending")

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
        appendStableSortBySelectorNote()

        when (f) {
            ArraysOfPrimitives -> {
                sample("samples.collections.Collections.Sorting.sortedPrimitiveArrayByDescending")
            }
            Sequences -> {
                sample("samples.collections.Sequences.Sorting.sortedByDescending")
            }
            else -> {
                sample("samples.collections.Collections.Sorting.sortedByDescending")
            }
        }

        specialFor(Sequences) {
            returns("SELF")
            doc {
                "Returns a sequence that yields elements of this sequence sorted descending according to natural sort order of the value returned by specified [selector] function."
            }
            appendStableSortBySelectorNote()
            sequenceClassification(intermediate, stateful)
        }

        body {
            "return sortedWith(compareByDescending(selector))"
        }
    }


    val f_shuffle = fn("shuffle()") {
        include(InvariantArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        since("1.4")
        returns("Unit")
        doc {
            """
            Randomly shuffles elements in this ${f.collection} in-place.
            """
        }
        body {
            "shuffle(Random)"
        }
    }

    val f_shuffleRandom = fn("shuffle(random: Random)") {
        include(Lists, InvariantArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        since("1.4")
        returns("Unit")
        doc {
            """
            Randomly shuffles elements in this ${f.collection} in-place using the specified [random] instance as the source of randomness.
            
            See: [A modern version of Fisher-Yates shuffle algorithm](https://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle#The_modern_algorithm).
            """
        }
        specialFor(Lists) {
            since("1.3")
            receiver("MutableList<T>")
        }
        body {
            """
            for (i in lastIndex downTo 1) {
                val j = random.nextInt(i + 1)
                val copy = this[i]
                this[i] = this[j]
                this[j] = copy
            }
            """
        }
        specialFor(Lists) {
            body {
                """
                for (i in lastIndex downTo 1) {
                    val j = random.nextInt(i + 1)
                    this[j] = this.set(i, this[j])
                }
                """
            }
        }
    }

    private fun MemberBuilder.isSortedSampleRef(methodName: String): String = "samples.generated.issorted." + when (f) {
        ArraysOfObjects -> "IsSortedArraySamples.$methodName"
        ArraysOfPrimitives, ArraysOfUnsigned -> "IsSorted${primitive!!.name}ArraySamples.$methodName"
        else -> "IsSorted${f}Samples.$methodName"
    }

    private fun MemberBuilder.appendIterationOrderNote() {
        if (f == Iterables || f == Sequences) {
            doc {
                doc + """
                Note that the result depends on the iteration order of the ${f.collection}.
                The iteration order of some [${f.toString().dropLast(1)}] implementations may be unstable
                (change from one invocation to the next),
                in which case this function may return inconsistent results.
                """
            }
        }
    }

    private fun MemberBuilder.comparedUsingPhrase(): String = when (f) {
        ArraysOfPrimitives if primitive?.isFloatingPoint() == true -> " using [${primitive!!.name}.compareTo]"
        ArraysOfPrimitives, ArraysOfUnsigned -> ""
        else -> " using [Comparable.compareTo]"
    }

    private fun MemberBuilder.appendFloatingPointNote() {
        if (f == ArraysOfPrimitives && primitive?.isFloatingPoint() == true) {
            doc {
                doc + """
                For floating-point arrays, `NaN` is considered greater than any other value
                (including positive infinity), and `-0.0` is considered less than `0.0`,
                consistent with [${primitive!!.name}.compareTo].
                """
            }
        }
        if (f == Iterables || f == Sequences || f == ArraysOfObjects) {
            doc {
                doc + """
                For elements of floating-point types (`Double`, `Float`), `NaN` is considered greater
                than any other value (including positive infinity), and `-0.0` is considered less than `0.0`,
                consistent with [Double.compareTo] and [Float.compareTo].
                """
            }
        }
    }

    val f_isSortedWith = fn("isSortedWith(comparator: Comparator<in T>)") {
        includeDefault()
        include(ArraysOfUnsigned)
    } builder {
        since("2.4")
        returns("Boolean")
        specialFor(Sequences) { sequenceClassification(terminal) }
        doc {
            """
            Returns `true` if each element in the ${f.collection} is less than or equal
            to the following element according to the specified [comparator].

            Returns `true` if the ${f.collection} has fewer than two elements.

            The elements are compared sequentially using [Comparator.compare],
            and the ${f.collection} is considered sorted if for each pair of adjacent elements
            the preceding element is not greater than the following one.
            """
        }
        appendIterationOrderNote()
        sample(isSortedSampleRef("isSortedWith"))
        body {
            """
            val iterator = iterator()
            if (!iterator.hasNext()) return true
            var current = iterator.next()
            while (iterator.hasNext()) {
                val next = iterator.next()
                if (comparator.compare(current, next) > 0) return false
                current = next
            }
            return true
            """
        }
        body(ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned) {
            """
            for (i in 1..lastIndex) {
                if (comparator.compare(this[i - 1], this[i]) > 0) return false
            }
            return true
            """
        }
    }

    val f_isSorted = fn("isSorted()") {
        includeDefault()
        include(ArraysOfUnsigned)
    } builder {
        since("2.4")
        returns("Boolean")
        typeParam("T : Comparable<T>")
        specialFor(Sequences) { sequenceClassification(terminal) }
        doc {
            """
            Returns `true` if each element in the ${f.collection} is less than or equal
            to the following element according to their natural sort order.

            Returns `true` if the ${f.collection} has fewer than two elements.

            The elements are compared sequentially${comparedUsingPhrase()},
            and the ${f.collection} is considered sorted if for each pair of adjacent elements
            the preceding element is not greater than the following one.
            """
        }
        appendIterationOrderNote()
        appendFloatingPointNote()
        sample(isSortedSampleRef("isSorted"))
        body { "return isSortedWith(naturalOrder())" }
        body(ArraysOfPrimitives, ArraysOfUnsigned) {
            val condition = if (primitive?.isFloatingPoint() == true)
                "this[i - 1].compareTo(this[i]) > 0"
            else
                "this[i - 1] > this[i]"
            """
            for (i in 1..lastIndex) {
                if ($condition) return false
            }
            return true
            """
        }
    }

    val f_isSortedDescending = fn("isSortedDescending()") {
        includeDefault()
        include(ArraysOfUnsigned)
    } builder {
        since("2.4")
        returns("Boolean")
        typeParam("T : Comparable<T>")
        specialFor(Sequences) { sequenceClassification(terminal) }
        doc {
            """
            Returns `true` if each element in the ${f.collection} is greater than or equal
            to the following element according to their natural sort order.

            Returns `true` if the ${f.collection} has fewer than two elements.

            The elements are compared sequentially${comparedUsingPhrase()},
            and the ${f.collection} is considered sorted in descending order if for each
            pair of adjacent elements the preceding element is not less than the following one.
            """
        }
        appendIterationOrderNote()
        appendFloatingPointNote()
        sample(isSortedSampleRef("isSortedDescending"))
        body { "return isSortedWith(reverseOrder())" }
        body(ArraysOfPrimitives, ArraysOfUnsigned) {
            val condition = if (primitive?.isFloatingPoint() == true)
                "this[i - 1].compareTo(this[i]) < 0"
            else
                "this[i - 1] < this[i]"
            """
            for (i in 1..lastIndex) {
                if ($condition) return false
            }
            return true
            """
        }
    }

    val f_isSortedBy = fn("isSortedBy(selector: (T) -> R?)") {
        includeDefault()
        include(ArraysOfUnsigned)
    } builder {
        since("2.4")
        inline()
        returns("Boolean")
        typeParam("R : Comparable<R>")
        specialFor(Sequences) { sequenceClassification(terminal) }
        doc {
            """
            Returns `true` if each element in the ${f.collection} yields a [selector] value
            that is less than or equal to the [selector] value of the following element
            according to the natural sort order of the selector values.

            Returns `true` if the ${f.collection} has fewer than two elements.

            The [selector] values of adjacent elements are compared sequentially using [compareValues],
            and the ${f.collection} is considered sorted if for each pair of adjacent elements
            the [selector] value of the preceding element is not greater than that of the following one.

            If the [selector] returns `null` for an element, the `null` value is treated as less than any non-null value.
            """
        }
        appendIterationOrderNote()
        sample(isSortedSampleRef("isSortedBy"))
        body {
            """
            val iterator = iterator()
            if (!iterator.hasNext()) return true
            val previous = iterator.next()
            if (!iterator.hasNext()) return true
            var previousValue = selector(previous)
            while (iterator.hasNext()) {
                val currentValue = selector(iterator.next())
                if (compareValues(previousValue, currentValue) > 0) return false
                previousValue = currentValue
            }
            return true
            """
        }
        body(ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned) {
            """
            if (size < 2) return true
            var previousValue = selector(this[0])
            for (i in 1..lastIndex) {
                val currentValue = selector(this[i])
                if (compareValues(previousValue, currentValue) > 0) return false
                previousValue = currentValue
            }
            return true
            """
        }
    }

    val f_isSortedByDescending = fn("isSortedByDescending(selector: (T) -> R?)") {
        includeDefault()
        include(ArraysOfUnsigned)
    } builder {
        since("2.4")
        inline()
        returns("Boolean")
        typeParam("R : Comparable<R>")
        specialFor(Sequences) { sequenceClassification(terminal) }
        doc {
            """
            Returns `true` if each element in the ${f.collection} yields a [selector] value
            that is greater than or equal to the [selector] value of the following element
            according to the natural sort order of the selector values.

            Returns `true` if the ${f.collection} has fewer than two elements.

            The [selector] values of adjacent elements are compared sequentially using [compareValues],
            and the ${f.collection} is considered sorted in descending order if for each pair
            of adjacent elements the [selector] value of the preceding element is not less
            than that of the following one.

            If the [selector] returns `null` for an element, the `null` value is treated as less than any non-null value.
            """
        }
        appendIterationOrderNote()
        sample(isSortedSampleRef("isSortedByDescending"))
        body {
            """
            val iterator = iterator()
            if (!iterator.hasNext()) return true
            val previous = iterator.next()
            if (!iterator.hasNext()) return true
            var previousValue = selector(previous)
            while (iterator.hasNext()) {
                val currentValue = selector(iterator.next())
                if (compareValues(previousValue, currentValue) < 0) return false
                previousValue = currentValue
            }
            return true
            """
        }
        body(ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned) {
            """
            if (size < 2) return true
            var previousValue = selector(this[0])
            for (i in 1..lastIndex) {
                val currentValue = selector(this[i])
                if (compareValues(previousValue, currentValue) < 0) return false
                previousValue = currentValue
            }
            return true
            """
        }
    }
}

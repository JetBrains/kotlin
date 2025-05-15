/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package templates

import templates.Family.*
import templates.SequenceClass.*

object Filtering : TemplateGroupBase() {

    init {
        val terminalOperationPattern = Regex("^\\w+To")
        defaultBuilder {
            if (terminalOperationPattern in signature)
                sequenceClassification(terminal)
            else
                sequenceClassification(intermediate, stateless)

            specialFor(ArraysOfUnsigned) {
                sinceAtLeast("1.3")
                annotation("@ExperimentalUnsignedTypes")
            }
        }
    }

    private fun subsequence(f: Family, start: String, end: String? = null): String {
        return when (f) {
            Strings -> "substring(${listOfNotNull(start, end).joinToString()})"
            CharSequences -> "subSequence($start, ${end ?: "length"})"
            else -> throw UnsupportedOperationException(f.toString())
        }
    }

    private fun sampleClass(f: Family): String = when(f) {
        Strings, CharSequences -> "samples.text.Strings"
        else -> "samples.collections.Collections.Transformations"
    }

    private fun toResult(f: Family): String = if (f == CharSequences) "" else ".toString()"

    private fun takeAll(f: Family): String = if (f == Strings) "this" else subsequence(f, "0")

    val f_drop = fn("drop(n: Int)") {
        includeDefault()
        include(CharSequences, Strings, ArraysOfUnsigned)
    } builder {
        val n = "\$n"
        doc { 
            """
            Returns a list containing all elements except first [n] elements.
            """
        }
        throws("IllegalArgumentException", "if [n] is negative.")
        sample("${sampleClass(f)}.drop")
        returns("List<T>")
        body {
            """
            require(n >= 0) { "Requested element count $n is less than zero." }
            if (n == 0) return toList()
            val list: ArrayList<T>
            if (this is Collection<*>) {
                val resultSize = size - n
                if (resultSize <= 0)
                    return emptyList()
                if (resultSize == 1)
                    return listOf(last())

                list = ArrayList<T>(resultSize)
                if (this is List<T>) {
                    if (this is RandomAccess) {
                        for (index in n until size)
                            list.add(this[index])
                    } else {
                        for (item in listIterator(n))
                            list.add(item)
                    }
                    return list
                }
            }
            else {
                list = ArrayList<T>()
            }
            var count = 0
            for (item in this) {
                if (count >= n) list.add(item) else ++count
            }
            return list.optimizeReadOnlyList()
            """
        }

        specialFor(Sequences) {
            doc { "Returns a sequence containing all elements except first [n] elements." }
            returns("Sequence<T>")
        }
        body(Sequences) {
            """
            require(n >= 0) { "Requested element count $n is less than zero." }
            return when {
                n == 0 -> this
                this is DropTakeSequence -> this.drop(n)
                else -> DropSequence(this, n)
            }
            """
        }

        specialFor(Strings, CharSequences) {
            returns("SELF")
            specialFor(Strings) {
                doc { "Returns a string with the first [n] characters removed."}
            }
            specialFor(CharSequences) {
                doc { "Returns a subsequence of this char sequence with the first [n] characters removed."}
            }
        }
        body(Strings, CharSequences) {
            """
            require(n >= 0) { "Requested character count $n is less than zero." }
            return ${subsequence(f, "n.coerceAtMost(length)")}
            """
        }

        body(ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned) {
            """
            require(n >= 0) { "Requested element count $n is less than zero." }
            return takeLast((size - n).coerceAtLeast(0))
            """
        }
    }

    val f_take = fn("take(n: Int)") {
        includeDefault()
        include(CharSequences, Strings, ArraysOfUnsigned)
    } builder {
        val n = "\$n"
        doc {
            """
            Returns a list containing first [n] elements.
            """
        }
        throws("IllegalArgumentException", "if [n] is negative.")
        sample("${sampleClass(f)}.take")
        returns("List<T>")
        body {
            """
            require(n >= 0) { "Requested element count $n is less than zero." }
            if (n == 0) return emptyList()
            if (this is Collection<T>) {
                if (n >= size) return toList()
                if (n == 1) return listOf(first())
            }
            var count = 0
            val list = ArrayList<T>(n)
            for (item in this) {
                list.add(item)
                if (++count == n)
                    break
            }
            return list.optimizeReadOnlyList()
            """
        }

        specialFor(Strings, CharSequences) {
            returns("SELF")
            specialFor(Strings) {
                doc { "Returns a string containing the first [n] characters from this string, or the entire string if this string is shorter." }
            }
            specialFor(CharSequences) {
                doc { "Returns a subsequence of this char sequence containing the first [n] characters from this char sequence, or the entire char sequence if this char sequence is shorter." }
            }
        }
        body(Strings, CharSequences) {
            """
            require(n >= 0) { "Requested character count $n is less than zero." }
            return ${subsequence(f, "0", "n.coerceAtMost(length)")}
            """
        }

        specialFor(Sequences) {
            doc { "Returns a sequence containing first [n] elements." }
            returns("Sequence<T>")
        }
        body(Sequences) {
            """
            require(n >= 0) { "Requested element count $n is less than zero." }
            return when {
                n == 0 -> emptySequence()
                this is DropTakeSequence -> this.take(n)
                else -> TakeSequence(this, n)
            }
            """
        }

        body(ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned) {
            """
            require(n >= 0) { "Requested element count $n is less than zero." }
            if (n == 0) return emptyList()
            if (n >= size) return toList()
            if (n == 1) return listOf(this[0])
            var count = 0
            val list = ArrayList<T>(n)
            for (item in this) {
                list.add(item)
                if (++count == n)
                    break
            }
            return list
            """
        }
    }

    val f_dropLast = fn("dropLast(n: Int)") {
        include(Lists, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned, CharSequences, Strings)
    } builder {
        val n = "\$n"

        doc { 
            """
            Returns a list containing all elements except last [n] elements.
            """
        }
        throws("IllegalArgumentException", "if [n] is negative.")
        sample("${sampleClass(f)}.drop")
        returns("List<T>")
        body {
            """
            require(n >= 0) { "Requested ${f.doc.element} count $n is less than zero." }
            return take((${f.code.size} - n).coerceAtLeast(0))
            """
        }

        specialFor(Strings, CharSequences) {
            returns("SELF")
            specialFor(Strings) {
                doc { "Returns a string with the last [n] characters removed." }
            }
            specialFor(CharSequences) {
                doc { "Returns a subsequence of this char sequence with the last [n] characters removed." }
            }
        }
    }

    val f_takeLast = fn("takeLast(n: Int)") {
        include(Lists, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned, CharSequences, Strings)
    } builder {
        val n = "\$n"
        doc { 
            """
            Returns a list containing last [n] elements.
            """
        }
        throws("IllegalArgumentException", "if [n] is negative.")
        sample("${sampleClass(f)}.take")
        returns("List<T>")
        specialFor(Strings, CharSequences) {
            returns("SELF")
            specialFor(Strings) {
                doc { "Returns a string containing the last [n] characters from this string, or the entire string if this string is shorter."}
            }
            specialFor(CharSequences) {
                doc { "Returns a subsequence of this char sequence containing the last [n] characters from this char sequence, or the entire char sequence if this char sequence is shorter."}
            }
        }
        body(Strings, CharSequences) {
            """
            require(n >= 0) { "Requested character count $n is less than zero." }
            val length = length
            return ${subsequence(f, "length - n.coerceAtMost(length)")}
            """
        }

        body(ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned) {
            """
            require(n >= 0) { "Requested element count $n is less than zero." }
            if (n == 0) return emptyList()
            val size = size
            if (n >= size) return toList()
            if (n == 1) return listOf(this[size - 1])

            val list = ArrayList<T>(n)
            for (index in size - n until size)
                list.add(this[index])
            return list
            """
        }
        body(Lists) {
            """
            require(n >= 0) { "Requested element count $n is less than zero." }
            if (n == 0) return emptyList()
            val size = size
            if (n >= size) return toList()
            if (n == 1) return listOf(last())

            val list = ArrayList<T>(n)
            if (this is RandomAccess) {
                for (index in size - n until size)
                    list.add(this[index])
            } else {
                for (item in listIterator(size - n))
                    list.add(item)
            }
            return list
            """
        }
    }

    val f_dropWhile = fn("dropWhile(predicate: (T) -> Boolean)") {
        includeDefault()
        include(CharSequences, Strings, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { 
            """
            Returns a list containing all elements except first elements that satisfy the given [predicate].
            """
        }
        sample("${sampleClass(f)}.drop")
        returns("List<T>")
        body {
            """
            var yielding = false
            val list = ArrayList<T>()
            for (item in this)
                if (yielding)
                    list.add(item)
                else if (!predicate(item)) {
                    list.add(item)
                    yielding = true
                }
            return list
            """
        }

        specialFor(Strings, CharSequences) {
            returns("SELF")
            specialFor(Strings) {
                doc { "Returns a string containing all characters except first characters that satisfy the given [predicate]." }
            }
            specialFor(CharSequences) {
                doc { "Returns a subsequence of this char sequence containing all characters except first characters that satisfy the given [predicate]." }
            }
        }
        body(Strings, CharSequences) {
            """
            for (index in this.indices)
                if (!predicate(this[index]))
                    return ${subsequence(f, "index")}

            return ""
            """
        }

        specialFor(Sequences) {
            inline(Inline.No)
            doc { "Returns a sequence containing all elements except first elements that satisfy the given [predicate]." }
            returns("Sequence<T>")
            body { """return DropWhileSequence(this, predicate)""" }
        }

    }

    val f_takeWhile = fn("takeWhile(predicate: (T) -> Boolean)") {
        includeDefault()
        include(CharSequences, Strings, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { 
            """
            Returns a list containing first elements satisfying the given [predicate].
            """
        }
        sample("${sampleClass(f)}.take")
        returns("List<T>")
        body {
            """
            val list = ArrayList<T>()
            for (item in this) {
                if (!predicate(item))
                    break
                list.add(item)
            }
            return list
            """
        }

        specialFor(Strings, CharSequences) {
            returns("SELF")
            specialFor(Strings) {
                doc { "Returns a string containing the first characters that satisfy the given [predicate]."}
            }
            specialFor(CharSequences) {
                doc { "Returns a subsequence of this char sequence containing the first characters that satisfy the given [predicate]."}
            }
        }
        body(Strings, CharSequences) {
            """
            for (index in 0 until length)
                if (!predicate(get(index))) {
                    return ${subsequence(f, "0", "index")}
                }
            return ${takeAll(f)}
            """
        }

        specialFor(Sequences) {
            inline(Inline.No)
            doc { "Returns a sequence containing first elements satisfying the given [predicate]." }
            returns("Sequence<T>")
            body { """return TakeWhileSequence(this, predicate)""" }
        }
    }

    val f_dropLastWhile = fn("dropLastWhile(predicate: (T) -> Boolean)") {
        include(Lists, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned, CharSequences, Strings)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { 
            """
            Returns a list containing all elements except last elements that satisfy the given [predicate].
            """
        }
        sample("${sampleClass(f)}.drop")
        returns("List<T>")

        body {
            """
            for (index in lastIndex downTo 0) {
                if (!predicate(this[index])) {
                    return take(index + 1)
                }
            }
            return emptyList()
            """
        }
        body(Lists) {
            """
            if (!isEmpty()) {
                val iterator = listIterator(size)
                while (iterator.hasPrevious()) {
                    if (!predicate(iterator.previous())) {
                        return take(iterator.nextIndex() + 1)
                    }
                }
            }
            return emptyList()
            """
        }

        specialFor(Strings, CharSequences) {
            returns("SELF")
            specialFor(Strings) {
                doc { "Returns a string containing all characters except last characters that satisfy the given [predicate]." }
            }
            specialFor(CharSequences) {
                doc { "Returns a subsequence of this char sequence containing all characters except last characters that satisfy the given [predicate]." }
            }
        }
        body(CharSequences, Strings) {
            """
            for (index in lastIndex downTo 0)
                if (!predicate(this[index]))
                    return ${subsequence(f, "0", "index + 1")}

            return ""
            """
        }
    }

    val f_takeLastWhile = fn("takeLastWhile(predicate: (T) -> Boolean)") {
        include(Lists, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned, CharSequences, Strings)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { 
            """
            Returns a list containing last elements satisfying the given [predicate].
            """
        }
        sample("${sampleClass(f)}.take")
        returns("List<T>")

        body {
            """
            for (index in lastIndex downTo 0) {
                if (!predicate(this[index])) {
                    return drop(index + 1)
                }
            }
            return toList()
            """
        }
        body(Lists) {
            """
            if (isEmpty())
                return emptyList()
            val iterator = listIterator(size)
            while (iterator.hasPrevious()) {
                if (!predicate(iterator.previous())) {
                    val _ = iterator.next()
                    val expectedSize = size - iterator.nextIndex()
                    if (expectedSize == 0) return emptyList()
                    return ArrayList<T>(expectedSize).apply {
                        while (iterator.hasNext())
                            add(iterator.next())
                    }
                }
            }
            return toList()
            """
            // TODO: Use iterator.toList() internal method in 1.1
//            return iterator.toList(size - iterator.nextIndex())
        }

        specialFor(Strings, CharSequences) {
            returns("SELF")
            specialFor(Strings) {
                doc { "Returns a string containing last characters that satisfy the given [predicate]." }
            }
            specialFor(CharSequences) {
                doc { "Returns a subsequence of this char sequence containing last characters that satisfy the given [predicate]." }
            }
        }
        body(Strings, CharSequences) {
            """
            for (index in lastIndex downTo 0) {
                if (!predicate(this[index])) {
                    return ${subsequence(f, "index + 1")}
                }
            }
            return ${takeAll(f)}
            """
        }
    }

    val f_filter = fn("filter(predicate: (T) -> Boolean)") {
        includeDefault()
        include(CharSequences, Strings, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { "Returns a ${f.mapResult} containing only ${f.element.pluralize()} matching the given [predicate]." }
        sample("samples.collections.Collections.Filtering.filter")
        returns("List<T>")
        body {
            """
            return filterTo(ArrayList<T>(), predicate)
            """
        }

        specialFor(Strings, CharSequences) {
            sample("samples.text.Strings.filter")
            returns("SELF")
            doc { "Returns a ${f.collection} containing only those characters from the original ${f.collection} that match the given [predicate]." }
            body { """return filterTo(StringBuilder(), predicate)${toResult(f)}""" }
        }

        specialFor(Sequences) {
            inline(Inline.No)
            returns("Sequence<T>")
            body { """return FilteringSequence(this, true, predicate)""" }
        }
    }

    val f_filterTo = fn("filterTo(destination: C, predicate: (T) -> Boolean)") {
        includeDefault()
        include(CharSequences, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { "Appends all ${f.element.pluralize()} matching the given [predicate] to the given [destination]." }
        sample("samples.collections.Collections.Filtering.filterTo")
        typeParam("C : TCollection")
        returns("C")

        body {
            """
            for (element in this) if (predicate(element)) destination.add(element)
            return destination
            """
        }

        body(CharSequences) {
            """
            for (index in 0 until length) {
                val element = get(index)
                if (predicate(element)) destination.append(element)
            }
            return destination
            """
        }
    }

    val f_filterIndexed = fn("filterIndexed(predicate: (index: Int, T) -> Boolean)") {
        includeDefault()
        include(CharSequences, Strings, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc {
            """
            Returns a ${f.mapResult} containing only ${f.element.pluralize()} matching the given [predicate].
            @param [predicate] function that takes the index of ${f.element.prefixWithArticle()} and the ${f.element} itself
            and returns the result of predicate evaluation on the ${f.element}.
            """
        }
        sample("samples.collections.Collections.Filtering.filterIndexed")
        returns("List<T>")
        body {
            """
            return filterIndexedTo(ArrayList<T>(), predicate)
            """
        }

        specialFor(Strings, CharSequences) {
            returns("SELF")
            doc {
                """
                Returns a ${f.collection} containing only those characters from the original ${f.collection} that match the given [predicate].
                @param [predicate] function that takes the index of ${f.element.prefixWithArticle()} and the ${f.element} itself
                and returns the result of predicate evaluation on the ${f.element}.
                """
            }
            body { """return filterIndexedTo(StringBuilder(), predicate)${toResult(f)}""" }
        }

        specialFor(Sequences) {
            inline(Inline.No)
            returns("Sequence<T>")
            body(Sequences) {
                """
                // TODO: Rewrite with generalized MapFilterIndexingSequence
                return TransformingSequence(FilteringSequence(IndexingSequence(this), true, { predicate(it.index, it.value) }), { it.value })
                """
            }
        }
    }

    val f_filterIndexedTo = fn("filterIndexedTo(destination: C, predicate: (index: Int, T) -> Boolean)") {
        includeDefault()
        include(CharSequences, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc {
            """
            Appends all ${f.element.pluralize()} matching the given [predicate] to the given [destination].
            @param [predicate] function that takes the index of ${f.element.prefixWithArticle()} and the ${f.element} itself
            and returns the result of predicate evaluation on the ${f.element}.
            """ }
        sample("samples.collections.Collections.Filtering.filterIndexedTo")
        typeParam("C : TCollection")
        returns("C")

        body {
            """
            forEachIndexed { index, element ->
                if (predicate(index, element)) destination.${ if (f==CharSequences) "append" else "add" }(element)
            }
            return destination
            """
        }
    }

    val f_filterNot = fn("filterNot(predicate: (T) -> Boolean)") {
        includeDefault()
        include(CharSequences, Strings, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { "Returns a list containing all elements not matching the given [predicate]." }
        sample("samples.collections.Collections.Filtering.filter")
        returns("List<T>")
        body {
            """
            return filterNotTo(ArrayList<T>(), predicate)
            """
        }

        specialFor(Strings, CharSequences) {
            sample("samples.text.Strings.filterNot")
            returns("SELF")
            doc { "Returns a ${f.collection} containing only those characters from the original ${f.collection} that do not match the given [predicate]." }
            body { """return filterNotTo(StringBuilder(), predicate)${toResult(f)}""" }
        }

        specialFor(Sequences) {
            inline(Inline.No)
            doc { "Returns a sequence containing all elements not matching the given [predicate]." }
            returns("Sequence<T>")
            body { """return FilteringSequence(this, false, predicate)""" }
        }
    }

    val f_filterNotTo = fn("filterNotTo(destination: C, predicate: (T) -> Boolean)") {
        includeDefault()
        include(CharSequences, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { "Appends all elements not matching the given [predicate] to the given [destination]." }
        sample("samples.collections.Collections.Filtering.filterTo")
        typeParam("C : TCollection")
        returns("C")

        body {
            """
            for (element in this) if (!predicate(element)) destination.add(element)
            return destination
            """
        }

        specialFor(CharSequences) {
            doc { "Appends all characters not matching the given [predicate] to the given [destination]." }
        }
        body(CharSequences) {
            """
            for (element in this) if (!predicate(element)) destination.append(element)
            return destination
            """
        }
    }

    val f_filterNotNull = fn("filterNotNull()") {
        include(Iterables, Sequences, ArraysOfObjects)
    } builder {
        doc { "Returns a list containing all elements that are not `null`." }
        sample("samples.collections.Collections.Filtering.filterNotNull")
        typeParam("T : Any")
        returns("List<T>")
        toNullableT = true
        body {
            """
            return filterNotNullTo(ArrayList<T>())
            """
        }

        specialFor(Sequences) {
            doc { "Returns a sequence containing all elements that are not `null`." }
            returns("Sequence<T>")
        }
        body(Sequences) {
            """
            @Suppress("UNCHECKED_CAST")
            return filterNot { it == null } as Sequence<T>
            """
        }
    }

    val f_filterNotNullTo = fn("filterNotNullTo(destination: C)") {
        include(Iterables, Sequences, ArraysOfObjects)
    } builder {
        doc { "Appends all elements that are not `null` to the given [destination]." }
        sample("samples.collections.Collections.Filtering.filterNotNullTo")
        returns("C")
        typeParam("C : TCollection")
        typeParam("T : Any")
        toNullableT = true
        body {
            """
            for (element in this) if (element != null) destination.add(element)
            return destination
            """
        }
    }

    val f_filterIsInstanceTo = fn("filterIsInstanceTo(destination: C)") {
        include(Iterables, Sequences, ArraysOfObjects)
    } builder {
        doc { "Appends all elements that are instances of specified type parameter R to the given [destination]." }
        sample("samples.collections.Collections.Filtering.filterIsInstanceTo")
        typeParam("reified R")
        typeParam("C : MutableCollection<in R>")
        inline()
        genericStarProjection = true
        returns("C")
        body {
            """
            for (element in this) if (element is R) destination.add(element)
            return destination
            """
        }
    }

    val f_filterIsInstance = fn("filterIsInstance()") {
        include(Iterables, Sequences, ArraysOfObjects)
    } builder {
        doc { "Returns a list containing all elements that are instances of specified type parameter R." }
        sample("samples.collections.Collections.Filtering.filterIsInstance")
        typeParam("reified R")
        returns("List<@kotlin.internal.NoInfer R>")
        inline()
        genericStarProjection = true
        body {
            """
            return filterIsInstanceTo(ArrayList<R>())
            """
        }

        specialFor(Sequences) {
            doc { "Returns a sequence containing all elements that are instances of specified type parameter R." }
            returns("Sequence<@kotlin.internal.NoInfer R>")
        }
        body(Sequences) {
            """
            @Suppress("UNCHECKED_CAST")
            return filter { it is R } as Sequence<R>
            """
        }
    }

    val f_filterIsInstanceTo_class = fn("filterIsInstanceTo(destination: C, klass: Class<R>)") {
        platforms(Platform.JVM)
        include(Iterables, ArraysOfObjects, Sequences)
    } builder {
        doc { "Appends all elements that are instances of specified class to the given [destination]." }
        sample("samples.collections.Collections.Filtering.filterIsInstanceToJVM")
        genericStarProjection = true
        typeParam("C : MutableCollection<in R>")
        typeParam("R")
        returns("C")
        body {
            """
            @Suppress("UNCHECKED_CAST")
            for (element in this) if (klass.isInstance(element)) destination.add(element as R)
            return destination
            """
        }
    }

    val f_filterIsInstance_class = fn("filterIsInstance(klass: Class<R>)") {
        platforms(Platform.JVM)
        include(Iterables, ArraysOfObjects, Sequences)
    } builder {
        doc { "Returns a list containing all elements that are instances of specified class." }
        sample("samples.collections.Collections.Filtering.filterIsInstanceJVM")
        genericStarProjection = true
        typeParam("R")
        returns("List<R>")
        body {
            """
            return filterIsInstanceTo(ArrayList<R>(), klass)
            """
        }

        specialFor(Sequences) {
            doc { "Returns a sequence containing all elements that are instances of specified class." }
            returns("Sequence<R>")
        }
        body(Sequences) {
            """
            @Suppress("UNCHECKED_CAST")
            return filter { klass.isInstance(it) } as Sequence<R>
            """
        }
    }



    val f_slice = fn("slice(indices: Iterable<Int>)") {
        include(CharSequences, Strings, Lists, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        doc { "Returns a list containing elements at specified [indices]." }
        returns("List<T>")
        body {
            """
            val size = indices.collectionSizeOrDefault(10)
            if (size == 0) return emptyList()
            val list = ArrayList<T>(size)
            for (index in indices) {
                list.add(get(index))
            }
            return list
            """
        }

        specialFor(Strings, CharSequences) {
            returns("SELF")
            doc { "Returns a ${f.collection} containing ${f.element.pluralize()} of the original ${f.collection} at specified [indices]." }
        }
        body(CharSequences) {
            """
            val size = indices.collectionSizeOrDefault(10)
            if (size == 0) return ""
            val result = StringBuilder(size)
            for (i in indices) {
                result.append(get(i))
            }
            return result
            """
        }
        specialFor(Strings) { inlineOnly() }
        body(Strings) {
            "return (this as CharSequence).slice(indices).toString()"
        }
    }

    val f_slice_range = fn("slice(indices: IntRange)") {
        include(CharSequences, Strings, Lists, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        doc { "Returns a list containing elements at indices in the specified [indices] range." }
        returns("List<T>")
        body(Lists) {
            """
            if (indices.isEmpty()) return listOf()
            return this.subList(indices.start, indices.endInclusive + 1).toList()
            """
        }
        body(ArraysOfPrimitives, ArraysOfObjects, ArraysOfUnsigned) {
            """
            if (indices.isEmpty()) return listOf()
            return copyOfRange(indices.start, indices.endInclusive + 1).asList()
            """
        }

        specialFor(Strings, CharSequences) {
            returns("SELF")
            doc { "Returns a ${f.collection} containing ${f.element.pluralize()} of the original ${f.collection} at the specified range of [indices]." }
        }
        body(CharSequences, Strings) {
            """
            if (indices.isEmpty()) return ""
            return ${ mapOf(Strings to "substring", CharSequences to "subSequence")[f]}(indices)
            """
        }
    }

    val f_sliceArray = fn("sliceArray(indices: Collection<Int>)") {
        include(InvariantArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        doc { "Returns an array containing elements of this array at specified [indices]." }
        returns("SELF")
        body(InvariantArraysOfObjects) {
            """
            val result = arrayOfNulls(this, indices.size)
            var targetIndex = 0
            for (sourceIndex in indices) {
                result[targetIndex++] = this[sourceIndex]
            }
            return result
            """
        }
        body(ArraysOfPrimitives) {
            """
            val result = SELF(indices.size)
            var targetIndex = 0
            for (sourceIndex in indices) {
                result[targetIndex++] = this[sourceIndex]
            }
            return result
            """
        }
        body(ArraysOfUnsigned) {
            """
            return SELF(storage.sliceArray(indices))
            """
        }
    }

    val f_sliceArrayRange = fn("sliceArray(indices: IntRange)") {
        include(InvariantArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        doc { "Returns an array containing elements at indices in the specified [indices] range." }
        returns("SELF")
        body(InvariantArraysOfObjects) {
            """
            if (indices.isEmpty()) return copyOfRange(0, 0)
            return copyOfRange(indices.start, indices.endInclusive + 1)
            """
        }
        body(ArraysOfPrimitives) {
            """
            if (indices.isEmpty()) return SELF(0)
            return copyOfRange(indices.start, indices.endInclusive + 1)
            """
        }
        body(ArraysOfUnsigned) {
            """
            return SELF(storage.sliceArray(indices))
            """
        }
    }
}

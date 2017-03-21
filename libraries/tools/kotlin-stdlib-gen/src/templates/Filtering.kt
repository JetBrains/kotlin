/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package templates

import templates.Family.*
import templates.SequenceClass.*

fun filtering(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    fun subsequence(f: Family, start: String, end: String? = null): String {
        return when (f) {
            Strings -> "substring(${listOfNotNull(start, end).joinToString()})"
            CharSequences -> "subSequence($start, ${end ?: "length"})"
            else -> throw UnsupportedOperationException(f.toString())
        }
    }

    fun toResult(f: Family): String = if (f == CharSequences) "" else ".toString()"

    fun takeAll(f: Family): String = if (f == Strings) "this" else subsequence(f, "0")

    templates add f("drop(n: Int)") {
        val n = "\$n"
        doc { "Returns a list containing all elements except first [n] elements." }
        sequenceClassification(nearly_stateless)
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
                        for (index in n..size - 1)
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
                if (count++ >= n) list.add(item)
            }
            return list.optimizeReadOnlyList()
            """
        }

        doc(Sequences) { "Returns a sequence containing all elements except first [n] elements." }
        returns(Sequences) { "Sequence<T>" }
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

        doc(Strings) { "Returns a string with the first [n] characters removed."}
        doc(CharSequences) { "Returns a subsequence of this char sequence with the first [n] characters removed."}
        returns(Strings, CharSequences) { "SELF" }
        body(Strings, CharSequences) { f ->
            """
            require(n >= 0) { "Requested character count $n is less than zero." }
            return ${subsequence(f, "n.coerceAtMost(length)")}
            """
        }

        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            require(n >= 0) { "Requested element count $n is less than zero." }
            return takeLast((size - n).coerceAtLeast(0))
            """
        }
    }

    templates add f("take(n: Int)") {
        val n = "\$n"
        doc { "Returns a list containing first [n] elements." }
        sequenceClassification(nearly_stateless)
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
                if (count++ == n)
                    break
                list.add(item)
            }
            return list.optimizeReadOnlyList()
            """
        }

        doc(Strings) { "Returns a string containing the first [n] characters from this string, or the entire string if this string is shorter."}
        doc(CharSequences) { "Returns a subsequence of this char sequence containing the first [n] characters from this char sequence, or the entire char sequence if this char sequence is shorter."}
        returns(Strings, CharSequences) { "SELF" }
        body(Strings, CharSequences) { f ->
            """
            require(n >= 0) { "Requested character count $n is less than zero." }
            return ${subsequence(f, "0", "n.coerceAtMost(length)")}
            """
        }

        doc(Sequences) { "Returns a sequence containing first [n] elements." }
        returns(Sequences) { "Sequence<T>" }
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

        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            require(n >= 0) { "Requested element count $n is less than zero." }
            if (n == 0) return emptyList()
            if (n >= size) return toList()
            if (n == 1) return listOf(this[0])
            var count = 0
            val list = ArrayList<T>(n)
            for (item in this) {
                if (count++ == n)
                    break
                list.add(item)
            }
            return list
            """
        }
    }

    templates add f("dropLast(n: Int)") {
        val n = "\$n"
        only(Lists, ArraysOfObjects, ArraysOfPrimitives, Strings, CharSequences)

        doc { "Returns a list containing all elements except last [n] elements." }
        returns("List<T>")
        body { f ->
            """
            require(n >= 0) { "Requested ${f.doc.element} count $n is less than zero." }
            return take((${f.code.size} - n).coerceAtLeast(0))
            """
        }

        doc(Strings) { "Returns a string with the last [n] characters removed." }
        doc(CharSequences) { "Returns a subsequence of this char sequence with the last [n] characters removed." }
        returns(Strings, CharSequences) { "SELF" }

    }

    templates add f("takeLast(n: Int)") {
        val n = "\$n"
        doc { "Returns a list containing last [n] elements." }
        only(Lists, ArraysOfObjects, ArraysOfPrimitives, Strings, CharSequences)
        returns("List<T>")

        doc(Strings) { "Returns a string containing the last [n] characters from this string, or the entire string if this string is shorter."}
        doc(CharSequences) { "Returns a subsequence of this char sequence containing the last [n] characters from this char sequence, or the entire char sequence if this char sequence is shorter."}
        returns(Strings, CharSequences) { "SELF" }
        body(Strings, CharSequences) { f ->
            """
            require(n >= 0) { "Requested character count $n is less than zero." }
            val length = length
            return ${subsequence(f, "length - n.coerceAtMost(length)")}
            """
        }

        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            require(n >= 0) { "Requested element count $n is less than zero." }
            if (n == 0) return emptyList()
            val size = size
            if (n >= size) return toList()
            if (n == 1) return listOf(this[size - 1])

            val list = ArrayList<T>(n)
            for (index in size - n .. size - 1)
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
                for (index in size - n .. size - 1)
                    list.add(this[index])
            } else {
                for (item in listIterator(n))
                    list.add(item)
            }
            return list
            """
        }
    }

    templates add f("dropWhile(predicate: (T) -> Boolean)") {
        inline(true)

        doc { "Returns a list containing all elements except first elements that satisfy the given [predicate]." }
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

        doc(Strings) { "Returns a string containing all characters except first characters that satisfy the given [predicate]." }
        doc(CharSequences) { "Returns a subsequence of this char sequence containing all characters except first characters that satisfy the given [predicate]." }
        returns(Strings, CharSequences) { "SELF" }
        body(Strings, CharSequences) { f ->
            """
            for (index in this.indices)
                if (!predicate(this[index]))
                    return ${subsequence(f, "index")}

            return ""
            """
        }

        inline(false, Sequences)
        doc(Sequences) { "Returns a sequence containing all elements except first elements that satisfy the given [predicate]." }
        returns(Sequences) { "Sequence<T>" }
        body(Sequences) {
            """
            return DropWhileSequence(this, predicate)
            """
        }

    }

    templates add f("takeWhile(predicate: (T) -> Boolean)") {
        inline(true)

        doc { "Returns a list containing first elements satisfying the given [predicate]." }
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

        doc(Strings) { "Returns a string containing the first characters that satisfy the given [predicate]."}
        doc(CharSequences) { "Returns a subsequence of this char sequence containing the first characters that satisfy the given [predicate]."}
        returns(Strings, CharSequences) { "SELF" }
        body(Strings, CharSequences) { f ->
            """
            for (index in 0..length - 1)
                if (!predicate(get(index))) {
                    return ${subsequence(f, "0", "index")}
                }
            return ${takeAll(f)}
            """
        }

        inline(false, Sequences)
        doc(Sequences) { "Returns a sequence containing first elements satisfying the given [predicate]." }
        returns(Sequences) { "Sequence<T>" }
        body(Sequences) {
            """
            return TakeWhileSequence(this, predicate)
            """
        }
    }

    templates add f("dropLastWhile(predicate: (T) -> Boolean)") {
        inline(true)
        only(Lists, ArraysOfObjects, ArraysOfPrimitives, CharSequences, Strings)
        doc { "Returns a list containing all elements except last elements that satisfy the given [predicate]." }
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

        doc(Strings) { "Returns a string containing all characters except last characters that satisfy the given [predicate]." }
        doc(CharSequences) { "Returns a subsequence of this char sequence containing all characters except last characters that satisfy the given [predicate]." }
        returns(CharSequences, Strings) { "SELF" }
        body(CharSequences, Strings) { f ->
            """
            for (index in this.indices.reversed())
                if (!predicate(this[index]))
                    return ${subsequence(f, "0", "index + 1")}

            return ""
            """
        }
    }

    templates add f("takeLastWhile(predicate: (T) -> Boolean)") {
        inline(true)
        only(Lists, ArraysOfObjects, ArraysOfPrimitives, Strings, CharSequences)
        doc { "Returns a list containing last elements satisfying the given [predicate]."}
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
                    iterator.next()
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

        doc(Strings) { "Returns a string containing last characters that satisfy the given [predicate]." }
        doc(CharSequences) { "Returns a subsequence of this char sequence containing last characters that satisfy the given [predicate]." }
        returns(Strings, CharSequences) { "SELF" }
        body(Strings, CharSequences) { f ->
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

    templates add f("filter(predicate: (T) -> Boolean)") {
        inline(true)

        doc { f -> "Returns a ${f.mapResult} containing only ${f.element.pluralize()} matching the given [predicate]." }
        returns("List<T>")
        body {
            """
            return filterTo(ArrayList<T>(), predicate)
            """
        }

        doc(CharSequences, Strings) { f -> "Returns a ${f.collection} containing only those characters from the original ${f.collection} that match the given [predicate]." }
        returns(CharSequences, Strings) { "SELF" }
        body(CharSequences, Strings) { f -> """return filterTo(StringBuilder(), predicate)${toResult(f)}""" }

        inline(false, Sequences)
        returns(Sequences) { "Sequence<T>" }
        body(Sequences) {
            """
            return FilteringSequence(this, true, predicate)
            """
        }
    }

    templates add f("filterTo(destination: C, predicate: (T) -> Boolean)") {
        inline(true)

        doc { f -> "Appends all ${f.element.pluralize()} matching the given [predicate] to the given [destination]." }
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
            for (index in 0..length - 1) {
                val element = get(index)
                if (predicate(element)) destination.append(element)
            }
            return destination
            """
        }
    }

    templates add f("filterIndexed(predicate: (index: Int, T) -> Boolean)") {
        inline(true)

        doc { f ->
            """
            Returns a ${f.mapResult} containing only ${f.element.pluralize()} matching the given [predicate].
            @param [predicate] function that takes the index of ${f.element.prefixWithArticle()} and the ${f.element} itself
            and returns the result of predicate evaluation on the ${f.element}.
            """
        }
        returns("List<T>")
        body {
            """
            return filterIndexedTo(ArrayList<T>(), predicate)
            """
        }

        doc(CharSequences, Strings) { f ->
            """
            Returns a ${f.collection} containing only those characters from the original ${f.collection} that match the given [predicate].
            @param [predicate] function that takes the index of ${f.element.prefixWithArticle()} and the ${f.element} itself
            and returns the result of predicate evaluation on the ${f.element}.
            """
        }
        returns(CharSequences, Strings) { "SELF" }
        body(CharSequences, Strings) { f -> """return filterIndexedTo(StringBuilder(), predicate)${toResult(f)}""" }

        inline(false, Sequences)
        returns(Sequences) { "Sequence<T>" }
        body(Sequences) {
            """
            // TODO: Rewrite with generalized MapFilterIndexingSequence
            return TransformingSequence(FilteringSequence(IndexingSequence(this), true, { predicate(it.index, it.value) }), { it.value })
            """
        }
    }


    templates add f("filterIndexedTo(destination: C, predicate: (index: Int, T) -> Boolean)") {
        inline(true)

        include(CharSequences)

        doc { f ->
            """
            Appends all ${f.element.pluralize()} matching the given [predicate] to the given [destination].
            @param [predicate] function that takes the index of ${f.element.prefixWithArticle()} and the ${f.element} itself
            and returns the result of predicate evaluation on the ${f.element}.
            """ }
        typeParam("C : TCollection")
        returns("C")

        body { f ->
            """
            forEachIndexed { index, element ->
                if (predicate(index, element)) destination.${ if (f==CharSequences) "append" else "add" }(element)
            }
            return destination
            """
        }
    }

    templates add f("filterNot(predicate: (T) -> Boolean)") {
        inline(true)

        doc { "Returns a list containing all elements not matching the given [predicate]." }
        returns("List<T>")
        body {
            """
            return filterNotTo(ArrayList<T>(), predicate)
            """
        }

        doc(CharSequences, Strings) { f -> "Returns a ${f.collection} containing only those characters from the original ${f.collection} that do not match the given [predicate]." }
        returns(CharSequences, Strings) { "SELF" }
        body(CharSequences, Strings) { f -> """return filterNotTo(StringBuilder(), predicate)${toResult(f)}""" }

        inline(false, Sequences)
        doc(Sequences) { "Returns a sequence containing all elements not matching the given [predicate]." }
        returns(Sequences) { "Sequence<T>" }
        body(Sequences) {
            """
            return FilteringSequence(this, false, predicate)
            """
        }
    }

    templates add f("filterNotTo(destination: C, predicate: (T) -> Boolean)") {
        inline(true)

        doc { "Appends all elements not matching the given [predicate] to the given [destination]." }
        typeParam("C : TCollection")
        returns("C")

        body {
            """
            for (element in this) if (!predicate(element)) destination.add(element)
            return destination
            """
        }

        doc(CharSequences) { "Appends all characters not matching the given [predicate] to the given [destination]." }
        body(CharSequences) {
            """
            for (element in this) if (!predicate(element)) destination.append(element)
            return destination
            """
        }
    }

    templates add f("filterNotNull()") {
        exclude(ArraysOfPrimitives)
        doc { "Returns a list containing all elements that are not `null`." }
        typeParam("T : Any")
        returns("List<T>")
        toNullableT = true
        body {
            """
            return filterNotNullTo(ArrayList<T>())
            """
        }

        doc(Sequences) { "Returns a sequence containing all elements that are not `null`." }
        returns(Sequences) { "Sequence<T>" }
        body(Sequences) {
            """
            @Suppress("UNCHECKED_CAST")
            return filterNot { it == null } as Sequence<T>
            """
        }
    }

    templates add f("filterNotNullTo(destination: C)") {
        exclude(ArraysOfPrimitives)
        doc { "Appends all elements that are not `null` to the given [destination]." }
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

    templates add f("filterIsInstanceTo(destination: C)") {
        doc { "Appends all elements that are instances of specified type parameter R to the given [destination]." }
        typeParam("reified R")
        typeParam("C : MutableCollection<in R>")
        inline(true)
        receiverAsterisk(true)
        returns("C")
        exclude(ArraysOfPrimitives, Strings)
        body {
            """
            for (element in this) if (element is R) destination.add(element)
            return destination
            """
        }
    }

    templates add f("filterIsInstance()") {
        doc { "Returns a list containing all elements that are instances of specified type parameter R." }
        typeParam("reified R")
        returns("List<@kotlin.internal.NoInfer R>")
        inline(true)
        receiverAsterisk(true)
        body {
            """
            return filterIsInstanceTo(ArrayList<R>())
            """
        }
        exclude(ArraysOfPrimitives, Strings)

        doc(Sequences) { "Returns a sequence containing all elements that are instances of specified type parameter R." }
        returns(Sequences) { "Sequence<@kotlin.internal.NoInfer R>" }
        inline(true)
        receiverAsterisk(true)
        body(Sequences) {
            """
            @Suppress("UNCHECKED_CAST")
            return filter { it is R } as Sequence<R>
            """
        }
    }


    templates add f("slice(indices: Iterable<Int>)") {
        only(Strings, Lists, ArraysOfPrimitives, ArraysOfObjects)
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

        doc(CharSequences, Strings) { f -> "Returns a ${f.collection} containing ${f.element.pluralize()} of the original ${f.collection} at specified [indices]." }
        returns(CharSequences, Strings) { "SELF" }
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
        inline(Strings) { Inline.Only }
        body(Strings) {
            "return (this as CharSequence).slice(indices).toString()"
        }
    }

    templates add f("slice(indices: IntRange)") {
        only(Strings, Lists, ArraysOfPrimitives, ArraysOfObjects)
        doc { "Returns a list containing elements at indices in the specified [indices] range." }
        returns("List<T>")
        body(Lists) {
            """
            if (indices.isEmpty()) return listOf()
            return this.subList(indices.start, indices.endInclusive + 1).toList()
            """
        }
        body(ArraysOfPrimitives, ArraysOfObjects) {
            """
            if (indices.isEmpty()) return listOf()
            return copyOfRange(indices.start, indices.endInclusive + 1).asList()
            """
        }

        doc(CharSequences, Strings) { f -> "Returns a ${f.collection} containing ${f.element.pluralize()} of the original ${f.collection} at the specified range of [indices]." }
        returns(CharSequences, Strings) { "SELF" }
        body(CharSequences, Strings) { f ->
            """
            if (indices.isEmpty()) return ""
            return ${ mapOf(Strings to "substring", CharSequences to "subSequence")[f]}(indices)
            """
        }
    }

    templates add f("sliceArray(indices: Collection<Int>)") {
        only(InvariantArraysOfObjects, ArraysOfPrimitives)
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
    }

    templates add f("sliceArray(indices: IntRange)") {
        only(InvariantArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns a list containing elements at indices in the specified [indices] range." }
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
    }


    val terminalOperationPattern = Regex("^\\w+To")
    templates.forEach { with (it) {
        if (sequenceClassification.isEmpty()) {
            sequenceClassification(if (signature.contains("index", ignoreCase = true)) nearly_stateless else stateless)
        }
        sequenceClassification.add(0, if (terminalOperationPattern in signature) terminal else intermediate)
    } }


    return templates
}
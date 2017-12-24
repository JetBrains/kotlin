/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

fun generators(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("plusElement(element: T)") {
        inline(Inline.Only)

        only(Iterables, Collections, Sets, Sequences)
        doc { "Returns a list containing all elements of the original collection and then the given [element]." }
        doc(Sets) {
            """
            Returns a set containing all elements of the original set and then the given [element] if it isn't already in this set.

            The returned set preserves the element iteration order of the original set.
            """
        }
        doc(Sequences) { "Returns a sequence containing all elements of the original sequence and then the given [element]." }
        sequenceClassification(intermediate, stateless)

        returns("List<T>")
        returns("SELF", Sets, Sequences)
        body { "return plus(element)" }
    }

    templates add f("plus(element: T)") {
        operator(true)

        only(Iterables, Collections, Sets, Sequences)
        doc { "Returns a list containing all elements of the original collection and then the given [element]." }
        sequenceClassification(intermediate, stateless)
        returns("List<T>")
        body {
            """
            if (this is Collection) return this.plus(element)
            val result = ArrayList<T>()
            result.addAll(this)
            result.add(element)
            return result
            """
        }
        body(Collections) {
            """
            val result = ArrayList<T>(size + 1)
            result.addAll(this)
            result.add(element)
            return result
            """
        }

        // TODO: use immutable sets when available
        returns("SELF", Sets, Sequences)
        doc(Sets) {
            """
            Returns a set containing all elements of the original set and then the given [element] if it isn't already in this set.

            The returned set preserves the element iteration order of the original set.
            """
        }
        body(Sets) {
            """
            val result = LinkedHashSet<T>(mapCapacity(size + 1))
            result.addAll(this)
            result.add(element)
            return result
            """
        }

        doc(Sequences) { "Returns a sequence containing all elements of the original sequence and then the given [element]." }
        body(Sequences) {
            """
            return sequenceOf(this, sequenceOf(element)).flatten()
            """
        }
    }

    templates add f("plus(elements: Iterable<T>)") {
        operator(true)

        only(Iterables, Collections, Sets, Sequences)
        doc { f -> "Returns a list containing all elements of the original ${f.collection} and then all elements of the given [elements] collection." }
        returns("List<T>")
        returns("SELF", Sets, Sequences)
        body {
            """
            if (this is Collection) return this.plus(elements)
            val result = ArrayList<T>()
            result.addAll(this)
            result.addAll(elements)
            return result
            """
        }
        body(Collections) {
            """
            if (elements is Collection) {
                val result = ArrayList<T>(this.size + elements.size)
                result.addAll(this)
                result.addAll(elements)
                return result
            } else {
                val result = ArrayList<T>(this)
                result.addAll(elements)
                return result
            }
            """
        }

        // TODO: use immutable set builder when available
        doc(Sets) {
            """
            Returns a set containing all elements of the original set and the given [elements] collection,
            which aren't already in this set.
            The returned set preserves the element iteration order of the original set.
            """
        }
        body(Sets) {
            """
            val result = LinkedHashSet<T>(mapCapacity(elements.collectionSizeOrNull()?.let { this.size + it } ?: this.size * 2))
            result.addAll(this)
            result.addAll(elements)
            return result
            """
        }

        doc(Sequences) {
            """
            Returns a sequence containing all elements of original sequence and then all elements of the given [elements] collection.

            Note that the source sequence and the collection being added are iterated only when an `iterator` is requested from
            the resulting sequence. Changing any of them between successive calls to `iterator` may affect the result.
            """
        }
        sequenceClassification(intermediate, stateless)
        body(Sequences) {
            """
            return sequenceOf(this, elements.asSequence()).flatten()
            """
        }
    }

    templates add f("plus(elements: Array<out T>)") {
        operator(true)

        only(Iterables, Collections, Sets, Sequences)
        doc { "Returns a list containing all elements of the original collection and then all elements of the given [elements] array." }
        returns("List<T>")
        returns("SELF", Sets, Sequences)
        body {
            """
            if (this is Collection) return this.plus(elements)
            val result = ArrayList<T>()
            result.addAll(this)
            result.addAll(elements)
            return result
            """
        }
        body(Collections) {
            """
            val result = ArrayList<T>(this.size + elements.size)
            result.addAll(this)
            result.addAll(elements)
            return result
            """
        }
        doc(Sets) {
            """
            Returns a set containing all elements of the original set and the given [elements] array,
            which aren't already in this set.

            The returned set preserves the element iteration order of the original set.
            """
        }
        body(Sets) {
            """
            val result = LinkedHashSet<T>(mapCapacity(this.size + elements.size))
            result.addAll(this)
            result.addAll(elements)
            return result
            """
        }
        doc(Sequences) {
            """
            Returns a sequence containing all elements of original sequence and then all elements of the given [elements] array.

            Note that the source sequence and the array being added are iterated only when an `iterator` is requested from
            the resulting sequence. Changing any of them between successive calls to `iterator` may affect the result.
            """
        }
        sequenceClassification(intermediate, stateless)
        body(Sequences) {
            """
            return this.plus(elements.asList())
            """
        }
    }


    templates add f("plus(elements: Sequence<T>)") {
        operator(true)

        only(Iterables, Sets, Sequences)
        doc { "Returns a list containing all elements of the original collection and then all elements of the given [elements] sequence." }
        returns("List<T>")
        returns("SELF", Sets, Sequences)
        body {
            """
            val result = ArrayList<T>()
            result.addAll(this)
            result.addAll(elements)
            return result
            """
        }
        body(Collections) {
            """
            val result = ArrayList<T>(this.size + 10)
            result.addAll(this)
            result.addAll(elements)
            return result
            """
        }

        // TODO: use immutable set builder when available
        doc(Sets) {
            """
            Returns a set containing all elements of the original set and the given [elements] sequence,
            which aren't already in this set.

            The returned set preserves the element iteration order of the original set.
            """
        }
        body(Sets) {
            """
            val result = LinkedHashSet<T>(mapCapacity(this.size * 2))
            result.addAll(this)
            result.addAll(elements)
            return result
            """
        }

        doc(Sequences) {
            """
            Returns a sequence containing all elements of original sequence and then all elements of the given [elements] sequence.

            Note that the source sequence and the sequence being added are iterated only when an `iterator` is requested from
            the resulting sequence. Changing any of them between successive calls to `iterator` may affect the result.
            """
        }
        sequenceClassification(intermediate, stateless)
        body(Sequences) {
            """
            return sequenceOf(this, elements).flatten()
            """
        }
    }

    templates add f("minusElement(element: T)") {
        inline(Inline.Only)

        only(Iterables, Sets, Sequences)
        doc { "Returns a list containing all elements of the original collection without the first occurrence of the given [element]." }
        doc(Sets) {
            """
            Returns a set containing all elements of the original set except the given [element].

            The returned set preserves the element iteration order of the original set.
            """
        }
        doc(Sequences) { "Returns a sequence containing all elements of the original sequence without the first occurrence of the given [element]." }
        sequenceClassification(intermediate, stateless)

        returns("List<T>")
        returns("SELF", Sets, Sequences)
        body { "return minus(element)" }
    }

    templates add f("minus(element: T)") {
        operator(true)

        only(Iterables, Sets, Sequences)
        doc { "Returns a list containing all elements of the original collection without the first occurrence of the given [element]." }
        returns("List<T>")
        body {
            """
            val result = ArrayList<T>(collectionSizeOrDefault(10))
            var removed = false
            return this.filterTo(result) { if (!removed && it == element) { removed = true; false } else true }
            """
        }

        returns("SELF", Sets, Sequences)
        doc(Sets) {
            """
            Returns a set containing all elements of the original set except the given [element].

            The returned set preserves the element iteration order of the original set.
            """
        }
        body(Sets) {
            """
            val result = LinkedHashSet<T>(mapCapacity(size))
            var removed = false
            return this.filterTo(result) { if (!removed && it == element) { removed = true; false } else true }
            """
        }


        doc(Sequences) { "Returns a sequence containing all elements of the original sequence without the first occurrence of the given [element]." }
        sequenceClassification(intermediate, stateless)
        body(Sequences) {
            """
            return object: Sequence<T> {
                override fun iterator(): Iterator<T> {
                    var removed = false
                    return this@minus.filter { if (!removed && it == element) { removed = true; false } else true }.iterator()
                }
            }
            """
        }
    }


    templates add f("minus(elements: Iterable<T>)") {
        operator(true)

        only(Iterables, Sets, Sequences)
        doc { "Returns a list containing all elements of the original collection except the elements contained in the given [elements] collection." }
        returns("List<T>")
        returns("SELF", Sets, Sequences)
        body {
            """
            val other = elements.convertToSetForSetOperationWith(this)
            if (other.isEmpty())
                return this.toList()

            return this.filterNot { it in other }
            """
        }

        doc(Sets) {
            """
            Returns a set containing all elements of the original set except the elements contained in the given [elements] collection.

            The returned set preserves the element iteration order of the original set.
            """
        }
        body(Sets) {
            """
            val other = elements.convertToSetForSetOperationWith(this)
            if (other.isEmpty())
                return this.toSet()
            if (other is Set)
                return this.filterNotTo(LinkedHashSet<T>()) { it in other }

            val result = LinkedHashSet<T>(this)
            result.removeAll(other)
            return result
            """
        }

        doc(Sequences) {
            """
            Returns a sequence containing all elements of original sequence except the elements contained in the given [elements] collection.

            Note that the source sequence and the collection being subtracted are iterated only when an `iterator` is requested from
            the resulting sequence. Changing any of them between successive calls to `iterator` may affect the result.
            """
        }
        sequenceClassification(intermediate, stateful)
        body(Sequences) {
            """
            return object: Sequence<T> {
                override fun iterator(): Iterator<T> {
                    val other = elements.convertToSetForSetOperation()
                    if (other.isEmpty())
                        return this@minus.iterator()
                    else
                        return this@minus.filterNot { it in other }.iterator()
                }
            }
            """
        }
    }

    templates add f("minus(elements: Array<out T>)") {
        operator(true)

        only(Iterables, Sets, Sequences)
        doc { "Returns a list containing all elements of the original collection except the elements contained in the given [elements] array." }
        returns("List<T>")
        returns("SELF", Sets, Sequences)
        body {
            """
            if (elements.isEmpty()) return this.toList()
            val other = elements.toHashSet()
            return this.filterNot { it in other }
            """
        }
        doc(Sets) {
            """
            Returns a set containing all elements of the original set except the elements contained in the given [elements] array.

            The returned set preserves the element iteration order of the original set.
            """
        }
        body(Sets) {
            """
            val result = LinkedHashSet<T>(this)
            result.removeAll(elements)
            return result
            """
        }

        doc(Sequences) {
            """
            Returns a sequence containing all elements of original sequence except the elements contained in the given [elements] array.

            Note that the source sequence and the array being subtracted are iterated only when an `iterator` is requested from
            the resulting sequence. Changing any of them between successive calls to `iterator` may affect the result.
            """
        }
        sequenceClassification(intermediate, stateful)
        body(Sequences) {
            """
            if (elements.isEmpty()) return this
            return object: Sequence<T> {
                override fun iterator(): Iterator<T> {
                    val other = elements.toHashSet()
                    return this@minus.filterNot { it in other }.iterator()
                }
            }
            """
        }
    }

    templates add f("minus(elements: Sequence<T>)") {
        operator(true)

        only(Iterables, Sets)
        doc { "Returns a list containing all elements of the original collection except the elements contained in the given [elements] sequence." }
        returns("List<T>")
        returns("SELF", Sets, Sequences)
        body {
            """
            val other = elements.toHashSet()
            if (other.isEmpty())
                return this.toList()

            return this.filterNot { it in other }
            """
        }
        doc(Sets) {
            """
            Returns a set containing all elements of the original set except the elements contained in the given [elements] sequence.

            The returned set preserves the element iteration order of the original set.
            """
        }
        body(Sets) {
            """
            val result = LinkedHashSet<T>(this)
            result.removeAll(elements)
            return result
            """
        }

        doc(Sequences) {
            """
            Returns a sequence containing all elements of original sequence except the elements contained in the given [elements] sequence.

            Note that the source sequence and the sequence being subtracted are iterated only when an `iterator` is requested from
            the resulting sequence. Changing any of them between successive calls to `iterator` may affect the result.

            The operation is _intermediate_ for this sequence and _terminal_ and _stateful_ for the [elements] sequence.
            """
        }
        body(Sequences) {
            """
            return object: Sequence<T> {
                override fun iterator(): Iterator<T> {
                    val other = elements.toHashSet()
                    if (other.isEmpty())
                        return this@minus.iterator()
                    else
                        return this@minus.filterNot { it in other }.iterator()
                }
            }
            """
        }
    }

    templates add f("partition(predicate: (T) -> Boolean)") {
        inline(true)

        doc { f ->
            """
            Splits the original ${f.collection} into pair of lists,
            where *first* list contains elements for which [predicate] yielded `true`,
            while *second* list contains elements for which [predicate] yielded `false`.
            """
        }
        sequenceClassification(terminal)
        returns("Pair<List<T>, List<T>>")
        body {
            """
            val first = ArrayList<T>()
            val second = ArrayList<T>()
            for (element in this) {
                if (predicate(element)) {
                    first.add(element)
                } else {
                    second.add(element)
                }
            }
            return Pair(first, second)
            """
        }

        doc(CharSequences, Strings) { f ->
            """
            Splits the original ${f.collection} into pair of ${f.collection}s,
            where *first* ${f.collection} contains characters for which [predicate] yielded `true`,
            while *second* ${f.collection} contains characters for which [predicate] yielded `false`.
            """
        }
        returns(CharSequences, Strings) { "Pair<SELF, SELF>" }
        body(CharSequences, Strings) { f ->
            val toString = if (f == Strings) ".toString()" else ""
            """
            val first = StringBuilder()
            val second = StringBuilder()
            for (element in this) {
                if (predicate(element)) {
                    first.append(element)
                } else {
                    second.append(element)
                }
            }
            return Pair(first$toString, second$toString)
            """
        }
    }

    templates add f("windowed(size: Int, step: Int = 1, partialWindows: Boolean = false, transform: (List<T>) -> R)") {
        since("1.2")
        only(Iterables, Sequences, CharSequences)
        doc { f ->
            """
            Returns a ${f.mapResult} of results of applying the given [transform] function to
            an each ${f.viewResult} representing a view over the window of the given [size]
            sliding along this ${f.collection} with the given [step].

            Note that the ${f.viewResult} passed to the [transform] function is ephemeral and is valid only inside that function.
            You should not store it or allow it to escape in some way, unless you made a snapshot of it.
            Several last ${f.viewResult.pluralize()} may have less ${f.element.pluralize()} than the given [size].

            Both [size] and [step] must be positive and can be greater than the number of elements in this ${f.collection}.
            @param size the number of elements to take in each window
            @param step the number of elements to move the window forward by on an each step, by default 1
            @param partialWindows controls whether or not to keep partial windows in the end if any,
            by default `false` which means partial windows won't be preserved

            @sample samples.collections.Sequences.Transformations.averageWindows
            """
        }

        typeParam("R")
        returns("List<R>")

        body {
            """
            checkWindowSizeStep(size, step)
            if (this is RandomAccess && this is List) {
                val thisSize = this.size
                val result = ArrayList<R>((thisSize + step - 1) / step)
                val window = MovingSubList(this)
                var index = 0
                while (index < thisSize) {
                    window.move(index, (index + size).coerceAtMost(thisSize))
                    if (!partialWindows && window.size < size) break
                    result.add(transform(window))
                    index += step
                }
                return result
            }
            val result = ArrayList<R>()
            windowedIterator(iterator(), size, step, partialWindows, reuseBuffer = true).forEach {
                result.add(transform(it))
            }
            return result
            """
        }

        customSignature(CharSequences) { "windowed(size: Int, step: Int = 1, partialWindows: Boolean = false, transform: (CharSequence) -> R)" }
        body(CharSequences) {
            """
            checkWindowSizeStep(size, step)
            val thisSize = this.length
            val result = ArrayList<R>((thisSize + step - 1) / step)
            var index = 0
            while (index < thisSize) {
                val end = index + size
                val coercedEnd = if (end > thisSize) { if (partialWindows) thisSize else break } else end
                result.add(transform(subSequence(index, coercedEnd)))
                index += step
            }
            return result
            """
        }

        returns(Sequences) { "Sequence<R>" }
        body(Sequences) {
            """
            return windowedSequence(size, step, partialWindows, reuseBuffer = true).map(transform)
            """
        }
    }

    templates add f("windowed(size: Int, step: Int = 1, partialWindows: Boolean = false)") {
        since("1.2")
        only(Iterables, Sequences, CharSequences)
        returns(Iterables) { "List<List<T>>" }
        returns(Sequences) { "Sequence<List<T>>" }
        returns(CharSequences) { "List<String>" }

        doc { f ->
            """
            Returns a ${f.mapResult} of snapshots of the window of the given [size]
            sliding along this ${f.collection} with the given [step], where each
            snapshot is ${f.snapshotResult.prefixWithArticle()}.

            Several last ${f.snapshotResult.pluralize()} may have less ${f.element.pluralize()} than the given [size].

            Both [size] and [step] must be positive and can be greater than the number of elements in this ${f.collection}.
            @param size the number of elements to take in each window
            @param step the number of elements to move the window forward by on an each step, by default 1
            @param partialWindows controls whether or not to keep partial windows in the end if any,
            by default `false` which means partial windows won't be preserved

            @sample samples.collections.Sequences.Transformations.takeWindows
            """
        }

        body {
            """
            checkWindowSizeStep(size, step)
            if (this is RandomAccess && this is List) {
                val thisSize = this.size
                val result = ArrayList<List<T>>((thisSize + step - 1) / step)
                var index = 0
                while (index < thisSize) {
                    val windowSize = size.coerceAtMost(thisSize - index)
                    if (windowSize < size && !partialWindows) break
                    result.add(List(windowSize) { this[it + index] })
                    index += step
                }
                return result
            }
            val result = ArrayList<List<T>>()
            windowedIterator(iterator(), size, step, partialWindows, reuseBuffer = false).forEach {
                result.add(it)
            }
            return result
            """
        }
        body(CharSequences) { "return windowed(size, step, partialWindows) { it.toString() }" }
        body(Sequences) {
            """
            return windowedSequence(size, step, partialWindows, reuseBuffer = false)
            """
        }
    }

    templates add f("windowedSequence(size: Int, step: Int = 1, partialWindows: Boolean = false, transform: (CharSequence) -> R)") {
        since("1.2")
        only(CharSequences)
        doc { f ->
            """
            Returns a sequence of results of applying the given [transform] function to
            an each ${f.viewResult} representing a view over the window of the given [size]
            sliding along this ${f.collection} with the given [step].

            Note that the ${f.viewResult} passed to the [transform] function is ephemeral and is valid only inside that function.
            You should not store it or allow it to escape in some way, unless you made a snapshot of it.
            Several last ${f.viewResult.pluralize()} may have less ${f.element.pluralize()} than the given [size].

            Both [size] and [step] must be positive and can be greater than the number of elements in this ${f.collection}.
            @param size the number of elements to take in each window
            @param step the number of elements to move the window forward by on an each step, by default 1
            @param partialWindows controls whether or not to keep partial windows in the end if any,
            by default `false` which means partial windows won't be preserved

            @sample samples.collections.Sequences.Transformations.averageWindows
            """
        }
        typeParam("R")
        returns { "Sequence<R>" }

        body {
            """
            checkWindowSizeStep(size, step)
            val windows = (if (partialWindows) indices else 0 until length - size + 1) step step
            return windows.asSequence().map { index -> transform(subSequence(index, (index + size).coerceAtMost(length))) }
            """
        }
    }

    templates add f("windowedSequence(size: Int, step: Int = 1, partialWindows: Boolean = false)") {
        since("1.2")
        only(CharSequences)
        doc { f ->
            """
            Returns a sequence of snapshots of the window of the given [size]
            sliding along this ${f.collection} with the given [step], where each
            snapshot is ${f.snapshotResult.prefixWithArticle()}.

            Several last ${f.snapshotResult.pluralize()} may have less ${f.element.pluralize()} than the given [size].

            Both [size] and [step] must be positive and can be greater than the number of elements in this ${f.collection}.
            @param size the number of elements to take in each window
            @param step the number of elements to move the window forward by on an each step, by default 1
            @param partialWindows controls whether or not to keep partial windows in the end if any,
            by default `false` which means partial windows won't be preserved

            @sample samples.collections.Sequences.Transformations.takeWindows
            """
        }
        returns { "Sequence<String>" }

        body(CharSequences) { "return windowedSequence(size, step, partialWindows) { it.toString() }" }
    }

    templates add f("chunked(size: Int, transform: (List<T>) -> R)") {
        since("1.2")
        only(Iterables, Sequences, CharSequences)
        doc { f ->
            """
            Splits this ${f.collection} into several ${f.viewResult.pluralize()} each not exceeding the given [size]
            and applies the given [transform] function to an each.

            @return ${f.mapResult} of results of the [transform] applied to an each ${f.viewResult}.

            Note that the ${f.viewResult} passed to the [transform] function is ephemeral and is valid only inside that function.
            You should not store it or allow it to escape in some way, unless you made a snapshot of it.
            The last ${f.viewResult} may have less ${f.element.pluralize()} than the given [size].

            @param size the number of elements to take in each ${f.viewResult}, must be positive and can be greater than the number of elements in this ${f.collection}.

            @sample samples.text.Strings.chunkedTransform
            """
        }

        typeParam("R")
        returns("List<R>")

        customSignature(CharSequences) { "chunked(size: Int, transform: (CharSequence) -> R)" }

        sequenceClassification(intermediate, stateful)
        returns(Sequences) { "Sequence<R>" }
        body { "return windowed(size, size, partialWindows = true, transform = transform)" }
    }

    templates add f("chunked(size: Int)") {
        since("1.2")
        only(Iterables, Sequences, CharSequences)
        doc { f ->
            """
            Splits this ${f.collection} into a ${f.mapResult} of ${f.snapshotResult.pluralize()} each not exceeding the given [size].

            The last ${f.snapshotResult} in the resulting ${f.mapResult} may have less ${f.element.pluralize()} than the given [size].

            @param size the number of elements to take in each ${f.snapshotResult}, must be positive and can be greater than the number of elements in this ${f.collection}.

            @sample samples.collections.Collections.Transformations.chunked
            """
        }
        returns(Iterables) { "List<List<T>>" }
        returns(Sequences) { "Sequence<List<T>>" }
        returns(CharSequences) { "List<String>" }

        sequenceClassification(intermediate, stateful)

        body { "return windowed(size, size, partialWindows = true)" }
    }

    templates add f("chunkedSequence(size: Int, transform: (CharSequence) -> R)") {
        since("1.2")
        only(CharSequences)
        doc { f ->
            """
            Splits this ${f.collection} into several ${f.viewResult.pluralize()} each not exceeding the given [size]
            and applies the given [transform] function to an each.

            @return sequence of results of the [transform] applied to an each ${f.viewResult}.

            Note that the ${f.viewResult} passed to the [transform] function is ephemeral and is valid only inside that function.
            You should not store it or allow it to escape in some way, unless you made a snapshot of it.
            The last ${f.viewResult} may have less ${f.element.pluralize()} than the given [size].

            @param size the number of elements to take in each ${f.viewResult}, must be positive and can be greater than the number of elements in this ${f.collection}.

            @sample samples.text.Strings.chunkedTransformToSequence
            """
        }

        typeParam("R")
        returns { "Sequence<R>" }

        body {
            """
            return windowedSequence(size, size, partialWindows = true, transform = transform)
            """
        }
    }

    templates add f("chunkedSequence(size: Int)") {
        since("1.2")
        only(CharSequences)
        doc { f ->
            """
            Splits this ${f.collection} into a sequence of ${f.snapshotResult.pluralize()} each not exceeding the given [size].

            The last ${f.snapshotResult} in the resulting sequence may have less ${f.element.pluralize()} than the given [size].

            @param size the number of elements to take in each ${f.snapshotResult}, must be positive and can be greater than the number of elements in this ${f.collection}.

            @sample samples.collections.Collections.Transformations.chunked
            """
        }
        returns { "Sequence<String>" }

        body(CharSequences) { "return chunkedSequence(size) { it.toString() }" }
    }

    templates add f("zipWithNext(transform: (a: T, b: T) -> R)") {
        since("1.2")
        only(Iterables, Sequences, CharSequences)
        typeParam("R")
        doc { f ->
            """
            Returns a ${f.mapResult} containing the results of applying the given [transform] function
            to an each pair of two adjacent ${f.element.pluralize()} in this ${f.collection}.

            The returned ${f.mapResult} is empty if this ${f.collection} contains less than two ${f.element.pluralize()}.

            @sample samples.collections.Collections.Transformations.zipWithNextToFindDeltas
            """
        }
        returns("List<R>")
        inline(true)
        body {
            """
            val iterator = iterator()
            if (!iterator.hasNext()) return emptyList()
            val result = mutableListOf<R>()
            var current = iterator.next()
            while (iterator.hasNext()) {
                val next = iterator.next()
                result.add(transform(current, next))
                current = next
            }
            return result
            """
        }
        body(CharSequences) { f ->
            """
            val size = ${if (f == CharSequences) "length" else "size" } - 1
            if (size < 1) return emptyList()
            val result = ArrayList<R>(size)
            for (index in 0..size - 1) {
                result.add(transform(this[index], this[index + 1]))
            }
            return result
            """

        }
        inline(false, Sequences)
        sequenceClassification(intermediate, stateless)
        returns(Sequences) { "Sequence<R>" }
        body(Sequences) {
            """
            return buildSequence result@ {
                val iterator = iterator()
                if (!iterator.hasNext()) return@result
                var current = iterator.next()
                while (iterator.hasNext()) {
                    val next = iterator.next()
                    yield(transform(current, next))
                    current = next
                }
            }
            """
        }
    }

    templates add f("zipWithNext()") {
        since("1.2")
        only(Iterables, Sequences, CharSequences)
        returns("List<Pair<T, T>>")
        doc { f ->
            """
            Returns a ${f.mapResult} of pairs of each two adjacent ${f.element.pluralize()} in this ${f.collection}.

            The returned ${f.mapResult} is empty if this ${f.collection} contains less than two ${f.element.pluralize()}.

            @sample samples.collections.Collections.Transformations.zipWithNext
            """
        }
        sequenceClassification(intermediate, stateless)
        returns(Sequences) { "Sequence<Pair<T, T>>" }
        body {
            "return zipWithNext { a, b -> a to b }"
        }
    }

    templates add f("zip(other: Iterable<R>, transform: (a: T, b: R) -> V)") {
        exclude(Sequences)
        doc {
            """
            Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
            """
        }
        typeParam("R")
        typeParam("V")
        returns("List<V>")
        inline(true)
        body {
            """
            val first = iterator()
            val second = other.iterator()
            val list = ArrayList<V>(minOf(collectionSizeOrDefault(10), other.collectionSizeOrDefault(10)))
            while (first.hasNext() && second.hasNext()) {
                list.add(transform(first.next(), second.next()))
            }
            return list
            """
        }
        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            val arraySize = size
            val list = ArrayList<V>(minOf(other.collectionSizeOrDefault(10), arraySize))
            var i = 0
            for (element in other) {
                if (i >= arraySize) break
                list.add(transform(this[i++], element))
            }
            return list
            """
        }
    }

    templates add f("zip(other: Array<out R>, transform: (a: T, b: R) -> V)") {
        exclude(Sequences)
        doc {
            """
            Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
            """
        }
        typeParam("R")
        typeParam("V")
        returns("List<V>")
        inline(true)
        body {
            """
            val arraySize = other.size
            val list = ArrayList<V>(minOf(collectionSizeOrDefault(10), arraySize))
            var i = 0
            for (element in this) {
                if (i >= arraySize) break
                list.add(transform(element, other[i++]))
            }
            return list
            """
        }
        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            val size = minOf(size, other.size)
            val list = ArrayList<V>(size)
            for (i in 0..size-1) {
                list.add(transform(this[i], other[i]))
            }
            return list
            """
        }

    }

    templates add f("zip(other: SELF, transform: (a: T, b: T) -> V)") {
        only(ArraysOfPrimitives)
        doc {
            """
            Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
            """
        }
        typeParam("V")
        returns("List<V>")
        inline(true)
        body() {
            """
            val size = minOf(size, other.size)
            val list = ArrayList<V>(size)
            for (i in 0..size-1) {
                list.add(transform(this[i], other[i]))
            }
            return list
            """
        }
    }

    templates add f("zip(other: Sequence<R>, transform: (a: T, b: R) -> V)") {
        only(Sequences)
        doc {
            """
            Returns a sequence of values built from elements of both collections with same indexes using provided [transform]. Resulting sequence has length of shortest input sequences.
            """
        }
        sequenceClassification(intermediate, stateless)
        typeParam("R")
        typeParam("V")
        returns("Sequence<V>")
        body {
            """
            return MergingSequence(this, other, transform)
            """
        }
    }

    templates add f("zip(other: CharSequence, transform: (a: Char, b: Char) -> V)") {
        only(CharSequences)
        doc {
            """
            Returns a list of values built from characters of both char sequences with same indexes using provided [transform]. List has length of shortest char sequence.
            """
        }
        typeParam("V")
        returns("List<V>")
        inline(true)
        body {
            """
            val length = minOf(this.length, other.length)

            val list = ArrayList<V>(length)
            for (i in 0..length-1) {
                list.add(transform(this[i], other[i]))
            }
            return list
            """
        }
    }


    templates add f("zip(other: Iterable<R>)") {
        infix(true)
        exclude(Sequences)
        doc {
            """
            Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
            """
        }
        typeParam("R")
        returns("List<Pair<T, R>>")
        body {
            """
            return zip(other) { t1, t2 -> t1 to t2 }
            """
        }
    }

    templates add f("zip(other: CharSequence)") {
        infix(true)
        only(CharSequences)
        doc {
            """
            Returns a list of pairs built from characters of both char sequences with same indexes. List has length of shortest char sequence.
            """
        }
        returns("List<Pair<Char, Char>>")
        body {
            """
            return zip(other) { c1, c2 -> c1 to c2 }
            """
        }
    }

    templates add f("zip(other: Array<out R>)") {
        infix(true)
        exclude(Sequences)
        doc {
            """
            Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
            """
        }
        typeParam("R")
        returns("List<Pair<T, R>>")
        body {
            """
            return zip(other) { t1, t2 -> t1 to t2 }
            """
        }
    }

    templates add f("zip(other: SELF)") {
        infix(true)
        only(ArraysOfPrimitives)
        doc {
            """
            Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
            """
        }
        returns("List<Pair<T, T>>")
        body {
            """
            return zip(other) { t1, t2 -> t1 to t2 }
            """
        }
    }

    templates add f("zip(other: Sequence<R>)") {
        infix(true)
        only(Sequences)
        doc {
            """
            Returns a sequence of pairs built from elements of both sequences with same indexes.
            Resulting sequence has length of shortest input sequence.
            """
        }
        sequenceClassification(intermediate, stateless)
        typeParam("R")
        returns("Sequence<Pair<T, R>>")
        body {
            """
            return MergingSequence(this, other) { t1, t2 -> t1 to t2 }
            """
        }
    }

    return templates
}

// documentation helpers

private val Family.snapshotResult: String
    get() = when (this) {
        CharSequences, Strings -> "string"
        else -> "list"
    }

private val Family.viewResult: String
    get() = when (this) {
        CharSequences, Strings -> "char sequence"
        else -> "list"
    }

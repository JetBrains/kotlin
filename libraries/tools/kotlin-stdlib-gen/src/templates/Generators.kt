/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package templates

import templates.Family.*
import templates.SequenceClass.*

object Generators : TemplateGroupBase() {

    val f_plusElement = fn("plusElement(element: T)") {
        include(Iterables, Collections, Sets, Sequences)
    } builder {
        inlineOnly()

        doc { "Returns a list containing all elements of the original collection and then the given [element]." }
        specialFor(Sets) {
            doc {
                """
            Returns a set containing all elements of the original set and then the given [element] if it isn't already in this set.

            The returned set preserves the element iteration order of the original set.
            """
            }
        }
        specialFor(Sequences) {
            doc { "Returns a sequence containing all elements of the original sequence and then the given [element]." }
        }
        sequenceClassification(intermediate, stateless)

        returns("List<T>")
        specialFor(Sets, Sequences) { returns("SELF") }
        body { "return plus(element)" }
    }

    val f_plus = fn("plus(element: T)") {
        include(Iterables, Collections, Sets, Sequences)
    } builder {
        operator(true)

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

        specialFor(Sets, Sequences) { returns("SELF") }
        specialFor(Sets) {
            doc {
                """
                Returns a set containing all elements of the original set and then the given [element] if it isn't already in this set.

                The returned set preserves the element iteration order of the original set.
                """
            }
            body {
                """
                val result = LinkedHashSet<T>(mapCapacity(size + 1))
                result.addAll(this)
                result.add(element)
                return result
                """
            }
        }

        specialFor(Sequences) {
            doc { "Returns a sequence containing all elements of the original sequence and then the given [element]." }
            body {
                """
                return sequenceOf(this, sequenceOf(element)).flatten()
                """
            }
        }
    }

    val f_plus_iterable = fn("plus(elements: Iterable<T>)") {
        include(Iterables, Collections, Sets, Sequences)
    } builder {
        operator(true)

        doc {  "Returns a list containing all elements of the original ${f.collection} and then all elements of the given [elements] collection." }
        returns("List<T>")
        specialFor(Sets, Sequences) { returns("SELF") }
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

        specialFor(Sets) {
            doc {
                """
                Returns a set containing all elements of the original set and the given [elements] collection,
                which aren't already in this set.
                The returned set preserves the element iteration order of the original set.
                """
            }
            body {
                """
                val result = LinkedHashSet<T>(mapCapacity(elements.collectionSizeOrNull()?.let { this.size + it } ?: this.size * 2))
                result.addAll(this)
                result.addAll(elements)
                return result
                """
            }
        }
        specialFor(Sequences) {
            doc {
                """
                Returns a sequence containing all elements of original sequence and then all elements of the given [elements] collection.

                Note that the source sequence and the collection being added are iterated only when an `iterator` is requested from
                the resulting sequence. Changing any of them between successive calls to `iterator` may affect the result.
                """
            }
            sequenceClassification(intermediate, stateless)
            body {
                """
                return sequenceOf(this, elements.asSequence()).flatten()
                """
            }
        }

    }

    val f_plus_array = fn("plus(elements: Array<out T>)") {
        include(Iterables, Collections, Sets, Sequences)
    } builder {
        operator(true)

        doc { "Returns a list containing all elements of the original collection and then all elements of the given [elements] array." }
        returns("List<T>")
        specialFor(Sets, Sequences) { returns("SELF") }
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
        specialFor(Sets) {
            doc {
                """
                Returns a set containing all elements of the original set and the given [elements] array,
                which aren't already in this set.

                The returned set preserves the element iteration order of the original set.
                """
            }
            body {
                """
                val result = LinkedHashSet<T>(mapCapacity(this.size + elements.size))
                result.addAll(this)
                result.addAll(elements)
                return result
                """
            }
        }
        specialFor(Sequences) {
            doc {
                """
                Returns a sequence containing all elements of original sequence and then all elements of the given [elements] array.

                Note that the source sequence and the array being added are iterated only when an `iterator` is requested from
                the resulting sequence. Changing any of them between successive calls to `iterator` may affect the result.
                """
            }
            sequenceClassification(intermediate, stateless)
            body {
                """
                return this.plus(elements.asList())
                """
            }
        }
    }


    val f_plus_sequence = fn("plus(elements: Sequence<T>)") {
        include(Iterables, Sets, Sequences, Collections)
    } builder {
        operator(true)

        doc { "Returns a list containing all elements of the original collection and then all elements of the given [elements] sequence." }
        returns("List<T>")
        specialFor(Sets, Sequences) { returns("SELF") }
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


        specialFor(Sets) {
            doc {
                """
                Returns a set containing all elements of the original set and the given [elements] sequence,
                which aren't already in this set.

                The returned set preserves the element iteration order of the original set.
                """
            }
            body {
                """
                val result = LinkedHashSet<T>(mapCapacity(this.size * 2))
                result.addAll(this)
                result.addAll(elements)
                return result
                """
            }
        }

        specialFor(Sequences) {
            doc {
                """
                Returns a sequence containing all elements of original sequence and then all elements of the given [elements] sequence.

                Note that the source sequence and the sequence being added are iterated only when an `iterator` is requested from
                the resulting sequence. Changing any of them between successive calls to `iterator` may affect the result.
                """
            }
            sequenceClassification(intermediate, stateless)
            body {
                """
                return sequenceOf(this, elements).flatten()
                """
            }
        }
    }

    val f_minusElement = fn("minusElement(element: T)") {
        include(Iterables, Sets, Sequences)
    } builder {
        inline(Inline.Only)

        doc { "Returns a list containing all elements of the original collection without the first occurrence of the given [element]." }
        specialFor(Sets) {
            doc {
                """
                Returns a set containing all elements of the original set except the given [element].

                The returned set preserves the element iteration order of the original set.
                """
            }
        }
        specialFor(Sequences) {
            doc { "Returns a sequence containing all elements of the original sequence without the first occurrence of the given [element]." }
            sequenceClassification(intermediate, stateless)
        }

        returns("List<T>")
        specialFor(Sets, Sequences) { returns("SELF") }
        body { "return minus(element)" }
    }

    val f_minus = fn("minus(element: T)") {
        include(Iterables, Sets, Sequences)
    } builder {
        operator(true)

        doc { "Returns a list containing all elements of the original collection without the first occurrence of the given [element]." }
        returns("List<T>")
        body {
            """
            val result = ArrayList<T>(collectionSizeOrDefault(10))
            var removed = false
            return this.filterTo(result) { if (!removed && it == element) { removed = true; false } else true }
            """
        }

        specialFor(Sets, Sequences) { returns("SELF") }
        specialFor(Sets) {
            doc {
                """
                Returns a set containing all elements of the original set except the given [element].

                The returned set preserves the element iteration order of the original set.
                """
            }
            body {
                """
                val result = LinkedHashSet<T>(mapCapacity(size))
                var removed = false
                return this.filterTo(result) { if (!removed && it == element) { removed = true; false } else true }
                """
            }
        }


        specialFor(Sequences) {
            doc { "Returns a sequence containing all elements of the original sequence without the first occurrence of the given [element]." }
            sequenceClassification(intermediate, stateless)
            body {
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
    }


    val f_minus_iterable = fn("minus(elements: Iterable<T>)") {
        include(Iterables, Sets, Sequences)
    } builder {
        operator(true)

        doc { "Returns a list containing all elements of the original collection except the elements contained in the given [elements] collection." }
        returns("List<T>")
        specialFor(Sets, Sequences) { returns("SELF") }
        body {
            """
            val other = elements.convertToSetForSetOperationWith(this)
            if (other.isEmpty())
                return this.toList()

            return this.filterNot { it in other }
            """
        }

        specialFor(Sets) {
            doc {
                """
                Returns a set containing all elements of the original set except the elements contained in the given [elements] collection.

                The returned set preserves the element iteration order of the original set.
                """
            }
            body {
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
        }

        specialFor(Sequences) {
            doc {
                """
                Returns a sequence containing all elements of original sequence except the elements contained in the given [elements] collection.

                Note that the source sequence and the collection being subtracted are iterated only when an `iterator` is requested from
                the resulting sequence. Changing any of them between successive calls to `iterator` may affect the result.
                """
            }
            sequenceClassification(intermediate, stateful)
            body {
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
    }

    val f_minus_array = fn("minus(elements: Array<out T>)") {
        include(Iterables, Sets, Sequences)
    } builder {
        operator(true)

        doc { "Returns a list containing all elements of the original collection except the elements contained in the given [elements] array." }
        returns("List<T>")
        specialFor(Sets, Sequences) { returns("SELF") }
        body {
            """
            if (elements.isEmpty()) return this.toList()
            val other = elements.toHashSet()
            return this.filterNot { it in other }
            """
        }
        specialFor(Sets) {
            doc {
                """
                Returns a set containing all elements of the original set except the elements contained in the given [elements] array.

                The returned set preserves the element iteration order of the original set.
                """
            }
            body {
                """
                val result = LinkedHashSet<T>(this)
                result.removeAll(elements)
                return result
                """
            }
        }

        specialFor(Sequences) {
            doc {
                """
                Returns a sequence containing all elements of original sequence except the elements contained in the given [elements] array.

                Note that the source sequence and the array being subtracted are iterated only when an `iterator` is requested from
                the resulting sequence. Changing any of them between successive calls to `iterator` may affect the result.
                """
            }
            sequenceClassification(intermediate, stateful)
            body {
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
    }

    val f_minus_sequence = fn("minus(elements: Sequence<T>)") {
        include(Iterables, Sets, Sequences)
    } builder {
        operator(true)

        doc { "Returns a list containing all elements of the original collection except the elements contained in the given [elements] sequence." }
        returns("List<T>")
        specialFor(Sets, Sequences) { returns("SELF") }
        body {
            """
            val other = elements.toHashSet()
            if (other.isEmpty())
                return this.toList()

            return this.filterNot { it in other }
            """
        }
        specialFor(Sets) {
            doc {
                """
                Returns a set containing all elements of the original set except the elements contained in the given [elements] sequence.

                The returned set preserves the element iteration order of the original set.
                """
            }
            body {
                """
                val result = LinkedHashSet<T>(this)
                result.removeAll(elements)
                return result
                """
            }
        }

        specialFor(Sequences) {
            doc {
                """
                Returns a sequence containing all elements of original sequence except the elements contained in the given [elements] sequence.

                Note that the source sequence and the sequence being subtracted are iterated only when an `iterator` is requested from
                the resulting sequence. Changing any of them between successive calls to `iterator` may affect the result.

                The operation is _intermediate_ for this sequence and _terminal_ and _stateful_ for the [elements] sequence.
                """
            }
            body {
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
    }

    val f_partition = fn("partition(predicate: (T) -> Boolean)") {
        includeDefault()
        include(CharSequences, Strings)
    } builder {
        inline()

        doc {
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

        specialFor(CharSequences, Strings) {
            doc {
                """
                Splits the original ${f.collection} into pair of ${f.collection}s,
                where *first* ${f.collection} contains characters for which [predicate] yielded `true`,
                while *second* ${f.collection} contains characters for which [predicate] yielded `false`.
                """
            }
            returns("Pair<SELF, SELF>")
        }
        body(CharSequences, Strings) {
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

    val f_windowed_transform = fn("windowed(size: Int, step: Int = 1, partialWindows: Boolean = false, transform: (List<T>) -> R)") {
        include(Iterables, Sequences, CharSequences)
    } builder {
        since("1.2")
        doc {
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
            """
        }
        sample("samples.collections.Sequences.Transformations.averageWindows")

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

        specialFor(CharSequences) {
            signature("windowed(size: Int, step: Int = 1, partialWindows: Boolean = false, transform: (CharSequence) -> R)")
        }
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

        specialFor(Sequences) { returns("Sequence<R>") }
        body(Sequences) {
            """
            return windowedSequence(size, step, partialWindows, reuseBuffer = true).map(transform)
            """
        }
    }

    val f_windowed = fn("windowed(size: Int, step: Int = 1, partialWindows: Boolean = false)") {
        include(Iterables, Sequences, CharSequences)
    } builder {
        since("1.2")
        specialFor(Iterables) { returns("List<List<T>>") }
        specialFor(Sequences) { returns("Sequence<List<T>>") }
        specialFor(CharSequences) { returns("List<String>") }

        doc {
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
            """
        }
        sample("samples.collections.Sequences.Transformations.takeWindows")

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

    val f_windowedSequence_transform = fn("windowedSequence(size: Int, step: Int = 1, partialWindows: Boolean = false, transform: (CharSequence) -> R)") {
        include(CharSequences)
    } builder {
        since("1.2")
        doc {
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
            """
        }
        sample("samples.collections.Sequences.Transformations.averageWindows")
        typeParam("R")
        returns("Sequence<R>")

        body {
            """
            checkWindowSizeStep(size, step)
            val windows = (if (partialWindows) indices else 0 until length - size + 1) step step
            return windows.asSequence().map { index -> transform(subSequence(index, (index + size).coerceAtMost(length))) }
            """
        }
    }

    val f_windowedSequence = fn("windowedSequence(size: Int, step: Int = 1, partialWindows: Boolean = false)") {
        include(CharSequences)
    } builder {
        since("1.2")
        doc {
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
            """
        }
        sample("samples.collections.Sequences.Transformations.takeWindows")
        returns("Sequence<String>")

        body(CharSequences) { "return windowedSequence(size, step, partialWindows) { it.toString() }" }
    }

    val f_chunked_transform = fn("chunked(size: Int, transform: (List<T>) -> R)") {
        include(Iterables, Sequences, CharSequences)
    } builder {
        since("1.2")
        doc {
            """
            Splits this ${f.collection} into several ${f.viewResult.pluralize()} each not exceeding the given [size]
            and applies the given [transform] function to an each.

            @return ${f.mapResult} of results of the [transform] applied to an each ${f.viewResult}.

            Note that the ${f.viewResult} passed to the [transform] function is ephemeral and is valid only inside that function.
            You should not store it or allow it to escape in some way, unless you made a snapshot of it.
            The last ${f.viewResult} may have less ${f.element.pluralize()} than the given [size].

            @param size the number of elements to take in each ${f.viewResult}, must be positive and can be greater than the number of elements in this ${f.collection}.
            """
        }
        sample("samples.text.Strings.chunkedTransform")

        typeParam("R")
        returns("List<R>")

        specialFor(CharSequences) {
            signature("chunked(size: Int, transform: (CharSequence) -> R)")
        }

        sequenceClassification(intermediate, stateful)
        specialFor(Sequences) { returns("Sequence<R>") }
        body { "return windowed(size, size, partialWindows = true, transform = transform)" }
    }

    val f_chunked = fn("chunked(size: Int)") {
        include(Iterables, Sequences, CharSequences)
    } builder {
        since("1.2")
        doc {
            """
            Splits this ${f.collection} into a ${f.mapResult} of ${f.snapshotResult.pluralize()} each not exceeding the given [size].

            The last ${f.snapshotResult} in the resulting ${f.mapResult} may have less ${f.element.pluralize()} than the given [size].

            @param size the number of elements to take in each ${f.snapshotResult}, must be positive and can be greater than the number of elements in this ${f.collection}.
            """
        }
        sample("samples.collections.Collections.Transformations.chunked")
        specialFor(Iterables) { returns("List<List<T>>") }
        specialFor(Sequences) { returns("Sequence<List<T>>") }
        specialFor(CharSequences) { returns("List<String>") }

        sequenceClassification(intermediate, stateful)

        body { "return windowed(size, size, partialWindows = true)" }
    }

    val f_chunkedSequence_transform = fn("chunkedSequence(size: Int, transform: (CharSequence) -> R)") {
        include(CharSequences)
    } builder {
        since("1.2")
        doc {
            """
            Splits this ${f.collection} into several ${f.viewResult.pluralize()} each not exceeding the given [size]
            and applies the given [transform] function to an each.

            @return sequence of results of the [transform] applied to an each ${f.viewResult}.

            Note that the ${f.viewResult} passed to the [transform] function is ephemeral and is valid only inside that function.
            You should not store it or allow it to escape in some way, unless you made a snapshot of it.
            The last ${f.viewResult} may have less ${f.element.pluralize()} than the given [size].

            @param size the number of elements to take in each ${f.viewResult}, must be positive and can be greater than the number of elements in this ${f.collection}.
            """
        }
        sample("samples.text.Strings.chunkedTransformToSequence")

        typeParam("R")
        returns("Sequence<R>")

        body {
            """
            return windowedSequence(size, size, partialWindows = true, transform = transform)
            """
        }
    }

    val f_chunkedSequence = fn("chunkedSequence(size: Int)") {
        include(CharSequences)
    } builder {
        since("1.2")
        doc {
            """
            Splits this ${f.collection} into a sequence of ${f.snapshotResult.pluralize()} each not exceeding the given [size].

            The last ${f.snapshotResult} in the resulting sequence may have less ${f.element.pluralize()} than the given [size].

            @param size the number of elements to take in each ${f.snapshotResult}, must be positive and can be greater than the number of elements in this ${f.collection}.
            """
        }
        sample("samples.collections.Collections.Transformations.chunked")
        returns("Sequence<String>")

        body(CharSequences) { "return chunkedSequence(size) { it.toString() }" }
    }

    val f_zipWithNext_transform = fn("zipWithNext(transform: (a: T, b: T) -> R)") {
        include(Iterables, Sequences, CharSequences)
    } builder {
        since("1.2")
        typeParam("R")
        doc {
            """
            Returns a ${f.mapResult} containing the results of applying the given [transform] function
            to an each pair of two adjacent ${f.element.pluralize()} in this ${f.collection}.

            The returned ${f.mapResult} is empty if this ${f.collection} contains less than two ${f.element.pluralize()}.
            """
        }
        sample("samples.collections.Collections.Transformations.zipWithNextToFindDeltas")
        returns("List<R>")
        inline()
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
        body(CharSequences) {
            """
            val size = ${if (f == CharSequences) "length" else "size" } - 1
            if (size < 1) return emptyList()
            val result = ArrayList<R>(size)
            for (index in 0 until size) {
                result.add(transform(this[index], this[index + 1]))
            }
            return result
            """
        }
        sequenceClassification(intermediate, stateless)
        specialFor(Sequences) {
            inline(Inline.No)
            returns("Sequence<R>")
        }
        body(Sequences) {
            """
            return sequence result@ {
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

    val f_zipWithNext = fn("zipWithNext()") {
        include(Iterables, Sequences, CharSequences)
    } builder {
        since("1.2")
        returns("List<Pair<T, T>>")
        doc {
            """
            Returns a ${f.mapResult} of pairs of each two adjacent ${f.element.pluralize()} in this ${f.collection}.

            The returned ${f.mapResult} is empty if this ${f.collection} contains less than two ${f.element.pluralize()}.
            """
        }
        sample("samples.collections.Collections.Transformations.zipWithNext")
        sequenceClassification(intermediate, stateless)
        specialFor(Sequences) { returns("Sequence<Pair<T, T>>") }
        body {
            "return zipWithNext { a, b -> a to b }"
        }
    }

    val f_zip_transform = fn("zip(other: Iterable<R>, transform: (a: T, b: R) -> V)") {
        include(Iterables, ArraysOfObjects, ArraysOfPrimitives)
    } builder {
        doc {
            """
            Returns a list of values built from the elements of `this` ${f.collection} and the [other] collection with the same index
            using the provided [transform] function applied to each pair of elements.
            The returned list has length of the shortest collection.
            """
        }
        sample("samples.collections.Iterables.Operations.zipIterableWithTransform")
        typeParam("R")
        typeParam("V")
        returns("List<V>")
        inline()
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

    val f_zip_array_transform = fn("zip(other: Array<out R>, transform: (a: T, b: R) -> V)") {
        include(Iterables, ArraysOfObjects, ArraysOfPrimitives)
    } builder {
        doc {
            """
            Returns a list of values built from the elements of `this` ${f.collection} and the [other] array with the same index
            using the provided [transform] function applied to each pair of elements.
            The returned list has length of the shortest collection.
            """
        }
        sample("samples.collections.Iterables.Operations.zipIterableWithTransform")
        typeParam("R")
        typeParam("V")
        returns("List<V>")
        inline()
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
            for (i in 0 until size) {
                list.add(transform(this[i], other[i]))
            }
            return list
            """
        }

    }

    val f_zip_sameArray_transform = fn("zip(other: SELF, transform: (a: T, b: T) -> V)") {
        include(ArraysOfPrimitives)
    } builder {
        doc {
            """
            Returns a list of values built from the elements of `this` array and the [other] array with the same index
            using the provided [transform] function applied to each pair of elements.
            The returned list has length of the shortest array.
            """
        }
        sample("samples.collections.Iterables.Operations.zipIterableWithTransform")
        typeParam("V")
        returns("List<V>")
        inline()
        body {
            """
            val size = minOf(size, other.size)
            val list = ArrayList<V>(size)
            for (i in 0 until size) {
                list.add(transform(this[i], other[i]))
            }
            return list
            """
        }
    }

    val f_zip_sequence_transform = fn("zip(other: Sequence<R>, transform: (a: T, b: R) -> V)") {
        include(Sequences)
    } builder {
        doc {
            """
            Returns a sequence of values built from the elements of `this` sequence and the [other] sequence with the same index
            using the provided [transform] function applied to each pair of elements.
            The resulting sequence ends as soon as the shortest input sequence ends.
            """
        }
        sample("samples.collections.Sequences.Transformations.zipWithTransform")
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

    val f_zip_charSequence_transform = fn("zip(other: CharSequence, transform: (a: Char, b: Char) -> V)") {
        include(CharSequences)
    } builder {
        doc {
            """
            Returns a list of values built from the characters of `this` and the [other] char sequences with the same index
            using the provided [transform] function applied to each pair of characters.
            The returned list has length of the shortest char sequence.
            """
        }
        sample("samples.text.Strings.zipWithTransform")
        typeParam("V")
        returns("List<V>")
        inline()
        body {
            """
            val length = minOf(this.length, other.length)

            val list = ArrayList<V>(length)
            for (i in 0 until length) {
                list.add(transform(this[i], other[i]))
            }
            return list
            """
        }
    }


    val f_zip = fn("zip(other: Iterable<R>)") {
        include(Iterables, ArraysOfObjects, ArraysOfPrimitives)
    } builder {
        infix(true)
        doc {
            """
            Returns a list of pairs built from the elements of `this` collection and [other] ${f.collection} with the same index.
            The returned list has length of the shortest collection.
            """
        }
        sample("samples.collections.Iterables.Operations.zipIterable")
        typeParam("R")
        returns("List<Pair<T, R>>")
        body {
            """
            return zip(other) { t1, t2 -> t1 to t2 }
            """
        }
    }

    val f_zip_charSequence = fn("zip(other: CharSequence)") {
        include(CharSequences)
    } builder {
        infix(true)
        doc {
            """
            Returns a list of pairs built from the characters of `this` and the [other] char sequences with the same index
            The returned list has length of the shortest char sequence.
            """
        }
        sample("samples.text.Strings.zip")
        returns("List<Pair<Char, Char>>")
        body {
            """
            return zip(other) { c1, c2 -> c1 to c2 }
            """
        }
    }

    val f_zip_array = fn("zip(other: Array<out R>)") {
        include(Iterables, ArraysOfObjects, ArraysOfPrimitives)
    } builder {
        infix(true)
        doc {
            """
            Returns a list of pairs built from the elements of `this` ${f.collection} and the [other] array with the same index.
            The returned list has length of the shortest collection.
            """
        }
        sample("samples.collections.Iterables.Operations.zipIterable")
        typeParam("R")
        returns("List<Pair<T, R>>")
        body {
            """
            return zip(other) { t1, t2 -> t1 to t2 }
            """
        }
    }

    val f_zip_sameArray = fn("zip(other: SELF)") {
        include(ArraysOfPrimitives)
    } builder {
        infix(true)
        doc {
            """
            Returns a list of pairs built from the elements of `this` array and the [other] array with the same index.
            The returned list has length of the shortest collection.
            """
        }
        sample("samples.collections.Iterables.Operations.zipIterable")
        returns("List<Pair<T, T>>")
        body {
            """
            return zip(other) { t1, t2 -> t1 to t2 }
            """
        }
    }

    val f_zip_sequence = fn("zip(other: Sequence<R>)") {
        include(Sequences)
    } builder {
        infix(true)
        doc {
            """
            Returns a sequence of values built from the elements of `this` sequence and the [other] sequence with the same index.
            The resulting sequence ends as soon as the shortest input sequence ends.
            """
        }
        sample("samples.collections.Sequences.Transformations.zip")
        sequenceClassification(intermediate, stateless)
        typeParam("R")
        returns("Sequence<Pair<T, R>>")
        body {
            """
            return MergingSequence(this, other) { t1, t2 -> t1 to t2 }
            """
        }
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
}

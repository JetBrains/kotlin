/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package templates

import templates.Family.*
import templates.SequenceClass.*

object Aggregates : TemplateGroupBase() {

    init {
        defaultBuilder {
            if (sequenceClassification.isEmpty()) {
                sequenceClassification(terminal)
            }
            specialFor(ArraysOfUnsigned) {
                since("1.3")
                annotation("@ExperimentalUnsignedTypes")
            }
        }
    }

    val f_all = fn("all(predicate: (T) -> Boolean)") {
        includeDefault()
        include(Maps, CharSequences, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc {
            """
            Returns `true` if all ${f.element.pluralize()} match the given [predicate].
            """
        }
        sample("samples.collections.Collections.Aggregates.all")
        returns("Boolean")
        body {
            """
            ${when (f) {
                Iterables -> "if (this is Collection && isEmpty()) return true"
                Maps -> "if (isEmpty()) return true"
                else -> ""
            }}
            for (element in this) if (!predicate(element)) return false
            return true
            """
        }
    }

    val f_none_predicate = fn("none(predicate: (T) -> Boolean)") {
        includeDefault()
        include(Maps, CharSequences, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc {
            """
            Returns `true` if no ${f.element.pluralize()} match the given [predicate].
            """
        }
        sample("samples.collections.Collections.Aggregates.noneWithPredicate")
        returns("Boolean")
        body {
            """
            ${when (f) {
                Iterables -> "if (this is Collection && isEmpty()) return true"
                Maps -> "if (isEmpty()) return true"
                else -> ""
            }}
            for (element in this) if (predicate(element)) return false
            return true
            """
        }
    }

    val f_none = fn("none()") {
        includeDefault()
        include(Maps, CharSequences, ArraysOfUnsigned)
    } builder {
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc {
            """
            Returns `true` if the ${f.collection} has no ${f.element.pluralize()}.
            """
        }
        sample("samples.collections.Collections.Aggregates.none")
        returns("Boolean")
        body {
            "return !iterator().hasNext()"
        }
        specialFor(Iterables) {
            body {
                """
                if (this is Collection) return isEmpty()
                return !iterator().hasNext()
                """
            }
        }

        body(Maps, CharSequences, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned) {
            "return isEmpty()"
        }
    }

    val f_any_predicate = fn("any(predicate: (T) -> Boolean)") {
        includeDefault()
        include(Maps, CharSequences, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc {
            """
            Returns `true` if at least one ${f.element} matches the given [predicate].
            """
        }
        sample("samples.collections.Collections.Aggregates.anyWithPredicate")
        returns("Boolean")
        body {
            """
            ${when (f) {
                Iterables -> "if (this is Collection && isEmpty()) return false"
                Maps -> "if (isEmpty()) return false"
                else -> ""
            }}
            for (element in this) if (predicate(element)) return true
            return false
            """
        }
    }

    val f_any = fn("any()") {
        includeDefault()
        include(Maps, CharSequences, ArraysOfUnsigned)
    } builder {
        doc {
            """
            Returns `true` if ${f.collection} has at least one ${f.element}.
            """
        }
        sample("samples.collections.Collections.Aggregates.any")
        returns("Boolean")
        body {
            "return iterator().hasNext()"
        }
        body(Iterables) {
            """
            if (this is Collection) return !isEmpty()
            return iterator().hasNext()
            """
        }
        body(Maps, CharSequences, ArraysOfObjects, ArraysOfPrimitives) { "return !isEmpty()" }

        specialFor(ArraysOfUnsigned) {
            inlineOnly()
            body { "return storage.any()" }
        }
    }


    val f_count_predicate = fn("count(predicate: (T) -> Boolean)") {
        includeDefault()
        include(Maps, CharSequences, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { "Returns the number of ${f.element.pluralize()} matching the given [predicate]." }
        returns("Int")
        body {
            fun checkOverflow(value: String) = if (f == Sequences || f == Iterables) "checkCountOverflow($value)" else value
            """
            ${when (f) {
                Iterables -> "if (this is Collection && isEmpty()) return 0"
                Maps -> "if (isEmpty()) return 0"
                else -> ""
            }}
            var count = 0
            for (element in this) if (predicate(element)) ${checkOverflow("++count")}
            return count
            """
        }
    }

    val f_count = fn("count()") {
        includeDefault()
        include(Collections, Maps, CharSequences)
    } builder {
        doc { "Returns the number of ${f.element.pluralize()} in this ${f.collection}." }
        returns("Int")
        body {
            fun checkOverflow(value: String) = if (f == Sequences || f == Iterables) "checkCountOverflow($value)" else value
            """
            ${if (f == Iterables) "if (this is Collection) return size" else ""}
            var count = 0
            for (element in this) ${checkOverflow("++count")}
            return count
            """
        }

        specialFor(CharSequences, Maps, Collections, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned) { inlineOnly() }

        specialFor(CharSequences) {
            doc { "Returns the length of this char sequence." }
            body { "return length" }
        }
        body(Maps, Collections, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned) {
            "return size"
        }
    }

    val f_sumBy = fn("sumBy(selector: (T) -> Int)") {
        includeDefault()
        include(CharSequences, ArraysOfUnsigned)
    } builder {
        inline()
        doc { "Returns the sum of all values produced by [selector] function applied to each ${f.element} in the ${f.collection}." }
        returns("Int")
        body {
            """
            var sum: Int = 0
            for (element in this) {
                sum += selector(element)
            }
            return sum
            """
        }

        specialFor(ArraysOfUnsigned) {
            inlineOnly()
            signature("sumBy(selector: (T) -> UInt)")
            returns("UInt")
            body {
                """
                var sum: UInt = 0u
                for (element in this) {
                    sum += selector(element)
                }
                return sum
                """
            }
        }
    }

    val f_sumByDouble = fn("sumByDouble(selector: (T) -> Double)") {
        includeDefault()
        include(CharSequences, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { "Returns the sum of all values produced by [selector] function applied to each ${f.element} in the ${f.collection}." }
        returns("Double")
        body {
            """
            var sum: Double = 0.0
            for (element in this) {
                sum += selector(element)
            }
            return sum
            """
        }
    }


    val f_minMax = run {
        val genericSpecializations = PrimitiveType.floatingPointPrimitives + setOf(null)

        listOf("min", "max").map { op ->
            fn("$op()") {
                include(Iterables, genericSpecializations)
                include(Sequences, genericSpecializations)
                include(ArraysOfObjects, genericSpecializations)
                include(ArraysOfPrimitives, PrimitiveType.defaultPrimitives - PrimitiveType.Boolean)
                include(ArraysOfUnsigned)
                include(CharSequences)
            } builder {
                val isFloat = primitive?.isFloatingPoint() == true
                val isGeneric = f in listOf(Iterables, Sequences, ArraysOfObjects)

                typeParam("T : Comparable<T>")
                if (primitive != null) {
                    if (isFloat && isGeneric)
                        since("1.1")
                }
                doc {
                    "Returns the ${if (op == "max") "largest" else "smallest"} ${f.element} or `null` if there are no ${f.element.pluralize()}." +
                    if (isFloat) "\n\n" + "If any of ${f.element.pluralize()} is `NaN` returns `NaN`." else ""
                }
                returns("T?")

                body {
                    """
                    val iterator = iterator()
                    if (!iterator.hasNext()) return null
                    var $op = iterator.next()
                    ${if (isFloat) "if ($op.isNaN()) return $op" else "\\"}

                    while (iterator.hasNext()) {
                        val e = iterator.next()
                        ${if (isFloat) "if (e.isNaN()) return e" else "\\"}
                        if ($op ${if (op == "max") "<" else ">"} e) $op = e
                    }
                    return $op
                    """
                }
                body(ArraysOfObjects, ArraysOfPrimitives, CharSequences, ArraysOfUnsigned) {
                    """
                    if (isEmpty()) return null
                    var $op = this[0]
                    ${if (isFloat) "if ($op.isNaN()) return $op" else "\\"}

                    for (i in 1..lastIndex) {
                        val e = this[i]
                        ${if (isFloat) "if (e.isNaN()) return e" else "\\"}
                        if ($op ${if (op == "max") "<" else ">"} e) $op = e
                    }
                    return $op
                    """
                }
                body {
                    body!!.replace(Regex("""^\s+\\\n""", RegexOption.MULTILINE), "") // remove empty lines ending with \
                }
            }
        }
    }

    val f_minBy = fn("minBy(selector: (T) -> R)") {
        includeDefault()
        include(Maps, CharSequences, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { "Returns the first ${f.element} yielding the smallest value of the given function or `null` if there are no ${f.element.pluralize()}." }
        sample("samples.collections.Collections.Aggregates.minBy")
        typeParam("R : Comparable<R>")
        returns("T?")
        body {
            """
            val iterator = iterator()
            if (!iterator.hasNext()) return null

            var minElem = iterator.next()
            if (!iterator.hasNext()) return minElem
            var minValue = selector(minElem)
            do {
                val e = iterator.next()
                val v = selector(e)
                if (minValue > v) {
                    minElem = e
                    minValue = v
                }
            } while (iterator.hasNext())
            return minElem
            """
        }
        body(CharSequences, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned) {
            """
            if (isEmpty()) return null

            var minElem = this[0]
            val lastIndex = this.lastIndex
            if (lastIndex == 0) return minElem
            var minValue = selector(minElem)
            for (i in 1..lastIndex) {
                val e = this[i]
                val v = selector(e)
                if (minValue > v) {
                    minElem = e
                    minValue = v
                }
            }
            return minElem
            """
        }
        body(Maps) {
            "return entries.minBy(selector)"
        }
    }

    val f_minWith = fn("minWith(comparator: Comparator<in T>)") {
        includeDefault()
        include(Maps, CharSequences, ArraysOfUnsigned)
    } builder {
        doc { "Returns the first ${f.element} having the smallest value according to the provided [comparator] or `null` if there are no ${f.element.pluralize()}." }
        returns("T?")
        body {
            """
            val iterator = iterator()
            if (!iterator.hasNext()) return null

            var min = iterator.next()
            while (iterator.hasNext()) {
                val e = iterator.next()
                if (comparator.compare(min, e) > 0) min = e
            }
            return min
            """
        }
        body(CharSequences, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned) {
            """
            if (isEmpty()) return null
            var min = this[0]
            for (i in 1..lastIndex) {
                val e = this[i]
                if (comparator.compare(min, e) > 0) min = e
            }
            return min
            """
        }
        body(Maps) { "return entries.minWith(comparator)" }
    }

    val f_maxBy = fn("maxBy(selector: (T) -> R)") {
        includeDefault()
        include(Maps, CharSequences, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { "Returns the first ${f.element} yielding the largest value of the given function or `null` if there are no ${f.element.pluralize()}." }
        sample("samples.collections.Collections.Aggregates.maxBy")
        typeParam("R : Comparable<R>")
        returns("T?")
        body {
            """
            val iterator = iterator()
            if (!iterator.hasNext()) return null

            var maxElem = iterator.next()
            if (!iterator.hasNext()) return maxElem
            var maxValue = selector(maxElem)
            do {
                val e = iterator.next()
                val v = selector(e)
                if (maxValue < v) {
                    maxElem = e
                    maxValue = v
                }
            } while (iterator.hasNext())
            return maxElem
            """
        }
        body(CharSequences, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned) {
            """
            if (isEmpty()) return null

            var maxElem = this[0]
            val lastIndex = this.lastIndex
            if (lastIndex == 0) return maxElem
            var maxValue = selector(maxElem)
            for (i in 1..lastIndex) {
                val e = this[i]
                val v = selector(e)
                if (maxValue < v) {
                    maxElem = e
                    maxValue = v
                }
            }
            return maxElem
            """
        }
        specialFor(Maps) {
            inlineOnly()
            body { "return entries.maxBy(selector)" }
        }
    }

    val f_maxWith = fn("maxWith(comparator: Comparator<in T>)") {
        includeDefault()
        include(Maps, CharSequences, ArraysOfUnsigned)
    } builder {
        doc { "Returns the first ${f.element} having the largest value according to the provided [comparator] or `null` if there are no ${f.element.pluralize()}." }
        returns("T?")
        body {
            """
        val iterator = iterator()
        if (!iterator.hasNext()) return null

        var max = iterator.next()
        while (iterator.hasNext()) {
            val e = iterator.next()
            if (comparator.compare(max, e) < 0) max = e
        }
        return max
        """
        }
        body(CharSequences, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned) {
            """
            if (isEmpty()) return null

            var max = this[0]
            for (i in 1..lastIndex) {
                val e = this[i]
                if (comparator.compare(max, e) < 0) max = e
            }
            return max
            """
        }
        specialFor(Maps) {
            inlineOnly()
            body { "return entries.maxWith(comparator)" }
        }
    }

    val f_foldIndexed = fn("foldIndexed(initial: R, operation: (index: Int, acc: R, T) -> R)") {
        includeDefault()
        include(CharSequences)
        include(ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc {
            """
            Accumulates value starting with [initial] value and applying [operation] from left to right
            to current accumulator value and each ${f.element} with its index in the original ${f.collection}.
            @param [operation] function that takes the index of ${f.element.prefixWithArticle()}, current accumulator value
            and the ${f.element} itself, and calculates the next accumulator value.
            """
        }
        typeParam("R")
        returns("R")
        body {
            fun checkOverflow(value: String) = if (f == Sequences || f == Iterables) "checkIndexOverflow($value)" else value
            """
            var index = 0
            var accumulator = initial
            for (element in this) accumulator = operation(${checkOverflow("index++")}, accumulator, element)
            return accumulator
            """
        }
    }

    val f_foldRightIndexed = fn("foldRightIndexed(initial: R, operation: (index: Int, T, acc: R) -> R)") {
        include(CharSequences, Lists, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc {
            """
            Accumulates value starting with [initial] value and applying [operation] from right to left
            to each ${f.element} with its index in the original ${f.collection} and current accumulator value.
            @param [operation] function that takes the index of ${f.element.prefixWithArticle()}, the ${f.element} itself
            and current accumulator value, and calculates the next accumulator value.
            """
        }
        typeParam("R")
        returns("R")
        body {
            """
            var index = lastIndex
            var accumulator = initial
            while (index >= 0) {
                accumulator = operation(index, get(index), accumulator)
                --index
            }
            return accumulator
            """
        }
        body(Lists) {
            """
            var accumulator = initial
            if (!isEmpty()) {
                val iterator = listIterator(size)
                while (iterator.hasPrevious()) {
                    val index = iterator.previousIndex()
                    accumulator = operation(index, iterator.previous(), accumulator)
                }
            }
            return accumulator
            """
        }
    }

    val f_fold = fn("fold(initial: R, operation: (acc: R, T) -> R)") {
        includeDefault()
        include(CharSequences)
        include(ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { "Accumulates value starting with [initial] value and applying [operation] from left to right to current accumulator value and each ${f.element}." }
        typeParam("R")
        returns("R")
        body {
            """
            var accumulator = initial
            for (element in this) accumulator = operation(accumulator, element)
            return accumulator
            """
        }
    }

    val f_foldRight = fn("foldRight(initial: R, operation: (T, acc: R) -> R)") {
        include(CharSequences, Lists, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { "Accumulates value starting with [initial] value and applying [operation] from right to left to each ${f.element} and current accumulator value." }
        typeParam("R")
        returns("R")
        body {
            """
            var index = lastIndex
            var accumulator = initial
            while (index >= 0) {
                accumulator = operation(get(index--), accumulator)
            }
            return accumulator
            """
        }
        body(Lists) {
            """
            var accumulator = initial
            if (!isEmpty()) {
                val iterator = listIterator(size)
                while (iterator.hasPrevious()) {
                    accumulator = operation(iterator.previous(), accumulator)
                }
            }
            return accumulator
            """
        }
    }

    val f_reduceIndexed = fn("reduceIndexed(operation: (index: Int, acc: T, T) -> T)") {
        include(ArraysOfPrimitives, ArraysOfUnsigned, CharSequences)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc {
            """
            Accumulates value starting with the first ${f.element} and applying [operation] from left to right
            to current accumulator value and each ${f.element} with its index in the original ${f.collection}.
            @param [operation] function that takes the index of ${f.element.prefixWithArticle()}, current accumulator value
            and the ${f.element} itself and calculates the next accumulator value.
            """
        }
        returns("T")
        body {
            """
            if (isEmpty())
                throw UnsupportedOperationException("Empty ${f.doc.collection} can't be reduced.")

            var accumulator = this[0]
            for (index in 1..lastIndex) {
                accumulator = operation(index, accumulator, this[index])
            }
            return accumulator
            """
        }
    }

    val f_reduceIndexedSuper = fn("reduceIndexed(operation: (index: Int, acc: S, T) -> S)") {
        include(ArraysOfObjects, Iterables, Sequences)
    } builder {
        inline()

        doc {
            """
            Accumulates value starting with the first ${f.element} and applying [operation] from left to right
            to current accumulator value and each ${f.element} with its index in the original ${f.collection}.
            @param [operation] function that takes the index of ${f.element.prefixWithArticle()}, current accumulator value
            and the ${f.element} itself and calculates the next accumulator value.
            """
        }
        typeParam("S")
        typeParam("T : S")
        returns("S")
        body {
            fun checkOverflow(value: String) = if (f == Sequences || f == Iterables) "checkIndexOverflow($value)" else value
            """
            val iterator = this.iterator()
            if (!iterator.hasNext()) throw UnsupportedOperationException("Empty ${f.doc.collection} can't be reduced.")

            var index = 1
            var accumulator: S = iterator.next()
            while (iterator.hasNext()) {
                accumulator = operation(${checkOverflow("index++")}, accumulator, iterator.next())
            }
            return accumulator
            """
        }
        body(ArraysOfObjects) {
            """
            if (isEmpty())
                throw UnsupportedOperationException("Empty ${f.doc.collection} can't be reduced.")

            var accumulator: S = this[0]
            for (index in 1..lastIndex) {
                accumulator = operation(index, accumulator, this[index])
            }
            return accumulator
            """
        }
    }

    val f_reduceRightIndexed = fn("reduceRightIndexed(operation: (index: Int, T, acc: T) -> T)") {
        include(CharSequences, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc {
            """
            Accumulates value starting with last ${f.element} and applying [operation] from right to left
            to each ${f.element} with its index in the original ${f.collection} and current accumulator value.
            @param [operation] function that takes the index of ${f.element.prefixWithArticle()}, the ${f.element} itself
            and current accumulator value, and calculates the next accumulator value.
            """
        }
        returns("T")
        body {
            """
            var index = lastIndex
            if (index < 0) throw UnsupportedOperationException("Empty ${f.doc.collection} can't be reduced.")

            var accumulator = get(index--)
            while (index >= 0) {
                accumulator = operation(index, get(index), accumulator)
                --index
            }

            return accumulator
            """
        }
    }

    val f_reduceRightIndexedSuper = fn("reduceRightIndexed(operation: (index: Int, T, acc: S) -> S)") {
        include(Lists, ArraysOfObjects)
    } builder {
        inline()

        doc {
            """
            Accumulates value starting with last ${f.element} and applying [operation] from right to left
            to each ${f.element} with its index in the original ${f.collection} and current accumulator value.
            @param [operation] function that takes the index of ${f.element.prefixWithArticle()}, the ${f.element} itself
            and current accumulator value, and calculates the next accumulator value.
            """
        }
        typeParam("S")
        typeParam("T : S")
        returns("S")
        body {
            """
            var index = lastIndex
            if (index < 0) throw UnsupportedOperationException("Empty ${f.doc.collection} can't be reduced.")

            var accumulator: S = get(index--)
            while (index >= 0) {
                accumulator = operation(index, get(index), accumulator)
                --index
            }

            return accumulator
            """
        }
        body(Lists) {
            """
            val iterator = listIterator(size)
            if (!iterator.hasPrevious())
                throw UnsupportedOperationException("Empty list can't be reduced.")

            var accumulator: S = iterator.previous()
            while (iterator.hasPrevious()) {
                val index = iterator.previousIndex()
                accumulator = operation(index, iterator.previous(), accumulator)
            }

            return accumulator
            """
        }
    }

    val f_reduce = fn("reduce(operation: (acc: T, T) -> T)") {
        include(ArraysOfPrimitives, ArraysOfUnsigned, CharSequences)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { "Accumulates value starting with the first ${f.element} and applying [operation] from left to right to current accumulator value and each ${f.element}." }
        returns("T")
        body {
            """
            if (isEmpty())
                throw UnsupportedOperationException("Empty ${f.doc.collection} can't be reduced.")

            var accumulator = this[0]
            for (index in 1..lastIndex) {
                accumulator = operation(accumulator, this[index])
            }
            return accumulator
            """
        }
    }

    val f_reduceSuper = fn("reduce(operation: (acc: S, T) -> S)") {
        include(ArraysOfObjects, Iterables, Sequences)
    } builder {
        inline()

        doc { "Accumulates value starting with the first ${f.element} and applying [operation] from left to right to current accumulator value and each ${f.element}." }
        typeParam("S")
        typeParam("T : S")
        returns("S")
        body {
            """
            val iterator = this.iterator()
            if (!iterator.hasNext()) throw UnsupportedOperationException("Empty ${f.doc.collection} can't be reduced.")

            var accumulator: S = iterator.next()
            while (iterator.hasNext()) {
                accumulator = operation(accumulator, iterator.next())
            }
            return accumulator
            """
        }
        body(ArraysOfObjects) {
            """
            if (isEmpty())
                throw UnsupportedOperationException("Empty ${f.doc.collection} can't be reduced.")

            var accumulator: S = this[0]
            for (index in 1..lastIndex) {
                accumulator = operation(accumulator, this[index])
            }
            return accumulator
            """
        }
    }

    val f_reduceOrNull = fn("reduceOrNull(operation: (acc: T, T) -> T)") {
        include(ArraysOfPrimitives, ArraysOfUnsigned, CharSequences)
    } builder {
        since("1.3")
        annotation("@ExperimentalStdlibApi")
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { "Accumulates value starting with the first ${f.element} and applying [operation] from left to right to current accumulator value and each ${f.element}. Returns null if the ${f.collection} is empty." }
        returns("T?")
        body {
            """
            if (isEmpty())
                return null

            var accumulator = this[0]
            for (index in 1..lastIndex) {
                accumulator = operation(accumulator, this[index])
            }
            return accumulator
            """
        }
    }

    val f_reduceOrNullSuper = fn("reduceOrNull(operation: (acc: S, T) -> S)") {
        include(ArraysOfObjects, Iterables, Sequences)
    } builder {
        since("1.3")
        annotation("@ExperimentalStdlibApi")
        inline()

        doc { "Accumulates value starting with the first ${f.element} and applying [operation] from left to right to current accumulator value and each ${f.element}. Returns null if the ${f.collection} is empty." }
        typeParam("S")
        typeParam("T : S")
        returns("S?")
        body {
            """
            val iterator = this.iterator()
            if (!iterator.hasNext()) return null

            var accumulator: S = iterator.next()
            while (iterator.hasNext()) {
                accumulator = operation(accumulator, iterator.next())
            }
            return accumulator
            """
        }
        body(ArraysOfObjects) {
            """
            if (isEmpty())
                return null

            var accumulator: S = this[0]
            for (index in 1..lastIndex) {
                accumulator = operation(accumulator, this[index])
            }
            return accumulator
            """
        }
    }

    val f_reduceRight = fn("reduceRight(operation: (T, acc: T) -> T)") {
        include(CharSequences, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { "Accumulates value starting with last ${f.element} and applying [operation] from right to left to each ${f.element} and current accumulator value." }
        returns("T")
        body {
            """
            var index = lastIndex
            if (index < 0) throw UnsupportedOperationException("Empty ${f.doc.collection} can't be reduced.")

            var accumulator = get(index--)
            while (index >= 0) {
                accumulator = operation(get(index--), accumulator)
            }

            return accumulator
            """
        }
    }

    val f_reduceRightSuper = fn("reduceRight(operation: (T, acc: S) -> S)") {
        include(Lists, ArraysOfObjects)
    } builder {
        inline()
        doc { "Accumulates value starting with last ${f.element} and applying [operation] from right to left to each ${f.element} and current accumulator value." }
        typeParam("S")
        typeParam("T : S")
        returns("S")
        body {
            """
            var index = lastIndex
            if (index < 0) throw UnsupportedOperationException("Empty ${f.doc.collection} can't be reduced.")

            var accumulator: S = get(index--)
            while (index >= 0) {
                accumulator = operation(get(index--), accumulator)
            }

            return accumulator
            """
        }
        body(Lists) {
            """
            val iterator = listIterator(size)
            if (!iterator.hasPrevious())
                throw UnsupportedOperationException("Empty list can't be reduced.")

            var accumulator: S = iterator.previous()
            while (iterator.hasPrevious()) {
                accumulator = operation(iterator.previous(), accumulator)
            }

            return accumulator
            """
        }
    }

    val f_onEach = fn("onEach(action: (T) -> Unit)") {
        include(Iterables, Maps, CharSequences, Sequences)
    } builder {
        since("1.1")

        specialFor(Iterables, Maps, CharSequences) {
            inline()
            doc { "Performs the given [action] on each ${f.element} and returns the ${f.collection} itself afterwards." }
            val collectionType = when (f) {
                Maps -> "M"
                CharSequences -> "S"
                else -> "C"
            }
            receiver(collectionType)
            returns(collectionType)
            typeParam("$collectionType : SELF")

            body { "return apply { for (element in this) action(element) }" }
        }

        specialFor(Sequences) {
            returns("SELF")
            doc { "Returns a sequence which performs the given [action] on each ${f.element} of the original sequence as they pass through it." }
            sequenceClassification(intermediate, stateless)
            body {
                """
                return map {
                    action(it)
                    it
                }
                """
            }
        }
    }

    val f_forEach = fn("forEach(action: (T) -> Unit)") {
        includeDefault()
        include(Maps, CharSequences, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { "Performs the given [action] on each ${f.element}." }
        specialFor(Iterables, Maps) { annotation("@kotlin.internal.HidesMembers") }
        returns("Unit")
        body {
            """
            for (element in this) action(element)
            """
        }
    }

    val f_forEachIndexed = fn("forEachIndexed(action: (index: Int, T) -> Unit)") {
        includeDefault()
        include(CharSequences, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc {
            """
            Performs the given [action] on each ${f.element}, providing sequential index with the ${f.element}.
            @param [action] function that takes the index of ${f.element.prefixWithArticle()} and the ${f.element} itself
            and performs the desired action on the ${f.element}.
            """ }
        returns("Unit")
        body {
            fun checkOverflow(value: String) = if (f == Sequences || f == Iterables) "checkIndexOverflow($value)" else value
            """
            var index = 0
            for (item in this) action(${checkOverflow("index++")}, item)
            """
        }
    }
}

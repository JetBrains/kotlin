package templates

import templates.Family.*

fun aggregates(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("all(predicate: (T) -> Boolean)") {
        inline(true)
        doc { f -> "Returns `true` if all ${f.element.pluralize()} match the given [predicate]." }
        returns("Boolean")
        body {
            """
            for (element in this) if (!predicate(element)) return false
            return true
            """
        }
        include(Maps, CharSequences)
    }

    templates add f("none(predicate: (T) -> Boolean)") {
        inline(true)

        doc { f -> "Returns `true` if no ${f.element.pluralize()} match the given [predicate]." }
        returns("Boolean")
        body {
            """
            for (element in this) if (predicate(element)) return false
            return true
            """
        }
        include(Maps, CharSequences)
    }

    templates add f("none()") {
        doc { f -> "Returns `true` if the ${f.collection} has no ${f.element.pluralize()}." }
        returns("Boolean")
        body {
            """
            for (element in this) return false
            return true
            """
        }
        include(Maps, CharSequences)
    }

    templates add f("any(predicate: (T) -> Boolean)") {
        inline(true)

        doc { f -> "Returns `true` if at least one ${f.element} matches the given [predicate]." }
        returns("Boolean")
        body {
            """
            for (element in this) if (predicate(element)) return true
            return false
            """
        }
        include(Maps, CharSequences)
    }

    templates add f("any()") {
        doc { f -> "Returns `true` if ${f.collection} has at least one ${f.element}." }
        returns("Boolean")
        body {
            """
            for (element in this) return true
            return false
            """
        }
        include(Maps, CharSequences)
    }

    templates add f("count(predicate: (T) -> Boolean)") {
        inline(true)

        doc { f -> "Returns the number of ${f.element.pluralize()} matching the given [predicate]." }
        returns("Int")
        body {
            """
            var count = 0
            for (element in this) if (predicate(element)) count++
            return count
            """
        }
        include(Maps, CharSequences)
    }

    templates add f("count()") {
        doc { f -> "Returns the number of ${f.element.pluralize()} in this ${f.collection}." }
        returns("Int")
        body {
            """
            var count = 0
            for (element in this) count++
            return count
            """
        }
        doc(CharSequences) { "Returns the length of this char sequence."}
        body(CharSequences) {
            "return length"
        }
        body(Maps, Collections, ArraysOfObjects, ArraysOfPrimitives) {
            "return size"
        }
    }

    templates add f("sumBy(selector: (T) -> Int)") {
        inline(true)
        include(CharSequences)
        doc { f -> "Returns the sum of all values produced by [selector] function applied to each ${f.element} in the ${f.collection}." }
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
    }

    templates add f("sumByDouble(selector: (T) -> Double)") {
        inline(true)
        include(CharSequences)
        doc { f -> "Returns the sum of all values produced by [selector] function applied to each ${f.element} in the ${f.collection}." }
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

    templates add f("min()") {
        doc { f -> "Returns the smallest ${f.element} or `null` if there are no ${f.element.pluralize()}." }
        returns("T?")
        exclude(PrimitiveType.Boolean)
        typeParam("T : Comparable<T>")
        body {
            """
            val iterator = iterator()
            if (!iterator.hasNext()) return null

            var min = iterator.next()
            while (iterator.hasNext()) {
                val e = iterator.next()
                if (min > e) min = e
            }
            return min
            """
        }
        body(CharSequences, ArraysOfObjects, ArraysOfPrimitives) {
            """
            if (isEmpty()) return null
            var min = this[0]
            for (i in 1..lastIndex) {
                val e = this[i]
                if (min > e) min = e
            }
            return min
            """
        }
    }

    templates add f("minBy(selector: (T) -> R)") {
        inline(true)

        doc { f -> "Returns the first ${f.element} yielding the smallest value of the given function or `null` if there are no ${f.element.pluralize()}." }
        typeParam("R : Comparable<R>")
        returns("T?")
        body {
            """
            val iterator = iterator()
            if (!iterator.hasNext()) return null

            var minElem = iterator.next()
            var minValue = selector(minElem)
            while (iterator.hasNext()) {
                val e = iterator.next()
                val v = selector(e)
                if (minValue > v) {
                    minElem = e
                    minValue = v
                }
            }
            return minElem
            """
        }
        body(CharSequences, ArraysOfObjects, ArraysOfPrimitives) {
            """
            if (isEmpty()) return null

            var minElem = this[0]
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
        body(Maps) { "return entries.minBy(selector)" }
    }

    templates add f("minWith(comparator: Comparator<in T>)") {
        doc { f -> "Returns the first ${f.element} having the smallest value according to the provided [comparator] or `null` if there are no ${f.element.pluralize()}." }
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
        body(CharSequences, ArraysOfObjects, ArraysOfPrimitives) {
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

    templates add f("max()") {
        doc { f -> "Returns the largest ${f.element} or `null` if there are no ${f.element.pluralize()}." }
        returns("T?")
        exclude(PrimitiveType.Boolean)
        typeParam("T : Comparable<T>")
        body {
            """
            val iterator = iterator()
            if (!iterator.hasNext()) return null

            var max = iterator.next()
            while (iterator.hasNext()) {
                val e = iterator.next()
                if (max < e) max = e
            }
            return max
            """
        }

        body(CharSequences, ArraysOfObjects, ArraysOfPrimitives) {
            """
            if (isEmpty()) return null

            var max = this[0]
            for (i in 1..lastIndex) {
                val e = this[i]
                if (max < e) max = e
            }
            return max
            """
        }
    }

    templates add f("maxBy(selector: (T) -> R)") {
        inline(true)

        doc { f -> "Returns the first ${f.element} yielding the largest value of the given function or `null` if there are no ${f.element.pluralize()}." }
        typeParam("R : Comparable<R>")
        returns("T?")
        body {
            """
            val iterator = iterator()
            if (!iterator.hasNext()) return null

            var maxElem = iterator.next()
            var maxValue = selector(maxElem)
            while (iterator.hasNext()) {
                val e = iterator.next()
                val v = selector(e)
                if (maxValue < v) {
                    maxElem = e
                    maxValue = v
                }
            }
            return maxElem
            """
        }
        body(CharSequences, ArraysOfObjects, ArraysOfPrimitives) {
            """
            if (isEmpty()) return null

            var maxElem = this[0]
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
        body(Maps) { "return entries.maxBy(selector)" }
    }

    templates add f("maxWith(comparator: Comparator<in T>)") {
        doc { f -> "Returns the first ${f.element} having the largest value according to the provided [comparator] or `null` if there are no ${f.element.pluralize()}." }
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
        body(CharSequences, ArraysOfObjects, ArraysOfPrimitives) {
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
        body(Maps) { "return entries.maxWith(comparator)" }
    }

    templates add f("foldIndexed(initial: R, operation: (Int, R, T) -> R)") {
        inline(true)

        include(CharSequences)
        doc { f ->
            """
            Accumulates value starting with [initial] value and applying [operation] from left to right
            to current accumulator value and each ${f.element} with its index in the original ${f.collection}.
            """
        }
        typeParam("R")
        returns("R")
        body {
            """
            var index = 0
            var accumulator = initial
            for (element in this) accumulator = operation(index++, accumulator, element)
            return accumulator
            """
        }
    }

    templates add f("foldRightIndexed(initial: R, operation: (Int, T, R) -> R)") {
        inline(true)

        only(CharSequences, Lists, ArraysOfObjects, ArraysOfPrimitives)
        doc { f ->
            """
            Accumulates value starting with [initial] value and applying [operation] from right to left
            to each ${f.element} with its index in the original ${f.collection} and current accumulator value.
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
    }

    templates add f("fold(initial: R, operation: (R, T) -> R)") {
        inline(true)

        include(CharSequences)
        doc { f -> "Accumulates value starting with [initial] value and applying [operation] from left to right to current accumulator value and each ${f.element}." }
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

    templates add f("foldRight(initial: R, operation: (T, R) -> R)") {
        inline(true)

        only(CharSequences, Lists, ArraysOfObjects, ArraysOfPrimitives)
        doc { f -> "Accumulates value starting with [initial] value and applying [operation] from right to left to each ${f.element} and current accumulator value." }
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
    }

    templates add f("reduceIndexed(operation: (Int, T, T) -> T)") {
        inline(true)
        include(CharSequences)
        exclude(ArraysOfObjects, Iterables, Sequences)

        doc { f ->
            """
            Accumulates value starting with the first ${f.element} and applying [operation] from left to right
            to current accumulator value and each ${f.element} with its index in the original ${f.collection}.
            """
        }
        returns("T")
        body {
            """
            val iterator = this.iterator()
            if (!iterator.hasNext()) throw UnsupportedOperationException("Empty iterable can't be reduced.")

            var index = 1
            var accumulator = iterator.next()
            while (iterator.hasNext()) {
                accumulator = operation(index++, accumulator, iterator.next())
            }
            return accumulator
            """
        }
    }

    templates add f("reduceIndexed(operation: (Int, S, T) -> S)") {
        inline(true)
        only(ArraysOfObjects, Iterables, Sequences)

        doc { f ->
            """
            Accumulates value starting with the first ${f.element} and applying [operation] from left to right
            to current accumulator value and each ${f.element} with its index in the original ${f.collection}.
            """
        }
        typeParam("S")
        typeParam("T: S")
        returns("S")
        body {
            """
            val iterator = this.iterator()
            if (!iterator.hasNext()) throw UnsupportedOperationException("Empty iterable can't be reduced.")

            var index = 1
            var accumulator: S = iterator.next()
            while (iterator.hasNext()) {
                accumulator = operation(index++, accumulator, iterator.next())
            }
            return accumulator
            """
        }
    }

    templates add f("reduceRightIndexed(operation: (Int, T, T) -> T)") {
        inline(true)

        only(CharSequences, ArraysOfPrimitives)
        doc { f ->
            """
            Accumulates value starting with last ${f.element} and applying [operation] from right to left
            to each ${f.element} with its index in the original ${f.collection} and current accumulator value.
            """
        }
        returns("T")
        body {
            """
            var index = lastIndex
            if (index < 0) throw UnsupportedOperationException("Empty iterable can't be reduced.")

            var accumulator = get(index--)
            while (index >= 0) {
                accumulator = operation(index, get(index), accumulator)
                --index
            }

            return accumulator
            """
        }
    }

    templates add f("reduceRightIndexed(operation: (Int, T, S) -> S)") {
        inline(true)

        only(Lists, ArraysOfObjects)
        doc { f ->
            """
            Accumulates value starting with last ${f.element} and applying [operation] from right to left
            to each ${f.element} with its index in the original ${f.collection} and current accumulator value.
            """
        }
        typeParam("S")
        typeParam("T: S")
        returns("S")
        body {
            """
            var index = lastIndex
            if (index < 0) throw UnsupportedOperationException("Empty iterable can't be reduced.")

            var accumulator: S = get(index--)
            while (index >= 0) {
                accumulator = operation(index, get(index), accumulator)
                --index
            }

            return accumulator
            """
        }
    }

    templates add f("reduce(operation: (T, T) -> T)") {
        inline(true)
        include(CharSequences)
        exclude(ArraysOfObjects, Iterables, Sequences)

        doc { f -> "Accumulates value starting with the first ${f.element} and applying [operation] from left to right to current accumulator value and each ${f.element}." }
        returns("T")
        body {
            """
            val iterator = this.iterator()
            if (!iterator.hasNext()) throw UnsupportedOperationException("Empty iterable can't be reduced.")

            var accumulator = iterator.next()
            while (iterator.hasNext()) {
                accumulator = operation(accumulator, iterator.next())
            }
            return accumulator
            """
        }
    }

    templates add f("reduce(operation: (S, T) -> S)") {
        inline(true)
        only(ArraysOfObjects, Iterables, Sequences)

        doc { f -> "Accumulates value starting with the first ${f.element} and applying [operation] from left to right to current accumulator value and each ${f.element}." }
        typeParam("S")
        typeParam("T: S")
        returns("S")
        body {
            """
            val iterator = this.iterator()
            if (!iterator.hasNext()) throw UnsupportedOperationException("Empty iterable can't be reduced.")

            var accumulator: S = iterator.next()
            while (iterator.hasNext()) {
                accumulator = operation(accumulator, iterator.next())
            }
            return accumulator
            """
        }
    }

    templates add f("reduceRight(operation: (T, T) -> T)") {
        inline(true)

        only(CharSequences, ArraysOfPrimitives)
        doc { f -> "Accumulates value starting with last ${f.element} and applying [operation] from right to left to each ${f.element} and current accumulator value." }
        returns("T")
        body {
            """
            var index = lastIndex
            if (index < 0) throw UnsupportedOperationException("Empty iterable can't be reduced.")

            var accumulator = get(index--)
            while (index >= 0) {
                accumulator = operation(get(index--), accumulator)
            }

            return accumulator
            """
        }
    }

    templates add f("reduceRight(operation: (T, S) -> S)") {
        inline(true)

        only(Lists, ArraysOfObjects)
        doc { f -> "Accumulates value starting with last ${f.element} and applying [operation] from right to left to each ${f.element} and current accumulator value." }
        typeParam("S")
        typeParam("T: S")
        returns("S")
        body {
            """
            var index = lastIndex
            if (index < 0) throw UnsupportedOperationException("Empty iterable can't be reduced.")

            var accumulator: S = get(index--)
            while (index >= 0) {
                accumulator = operation(get(index--), accumulator)
            }

            return accumulator
            """
        }
    }

    templates add f("forEach(action: (T) -> Unit)") {
        inline(true)

        doc { f -> "Performs the given [action] on each ${f.element}." }
        annotations(Iterables, Maps) { "@kotlin.internal.HidesMembers" }
        returns("Unit")
        body {
            """
            for (element in this) action(element)
            """
        }
        include(Maps, CharSequences)
    }

    templates add f("forEachIndexed(action: (Int, T) -> Unit)") {
        inline(true)
        include(CharSequences)
        doc { f -> "Performs the given [action] on each ${f.element}, providing sequential index with the ${f.element}." }
        returns("Unit")
        body {
            """
            var index = 0
            for (item in this) action(index++, item)
            """
        }
    }

    return templates
}
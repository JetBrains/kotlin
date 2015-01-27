package templates

import templates.Family.*

fun aggregates(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("all(predicate: (T) -> Boolean)") {
        inline(true)
        doc { "Returns *true* if all elements match the given *predicate*" }
        returns("Boolean")
        body {
            """
            for (element in this) if (!predicate(element)) return false
            return true
            """
        }
        include(Maps)
    }

    templates add f("none(predicate: (T) -> Boolean)") {
        inline(true)

        doc { "Returns *true* if no elements match the given *predicate*" }
        returns("Boolean")
        body {
            """
            for (element in this) if (predicate(element)) return false
            return true
            """
        }
        include(Maps)
    }

    templates add f("none()") {
        doc { "Returns *true* if collection has no elements" }
        returns("Boolean")
        body {
            """
            for (element in this) return false
            return true
            """
        }
        include(Maps)
    }

    templates add f("any(predicate: (T) -> Boolean)") {
        inline(true)

        doc { "Returns *true* if any element matches the given *predicate*" }
        returns("Boolean")
        body {
            """
            for (element in this) if (predicate(element)) return true
            return false
            """
        }
        include(Maps)
    }

    templates add f("any()") {
        doc { "Returns *true* if collection has at least one element" }
        returns("Boolean")
        body {
            """
            for (element in this) return true
            return false
            """
        }
        include(Maps)
    }

    templates add f("count(predicate: (T) -> Boolean)") {
        inline(true)

        doc { "Returns the number of elements matching the given *predicate*" }
        returns("Int")
        body {
            """
            var count = 0
            for (element in this) if (predicate(element)) count++
            return count
            """
        }
        include(Maps)
    }

    templates add f("count()") {
        doc { "Returns the number of elements" }
        returns("Int")
        body {
            """
            var count = 0
            for (element in this) count++
            return count
            """
        }
        body(Strings) {
            "return length()"
        }
        body(Maps, Collections, ArraysOfObjects, ArraysOfPrimitives) {
            "return size()"
        }
    }

    templates add f("sumBy(transform: (T) -> Int)") {
        inline(true)
        doc { "Returns the sum of all values produced by `transform` function from elements in the collection" }
        returns("Int")
        body {
            """
            var sum: Int = 0
            for (element in this) {
                sum += transform(element)
            }
            return sum
            """
        }
    }

    templates add f("sumByDouble(transform: (T) -> Double)") {
        inline(true)
        doc { "Returns the sum of all values produced by `transform` function from elements in the collection" }
        returns("Double")
        body {
            """
            var sum: Double = 0.0
            for (element in this) {
                sum += transform(element)
            }
            return sum
            """
        }
    }

    templates add f("min()") {
        doc { "Returns the smallest element or null if there are no elements" }
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
        body(ArraysOfObjects, ArraysOfPrimitives) {
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

    templates add f("minBy(f: (T) -> R)") {
        inline(true)

        doc { "Returns the first element yielding the smallest value of the given function or null if there are no elements" }
        typeParam("R : Comparable<R>")
        typeParam("T : Any")
        returns("T?")
        body {
            """
            val iterator = iterator()
            if (!iterator.hasNext()) return null

            var minElem = iterator.next()
            var minValue = f(minElem)
            while (iterator.hasNext()) {
                val e = iterator.next()
                val v = f(e)
                if (minValue > v) {
                    minElem = e
                    minValue = v
                }
            }
            return minElem
            """
        }
        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            if (size() == 0) return null

            var minElem = this[0]
            var minValue = f(minElem)
            for (i in 1..lastIndex) {
                val e = this[i]
                val v = f(e)
                if (minValue > v) {
                    minElem = e
                    minValue = v
                }
            }
            return minElem
            """
        }
    }

    templates add f("minBy(f: (T) -> R)") {
        inline(true)

        only(Maps)
        doc { "Returns the first element yielding the smallest value of the given function or null if there are no elements" }
        typeParam("R : Comparable<R>")
        returns("T?")
        body {
            """
            val iterator = iterator()
            if (!iterator.hasNext()) return null

            var minElem = iterator.next()
            var minValue = f(minElem)
            while (iterator.hasNext()) {
                val e = iterator.next()
                val v = f(e)
                if (minValue > v) {
                    minElem = e
                    minValue = v
                }
            }
            return minElem
            """
        }
    }

    templates add f("max()") {
        doc { "Returns the largest element or null if there are no elements" }
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

        body(ArraysOfObjects, ArraysOfPrimitives) {
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

    templates add f("maxBy(f: (T) -> R)") {
        inline(true)

        doc { "Returns the first element yielding the largest value of the given function or null if there are no elements" }
        typeParam("R : Comparable<R>")
        typeParam("T : Any")
        returns("T?")
        body {
            """
            val iterator = iterator()
            if (!iterator.hasNext()) return null

            var maxElem = iterator.next()
            var maxValue = f(maxElem)
            while (iterator.hasNext()) {
                val e = iterator.next()
                val v = f(e)
                if (maxValue < v) {
                    maxElem = e
                    maxValue = v
                }
            }
            return maxElem
            """
        }
        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            if (isEmpty()) return null

            var maxElem = this[0]
            var maxValue = f(maxElem)
            for (i in 1..lastIndex) {
                val e = this[i]
                val v = f(e)
                if (maxValue < v) {
                    maxElem = e
                    maxValue = v
                }
            }
            return maxElem
            """
        }
    }

    templates add f("maxBy(f: (T) -> R)") {
        inline(true)

        only(Maps)
        doc { "Returns the first element yielding the largest value of the given function or null if there are no elements" }
        typeParam("R : Comparable<R>")
        returns("T?")
        body {
            """
            val iterator = iterator()
            if (!iterator.hasNext()) return null

            var maxElem = iterator.next()
            var maxValue = f(maxElem)
            while (iterator.hasNext()) {
                val e = iterator.next()
                val v = f(e)
                if (maxValue < v) {
                    maxElem = e
                    maxValue = v
                }
            }
            return maxElem
            """
        }
    }

    templates add f("fold(initial: R, operation: (R, T) -> R)") {
        inline(true)

        doc { "Accumulates value starting with *initial* value and applying *operation* from left to right to current accumulator value and each element" }
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

        only(Strings, Lists, ArraysOfObjects, ArraysOfPrimitives)
        doc { "Accumulates value starting with *initial* value and applying *operation* from right to left to each element and current accumulator value" }
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


    templates add f("reduce(operation: (T, T) -> T)") {
        inline(true)

        doc { "Accumulates value starting with the first element and applying *operation* from left to right to current accumulator value and each element" }
        returns("T")
        body {
            """
            val iterator = this.iterator()
            if (!iterator.hasNext()) throw UnsupportedOperationException("Empty iterable can't be reduced")

            var accumulator = iterator.next()
            while (iterator.hasNext()) {
                accumulator = operation(accumulator, iterator.next())
            }
            return accumulator
            """
        }
    }

    templates add f("reduceRight(operation: (T, T) -> T)") {
        inline(true)

        only(Strings, Lists, ArraysOfObjects, ArraysOfPrimitives)
        doc { "Accumulates value starting with last element and applying *operation* from right to left to each element and current accumulator value" }
        returns("T")
        body {
            """
            var index = lastIndex
            if (index < 0) throw UnsupportedOperationException("Empty iterable can't be reduced")

            var accumulator = get(index--)
            while (index >= 0) {
                accumulator = operation(get(index--), accumulator)
            }

            return accumulator
            """
        }
    }


    templates add f("forEach(operation: (T) -> Unit)") {
        inline(true)

        doc { "Performs the given *operation* on each element" }
        returns("Unit")
        body {
            """
            for (element in this) operation(element)
            """
        }
        include(Maps)
    }

    templates add f("forEachIndexed(operation: (Int, T) -> Unit)") {
        inline(true)
        doc { "Performs the given *operation* on each element, providing sequential index with the element" }
        returns("Unit")
        body {
            """
            var index = 0
            for (item in this) operation(index++, item)
            """
        }
    }

    return templates
}
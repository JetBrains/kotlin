package templates

import templates.Family.*

fun collections() {
    f("all(predicate: (T) -> Boolean)") {
        doc = "Returns *true* if all elements match the given *predicate*"
        returns("Boolean")

        body {
            """
                for (element in this) if (!predicate(element)) return false
                return true
            """
        }
    }

    f("any(predicate: (T) -> Boolean)") {
        doc = "Returns *true* if any elements match the given *predicate*"
        returns("Boolean")

        body {
            """
                for (element in this) if (predicate(element)) return true
                return false
            """
        }
    }

    f("count(predicate: (T) -> Boolean)") {
        doc = "Returns the number of elements which match the given *predicate*"
        returns("Int")
        body {
           """
               var count = 0
               for (element in this) if (predicate(element)) count++
               return count
           """
        }
    }


    f("find(predicate: (T) -> Boolean)") {
        doc = "Returns the first element which matches the given *predicate* or *null* if none matched"
        typeParam("T:Any")
        returns("T?")

        body {
            """
                for (element in this) if (predicate(element)) return element
                return null
            """
        }
    }

    f("filter(predicate: (T) -> Boolean)") {
        doc = "Returns a list containing all elements which match the given *predicate*"
        returns("List<T>")

        body {
            "return filterTo(ArrayList<T>(), predicate)"
        }

        Iterators.returns("Iterator<T")
        Iterators.body {
            "return FilterIterator<T>(this, predicate)"
        }
    }

    f("filterTo(result: C, predicate: (T) -> Boolean)") {
        doc = "Filters all elements which match the given predicate into the given list"
        typeParam("C: MutableCollection<in T>")
        returns("C")

        body {
            """
                for (element in this) if (predicate(element)) result.add(element)
                return result
            """
        }
    }

    f("filterNot(predicate: (T) -> Boolean)") {
        doc = "Returns a list containing all elements which do not match the given *predicate*"
        returns("List<T>")

        body {
            "return filterNotTo(ArrayList<T>(), predicate)"
        }
    }

    f("filterNotTo(result: C, predicate: (T) -> Boolean)") {
        doc = "Returns a list containing all elements which do not match the given *predicate*"
        typeParam("C: MutableCollection<in T>")
        returns("C")

        body {
            """
                for (element in this) if (!predicate(element)) result.add(element)
                return result
            """
        }
    }

    f("filterNotNull()") {
        absentFor(PrimitiveArrays) // Those are inherently non-nulls
        doc = "Returns a list containing all the non-*null* elements"
        typeParam("T:Any")
        toNullableT = true
        returns("List<T>")

        body {
            "return filterNotNullTo<T, ArrayList<T>>(ArrayList<T>())"
        }
    }

    f("filterNotNullTo(result: C)") {
        absentFor(PrimitiveArrays) // Those are inherently non-nulls
        doc = "Filters all non-*null* elements into the given list"
        typeParam("T:Any")
        toNullableT = true
        typeParam("C: MutableCollection<in T>")
        returns("C")

        body {
            """
                for (element in this) if (element != null) result.add(element)
                return result
            """
        }
    }

    f("partition(predicate: (T) -> Boolean)") {
        doc = "Partitions this collection into a pair of collections"
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
    }

    f("map(transform : (T) -> R)") {
        doc = "Returns a new List containing the results of applying the given *transform* function to each element in this collection"
        typeParam("R")
        returns("List<R>")

        body {
            "return mapTo(ArrayList<R>(), transform)"
        }
    }

    f("mapTo(result: C, transform : (T) -> R)") {
        doc = """
            Transforms each element of this collection with the given *transform* function and
            adds each return value to the given *results* collection
        """

        typeParam("R")
        typeParam("C: MutableCollection<in R>")
        returns("C")

        body {
            """
                for (item in this)
                    result.add(transform(item))
                return result
            """
        }
    }

    f("flatMap(transform: (T)-> Iterable<R>)") {
        doc = "Returns the result of transforming each element to one or more values which are concatenated together into a single list"
        typeParam("R")
        returns("List<R>")

        body {
            "return flatMapTo(ArrayList<R>(), transform)"
        }
    }


    f("flatMapTo(result: C, transform: (T) -> Iterable<R>)") {
        doc = "Returns the result of transforming each element to one or more values which are concatenated together into a single collection"
        typeParam("R")
        typeParam("C: MutableCollection<in R>")
        returns("C")

        body {
            """
                for (element in this) {
                    val list = transform(element)
                    for (r in list) result.add(r)
                }
                return result
            """
        }
    }

    f("forEach(operation: (T) -> Unit)") {
        doc = "Performs the given *operation* on each element"
        returns("Unit")
        body {
            """
                for (element in this) operation(element)
            """
        }
    }

    f("fold(initial: R, operation: (R, T) -> R)") {
        doc = "Folds all elements from from left to right with the *initial* value to perform the operation on sequential pairs of elements"
        typeParam("R")
        returns("R")

        body {
            """
                var answer = initial
                for (element in this) answer = operation(answer, element)
                return answer
            """
        }
    }

    f("foldRight(initial: R, operation: (T, R) -> R)") {
        doc = "Folds all elements from right to left with the *initial* value to perform the operation on sequential pairs of elements"
        typeParam("R")
        returns("R")

        absentFor(Iterators, Iterables, Collections)

        body {
            """
                var r = initial
                var index = size - 1

                while (index >= 0) {
                    r = operation(get(index--), r)
                }

                return r
            """
        }
    }

    f("reduce(operation: (T, T) -> T)") {
        doc = """
          Applies binary operation to all elements of iterable, going from left to right.
          Similar to fold function, but uses the first element as initial value
        """
        returns("T")

        body {
            """
                val iterator = this.iterator()
                if (!iterator.hasNext()) {
                    throw UnsupportedOperationException("Empty iterable can't be reduced")
                }

                var result: T = iterator.next() //compiler doesn't understand that result will initialized anyway
                while (iterator.hasNext()) {
                    result = operation(result, iterator.next())
                }

                return result
            """
        }
    }

    f("reduceRight(operation: (T, T) -> T)") {
        doc = """
          Applies binary operation to all elements of iterable, going from right to left.
          Similar to foldRight function, but uses the last element as initial value
        """
        returns("T")
        absentFor(Iterators, Iterables, Collections)

        body {
            """
                var index = size - 1
                if (index < 0) {
                    throw UnsupportedOperationException("Empty iterable can't be reduced")
                }

                var r = get(index--)
                while (index >= 0) {
                    r = operation(get(index--), r)
                }

                return r
            """
        }
    }

    f("groupBy(toKey: (T) -> K)") {
        doc = "Groups the elements in the collection into a new [[Map]] using the supplied *toKey* function to calculate the key to group the elements by"
        typeParam("K")
        returns("Map<K, List<T>>")

        body { "return groupByTo(HashMap<K, MutableList<T>>(), toKey)" }
    }

    f("groupByTo(result: MutableMap<K, MutableList<T>>, toKey: (T) -> K)") {
        typeParam("K")
        returns("Map<K, MutableList<T>>")
        body {
            """
                for (element in this) {
                    val key = toKey(element)
                    val list = result.getOrPut(key) { ArrayList<T>() }
                    list.add(element)
                }
                return result
            """
        }
    }

    f("drop(n: Int)") {
        doc = "Returns a list containing everything but the first *n* elements"
        returns("List<T>")
        body {
            "return dropWhile(countTo(n))"
        }
    }

    f("dropWhile(predicate: (T) -> Boolean)") {
        doc = "Returns a list containing the everything but the first elements that satisfy the given *predicate*"
        returns("List<T>")
        body {
            "return dropWhileTo(ArrayList<T>(), predicate)"
        }
    }


    f("dropWhileTo(result: L, predicate: (T) -> Boolean)") {
        doc = "Returns a list containing the everything but the first elements that satisfy the given *predicate*"
        typeParam("L: MutableList<in T>")
        returns("L")

        body {
            """
                var start = true
                for (element in this) {
                    if (start && predicate(element)) {
                        // ignore
                    } else {
                        start = false
                        result.add(element)
                    }
                }
                return result
            """
        }
    }

    f("take(n: Int)") {
        doc = "Returns a list containing the first *n* elements"
        returns("List<T>")
        body {
            "return takeWhile(countTo(n))"
        }
    }

    f("takeWhile(predicate: (T) -> Boolean)") {
        doc = "Returns a list containing the first elements that satisfy the given *predicate*"
        returns("List<T>")

        body {
            "return takeWhileTo(ArrayList<T>(), predicate)"
        }
    }

    f("takeWhileTo(result: C, predicate: (T) -> Boolean)") {
        doc = "Returns a list containing the first elements that satisfy the given *predicate*"
        typeParam("C: MutableCollection<in T>")
        returns("C")

        body {
            """
                for (element in this) if (predicate(element)) result.add(element) else break
                return result
            """
        }
    }

    f("toCollection(result: C)") {
        doc = "Copies all elements into the given collection"
        typeParam("C: MutableCollection<in T>")
        returns("C")

        body {
            """
                for (element in this) result.add(element)
                return result
            """
        }
    }

    f("reverse()") {
        doc = "Reverses the order the elements into a list"
        returns("List<T>")
        body {
            """
                val list = toCollection(ArrayList<T>())
                Collections.reverse(list)
                return list
            """
        }
    }

    f("toLinkedList()") {
        doc = "Copies all elements into a [[LinkedList]]"
        returns("LinkedList<T>")

        body { "return toCollection(LinkedList<T>())" }
    }

    f("toList()") {
        doc = "Copies all elements into a [[List]]"
        returns("List<T>")

        body { "return toCollection(ArrayList<T>())" }
    }

    f("toSet()") {
        doc = "Copies all elements into a [[Set]]"
        returns("Set<T>")

        body { "return toCollection(LinkedHashSet<T>())" }
    }

    f("toSortedSet()") {
        doc = "Copies all elements into a [[SortedSet]]"
        returns("SortedSet<T>")

        body { "return toCollection(TreeSet<T>())" }
    }

    f("requireNoNulls()") {
        absentFor(PrimitiveArrays) // Those are inherently non-nulls
        doc = "Returns a original Iterable containing all the non-*null* elements, throwing an [[IllegalArgumentException]] if there are any null elements"
        typeParam("T:Any")
        toNullableT = true
        returns("SELF")

        body {
            val THIS = "\$this"
            """
                for (element in this) {
                    if (element == null) {
                        throw IllegalArgumentException("null element found in $THIS")
                    }
                }
                return this as SELF
            """
        }

    }

    f("plus(element: T)") {
        doc = "Creates an [[Iterator]] which iterates over this iterator then the given element at the end"
        returns("List<T>")

        body {
            """
                val answer = ArrayList<T>()
                toCollection(answer)
                answer.add(element)
                return answer
            """
        }

    }

    f("plus(iterator: Iterator<T>)") {
        doc = "Creates an [[Iterator]] which iterates over this iterator then the following iterator"
        returns("List<T>")

        body {
            """
                val answer = ArrayList<T>()
                toCollection(answer)
                for (element in iterator) {
                    answer.add(element)
                }
                return answer
            """
        }
    }

    f("plus(collection: Iterable<T>)") {
        doc = "Creates an [[Iterator]] which iterates over this iterator then the following collection"
        returns("List<T>")

        body {
            "return plus(collection.iterator())"
        }
    }

    f("withIndices()") {
        doc = "Returns an iterator of Pairs(index, data)"
        returns("Iterator<Pair<Int, T>>")

        body {
            "return IndexIterator(iterator())"
        }
    }

    f("sortBy(f: (T) -> R)") {
        doc = """
        Copies all elements into a [[List]] and sorts it by value of compare_function(element)
        E.g. arrayList("two" to 2, "one" to 1).sortBy({it._2}) returns list sorted by second element of tuple
        """
        returns("List<T>")
        typeParam("R: Comparable<R>")

        body {
            """
                val sortedList = toCollection(ArrayList<T>())
                val sortBy: Comparator<T> = comparator<T> {(x: T, y: T) ->
                    val xr = f(x)
                    val yr = f(y)
                    xr.compareTo(yr)
                }
                java.util.Collections.sort(sortedList, sortBy)
                return sortedList
            """
        }
    }

    f("appendString(buffer: Appendable, separator: String = \", \", prefix: String =\"\", postfix: String = \"\", limit: Int = -1, truncated: String = \"...\")") {
        doc =
        """
              Appends the string from all the elements separated using the *separator* and using the given *prefix* and *postfix* if supplied

              If a collection could be huge you can specify a non-negative value of *limit* which will only show a subset of the collection then it will
              a special *truncated* separator (which defaults to "..."
            """
        returns("Unit")

        body {
            """
            buffer.append(prefix)
            var count = 0
            for (element in this) {
                if (++count > 1) buffer.append(separator)
                if (limit < 0 || count <= limit) {
                    val text = if (element == null) "null" else element.toString()
                    buffer.append(text)
                } else break
            }
            if (limit >= 0 && count > limit) buffer.append(truncated)
            buffer.append(postfix)
            """
        }
    }

    f("makeString(separator: String = \", \", prefix: String = \"\", postfix: String = \"\", limit: Int = -1, truncated: String = \"...\")") {
        doc = """
          Creates a string from all the elements separated using the *separator* and using the given *prefix* and *postfix* if supplied.

          If a collection could be huge you can specify a non-negative value of *limit* which will only show a subset of the collection then it will
          a special *truncated* separator (which defaults to "..."
        """

        returns("String")
        body {
            """
                val buffer = StringBuilder()
                appendString(buffer, separator, prefix, postfix, limit, truncated)
                return buffer.toString()
            """
        }
    }
}

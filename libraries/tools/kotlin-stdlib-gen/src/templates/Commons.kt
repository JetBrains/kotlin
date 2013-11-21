package templates

import java.util.ArrayList
import templates.Family.*

fun commons(): ArrayList<GenericFunction> {

    val templates = ArrayList<GenericFunction>()

    templates add f("all(predicate: (T) -> Boolean)") {
        doc = "Returns *true* if all elements match the given *predicate*"
        returns("Boolean")

        body {
            """
                for (element in this) if (!predicate(element)) return false
                return true
            """
        }
    }

    templates add f("any(predicate: (T) -> Boolean)") {
        doc = "Returns *true* if any elements match the given *predicate*"
        returns("Boolean")

        body {
            """
                for (element in this) if (predicate(element)) return true
                return false
            """
        }
    }

    templates add f("count(predicate: (T) -> Boolean)") {
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


    templates add f("find(predicate: (T) -> Boolean)") {
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

    templates add f("filterTo(result: C, predicate: (T) -> Boolean)") {
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

    templates add f("filterNotTo(result: C, predicate: (T) -> Boolean)") {
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

    templates add f("filterNotNullTo(result: C)") {
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

    templates add f("partition(predicate: (T) -> Boolean)") {
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

    templates add f("mapTo(result: C, transform : (T) -> R)") {
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

    templates add f("flatMapTo(result: C, transform: (T) -> Iterable<R>)") {
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

    templates add f("forEach(operation: (T) -> Unit)") {
        doc = "Performs the given *operation* on each element"
        returns("Unit")
        body {
            """
                for (element in this) operation(element)
            """
        }
    }

    templates add f("fold(initial: R, operation: (R, T) -> R)") {
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

    templates add f("foldRight(initial: R, operation: (T, R) -> R)") {
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

    templates add f("reduce(operation: (T, T) -> T)") {
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

    templates add f("reduceRight(operation: (T, T) -> T)") {
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

    templates add f("groupBy(toKey: (T) -> K)") {
        doc = "Groups the elements in the collection into a new [[Map]] using the supplied *toKey* function to calculate the key to group the elements by"
        typeParam("K")
        returns("Map<K, List<T>>")

        body { "return groupByTo(HashMap<K, MutableList<T>>(), toKey)" }
    }

    templates add f("groupByTo(result: MutableMap<K, MutableList<T>>, toKey: (T) -> K)") {
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

    templates add f("drop(n: Int)") {
        doc = "Returns a list containing everything but the first *n* elements"
        returns("List<T>")
        body {
            "return dropWhile(countTo(n))"
        }
    }

    templates add f("dropWhile(predicate: (T) -> Boolean)") {
        doc = "Returns a list containing the everything but the first elements that satisfy the given *predicate*"
        returns("List<T>")
        body {
            "return dropWhileTo(ArrayList<T>(), predicate)"
        }
    }


    templates add f("dropWhileTo(result: L, predicate: (T) -> Boolean)") {
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

    templates add f("takeWhileTo(result: C, predicate: (T) -> Boolean)") {
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

    templates add f("toCollection(result: C)") {
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

    templates add f("reverse()") {
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

    templates add f("toLinkedList()") {
        doc = "Copies all elements into a [[LinkedList]]"
        returns("LinkedList<T>")

        body { "return toCollection(LinkedList<T>())" }
    }

    templates add f("toList()") {
        doc = "Copies all elements into a [[List]]"
        returns("List<T>")

        body { "return toCollection(ArrayList<T>())" }
    }

    templates add f("toSet()") {
        doc = "Copies all elements into a [[Set]]"
        returns("Set<T>")

        body { "return toCollection(LinkedHashSet<T>())" }
    }

    templates add f("toSortedSet()") {
        doc = "Copies all elements into a [[SortedSet]]"
        returns("SortedSet<T>")

        body { "return toCollection(TreeSet<T>())" }
    }

    templates add f("withIndices()") {
        doc = "Returns an iterator of Pairs(index, data)"
        returns("Iterator<Pair<Int, T>>")

        body {
            "return IndexIterator(iterator())"
        }
    }

    templates add f("sortBy(f: (T) -> R)") {
        doc = """
        Copies all elements into a [[List]] and sorts it by value of compare_function(element)
        E.g. arrayList("two" to 2, "one" to 1).sortBy({it.second}) returns list sorted by second element of pair
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

    templates add f("appendString(buffer: Appendable, separator: String = \", \", prefix: String =\"\", postfix: String = \"\", limit: Int = -1, truncated: String = \"...\")") {
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

    templates add f("makeString(separator: String = \", \", prefix: String = \"\", postfix: String = \"\", limit: Int = -1, truncated: String = \"...\")") {
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

    return templates
}

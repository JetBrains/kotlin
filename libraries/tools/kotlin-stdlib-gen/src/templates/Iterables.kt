package templates

import java.util.ArrayList
import templates.Family.*

fun iterables(): List<GenericFunction> {

    val templates = commons()

    templates add f("filter(predicate: (T) -> Boolean)") {
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

    templates add f("filterNot(predicate: (T) -> Boolean)") {
        doc = "Returns a list containing all elements which do not match the given *predicate*"
        returns("List<T>")

        body {
            "return filterNotTo(ArrayList<T>(), predicate)"
        }
    }

    templates add f("filterNotNull()") {
        absentFor(PrimitiveArrays) // Those are inherently non-nulls
        doc = "Returns a list containing all the non-*null* elements"
        typeParam("T:Any")
        toNullableT = true
        returns("List<T>")

        body {
            "return filterNotNullTo<T, ArrayList<T>>(ArrayList<T>())"
        }
    }

    templates add f("map(transform : (T) -> R)") {
        doc = "Returns a new List containing the results of applying the given *transform* function to each element in this collection"
        typeParam("R")
        returns("List<R>")

        body {
            "return mapTo(ArrayList<R>(), transform)"
        }
    }

    templates add f("flatMap(transform: (T)-> Iterable<R>)") {
        doc = "Returns the result of transforming each element to one or more values which are concatenated together into a single list"
        typeParam("R")
        returns("List<R>")

        body {
            "return flatMapTo(ArrayList<R>(), transform)"
        }
    }

    templates add f("take(n: Int)") {
        doc = "Returns a list containing the first *n* elements"
        returns("List<T>")
        body {
            "return takeWhile(countTo(n))"
        }
    }

    templates add f("takeWhile(predicate: (T) -> Boolean)") {
        doc = "Returns a list containing the first elements that satisfy the given *predicate*"
        returns("List<T>")

        body {
            "return takeWhileTo(ArrayList<T>(), predicate)"
        }
    }

    templates add f("requireNoNulls()") {
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

    templates add f("plus(element: T)") {
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

    templates add f("plus(iterator: Iterator<T>)") {
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

    templates add f("plus(collection: Iterable<T>)") {
        doc = "Creates an [[Iterator]] which iterates over this iterator then the following collection"
        returns("List<T>")

        body {
            "return plus(collection.iterator())"
        }
    }

    return templates.sort()
}

package templates

fun iterators() {
    f("filter(predicate: (T) -> Boolean)") {
        doc = "Returns an iterator over elements which match the given *predicate*"

        returns("Iterator<T>")
        body {
            "return FilterIterator<T>(this, predicate)"
        }
    }

    f("filterNot(predicate: (T) -> Boolean)") {
        doc = "Returns an iterator over elements which don't match the given *predicate*"
        returns("Iterator<T>")

        body {
            "return filter {!predicate(it)}"
        }
    }

    f("filterNotNull()") {
        doc = "Returns an iterator over non-*null* elements"
        typeParam("T:Any")
        toNullableT = true
        returns("Iterator<T>")

        body {
            "return FilterNotNullIterator(this)"
        }
    }

    f("map(transform : (T) -> R)") {
        doc = "Returns an iterator obtained by applying *transform*, a function transforming an object of type *T* into an object of type *R*"
        typeParam("R")
        returns("Iterator<R>")

        body {
            "return MapIterator<T, R>(this, transform)"
        }
    }

    f("flatMap(transform: (T) -> Iterator<R>)") {
        doc = "Returns an iterator over the concatenated results of transforming each element to one or more values"
        typeParam("R")
        returns("Iterator<R>")

        body {
            "return FlatMapIterator<T, R>(this, transform)"
        }
    }

    f("requireNoNulls()") {
        doc = "Returns a original Iterable containing all the non-*null* elements, throwing an [[IllegalArgumentException]] if there are any null elements"
        typeParam("T:Any")
        toNullableT = true
        returns("Iterator<T>")

        body {
            val THIS = "\$this"
            """
                return map<T?, T>{
                    if (it == null) throw IllegalArgumentException("null element in iterator $THIS") else it
                }
            """
        }
    }


    f("take(n: Int)") {
        doc = "Returns an iterator restricted to the first *n* elements"
        returns("Iterator<T>")
        body {
            """
                var count = n
                return takeWhile{ --count >= 0 }
            """
        }
    }

    f("takeWhile(predicate: (T) -> Boolean)") {
        doc = "Returns an iterator restricted to the first elements that match the given *predicate*"
        returns("Iterator<T>")

        body {
            "return TakeWhileIterator<T>(this, predicate)"
        }
    }

    // TODO: drop(n), dropWhile

    f("plus(element: T)") {
        doc = "Creates an [[Iterator]] which iterates over this iterator then the given element at the end"
        returns("Iterator<T>")

        body {
            "return CompositeIterator<T>(this, SingleIterator(element))"
        }

    }

    f("plus(iterator: Iterator<T>)") {
        doc = "Creates an [[Iterator]] which iterates over this iterator then the following iterator"
        returns("Iterator<T>")

        body {
            "return CompositeIterator<T>(this, iterator)"
        }
    }

    f("plus(collection: Iterable<T>)") {
        doc = "Creates an [[Iterator]] which iterates over this iterator then the following collection"
        returns("Iterator<T>")

        body {
            "return plus(collection.iterator())"
        }
    }
}

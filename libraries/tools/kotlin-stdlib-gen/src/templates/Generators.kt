package templates

import templates.Family.*

fun generators(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("plus(element: T)") {
        exclude(Strings)
        doc { "Returns a list containing all elements of original collection and then the given element" }
        returns("List<T>")
        body {
            """
            val answer = toArrayList()
            answer.add(element)
            return answer
            """
        }

        doc(Streams) { "Returns a stream containing all elements of original stream and then the given element" }
        returns(Streams) { "Stream<T>" }
        body(Streams) {
            """
            return Multistream(streamOf(this, streamOf(element)))
            """
        }
    }

    templates add f("plus(collection: Iterable<T>)") {
        exclude(Strings, Streams)
        doc { "Returns a list containing all elements of original collection and then all elements of the given *collection*" }
        returns("List<T>")
        body {
            """
            val answer = toArrayList()
            answer.addAll(collection)
            return answer
            """
        }
    }

    templates add f("plus(array: Array<out T>)") {
        exclude(Strings, Streams)
        doc { "Returns a list containing all elements of original collection and then all elements of the given *collection*" }
        returns("List<T>")
        body {
            """
            val answer = toArrayList()
            answer.addAll(array)
            return answer
            """
        }
    }

    templates add f("plus(collection: Iterable<T>)") {
        only(Streams)
        doc { "Returns a stream containing all elements of original stream and then all elements of the given *collection*" }
        returns("Stream<T>")
        body {
            """
            return Multistream(streamOf(this, collection.stream()))
            """
        }
    }

    templates add f("plus(stream: Stream<T>)") {
        only(Streams)
        doc { "Returns a stream containing all elements of original stream and then all elements of the given *stream*" }
        returns("Stream<T>")
        body {
            """
            return Multistream(streamOf(this, stream))
            """
        }
    }

    templates add f("partition(predicate: (T) -> Boolean)") {
        inline(true)

        doc {
            """
            Splits original collection into pair of collections,
            where *first* collection contains elements for which predicate yielded *true*,
            while *second* collection contains elements for which predicate yielded *false*
            """
        }
        // TODO: Stream variant
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

        returns(Strings) { "Pair<String, String>" }
        body(Strings) {
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
            return Pair(first.toString(), second.toString())
            """
        }
    }

    templates add f("merge(other: Iterable<R>, transform: (T, R) -> V)") {
        exclude(Streams, Strings)
        doc {
            """
            Returns a list of values built from elements of both collections with same indexes using provided *transform*. List has length of shortest collection.
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
            val list = ArrayList<V>(collectionSizeOrDefault(10))
            while (first.hasNext() && second.hasNext()) {
                list.add(transform(first.next(), second.next()))
            }
            return list
            """
        }
        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            val first = iterator()
            val second = other.iterator()
            val list = ArrayList<V>(size())
            while (first.hasNext() && second.hasNext()) {
                list.add(transform(first.next(), second.next()))
            }
            return list
            """
        }
    }

    templates add f("merge(array: Array<out R>, transform: (T, R) -> V)") {
        exclude(Streams, Strings)
        doc {
            """
            Returns a list of values built from elements of both collections with same indexes using provided *transform*. List has length of shortest collection.
            """
        }
        typeParam("R")
        typeParam("V")
        returns("List<V>")
        inline(true)
        body {
            """
            val first = iterator()
            val second = array.iterator()
            val list = ArrayList<V>(collectionSizeOrDefault(10))
            while (first.hasNext() && second.hasNext()) {
                list.add(transform(first.next(), second.next()))
            }
            return list
            """
        }
        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            val first = iterator()
            val second = array.iterator()
            val list = ArrayList<V>(size())
            while (first.hasNext() && second.hasNext()) {
                list.add(transform(first.next(), second.next()))
            }
            return list
            """
        }

    }


    templates add f("merge(stream: Stream<R>, transform: (T, R) -> V)") {
        only(Streams)
        doc {
            """
            Returns a stream of values built from elements of both collections with same indexes using provided *transform*. Stream has length of shortest stream.
            """
        }
        typeParam("R")
        typeParam("V")
        returns("Stream<V>")
        body {
            """
            return MergingStream(this, stream, transform)
            """
        }
    }


    templates add f("zip(other: Iterable<R>)") {
        exclude(Streams, Strings)
        doc {
            """
            Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
            """
        }
        typeParam("R")
        returns("List<Pair<T, R>>")
        body {
            """
            return merge(other) { (t1, t2) -> t1 to t2 }
            """
        }
    }

    templates add f("zip(other: String)") {
        only(Strings)
        doc {
            """
            Returns a list of pairs built from characters of both strings with same indexes. List has length of shortest collection.
            """
        }
        returns("List<Pair<Char, Char>>")
        body {
            """
            val first = iterator()
            val second = other.iterator()
            val list = ArrayList<Pair<Char, Char>>(length())
            while (first.hasNext() && second.hasNext()) {
                list.add(first.next() to second.next())
            }
            return list
            """
        }
    }

    templates add f("zip(array: Array<out R>)") {
        exclude(Streams, Strings)
        doc {
            """
            Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
            """
        }
        typeParam("R")
        returns("List<Pair<T, R>>")
        body {
            """
            return merge(array) { (t1, t2) -> t1 to t2 }
            """
        }
    }

    templates add f("zip(stream: Stream<R>)") {
        only(Streams)
        doc {
            """
            Returns a stream of pairs built from elements of both collections with same indexes. Stream has length of shortest stream.
            """
        }
        typeParam("R")
        returns("Stream<Pair<T, R>>")
        body {
            """
            return MergingStream(this, stream) { (t1, t2) -> t1 to t2 }
            """
        }
    }

    return templates
}
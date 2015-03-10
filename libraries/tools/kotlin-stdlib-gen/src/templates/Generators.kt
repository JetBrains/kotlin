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

        doc(Sequences) { "Returns a sequence containing all elements of original sequence and then the given element" }
        returns(Sequences) { "Sequence<T>" }
        body(Sequences) {
            """
            return MultiSequence(sequenceOf(this, sequenceOf(element)))
            """
        }
    }

    templates add f("plus(collection: Iterable<T>)") {
        exclude(Strings, Sequences)
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
        exclude(Strings, Sequences)
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
        only(Sequences)
        doc { "Returns a sequence containing all elements of original sequence and then all elements of the given [collection]" }
        returns("Sequence<T>")
        body {
            """
            return MultiSequence(sequenceOf(this, collection.sequence()))
            """
        }
    }

    templates add f("plus(sequence: Sequence<T>)") {
        only(Sequences)
        doc { "Returns a sequence containing all elements of original sequence and then all elements of the given [sequence]" }
        returns("Sequence<T>")
        body {
            """
            return MultiSequence(sequenceOf(this, sequence))
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
        // TODO: Sequence variant
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
        exclude(Sequences, Strings)
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
        exclude(Sequences, Strings)
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


    templates add f("merge(sequence: Sequence<R>, transform: (T, R) -> V)") {
        only(Sequences)
        doc {
            """
            Returns a sequence of values built from elements of both collections with same indexes using provided *transform*. Resulting sequence has length of shortest input sequences.
            """
        }
        typeParam("R")
        typeParam("V")
        returns("Sequence<V>")
        body {
            """
            return MergingSequence(this, sequence, transform)
            """
        }
    }


    templates add f("zip(other: Iterable<R>)") {
        exclude(Sequences, Strings)
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
        exclude(Sequences, Strings)
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

    templates add f("zip(sequence: Sequence<R>)") {
        only(Sequences)
        doc {
            """
            Returns a sequence of pairs built from elements of both collections with same indexes.
            Resulting sequence has length of shortest input sequences.
            """
        }
        typeParam("R")
        returns("Sequence<Pair<T, R>>")
        body {
            """
            return MergingSequence(this, sequence) { (t1, t2) -> t1 to t2 }
            """
        }
    }

    return templates
}
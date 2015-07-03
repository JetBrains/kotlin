package templates

import templates.Family.*

fun generators(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("plus(element: T)") {
        exclude(Strings)
        doc { "Returns a list containing all elements of the original collection and then the given [element]." }
        returns("List<T>")
        returns("SELF", ArraysOfObjects, ArraysOfPrimitives, Sets, Sequences)
        body {
            """
            if (this is Collection) return this.plus(element)
            val answer = ArrayList<T>()
            answer.addAll(this)
            answer.add(element)
            return answer
            """
        }
        body(Collections) {
            """
            val answer = ArrayList<T>(size() + 1)
            answer.addAll(this)
            answer.add(element)
            return answer
            """
        }


        doc(ArraysOfObjects, ArraysOfPrimitives) { "Returns an array containing all elements of the original array and then the given [element]." }
        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            val answer = this.copyOf(size() + 1)
            answer[size()] = element
            return answer as SELF
            """
        }

        // TODO: use build scope function when available
        // TODO: use immutable sets when available
        // TODO: precalculate size
        doc(Sets) { "Returns a set containing all elements of the original set and then the given [element]." }
        body(Sets) {
            """
            val copyOfSet = LinkedHashSet<T>(mapCapacity(size() + 1))
            copyOfSet.addAll(this)
            copyOfSet.add(element)
            return copyOfSet
            """
        }

        doc(Sequences) { "Returns a sequence containing all elements of the original sequence and then the given [element]." }
        body(Sequences) {
            """
            return sequenceOf(this, sequenceOf(element)).flatten()
            """
        }
    }

    templates add f("plus(collection: Iterable<T>)") {
        only(Iterables, Collections, Sets, Sequences)
        doc { "Returns a list containing all elements of the original collection and then all elements of the given [collection]." }
        returns("List<T>")
        returns("SELF", ArraysOfObjects, ArraysOfPrimitives, Sets, Sequences)
        body {
            """
            if (this is Collection) return this.plus(collection)
            val answer = ArrayList<T>(0)
            answer.addAll(this)
            answer.addAll(collection)
            return answer
            """
        }
        body(Collections) {
            """
            if (collection is Collection) return this.plus(collection)
            val answer = ArrayList<T>(this)
            answer.addAll(collection)
            return answer
            """
        }

        // TODO: try to precalculate size
        // TODO: use immutable set builder when available
        body(Sets) {
            """
            val result = LinkedHashSet<T>(mapCapacity(collection.collectionSizeOrNull()?.let { this.size() + it } ?: this.size() * 2))
            result.addAll(this)
            result.addAll(collection)
            return result
            """
        }

        doc(Sequences) { "Returns a sequence containing all elements of original sequence and then all elements of the given [collection]" }
        body(Sequences) {
            """
            return sequenceOf(this, collection.asSequence()).flatten()
            """
        }
    }

    templates add f("plus(collection: Collection<T>)") {
        only(Collections, ArraysOfPrimitives, ArraysOfObjects)
        returns("List<T>")
        returns("SELF", ArraysOfObjects, ArraysOfPrimitives)
        body(Collections) {
            """
            val answer = ArrayList<T>(this.size() + collection.size())
            answer.addAll(this)
            answer.addAll(collection)
            return answer
            """
        }
        // ?
        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            val thisSize = size()
            val answer = this.copyOf(thisSize + collection.size())
            collection.forEachIndexed { i, element ->
                answer[thisSize + i] = element
            }
            return answer as SELF
            """
        }
    }

    templates add f("plus(array: Array<out T>)") {
        only(Iterables, Collections, Sets, Sequences)
        doc { "Returns a list containing all elements of original collection and then all elements of the given [array]." }
        returns("List<T>")
        returns("SELF", Sets, Sequences)
        body {
            """
            if (this is Collection) return this.plus(array)
            val answer = ArrayList<T>()
            answer.addAll(this)
            answer.addAll(array)
            return answer
            """
        }
        body(Collections) {
            """
            val answer = ArrayList<T>(this.size() + array.size())
            answer.addAll(this)
            answer.addAll(array)
            return answer
            """
        }
        body(Sets) {
            """
            val result = LinkedHashSet<T>(mapCapacity(this.size() + array.size()))
            result.addAll(this)
            result.addAll(array)
            return result
            """
        }
        body(Sequences) {
            """
            return this.plus(array.asList())
            """
        }
    }

    templates add f("plus(collection: SELF)") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        returns("SELF")
        body {
            """
            val thisSize = size()
            val answer = this.copyOf(thisSize + collection.size())
            collection.forEachIndexed { i, element ->
                answer[thisSize + i] = element
            }
            return answer as SELF
            """
        }
    }

    templates add f("plus(sequence: Sequence<T>)") {
        only(Sequences)
        doc { "Returns a sequence containing all elements of original sequence and then all elements of the given [sequence]." }
        returns("Sequence<T>")
        body {
            """
            return sequenceOf(this, sequence).flatten()
            """
        }
    }

    templates add f("partition(predicate: (T) -> Boolean)") {
        inline(true)

        doc {
            """
            Splits original collection into pair of collections,
            where *first* collection contains elements for which [predicate] yielded `true`,
            while *second* collection contains elements for which [predicate] yielded `false`.
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
            Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
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
            Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
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
            val list = ArrayList<V>(Math.min(size(), array.size()))
            while (first.hasNext() && second.hasNext()) {
                list.add(transform(first.next(), second.next()))
            }
            return list
            """
        }

    }


    templates add f("merge(array: SELF, transform: (T, T) -> V)") {
        only(ArraysOfPrimitives)
        doc {
            """
            Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
            """
        }
        typeParam("V")
        returns("List<V>")
        inline(true)
        body() {
            """
            val first = iterator()
            val second = array.iterator()
            val list = ArrayList<V>(Math.min(size(), array.size()))
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
            Returns a sequence of values built from elements of both collections with same indexes using provided [transform]. Resulting sequence has length of shortest input sequences.
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
            return merge(other) { t1, t2 -> t1 to t2 }
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
            return merge(array) { t1, t2 -> t1 to t2 }
            """
        }
    }

    templates add f("zip(array: SELF)") {
        only(ArraysOfPrimitives)
        doc {
            """
            Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
            """
        }
        returns("List<Pair<T, T>>")
        body {
            """
            return merge(array) { t1, t2 -> t1 to t2 }
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
            return MergingSequence(this, sequence) { t1, t2 -> t1 to t2 }
            """
        }
    }

    return templates
}
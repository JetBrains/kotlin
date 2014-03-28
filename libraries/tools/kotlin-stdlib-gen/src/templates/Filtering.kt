package templates

import templates.Family.*

fun filtering(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("drop(n: Int)") {
        doc { "Returns a list containing all elements except first *n* elements" }
        returns("List<T>")
        body {
            """
            var count = 0
            val list = ArrayList<T>()
            for (item in this) {
                if (count++ >= n) list.add(item)
            }
            return list
            """
        }

        doc(Streams) { "Returns a stream containing all elements except first *n* elements" }
        returns(Streams) { "Stream<T>" }
        body(Streams) {
            """
            var count = 0;
            return FilteringStream(this) { count++ >= n }
            """
        }

        include(Collections)
        body(Collections, ArraysOfObjects, ArraysOfPrimitives) {
            """
            if (n >= size)
                return ArrayList<T>()

            var count = 0
            val list = ArrayList<T>(size - n)
            for (item in this) {
                if (count++ >= n) list.add(item)
            }
            return list
            """
        }
    }

    templates add f("take(n: Int)") {
        doc { "Returns a list containing first *n* elements" }
        returns("List<T>")
        body {
            """
            var count = 0
            val list = ArrayList<T>(n)
            for (item in this) {
                if (count++ == n)
                    break
                list.add(item)
            }
            return list
            """
        }

        doc(Streams) { "Returns a stream containing first *n* elements" }
        returns(Streams) { "Stream<T>" }
        body(Streams) {
            """
            var count = 0
            return LimitedStream(this) { count++ == n }
            """
        }

        include(Collections)
        body(Collections, ArraysOfObjects, ArraysOfPrimitives) {
            """
            var count = 0
            val realN = if (n > size) size else n
            val list = ArrayList<T>(realN)
            for (item in this) {
                if (count++ == realN)
                    break;
                list.add(item)
            }
            return list
            """
        }
    }

    templates add f("dropWhile(predicate: (T)->Boolean)") {
        inline(true)

        doc { "Returns a list containing all elements except first elements that satisfy the given *predicate*" }
        returns("List<T>")
        body {
            """
            var yielding = false
            val list = ArrayList<T>()
            for (item in this)
                if (yielding)
                    list.add(item)
                else if(!predicate(item)) {
                    list.add(item)
                    yielding = true
                }
            return list
            """
        }

        inline(false, Streams)
        doc(Streams) { "Returns a stream containing all elements except first elements that satisfy the given *predicate*" }
        returns(Streams) { "Stream<T>" }
        body(Streams) {
            """
            var yielding = false
            return FilteringStream(this) {
                        if (yielding)
                            true
                        else if (!predicate(it)) {
                            yielding = true
                            true
                        } else
                            false
                    }
            """
        }

    }

    templates add f("takeWhile(predicate: (T)->Boolean)") {
        inline(true)

        doc { "Returns a list containing first elements satisfying the given *predicate*" }
        returns("List<T>")
        body {
            """
            val list = ArrayList<T>()
            for (item in this) {
                 if(!predicate(item))
                    break;
                 list.add(item)
            }
            return list
            """
        }

        inline(false, Streams)
        doc(Streams) { "Returns a stream containing first elements satisfying the given *predicate*" }
        returns(Streams) { "Stream<T>" }
        body(Streams) {
            """
            return LimitedStream(this, false, predicate)
            """
        }
    }

    templates add f("filter(predicate: (T)->Boolean)") {
        inline(true)

        doc { "Returns a list containing all elements matching the given *predicate*" }
        returns("List<T>")
        body {
            """
            return filterTo(ArrayList<T>(), predicate)
            """
        }

        inline(false, Streams)
        doc(Streams) { "Returns a stream containing all elements matching the given *predicate*" }
        returns(Streams) { "Stream<T>" }
        body(Streams) {
            """
            return FilteringStream(this, true, predicate)
            """
        }
        include(Maps)
    }

    templates add f("filterTo(collection: C, predicate: (T) -> Boolean)") {
        inline(true)

        doc { "Appends all elements matching the given *predicate* into the given *collection*" }
        typeParam("C: MutableCollection<in T>")
        returns("C")

        body {
            """
            for (element in this) if (predicate(element)) collection.add(element)
            return collection
            """
        }
        include(Maps)
    }

    templates add f("filterNot(predicate: (T)->Boolean)") {
        inline(true)

        doc { "Returns a list containing all elements not matching the given *predicate*" }
        returns("List<T>")
        body {
            """
            return filterNotTo(ArrayList<T>(), predicate)
            """
        }

        inline(false, Streams)
        doc(Streams) { "Returns a stream containing all elements not matching the given *predicate*" }
        returns(Streams) { "Stream<T>" }
        body(Streams) {
            """
            return FilteringStream(this, false, predicate)
            """
        }
        include(Maps)
    }

    templates add f("filterNotTo(collection: C, predicate: (T) -> Boolean)") {
        inline(true)

        doc { "Appends all elements not matching the given *predicate* to the given *collection*" }
        typeParam("C: MutableCollection<in T>")
        returns("C")

        body {
            """
            for (element in this) if (!predicate(element)) collection.add(element)
            return collection
            """
        }
        include(Maps)
    }

    templates add f("filterNotNull()") {
        exclude(ArraysOfPrimitives)
        doc { "Returns a list containing all elements that are not null" }
        typeParam("T: Any")
        returns("List<T>")
        toNullableT = true
        body {
            """
            return filterNotNullTo(ArrayList<T>())
            """
        }

        doc(Streams) { "Returns a stream containing all elements that are not null" }
        returns(Streams) { "Stream<T>" }
        body(Streams) {
            """
            return FilteringStream(this, false, { it != null }) as Stream<T>
            """
        }
    }

    templates add f("filterNotNullTo(collection: C)") {
        exclude(ArraysOfPrimitives)
        doc { "Appends all elements that are not null to the given *collection*" }
        typeParam("C: MutableCollection<in T>")
        typeParam("T: Any")
        returns("C")
        toNullableT = true
        body {
            """
            for (element in this) if (element != null) collection.add(element)
            return collection
            """
        }
    }

    return templates
}
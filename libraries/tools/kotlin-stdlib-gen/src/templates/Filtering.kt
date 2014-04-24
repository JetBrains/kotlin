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

        body(Strings) { "return substring(Math.min(n, size))" }
        returns(Strings) { "String" }

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

        body(Strings) { "return substring(0, Math.min(n, size))" }
        returns(Strings) { "String" }

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

    templates add f("dropWhile(predicate: (T) -> Boolean)") {
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
                else if (!predicate(item)) {
                    list.add(item)
                    yielding = true
                }
            return list
            """
        }

        returns(Strings) { "String" }
        body(Strings) {
            """
            for (index in 0..length)
                if (!predicate(get(index))) {
                    return substring(index)
                }
            return ""
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

    templates add f("takeWhile(predicate: (T) -> Boolean)") {
        inline(true)

        doc { "Returns a list containing first elements satisfying the given *predicate*" }
        returns("List<T>")
        body {
            """
            val list = ArrayList<T>()
            for (item in this) {
                if (!predicate(item))
                    break;
                list.add(item)
            }
            return list
            """
        }

        returns(Strings) { "String" }
        body(Strings) {
            """
            for (index in 0..length)
                if (!predicate(get(index))) {
                    return substring(0, index)
                }
            return ""
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

    templates add f("filter(predicate: (T) -> Boolean)") {
        inline(true)

        doc { "Returns a list containing all elements matching the given *predicate*" }
        returns("List<T>")
        body {
            """
            return filterTo(ArrayList<T>(), predicate)
            """
        }

        returns(Strings) { "String" }
        body(Strings) {
            """
            return filterTo(StringBuilder(), predicate).toString()
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

    templates add f("filterTo(destination: C, predicate: (T) -> Boolean)") {
        inline(true)

        doc { "Appends all elements matching the given *predicate* into the given *destination*" }
        typeParam("C : TCollection")
        returns("C")

        body {
            """
            for (element in this) if (predicate(element)) destination.add(element)
            return destination
            """
        }

        doc(Strings) { "Appends all characters matching the given *predicate* to the given *destination*" }
        body(Strings) {
            """
            for (index in 0..length - 1) {
                val element = get(index)
                if (predicate(element)) destination.append(element)
            }
            return destination
            """
        }

        include(Maps)
    }

    templates add f("filterNot(predicate: (T) -> Boolean)") {
        inline(true)

        doc { "Returns a list containing all elements not matching the given *predicate*" }
        returns("List<T>")
        body {
            """
            return filterNotTo(ArrayList<T>(), predicate)
            """
        }

        returns(Strings) { "String" }
        body(Strings) {
            """
            return filterNotTo(StringBuilder(), predicate).toString()
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

    templates add f("filterNotTo(destination: C, predicate: (T) -> Boolean)") {
        inline(true)

        doc { "Appends all elements not matching the given *predicate* to the given *destination*" }
        typeParam("C : TCollection")
        returns("C")

        body {
            """
            for (element in this) if (!predicate(element)) destination.add(element)
            return destination
            """
        }

        doc(Strings) { "Appends all characters not matching the given *predicate* to the given *destination*" }
        body(Strings) {
            """
            for (element in this) if (!predicate(element)) destination.append(element)
            return destination
            """
        }
        include(Maps)
    }

    templates add f("filterNotNull()") {
        exclude(ArraysOfPrimitives, Strings)
        doc { "Returns a list containing all elements that are not null" }
        typeParam("T : Any")
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
            return FilteringStream(this, false, { it == null }) as Stream<T>
            """
        }
    }

    templates add f("filterNotNullTo(destination: C)") {
        exclude(ArraysOfPrimitives, Strings)
        doc { "Appends all elements that are not null to the given *destination*" }
        returns("C")
        typeParam("C : TCollection")
        typeParam("T : Any")
        toNullableT = true
        body {
            """
            for (element in this) if (element != null) destination.add(element)
            return destination
            """
        }
    }

    templates add f("slice(indices: Iterable<Int>)") {
        only(Strings, Lists, ArraysOfPrimitives, ArraysOfObjects)
        doc { "Returns a list containing elements at specified positions" }
        returns("List<T>")
        body {
            """
            val list = ArrayList<T>()
            for (index in indices) {
                list.add(get(index))
            }
            return list
            """
        }

        returns(Strings) { "String" }
        body(Strings) {
            """
            val result = StringBuilder()
            for (i in indices) {
                result.append(get(i))
            }
            return result.toString()
            """
        }
    }

    return templates
}

package templates

import templates.Family.*

fun filtering(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("drop(n: Int)") {
        val n = "\$n"
        doc { "Returns a list containing all elements except first [n] elements" }
        returns("List<T>")
        body {
            """
            require(n >= 0, { "Requested element count $n is less than zero." })
            val list: ArrayList<T>
            if (this is Collection<*>) {
                val resultSize = size() - n
                if (resultSize <= 0)
                    return emptyList()

                list = ArrayList<T>(resultSize)
                if (this is List<T>) {
                    for (index in n..size() - 1) {
                        list.add(this[index])
                    }
                    return list
                }
            }
            else {
                list = ArrayList<T>()
            }
            var count = 0
            for (item in this) {
                if (count++ >= n) list.add(item)
            }
            return list
            """
        }

        doc(Sequences) { "Returns a sequence containing all elements except first [n] elements" }
        returns(Sequences) { "Sequence<T>" }
        body(Sequences) {
            """
            require(n >= 0, { "Requested element count $n is less than zero." })
            return DropSequence(this, n)
            """
        }

        doc(Strings) { "Returns a string with the first [n] characters removed"}
        body(Strings) { "return substring(Math.min(n, length()))" }
        returns(Strings) { "String" }

        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            require(n >= 0, { "Requested element count $n is less than zero." })
            if (n >= size())
                return emptyList()

            val list = ArrayList<T>(size() - n)
            for (index in n..size() - 1) {
                list.add(this[index])
            }
            return list
            """
        }
    }

    templates add f("take(n: Int)") {
        val n = "\$n"
        doc { "Returns a list containing first [n] elements" }
        returns("List<T>")
        body {
            """
            require(n >= 0, { "Requested element count $n is less than zero." })
            var count = 0
            val list = ArrayList<T>(Math.min(n, collectionSizeOrDefault(n)))
            for (item in this) {
                if (count++ == n)
                    break
                list.add(item)
            }
            return list
            """
        }

        doc(Strings) { "Returns a string containing the first [n] characters from this string, or the entire string if this string is shorter"}
        body(Strings) {
            """
            require(n >= 0, { "Requested element count $n is less than zero." })
            return substring(0, Math.min(n, length()))
            """
        }
        returns(Strings) { "String" }

        doc(Sequences) { "Returns a sequence containing first *n* elements" }
        returns(Sequences) { "Sequence<T>" }
        body(Sequences) {
            """
            require(n >= 0, { "Requested element count $n is less than zero." })
            return TakeSequence(this, n)
            """
        }

        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            require(n >= 0, "Requested element count $n is less than zero.")
            var count = 0
            val realN = Math.min(n, size())
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

    templates add f("takeLast(n: Int)") {
        val n = "\$n"
        doc { "Returns a list containing last [n] elements" }
        only(Lists, ArraysOfObjects, ArraysOfPrimitives, Strings)
        returns("List<T>")

        doc(Strings) { "Returns a string containing the last [n] characters from this string, or the entire string if this string is shorter"}
        body(Strings) {
            """
            require(n >= 0, { "Requested element count $n is less than zero." })
            val length = length()
            return substring(length - Math.min(n, length), length)
            """
        }
        returns(Strings) { "String" }

        body(Lists, ArraysOfObjects, ArraysOfPrimitives) {
            """
            require(n >= 0, { "Requested element count $n is less than zero." })
            val size = size()
            val realN = Math.min(n, size)
            val list = ArrayList<T>(realN)
            for (index in size - realN .. size - 1)
                list.add(this[index])
            return list
            """
        }
    }

    templates add f("dropWhile(predicate: (T) -> Boolean)") {
        inline(true)

        doc { "Returns a list containing all elements except first elements that satisfy the given [predicate]" }
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

        doc(Strings) { "Returns a string containing all characters except first characters that satisfy the given [predicate]" }
        returns(Strings) { "String" }
        body(Strings) {
            """
            for (index in 0..length - 1)
                if (!predicate(get(index))) {
                    return substring(index)
                }
            return ""
            """
        }

        inline(false, Sequences)
        doc(Sequences) { "Returns a sequence containing all elements except first elements that satisfy the given [predicate]" }
        returns(Sequences) { "Sequence<T>" }
        body(Sequences) {
            """
            return DropWhileSequence(this, predicate)
            """
        }

    }

    templates add f("takeWhile(predicate: (T) -> Boolean)") {
        inline(true)

        doc { "Returns a list containing first elements satisfying the given [predicate]" }
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

        doc(Strings) { "Returns a string containing the first characters that satisfy the given [predicate]"}
        returns(Strings) { "String" }
        body(Strings) {
            """
            for (index in 0..length - 1)
                if (!predicate(get(index))) {
                    return substring(0, index)
                }
            return this
            """
        }

        inline(false, Sequences)
        doc(Sequences) { "Returns a sequence containing first elements satisfying the given [predicate]" }
        returns(Sequences) { "Sequence<T>" }
        body(Sequences) {
            """
            return TakeWhileSequence(this, predicate)
            """
        }
    }

    templates add f("filter(predicate: (T) -> Boolean)") {
        inline(true)

        doc { "Returns a list containing all elements matching the given [predicate]" }
        returns("List<T>")
        body {
            """
            return filterTo(ArrayList<T>(), predicate)
            """
        }

        doc(Strings) { "Returns a string containing only those characters from the original string that match the given [predicate]" }
        returns(Strings) { "String" }
        body(Strings) {
            """
            return filterTo(StringBuilder(), predicate).toString()
            """
        }

        inline(false, Sequences)
        doc(Sequences) { "Returns a sequence containing all elements matching the given [predicate]" }
        returns(Sequences) { "Sequence<T>" }
        body(Sequences) {
            """
            return FilteringSequence(this, true, predicate)
            """
        }
    }

    templates add f("filterTo(destination: C, predicate: (T) -> Boolean)") {
        inline(true)

        doc { "Appends all elements matching the given [predicate] into the given [destination]" }
        typeParam("C : TCollection")
        returns("C")

        body {
            """
            for (element in this) if (predicate(element)) destination.add(element)
            return destination
            """
        }

        doc(Strings) { "Appends all characters matching the given [predicate] to the given [destination]" }
        body(Strings) {
            """
            for (index in 0..length - 1) {
                val element = get(index)
                if (predicate(element)) destination.append(element)
            }
            return destination
            """
        }
    }

    templates add f("filterNot(predicate: (T) -> Boolean)") {
        inline(true)

        doc { "Returns a list containing all elements not matching the given [predicate]" }
        returns("List<T>")
        body {
            """
            return filterNotTo(ArrayList<T>(), predicate)
            """
        }

        doc(Strings) { "Returns a string containing only those characters from the original string that do not match the given [predicate]" }
        returns(Strings) { "String" }
        body(Strings) {
            """
            return filterNotTo(StringBuilder(), predicate).toString()
            """
        }

        inline(false, Sequences)
        doc(Sequences) { "Returns a sequence containing all elements not matching the given [predicate]" }
        returns(Sequences) { "Sequence<T>" }
        body(Sequences) {
            """
            return FilteringSequence(this, false, predicate)
            """
        }
    }

    templates add f("filterNotTo(destination: C, predicate: (T) -> Boolean)") {
        inline(true)

        doc { "Appends all elements not matching the given [predicate] to the given [destination]" }
        typeParam("C : TCollection")
        returns("C")

        body {
            """
            for (element in this) if (!predicate(element)) destination.add(element)
            return destination
            """
        }

        doc(Strings) { "Appends all characters not matching the given [predicate] to the given [destination]" }
        body(Strings) {
            """
            for (element in this) if (!predicate(element)) destination.append(element)
            return destination
            """
        }
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

        doc(Sequences) { "Returns a sequence containing all elements that are not null" }
        returns(Sequences) { "Sequence<T>" }
        body(Sequences) {
            """
            return filterNot { it == null } as Sequence<T>
            """
        }
    }

    templates add f("filterNotNullTo(destination: C)") {
        exclude(ArraysOfPrimitives, Strings)
        doc { "Appends all elements that are not null to the given [destination]" }
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
            val list = ArrayList<T>(indices.collectionSizeOrDefault(10))
            for (index in indices) {
                list.add(get(index))
            }
            return list
            """
        }

        doc(Strings) { "Returns a string containing characters at specified positions" }
        returns(Strings) { "String" }
        body(Strings) {
            """
            val result = StringBuilder(indices.collectionSizeOrDefault(10))
            for (i in indices) {
                result.append(get(i))
            }
            return result.toString()
            """
        }
    }

    return templates
}
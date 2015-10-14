package templates

import templates.Family.*

fun filtering(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("drop(n: Int)") {
        val n = "\$n"
        doc { "Returns a list containing all elements except first [n] elements." }
        returns("List<T>")
        body {
            """
            require(n >= 0, { "Requested element count $n is less than zero." })
            if (n == 0) return toList()
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

        doc(Sequences) { "Returns a sequence containing all elements except first [n] elements." }
        returns(Sequences) { "Sequence<T>" }
        body(Sequences) {
            """
            require(n >= 0, { "Requested element count $n is less than zero." })
            return if (n == 0) this else DropSequence(this, n)
            """
        }

        include(Strings)
        doc(Strings) { "Returns a string with the first [n] characters removed."}
        body(Strings) { "return substring(Math.min(n, length()))" }
        returns(Strings) { "String" }


        doc(CharSequences) { "Returns a char sequence with the first [n] characters removed."}
        body(CharSequences) { "return subSequence(Math.min(n, length()))" }
        returns(CharSequences) { "CharSequence" }

        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            require(n >= 0, { "Requested element count $n is less than zero." })
            if (n == 0)
                return toList()
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
        doc { "Returns a list containing first [n] elements." }
        returns("List<T>")
        body {
            """
            require(n >= 0, { "Requested element count $n is less than zero." })
            if (n == 0) return emptyList()
            if (this is Collection<T> && n >= size()) return toList()
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

        include(Strings)
        doc(Strings) { "Returns a string containing the first [n] characters from this string, or the entire string if this string is shorter."}
        body(Strings) {
            """
            require(n >= 0, { "Requested character count $n is less than zero." })
            return substring(0, Math.min(n, length()))
            """
        }
        returns(Strings) { "String" }

        doc(CharSequences) { "Returns a char sequence containing the first [n] characters from this char sequence, or the entire char sequence if this char sequence is shorter."}
        body(CharSequences) {
            """
            require(n >= 0, { "Requested character count $n is less than zero." })
            return subSequence(0, Math.min(n, length()))
            """
        }
        returns(CharSequences) { "CharSequence" }

        doc(Sequences) { "Returns a sequence containing first [n] elements." }
        returns(Sequences) { "Sequence<T>" }
        body(Sequences) {
            """
            require(n >= 0, { "Requested element count $n is less than zero." })
            return if (n == 0) emptySequence() else TakeSequence(this, n)
            """
        }

        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            require(n >= 0) { "Requested element count $n is less than zero." }
            if (n == 0) return emptyList()
            if (n >= size()) return toList()
            var count = 0
            val list = ArrayList<T>(n)
            for (item in this) {
                if (count++ == n)
                    break;
                list.add(item)
            }
            return list
            """
        }
    }

    templates add f("dropLast(n: Int)") {
        val n = "\$n"
        only(Lists, ArraysOfObjects, ArraysOfPrimitives, Strings, CharSequences)

        doc { "Returns a list containing all elements except last [n] elements." }
        returns("List<T>")
        body {
            """
            require(n >= 0, { "Requested element count $n is less than zero." })
            return take((size() - n).coerceAtLeast(0))
            """
        }

        doc(Strings) { "Returns a string with the last [n] characters removed." }
        returns("String", Strings)
        body(Strings) {
            """
            require(n >= 0, { "Requested character count $n is less than zero." })
            return take((length() - n).coerceAtLeast(0))
            """
        }

        doc(CharSequences) { "Returns a char sequence with the last [n] characters removed." }
        returns("CharSequence", CharSequences)
        body(CharSequences) {
            """
            require(n >= 0, { "Requested character count $n is less than zero." })
            return take((length() - n).coerceAtLeast(0))
            """
        }
    }

    templates add f("takeLast(n: Int)") {
        val n = "\$n"
        doc { "Returns a list containing last [n] elements." }
        only(Lists, ArraysOfObjects, ArraysOfPrimitives, Strings, CharSequences)
        returns("List<T>")

        doc(Strings) { "Returns a string containing the last [n] characters from this string, or the entire string if this string is shorter."}
        body(Strings) {
            """
            require(n >= 0, { "Requested character count $n is less than zero." })
            val length = length()
            return substring(length - Math.min(n, length), length)
            """
        }
        returns(Strings) { "String" }

        doc(CharSequences) { "Returns a char sequence containing the last [n] characters from this char sequence, or the entire char sequence if this char sequence is shorter."}
        body(CharSequences) {
            """
            require(n >= 0, { "Requested character count $n is less than zero." })
            val length = length()
            return subSequence(length - Math.min(n, length), length)
            """
        }
        returns(CharSequences) { "CharSequence" }

        body(Lists, ArraysOfObjects, ArraysOfPrimitives) {
            """
            require(n >= 0, { "Requested element count $n is less than zero." })
            if (n == 0) return emptyList()
            val size = size()
            if (n >= size) return toList()
            val list = ArrayList<T>(n)
            for (index in size - n .. size - 1)
                list.add(this[index])
            return list
            """
        }
    }

    templates add f("dropWhile(predicate: (T) -> Boolean)") {
        inline(true)

        doc { "Returns a list containing all elements except first elements that satisfy the given [predicate]." }
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

        doc(Strings) { "Returns a string containing all characters except first characters that satisfy the given [predicate]." }
        returns(Strings) { "String" }
        body(Strings) {
            """
            return trimStart(predicate)
            """
        }

        inline(false, Sequences)
        doc(Sequences) { "Returns a sequence containing all elements except first elements that satisfy the given [predicate]." }
        returns(Sequences) { "Sequence<T>" }
        body(Sequences) {
            """
            return DropWhileSequence(this, predicate)
            """
        }

    }

    templates add f("takeWhile(predicate: (T) -> Boolean)") {
        inline(true)

        doc { "Returns a list containing first elements satisfying the given [predicate]." }
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

        include(Strings)
        doc(Strings) { "Returns a string containing the first characters that satisfy the given [predicate]."}
        returns(Strings) { "String" }
        body(Strings) {
            """
            for (index in 0..length() - 1)
                if (!predicate(get(index))) {
                    return substring(0, index)
                }
            return this
            """
        }

        doc(CharSequences) { "Returns a char sequence containing the first characters that satisfy the given [predicate]."}
        returns(CharSequences) { "CharSequence" }
        body(CharSequences) {
            """
            for (index in 0..length() - 1)
                if (!predicate(get(index))) {
                    return subSequence(0, index)
                }
            return this
            """
        }

        inline(false, Sequences)
        doc(Sequences) { "Returns a sequence containing first elements satisfying the given [predicate]." }
        returns(Sequences) { "Sequence<T>" }
        body(Sequences) {
            """
            return TakeWhileSequence(this, predicate)
            """
        }
    }

    templates add f("dropLastWhile(predicate: (T) -> Boolean)") {
        inline(true)
        only(Lists, ArraysOfObjects, ArraysOfPrimitives, Strings)
        doc { "Returns a list containing all elements except last elements that satisfy the given [predicate]." }
        returns("List<T>")

        body {
            """
            for (index in lastIndex downTo 0) {
                if (!predicate(this[index])) {
                    return take(index + 1)
                }
            }
            return emptyList()
            """
        }

        doc(Strings) { "Returns a string containing all characters except last characters that satisfy the given [predicate]." }
        returns("String", Strings)
        body(Strings) {
            """
            return trimEnd(predicate)
            """
        }
    }

    templates add f("takeLastWhile(predicate: (T) -> Boolean)") {
        inline(true)
        only(Lists, ArraysOfObjects, ArraysOfPrimitives, Strings, CharSequences)
        doc { "Returns a list containing last elements satisfying the given [predicate]."}
        returns("List<T>")

        body {
            """
            for (index in lastIndex downTo 0) {
                if (!predicate(this[index])) {
                    return drop(index + 1)
                }
            }
            return toList()
            """
        }

        doc(Strings) { "Returns a string containing last characters that satisfy the given [predicate]." }
        returns("String", Strings)
        body(Strings) {
            """
            for (index in lastIndex downTo 0) {
                if (!predicate(this[index])) {
                    return substring(index + 1)
                }
            }
            return this
            """
        }

        doc(CharSequences) { "Returns a char sequence containing last characters that satisfy the given [predicate]." }
        returns("CharSequence", CharSequences)
        body(CharSequences) {
            """
            for (index in lastIndex downTo 0) {
                if (!predicate(this[index])) {
                    return subSequence(index + 1)
                }
            }
            return this
            """
        }
    }

    templates add f("filter(predicate: (T) -> Boolean)") {
        inline(true)

        doc { "Returns a list containing all elements matching the given [predicate]." }
        returns("List<T>")
        body {
            """
            return filterTo(ArrayList<T>(), predicate)
            """
        }

        exclude(CharSequences)
        include(Strings)
        doc(Strings) { "Returns a string containing only those characters from the original string that match the given [predicate]." }
        returns(Strings) { "String" }
        body(Strings) {
            """
            return filterTo(StringBuilder(), predicate).toString()
            """
        }

        inline(false, Sequences)
        doc(Sequences) { "Returns a sequence containing all elements matching the given [predicate]." }
        returns(Sequences) { "Sequence<T>" }
        body(Sequences) {
            """
            return FilteringSequence(this, true, predicate)
            """
        }
    }

    templates add f("filterTo(destination: C, predicate: (T) -> Boolean)") {
        inline(true)

        doc { "Appends all elements matching the given [predicate] into the given [destination]." }
        typeParam("C : TCollection")
        returns("C")

        body {
            """
            for (element in this) if (predicate(element)) destination.add(element)
            return destination
            """
        }

        doc(CharSequences) { "Appends all characters matching the given [predicate] to the given [destination]." }
        body(CharSequences) {
            """
            for (index in 0..length() - 1) {
                val element = get(index)
                if (predicate(element)) destination.append(element)
            }
            return destination
            """
        }
    }

    templates add f("filterNot(predicate: (T) -> Boolean)") {
        inline(true)

        doc { "Returns a list containing all elements not matching the given [predicate]." }
        returns("List<T>")
        body {
            """
            return filterNotTo(ArrayList<T>(), predicate)
            """
        }

        exclude(CharSequences)
        include(Strings)
        doc(Strings) { "Returns a string containing only those characters from the original string that do not match the given [predicate]." }
        returns(Strings) { "String" }
        body(Strings) {
            """
            return filterNotTo(StringBuilder(), predicate).toString()
            """
        }

        inline(false, Sequences)
        doc(Sequences) { "Returns a sequence containing all elements not matching the given [predicate]." }
        returns(Sequences) { "Sequence<T>" }
        body(Sequences) {
            """
            return FilteringSequence(this, false, predicate)
            """
        }
    }

    templates add f("filterNotTo(destination: C, predicate: (T) -> Boolean)") {
        inline(true)

        doc { "Appends all elements not matching the given [predicate] to the given [destination]." }
        typeParam("C : TCollection")
        returns("C")

        body {
            """
            for (element in this) if (!predicate(element)) destination.add(element)
            return destination
            """
        }

        doc(CharSequences) { "Appends all characters not matching the given [predicate] to the given [destination]." }
        body(CharSequences) {
            """
            for (element in this) if (!predicate(element)) destination.append(element)
            return destination
            """
        }
    }

    templates add f("filterNotNull()") {
        exclude(ArraysOfPrimitives, CharSequences)
        doc { "Returns a list containing all elements that are not `null`." }
        typeParam("T : Any")
        returns("List<T>")
        toNullableT = true
        body {
            """
            return filterNotNullTo(ArrayList<T>())
            """
        }

        doc(Sequences) { "Returns a sequence containing all elements that are not `null`." }
        returns(Sequences) { "Sequence<T>" }
        body(Sequences) {
            """
            return filterNot { it == null } as Sequence<T>
            """
        }
    }

    templates add f("filterNotNullTo(destination: C)") {
        exclude(ArraysOfPrimitives, CharSequences)
        doc { "Appends all elements that are not `null` to the given [destination]." }
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
        only(CharSequences, Lists, ArraysOfPrimitives, ArraysOfObjects)
        doc { "Returns a list containing elements at specified [indices]." }
        returns("List<T>")
        body {
            """
            val size = indices.collectionSizeOrDefault(10)
            if (size == 0) return listOf()
            val list = ArrayList<T>(size)
            for (index in indices) {
                list.add(get(index))
            }
            return list
            """
        }

        doc(CharSequences) { "Returns a string containing characters at specified [indices]." }
        returns(CharSequences) { "String" }
        body(CharSequences) {
            """
            val size = indices.collectionSizeOrDefault(10)
            if (size == 0) return ""
            val result = StringBuilder(size)
            for (i in indices) {
                result.append(get(i))
            }
            return result.toString()
            """
        }
    }

    templates add f("slice(indices: IntRange)") {
        only(CharSequences, Strings, Lists, ArraysOfPrimitives, ArraysOfObjects)
        doc { "Returns a list containing elements at indices in the specified [indices] range." }
        returns("List<T>")
        body(Lists) {
            """
            if (indices.isEmpty()) return listOf()
            return this.subList(indices.start, indices.end + 1).toList()
            """
        }
        body(ArraysOfPrimitives, ArraysOfObjects) {
            """
            if (indices.isEmpty()) return listOf()
            return copyOfRange(indices.start, indices.end + 1).asList()
            """
        }

        doc(Strings) { "Returns a string containing characters at indices at the specified [indices]." }
        returns(Strings) { "String" }
        body(Strings) {
            """
            if (indices.isEmpty()) return ""
            return substring(indices)
            """
        }

        doc(CharSequences) { "Returns a char sequence containing characters at indices at the specified [indices]." }
        returns(CharSequences) { "CharSequence" }
        body(CharSequences) {
            """
            if (indices.isEmpty()) return ""
            return subSequence(indices)
            """
        }
    }

    templates add f("sliceArray(indices: Collection<Int>)") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns an array containing elements of this array at specified [indices]." }
        returns("SELF")
        body(ArraysOfObjects) {
            """
            val result = arrayOfNulls(this, indices.size()) as Array<T>
            var targetIndex = 0
            for (sourceIndex in indices) {
                result[targetIndex++] = this[sourceIndex]
            }
            return result
            """
        }
        body(ArraysOfPrimitives) {
            """
            val result = SELF(indices.size())
            var targetIndex = 0
            for (sourceIndex in indices) {
                result[targetIndex++] = this[sourceIndex]
            }
            return result
            """
        }
    }

    templates add f("sliceArray(indices: IntRange)") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns a list containing elements at indices in the specified [indices] range." }
        returns("SELF")
        body(ArraysOfObjects) {
            """
            if (indices.isEmpty()) return copyOfRange(0, 0)
            return copyOfRange(indices.start, indices.end + 1)
            """
        }
        body(ArraysOfPrimitives) {
            """
            if (indices.isEmpty()) return SELF(0)
            return copyOfRange(indices.start, indices.end + 1)
            """
        }
    }

    return templates
}
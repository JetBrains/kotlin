package templates

import templates.Family.*

fun elements(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("contains(element: T)") {
        operator(true)

        doc { "Returns `true` if [element] is found in the collection." }
        returns("Boolean")
        body {
            """
            if (this is Collection)
                return contains(element)
            return indexOf(element) >= 0
            """
        }
        exclude(Strings, Lists, Collections)
        body(ArraysOfPrimitives, ArraysOfObjects, Sequences) {
            """
            return indexOf(element) >= 0
            """
        }
    }

    templates add f("indexOf(element: T)") {
        exclude(Strings, Lists) // has native implementation
        doc { "Returns first index of [element], or -1 if the collection does not contain element." }
        returns("Int")
        body {
            """
            var index = 0
            for (item in this) {
                if (element == item)
                    return index
                index++
            }
            return -1
            """
        }

        body(ArraysOfObjects) {
            """
            if (element == null) {
                for (index in indices) {
                    if (this[index] == null) {
                        return index
                    }
                }
            } else {
                for (index in indices) {
                    if (element == this[index]) {
                        return index
                    }
                }
            }
            return -1
           """
        }
        body(ArraysOfPrimitives) {
            """
            for (index in indices) {
                if (element == this[index]) {
                    return index
                }
            }
            return -1
           """
        }
    }

    templates add f("lastIndexOf(element: T)") {
        exclude(Strings, Lists) // has native implementation
        doc { "Returns last index of [element], or -1 if the collection does not contain element." }
        returns("Int")
        body {
            """
            var lastIndex = -1
            var index = 0
            for (item in this) {
                if (element == item)
                    lastIndex = index
                index++
            }
            return lastIndex
            """
        }

        body(ArraysOfObjects) {
            """
            if (element == null) {
                for (index in indices.reversed()) {
                    if (this[index] == null) {
                        return index
                    }
                }
            } else {
                for (index in indices.reversed()) {
                    if (element == this[index]) {
                        return index
                    }
                }
            }
            return -1
           """
        }
        body(ArraysOfPrimitives) {
            """
            for (index in indices.reversed()) {
                if (element == this[index]) {
                    return index
                }
            }
            return -1
           """
        }
    }

    templates add f("indexOfFirst(predicate: (T) -> Boolean)") {
        inline(true)

        doc { "Returns index of the first element matching the given [predicate], or -1 if the collection does not contain such element." }
        returns("Int")
        body {
            """
            var index = 0
            for (item in this) {
                if (predicate(item))
                    return index
                index++
            }
            return -1
            """
        }

        body(Lists, Strings, ArraysOfPrimitives, ArraysOfObjects) {
            """
            for (index in indices) {
                if (predicate(this[index])) {
                    return index
                }
            }
            return -1
            """
        }
    }

    templates add f("indexOfLast(predicate: (T) -> Boolean)") {
        inline(true)

        doc { "Returns index of the last element matching the given [predicate], or -1 if the collection does not contain such element." }
        returns("Int")
        body {
            """
            var lastIndex = -1
            var index = 0
            for (item in this) {
                if (predicate(item))
                    lastIndex = index
                index++
            }
            return lastIndex
            """
        }

        body(Lists, Strings, ArraysOfPrimitives, ArraysOfObjects) {
            """
            for (index in indices.reversed()) {
                if (predicate(this[index])) {
                    return index
                }
            }
            return -1
            """
        }
    }

    templates add f("elementAt(index: Int)") {
        val index = '$' + "index"
        doc { "Returns an element at the given [index] or throws an [IndexOutOfBoundsException] if the [index] is out of bounds of this collection." }
        returns("T")
        body {
            """
            if (this is List)
                return get(index)

            return elementAtOrElse(index) { throw IndexOutOfBoundsException("Collection doesn't contain element at index $index.") }
            """
        }
        body(Sequences) {
            """
            return elementAtOrElse(index) { throw IndexOutOfBoundsException("Sequence doesn't contain element at index $index.") }
            """
        }
        body(Strings, Lists, ArraysOfObjects, ArraysOfPrimitives) {
            """
            return get(index)
            """
        }
    }

    templates add f("elementAtOrElse(index: Int, defaultValue: (Int) -> T)") {
        doc { "Returns an element at the given [index] or the result of calling the [defaultValue] function if the [index] is out of bounds of this collection." }
        returns("T")
        body {
            """
            if (this is List)
                return this.getOrElse(index, defaultValue)
            if (index < 0)
                return defaultValue(index)
            val iterator = iterator()
            var count = 0
            while (iterator.hasNext()) {
                val element = iterator.next()
                if (index == count++)
                    return element
            }
            return defaultValue(index)
            """
        }
        body(Sequences) {
            """
            if (index < 0)
                return defaultValue(index)
            val iterator = iterator()
            var count = 0
            while (iterator.hasNext()) {
                val element = iterator.next()
                if (index == count++)
                    return element
            }
            return defaultValue(index)
            """
        }
        inline(true, Strings, Lists, ArraysOfObjects, ArraysOfPrimitives)
        body(Strings, Lists, ArraysOfObjects, ArraysOfPrimitives) {
            """
            return if (index >= 0 && index <= lastIndex) get(index) else defaultValue(index)
            """
        }
    }

    templates add f("getOrElse(index: Int, defaultValue: (Int) -> T)") {
        doc { "Returns an element at the given [index] or the result of calling the [defaultValue] function if the [index] is out of bounds of this collection." }
        returns("T")
        inline(true)
        only(Strings, Lists, ArraysOfObjects, ArraysOfPrimitives)
        body {
            """
            return if (index >= 0 && index <= lastIndex) get(index) else defaultValue(index)
            """
        }
    }


    templates add f("elementAtOrNull(index: Int)") {
        doc { "Returns an element at the given [index] or `null` if the [index] is out of bounds of this collection." }
        returns("T?")
        body {
            """
            if (this is List)
                return this.getOrNull(index)
            if (index < 0)
                return null
            val iterator = iterator()
            var count = 0
            while (iterator.hasNext()) {
                val element = iterator.next()
                if (index == count++)
                    return element
            }
            return null
            """
        }
        body(Sequences) {
            """
            if (index < 0)
                return null
            val iterator = iterator()
            var count = 0
            while (iterator.hasNext()) {
                val element = iterator.next()
                if (index == count++)
                    return element
            }
            return null
            """
        }
        body(Strings, Lists, ArraysOfObjects, ArraysOfPrimitives) {
            """
            return if (index >= 0 && index <= lastIndex) get(index) else null
            """
        }
    }

    templates add f("getOrNull(index: Int)") {
        doc { "Returns an element at the given [index] or `null` if the [index] is out of bounds of this collection." }
        returns("T?")
        only(Strings, Lists, ArraysOfObjects, ArraysOfPrimitives)
        body {
            """
            return if (index >= 0 && index <= lastIndex) get(index) else null
            """
        }
    }


    templates add f("first()") {
        doc { """Returns first element.
        @throws [NoSuchElementException] if the collection is empty.
        """ }
        doc(Strings) { """Returns first character.
        @throws [NoSuchElementException] if the string is empty.
        """ }
        returns("T")
        body {
            """
            when (this) {
                is List -> {
                    if (isEmpty())
                        throw NoSuchElementException("Collection is empty.")
                    else
                        return this[0]
                }
                else -> {
                    val iterator = iterator()
                    if (!iterator.hasNext())
                        throw NoSuchElementException("Collection is empty.")
                    return iterator.next()
                }
            }
            """
        }
        body(Strings, Lists, ArraysOfObjects, ArraysOfPrimitives) {
            """
            if (isEmpty())
                throw NoSuchElementException("Collection is empty.")
            return this[0]
            """
        }
        body(Sequences) {
            """
            val iterator = iterator()
            if (!iterator.hasNext())
                throw NoSuchElementException("Sequence is empty.")
            return iterator.next()
            """
        }
    }
    templates add f("firstOrNull()") {
        doc { "Returns the first element, or `null` if the collection is empty." }
        doc(Strings) { "Returns the first character, or `null` if string is empty." }
        returns("T?")
        body {
            """
            when (this) {
                is List -> {
                    if (isEmpty())
                        return null
                    else
                        return this[0]
                }
                else -> {
                    val iterator = iterator()
                    if (!iterator.hasNext())
                        return null
                    return iterator.next()
                }
            }
            """
        }
        body(Strings, Lists, ArraysOfObjects, ArraysOfPrimitives) {
            """
            return if (isEmpty()) null else this[0]
            """
        }
        body(Sequences) {
            """
            val iterator = iterator()
            if (!iterator.hasNext())
                return null
            return iterator.next()
            """
        }
    }

    templates add f("first(predicate: (T) -> Boolean)") {
        inline(true)

        doc { """Returns the first element matching the given [predicate].
        @throws [NoSuchElementException] if no such element is found.""" }
        doc(Strings) { """Returns the first character matching the given [predicate].
        @throws [NoSuchElementException] if no such character is found.""" }
        returns("T")
        body {
            """
            for (element in this) if (predicate(element)) return element
            throw NoSuchElementException("No element matching predicate was found.")
            """
        }
    }

    templates add f("firstOrNull(predicate: (T) -> Boolean)") {
        inline(true)

        doc { "Returns the first element matching the given [predicate], or `null` if element was not found." }
        doc(Strings) { "Returns the first character matching the given [predicate], or `null` if character was not found." }
        returns("T?")
        body {
            """
            for (element in this) if (predicate(element)) return element
            return null
            """
        }
    }

    templates add f("find(predicate: (T) -> Boolean)") {
        inline(true)
        doc { "Returns the first element matching the given [predicate], or `null` if element was not found." }
        doc(Strings) { "Returns the first character matching the given [predicate], or `null` if character was not found." }
        returns("T?")
        body { "return firstOrNull(predicate)"}
    }

    templates add f("last()") {
        doc { """Returns the last element.
        @throws [NoSuchElementException] if the collection is empty.""" }
        doc(Strings) { """"Returns the last character.
        @throws [NoSuchElementException] if the string is empty.""" }
        returns("T")
        body {
            """
            when (this) {
                is List -> {
                    if (isEmpty())
                        throw NoSuchElementException("Collection is empty.")
                    else
                        return this[this.lastIndex]
                }
                else -> {
                    val iterator = iterator()
                    if (!iterator.hasNext())
                        throw NoSuchElementException("Collection is empty.")
                    var last = iterator.next()
                    while (iterator.hasNext())
                        last = iterator.next()
                    return last
                }
            }
            """
        }
        body(Sequences) {
            """
            val iterator = iterator()
            if (!iterator.hasNext())
                throw NoSuchElementException("Sequence is empty.")
            var last = iterator.next()
            while (iterator.hasNext())
                last = iterator.next()
            return last
            """
        }
        body(Strings, Lists, ArraysOfObjects, ArraysOfPrimitives) {
            """
            if (isEmpty())
                throw NoSuchElementException("Collection is empty.")
            return this[lastIndex]
            """
        }
    }

    templates add f("lastOrNull()") {
        doc { "Returns the last element, or `null` if the collection is empty." }
        doc(Strings) { "Returns the last character, or `null` if the string is empty." }
        returns("T?")
        body {
            """
            when (this) {
                is List -> return if (isEmpty()) null else this[size() - 1]
                else -> {
                    val iterator = iterator()
                    if (!iterator.hasNext())
                        return null
                    var last = iterator.next()
                    while (iterator.hasNext())
                        last = iterator.next()
                    return last
                }
            }
            """
        }
        body(Sequences) {
            """
            val iterator = iterator()
            if (!iterator.hasNext())
                return null
            var last = iterator.next()
            while (iterator.hasNext())
                last = iterator.next()
            return last
            """
        }
        body(Strings) {
            """
            return if (isEmpty()) null else this[length() - 1]
            """
        }
        body(Lists, ArraysOfObjects, ArraysOfPrimitives) {
            """
            return if (isEmpty()) null else this[size() - 1]
            """
        }
    }

    templates add f("last(predicate: (T) -> Boolean)") {
        inline(true)
        doc { """Returns the last element matching the given [predicate].
        @throws [NoSuchElementException] if no such element is found.""" }
        doc(Strings) { """"Returns the last character matching the given [predicate].
        @throws [NoSuchElementException] if no such character is found.""" }
        returns("T")
        body {
            """
            var last: T? = null
            var found = false
            for (element in this) {
                if (predicate(element)) {
                    last = element
                    found = true
                }
            }
            if (!found) throw NoSuchElementException("Collection doesn't contain any element matching the predicate.")
            return last as T
            """
        }

        body(Iterables) {
            """
            if (this is List)
                return this.last(predicate)

            var last: T? = null
            var found = false
            for (element in this) {
                if (predicate(element)) {
                    last = element
                    found = true
                }
            }
            if (!found) throw NoSuchElementException("Collection doesn't contain any element matching the predicate.")
            return last as T
            """
        }

        body(ArraysOfPrimitives, ArraysOfObjects, Lists) {
            """
            for (index in this.indices.reversed()) {
                val element = this[index]
                if (predicate(element)) return element
            }
            throw NoSuchElementException("Collection doesn't contain any element matching the predicate.")
            """
        }
    }

    templates add f("lastOrNull(predicate: (T) -> Boolean)") {
        inline(true)
        doc { "Returns the last element matching the given [predicate], or `null` if no such element was found." }
        doc(Strings) { "Returns the last character matching the given [predicate], or `null` if no such character was found." }
        returns("T?")
        body {
            """
            var last: T? = null
            for (element in this) {
                if (predicate(element)) {
                    last = element
                }
            }
            return last
            """
        }

        body(Iterables) {
            """
            if (this is List)
                return this.lastOrNull(predicate)

            var last: T? = null
            for (element in this) {
                if (predicate(element)) {
                    last = element
                }
            }
            return last
            """
        }



        body(ArraysOfPrimitives, ArraysOfObjects, Lists) {
            """
            for (index in this.indices.reversed()) {
                val element = this[index]
                if (predicate(element)) return element
            }
            return null
            """
        }
    }

    templates add f("findLast(predicate: (T) -> Boolean)") {
        inline(true)
        include(Lists)
        doc { "Returns the last element matching the given [predicate], or `null` if no such element was found." }
        doc(Strings) { "Returns the last character matching the given [predicate], or `null` if no such character was found." }
        returns("T?")
        body { "return lastOrNull(predicate)"}
    }

    templates add f("single()") {
        doc { "Returns the single element, or throws an exception if the collection is empty or has more than one element." }
        doc(Strings) { "Returns the single character, or throws an exception if the string is empty or has more than one character." }
        returns("T")
        body {
            """
            when (this) {
                is List -> return when (size()) {
                    0 -> throw NoSuchElementException("Collection is empty.")
                    1 -> this[0]
                    else -> throw IllegalArgumentException("Collection has more than one element.")
                }
                else -> {
                    val iterator = iterator()
                    if (!iterator.hasNext())
                        throw NoSuchElementException("Collection is empty.")
                    var single = iterator.next()
                    if (iterator.hasNext())
                        throw IllegalArgumentException("Collection has more than one element.")
                    return single
                }
            }
            """
        }
        body(Sequences) {
            """
            val iterator = iterator()
            if (!iterator.hasNext())
                throw NoSuchElementException("Sequence is empty.")
            var single = iterator.next()
            if (iterator.hasNext())
                throw IllegalArgumentException("Sequence has more than one element.")
            return single
            """
        }
        body(Strings) {
            """
            return when (length()) {
                0 -> throw NoSuchElementException("Collection is empty.")
                1 -> this[0]
                else -> throw IllegalArgumentException("Collection has more than one element.")
            }
            """
        }
        body(Lists, ArraysOfObjects, ArraysOfPrimitives) {
            """
            return when (size()) {
                0 -> throw NoSuchElementException("Collection is empty.")
                1 -> this[0]
                else -> throw IllegalArgumentException("Collection has more than one element.")
            }
            """
        }
    }

    templates add f("singleOrNull()") {
        doc { "Returns single element, or `null` if the collection is empty or has more than one element." }
        doc(Strings) { "Returns the single character, or `null` if the string is empty or has more than one character." }
        returns("T?")
        body {
            """
            when (this) {
                is List -> return if (size() == 1) this[0] else null
                else -> {
                    val iterator = iterator()
                    if (!iterator.hasNext())
                        return null
                    var single = iterator.next()
                    if (iterator.hasNext())
                        return null
                    return single
                }
            }
            """
        }
        body(Sequences) {
            """
            val iterator = iterator()
            if (!iterator.hasNext())
                return null
            var single = iterator.next()
            if (iterator.hasNext())
                return null
            return single
            """
        }
        body(Strings) {
            """
            return if (length() == 1) this[0] else null
            """
        }
        body(Lists, ArraysOfObjects, ArraysOfPrimitives) {
            """
            return if (size() == 1) this[0] else null
            """
        }
    }

    templates add f("single(predicate: (T) -> Boolean)") {
        inline(true)
        doc { "Returns the single element matching the given [predicate], or throws exception if there is no or more than one matching element." }
        doc(Strings) { "Returns the single character matching the given [predicate], or throws exception if there is no or more than one matching character." }
        returns("T")
        body {
            """
            var single: T? = null
            var found = false
            for (element in this) {
                if (predicate(element)) {
                    if (found) throw IllegalArgumentException("Collection contains more than one matching element.")
                    single = element
                    found = true
                }
            }
            if (!found) throw NoSuchElementException("Collection doesn't contain any element matching predicate.")
            return single as T
            """
        }
    }

    templates add f("singleOrNull(predicate: (T) -> Boolean)") {
        inline(true)
        doc { "Returns the single element matching the given [predicate], or `null` if element was not found or more than one element was found." }
        doc(Strings) { "Returns the single character matching the given [predicate], or `null` if character was not found or more than one character was found." }
        returns("T?")
        body {
            """
            var single: T? = null
            var found = false
            for (element in this) {
                if (predicate(element)) {
                    if (found) return null
                    single = element
                    found = true
                }
            }
            if (!found) return null
            return single
            """
        }
    }

    templates addAll (1..5).map { n ->
        f("component$n()") {
            operator(true)
            inline(true)
            annotations("""@Suppress("NOTHING_TO_INLINE")""")
            fun getOrdinal(n: Int) = n.toString() + when (n) {
                1 -> "st"
                2 -> "nd"
                3 -> "rd"
                else -> "th"
            }
            doc { "Returns ${getOrdinal(n)} *element* from the collection." }
            returns("T")
            body { "return get(${n-1})" }
            only(Lists, ArraysOfObjects, ArraysOfPrimitives)
        }
    }

    return templates
}

package templates

import templates.Family.*

fun elements(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("contains(element: T)") {
        doc { "Returns true if *element* is found in the collection" }
        returns("Boolean")
        body {
            """
            if (this is Collection<*>)
                return contains(element)
            return indexOf(element) >= 0
            """
        }
        exclude(Strings, Lists, Collections)
        body(ArraysOfPrimitives, ArraysOfObjects) {
            """
            return indexOf(element) >= 0
            """
        }
    }

    templates add f("indexOf(element: T)") {
        exclude(Strings, Lists) // has native implementation
        doc { "Returns first index of [element], or -1 if the collection does not contain element" }
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
        doc { "Returns last index of *element*, or -1 if the collection does not contain element" }
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
                for (index in indices.reverse()) {
                    if (this[index] == null) {
                        return index
                    }
                }
            } else {
                for (index in indices.reverse()) {
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
            for (index in indices.reverse()) {
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

        doc { "Returns index of the first element matching the given [predicate], or -1 if the collection does not contain such element" }
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

        doc { "Returns index of the last element matching the given [predicate], or -1 if the collection does not contain such element" }
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
        doc { "Returns element at given *index*" }
        returns("T")
        body {
            """
            if (this is List<*>)
                return get(index) as T
            val iterator = iterator()
            var count = 0
            while (iterator.hasNext()) {
                val element = iterator.next()
                if (index == count++)
                    return element
            }
            throw IndexOutOfBoundsException("Collection doesn't contain element at index")
            """
        }
        body(Streams) {
            """
            val iterator = iterator()
            var count = 0
            while (iterator.hasNext()) {
                val element = iterator.next()
                if (index == count++)
                    return element
            }
            throw IndexOutOfBoundsException("Collection doesn't contain element at index")
            """
        }
        body(Strings, Lists, ArraysOfObjects, ArraysOfPrimitives) {
            """
            return get(index)
            """
        }
    }

    templates add f("first()") {
        doc { """Returns first element.
        @throws NoSuchElementException if the collection is empty.
        """ }
        doc(Strings) { """Returns first character.
        @throws NoSuchElementException if the string is empty.
        """ }
        returns("T")
        body {
            """
            when (this) {
                is List<*> -> {
                    if (isEmpty())
                        throw NoSuchElementException("Collection is empty")
                    else
                        return this[0] as T
                }
                else -> {
                    val iterator = iterator()
                    if (!iterator.hasNext())
                        throw NoSuchElementException("Collection is empty")
                    return iterator.next()
                }
            }
            """
        }
        body(Strings, Lists, ArraysOfObjects, ArraysOfPrimitives) {
            """
            if (isEmpty())
                throw NoSuchElementException("Collection is empty")
            return this[0]
            """
        }
    }
    templates add f("firstOrNull()") {
        doc { "Returns the first element, or null if the collection is empty." }
        doc(Strings) { "Returns the first character, or null if string is empty." }
        returns("T?")
        body {
            """
            when (this) {
                is List<*> -> {
                    if (isEmpty())
                        return null
                    else
                        return this[0] as T
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
    }

    templates add f("first(predicate: (T) -> Boolean)") {
        inline(true)

        doc { """"Returns the first element matching the given [predicate].
        @throws NoSuchElementException if no such element is found.""" }
        doc(Strings) { """Returns the first character matching the given [predicate].
        @throws NoSuchElementException if no such character is found.""" }
        returns("T")
        body {
            """
            for (element in this) if (predicate(element)) return element
            throw NoSuchElementException("No element matching predicate was found")
            """
        }
    }

    templates add f("firstOrNull(predicate: (T) -> Boolean)") {
        inline(true)

        doc { "Returns first element matching the given [predicate], or `null` if element was not found" }
        doc(Strings) { "Returns first character matching the given [predicate], or `null` if character was not found" }
        returns("T?")
        body {
            """
            for (element in this) if (predicate(element)) return element
            return null
            """
        }
    }

    templates add f("last()") {
        doc { """Returns the last element.
        @throws NoSuchElementException if the collection is empty.""" }
        doc(Strings) { """"Returns the last character.
        @throws NoSuchElementException if the string is empty.""" }
        returns("T")
        body {
            """
            when (this) {
                is List<*> -> {
                    if (isEmpty())
                        throw NoSuchElementException("Collection is empty")
                    else
                        return this[this.lastIndex] as T
                }
                else -> {
                    val iterator = iterator()
                    if (!iterator.hasNext())
                        throw NoSuchElementException("Collection is empty")
                    var last = iterator.next()
                    while (iterator.hasNext())
                        last = iterator.next()
                    return last
                }
            }
            """
        }
        body(Streams) {
            """
            val iterator = iterator()
            if (!iterator.hasNext())
                throw NoSuchElementException("Collection is empty")
            var last = iterator.next()
            while (iterator.hasNext())
                last = iterator.next()
            return last
            """
        }
        body(Strings, Lists, ArraysOfObjects, ArraysOfPrimitives) {
            """
            if (isEmpty())
                throw NoSuchElementException("Collection is empty")
            return this[lastIndex]
            """
        }
    }

    templates add f("lastOrNull()") {
        doc { "Returns the last element, or `null` if the collection is empty" }
        doc(Strings) { "Returns the last character, or `null` if the string is empty" }
        returns("T?")
        body {
            """
            when (this) {
                is List<*> -> return if (isEmpty()) null else this[size() - 1] as T
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
        body(Streams) {
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
        @throws NoSuchElementException if no such element is found.""" }
        doc(Strings) { """"Returns the last character matching the given [predicate].
        @throws NoSuchElementException if no such character is found.""" }
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
            if (!found) throw NoSuchElementException("Collection doesn't contain any element matching predicate")
            return last as T
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
    }

    templates add f("single()") {
        doc { "Returns the single element, or throws an exception if the collection is empty or has more than one element." }
        doc(Strings) { "Returns the single character, or throws an exception if the string is empty or has more than one character." }
        returns("T")
        body {
            """
            when (this) {
                is List<*> -> return when (size()) {
                    0 -> throw NoSuchElementException("Collection is empty")
                    1 -> this[0] as T
                    else -> throw IllegalArgumentException("Collection has more than one element")
                }
                else -> {
                    val iterator = iterator()
                    if (!iterator.hasNext())
                        throw NoSuchElementException("Collection is empty")
                    var single = iterator.next()
                    if (iterator.hasNext())
                        throw IllegalArgumentException("Collection has more than one element")
                    return single
                }
            }
            """
        }
        body(Strings) {
            """
            return when (length()) {
                0 -> throw NoSuchElementException("Collection is empty")
                1 -> this[0]
                else -> throw IllegalArgumentException("Collection has more than one element")
            }
            """
        }
        body(Lists, ArraysOfObjects, ArraysOfPrimitives) {
            """
            return when (size()) {
                0 -> throw NoSuchElementException("Collection is empty")
                1 -> this[0]
                else -> throw IllegalArgumentException("Collection has more than one element")
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
                is List<*> -> return if (size() == 1) this[0] as T else null
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
        doc { "Returns the single element matching the given [predicate], or throws exception if there is no or more than one matching element" }
        doc(Strings) { "Returns the single character matching the given [predicate], or throws exception if there is no or more than one matching character" }
        returns("T")
        body {
            """
            var single: T? = null
            var found = false
            for (element in this) {
                if (predicate(element)) {
                    if (found) throw IllegalArgumentException("Collection contains more than one matching element")
                    single = element
                    found = true
                }
            }
            if (!found) throw NoSuchElementException("Collection doesn't contain any element matching predicate")
            return single as T
            """
        }
    }

    templates add f("singleOrNull(predicate: (T) -> Boolean)") {
        inline(true)
        doc { "Returns the single element matching the given [predicate], or `null` if element was not found or more than one element was found" }
        doc(Strings) { "Returns the single character matching the given [predicate], or `null` if character was not found or more than one character was found" }
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

    templates add f("component1()") {
        inline(true)
        doc { "Returns 1st *element* from the collection." }
        returns("T")
        body { "return get(0)" }
        only(Lists, ArraysOfObjects, ArraysOfPrimitives)
    }
    templates add f("component2()") {
        inline(true)
        doc { "Returns 2nd *element* from the collection." }
        returns("T")
        body { "return get(1)" }
        only(Lists, ArraysOfObjects, ArraysOfPrimitives)
    }
    templates add f("component3()") {
        inline(true)
        doc { "Returns 3rd *element* from the collection." }
        returns("T")
        body { "return get(2)" }
        only(Lists, ArraysOfObjects, ArraysOfPrimitives)
    }
    templates add f("component4()") {
        inline(true)
        doc { "Returns 4th *element* from the collection." }
        returns("T")
        body { "return get(3)" }
        only(Lists, ArraysOfObjects, ArraysOfPrimitives)
    }
    templates add f("component5()") {
        inline(true)
        doc { "Returns 5th *element* from the collection." }
        returns("T")
        body { "return get(4)" }
        only(Lists, ArraysOfObjects, ArraysOfPrimitives)
    }

    return templates
}

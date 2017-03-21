package templates

import templates.Family.*
import templates.SequenceClass.*

fun elements(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()


    templates add f("contains(element: T)") {
        operator(true)

        only(Iterables, Sequences, ArraysOfObjects, ArraysOfPrimitives)
        doc { f -> "Returns `true` if [element] is found in the ${f.collection}." }
        typeParam("@kotlin.internal.OnlyInputTypes T")
        returns("Boolean")
        body(Iterables) { f ->
            """
                if (this is Collection)
                    return contains(element)
                return indexOf(element) >= 0
            """
        }
        body(ArraysOfPrimitives, ArraysOfObjects, Sequences) {
            """
            return indexOf(element) >= 0
            """
        }
    }


    templates add f("indexOf(element: T)") {
        only(Iterables, Sequences, ArraysOfObjects, ArraysOfPrimitives, Lists)
        doc { f -> "Returns first index of [element], or -1 if the ${f.collection} does not contain element." }
        typeParam("@kotlin.internal.OnlyInputTypes T")
        returns("Int")
        body { f ->
            """
            ${if (f == Iterables) "if (this is List) return this.indexOf(element)" else ""}
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
        body(Lists) { "return indexOf(element)" }
    }

    templates add f("lastIndexOf(element: T)") {
        only(Iterables, Sequences, ArraysOfObjects, ArraysOfPrimitives, Lists)
        doc { f -> "Returns last index of [element], or -1 if the ${f.collection} does not contain element." }
        typeParam("@kotlin.internal.OnlyInputTypes T")
        returns("Int")
        body { f ->
            """
            ${if (f == Iterables) "if (this is List) return this.lastIndexOf(element)" else ""}
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
        body(Lists) { "return lastIndexOf(element)" }
    }

    templates add f("indexOfFirst(predicate: (T) -> Boolean)") {
        inline(true)

        include(Lists)
        doc { f -> "Returns index of the first ${f.element} matching the given [predicate], or -1 if the ${f.collection} does not contain such ${f.element}." }
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

        body(CharSequences, ArraysOfPrimitives, ArraysOfObjects) {
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

        doc { f -> "Returns index of the last ${f.element} matching the given [predicate], or -1 if the ${f.collection} does not contain such ${f.element}." }
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

        body(CharSequences, ArraysOfPrimitives, ArraysOfObjects) {
            """
            for (index in indices.reversed()) {
                if (predicate(this[index])) {
                    return index
                }
            }
            return -1
            """
        }
        body(Lists) {
            """
            val iterator = this.listIterator(size)
            while (iterator.hasPrevious()) {
                if (predicate(iterator.previous())) {
                    return iterator.nextIndex()
                }
            }
            return -1
            """
        }
    }

    templates add f("elementAt(index: Int)") {
        val index = '$' + "index"
        doc { f -> "Returns ${f.element.prefixWithArticle()} at the given [index] or throws an [IndexOutOfBoundsException] if the [index] is out of bounds of this ${f.collection}." }
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
        inline(CharSequences, Lists, ArraysOfObjects, ArraysOfPrimitives) { Inline.Only }
        body(CharSequences, Lists, ArraysOfObjects, ArraysOfPrimitives) {
            """
            return get(index)
            """
        }
    }

    templates add f("elementAtOrElse(index: Int, defaultValue: (Int) -> T)") {
        doc { f -> "Returns ${f.element.prefixWithArticle()} at the given [index] or the result of calling the [defaultValue] function if the [index] is out of bounds of this ${f.collection}." }
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
        inline(CharSequences, Lists, ArraysOfObjects, ArraysOfPrimitives) { Inline.Only }
        body(CharSequences, Lists, ArraysOfObjects, ArraysOfPrimitives) {
            """
            return if (index >= 0 && index <= lastIndex) get(index) else defaultValue(index)
            """
        }
    }

    templates add f("getOrElse(index: Int, defaultValue: (Int) -> T)") {
        doc { f -> "Returns ${f.element.prefixWithArticle()} at the given [index] or the result of calling the [defaultValue] function if the [index] is out of bounds of this ${f.collection}." }
        returns("T")
        inline(Inline.Only)
        only(CharSequences, Lists, ArraysOfObjects, ArraysOfPrimitives)
        body {
            """
            return if (index >= 0 && index <= lastIndex) get(index) else defaultValue(index)
            """
        }
    }


    templates add f("elementAtOrNull(index: Int)") {
        doc { f -> "Returns ${f.element.prefixWithArticle()} at the given [index] or `null` if the [index] is out of bounds of this ${f.collection}." }
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
        inline(CharSequences, Lists, ArraysOfObjects, ArraysOfPrimitives) { Inline.Only }
        body(CharSequences, Lists, ArraysOfObjects, ArraysOfPrimitives) {
            """
            return this.getOrNull(index)
            """
        }
    }

    templates add f("getOrNull(index: Int)") {
        doc { f -> "Returns ${f.element.prefixWithArticle()} at the given [index] or `null` if the [index] is out of bounds of this ${f.collection}." }
        returns("T?")
        only(CharSequences, Lists, ArraysOfObjects, ArraysOfPrimitives)
        body {
            """
            return if (index >= 0 && index <= lastIndex) get(index) else null
            """
        }
    }


    templates add f("first()") {
        doc { f -> """Returns first ${f.element}.
        @throws [NoSuchElementException] if the ${f.collection} is empty.
        """ }
        returns("T")
        body {
            """
            when (this) {
                is List -> return this.first()
                else -> {
                    val iterator = iterator()
                    if (!iterator.hasNext())
                        throw NoSuchElementException("Collection is empty.")
                    return iterator.next()
                }
            }
            """
        }
        body(CharSequences, Lists, ArraysOfObjects, ArraysOfPrimitives) { f ->
            """
            if (isEmpty())
                throw NoSuchElementException("${f.doc.collection.capitalize()} is empty.")
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
        doc { f -> "Returns the first ${f.element}, or `null` if the ${f.collection} is empty." }
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
        body(CharSequences, Lists, ArraysOfObjects, ArraysOfPrimitives) {
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

        include(CharSequences)
        doc { f -> """Returns the first ${f.element} matching the given [predicate].
        @throws [NoSuchElementException] if no such ${f.element} is found.""" }
        returns("T")
        body { f ->
            """
            for (element in this) if (predicate(element)) return element
            throw NoSuchElementException("${f.doc.collection.capitalize()} contains no ${f.doc.element} matching the predicate.")
            """
        }
    }

    templates add f("firstOrNull(predicate: (T) -> Boolean)") {
        inline(true)

        include(CharSequences)
        doc { f -> "Returns the first ${f.element} matching the given [predicate], or `null` if ${f.element} was not found." }
        returns("T?")
        body {
            """
            for (element in this) if (predicate(element)) return element
            return null
            """
        }
    }

    templates add f("find(predicate: (T) -> Boolean)") {
        inline(Inline.Only)
        include(CharSequences)
        doc { f -> "Returns the first ${f.element} matching the given [predicate], or `null` if no such ${f.element} was found." }
        returns("T?")
        body { "return firstOrNull(predicate)"}
    }

    templates add f("last()") {
        doc { f -> """Returns the last ${f.element}.
        @throws [NoSuchElementException] if the ${f.collection} is empty.""" }
        returns("T")
        body {
            """
            when (this) {
                is List -> return this.last()
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
        body(CharSequences, Lists, ArraysOfObjects, ArraysOfPrimitives) { f ->
            """
            if (isEmpty())
                throw NoSuchElementException("${f.doc.collection.capitalize()} is empty.")
            return this[lastIndex]
            """
        }
    }

    templates add f("lastOrNull()") {
        doc { f -> "Returns the last ${f.element}, or `null` if the ${f.collection} is empty." }
        returns("T?")
        body {
            """
            when (this) {
                is List -> return if (isEmpty()) null else this[size - 1]
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
        body(CharSequences) {
            """
            return if (isEmpty()) null else this[length - 1]
            """
        }
        body(Lists, ArraysOfObjects, ArraysOfPrimitives) {
            """
            return if (isEmpty()) null else this[size - 1]
            """
        }
    }

    templates add f("last(predicate: (T) -> Boolean)") {
        inline(true)

        include(CharSequences)
        doc { f -> """Returns the last ${f.element} matching the given [predicate].
        @throws [NoSuchElementException] if no such ${f.element} is found.""" }
        returns("T")
        body { f ->
            """
            var last: T? = null
            var found = false
            for (element in this) {
                if (predicate(element)) {
                    last = element
                    found = true
                }
            }
            if (!found) throw NoSuchElementException("${f.doc.collection.capitalize()} contains no ${f.doc.element} matching the predicate.")
            return last as T
            """
        }

        body(CharSequences, ArraysOfPrimitives, ArraysOfObjects) { f ->
            """
            for (index in this.indices.reversed()) {
                val element = this[index]
                if (predicate(element)) return element
            }
            throw NoSuchElementException("${f.doc.collection.capitalize()} contains no ${f.doc.element} matching the predicate.")
            """
        }
        body(Lists) { f ->
            """
            val iterator = this.listIterator(size)
            while (iterator.hasPrevious()) {
                val element = iterator.previous()
                if (predicate(element)) return element
            }
            throw NoSuchElementException("${f.doc.collection.capitalize()} contains no ${f.doc.element} matching the predicate.")
            """
        }
    }

    templates add f("lastOrNull(predicate: (T) -> Boolean)") {
        inline(true)
        include(CharSequences)
        doc { f -> "Returns the last ${f.element} matching the given [predicate], or `null` if no such ${f.element} was found." }
        returns("T?")
        body { f ->
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

        body(CharSequences, ArraysOfPrimitives, ArraysOfObjects) {
            """
            for (index in this.indices.reversed()) {
                val element = this[index]
                if (predicate(element)) return element
            }
            return null
            """
        }
        body(Lists) {
            """
            val iterator = this.listIterator(size)
            while (iterator.hasPrevious()) {
                val element = iterator.previous()
                if (predicate(element)) return element
            }
            return null
            """
        }

    }

    templates add f("findLast(predicate: (T) -> Boolean)") {
        inline(Inline.Only)
        include(Lists, CharSequences)
        doc { f -> "Returns the last ${f.element} matching the given [predicate], or `null` if no such ${f.element} was found." }
        returns("T?")
        body { "return lastOrNull(predicate)"}
    }

    templates add f("single()") {
        doc { f -> "Returns the single ${f.element}, or throws an exception if the ${f.collection} is empty or has more than one ${f.element}." }
        returns("T")
        body {
            """
            when (this) {
                is List -> return this.single()
                else -> {
                    val iterator = iterator()
                    if (!iterator.hasNext())
                        throw NoSuchElementException("Collection is empty.")
                    val single = iterator.next()
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
            val single = iterator.next()
            if (iterator.hasNext())
                throw IllegalArgumentException("Sequence has more than one element.")
            return single
            """
        }
        body(CharSequences, Lists, ArraysOfObjects, ArraysOfPrimitives) { f ->
            """
            return when (${f.code.size}) {
                0 -> throw NoSuchElementException("${f.doc.collection.capitalize()} is empty.")
                1 -> this[0]
                else -> throw IllegalArgumentException("${f.doc.collection.capitalize()} has more than one element.")
            }
            """
        }
    }

    templates add f("singleOrNull()") {
        doc { f -> "Returns single ${f.element}, or `null` if the ${f.collection} is empty or has more than one ${f.element}." }
        returns("T?")
        body {
            """
            when (this) {
                is List -> return if (size == 1) this[0] else null
                else -> {
                    val iterator = iterator()
                    if (!iterator.hasNext())
                        return null
                    val single = iterator.next()
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
            val single = iterator.next()
            if (iterator.hasNext())
                return null
            return single
            """
        }
        body(CharSequences) {
            """
            return if (length == 1) this[0] else null
            """
        }
        body(Lists, ArraysOfObjects, ArraysOfPrimitives) {
            """
            return if (size == 1) this[0] else null
            """
        }
    }

    templates add f("single(predicate: (T) -> Boolean)") {
        inline(true)
        include(CharSequences)
        doc { f -> "Returns the single ${f.element} matching the given [predicate], or throws exception if there is no or more than one matching ${f.element}." }
        returns("T")
        body { f ->
            """
            var single: T? = null
            var found = false
            for (element in this) {
                if (predicate(element)) {
                    if (found) throw IllegalArgumentException("${f.doc.collection.capitalize()} contains more than one matching element.")
                    single = element
                    found = true
                }
            }
            if (!found) throw NoSuchElementException("${f.doc.collection.capitalize()} contains no ${f.doc.element} matching the predicate.")
            return single as T
            """
        }
    }

    templates add f("singleOrNull(predicate: (T) -> Boolean)") {
        inline(true)
        include(CharSequences)
        doc { f -> "Returns the single ${f.element} matching the given [predicate], or `null` if ${f.element} was not found or more than one ${f.element} was found." }
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
            inline(Inline.Only)
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

    templates.forEach { it.sequenceClassification(terminal) }
    return templates
}

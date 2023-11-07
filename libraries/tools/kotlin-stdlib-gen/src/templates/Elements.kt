/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package templates

import templates.DocExtensions.collection
import templates.DocExtensions.element
import templates.Family.*
import templates.SequenceClass.*

object Elements : TemplateGroupBase() {

    init {
        defaultBuilder {
            sequenceClassification(terminal)
            specialFor(ArraysOfUnsigned) {
                sinceAtLeast("1.3")
                annotation("@ExperimentalUnsignedTypes")
            }
            specialFor(RangesOfPrimitives) {
                if (primitive in PrimitiveType.unsignedPrimitives) {
                    sinceAtLeast("1.5")
                    wasExperimental("ExperimentalUnsignedTypes")
                    sourceFile(SourceFile.URanges)
                }
            }
            specialFor(ProgressionsOfPrimitives) {
                if (primitive in PrimitiveType.unsignedPrimitives) {
                    sourceFile(SourceFile.URanges)
                }
            }
            if (since?.let { it <= "1.4" } == true) {
                wasExperimentalAnnotations.clear()
            }
        }
    }

    private fun floatingSearchDeprecationMessage(signature: String, replacement: String): String {
        return "The function has unclear behavior when searching for NaN or zero values and will be removed soon. Use '$replacement' instead to continue using this behavior, or '.asList().$signature' to get the same search behavior as in a list."
    }

    val f_contains = fn("contains(element: T)") {
        include(Iterables, Sequences, ArraysOfObjects, ArraysOfPrimitives)
    } builder {
        operator(true)

        doc { "Returns `true` if [element] is found in the ${f.collection}." }
        typeParam("@kotlin.internal.OnlyInputTypes T")
        val isDeprecated = f == ArraysOfPrimitives && primitive!!.isFloatingPoint()
        if (isDeprecated) {
            val replacement = "any { it == element }"
            val message = floatingSearchDeprecationMessage(signature, replacement)
            deprecate(Deprecation(message, replacement, warningSince = "1.4", errorSince = "1.6", hiddenSince = "1.7"))
        }
        returns("Boolean")
        body(Iterables) {
            """
            if (this is Collection)
                return contains(element)
            return indexOf(element) >= 0
            """
        }
        if (!isDeprecated)
        body(ArraysOfPrimitives, ArraysOfObjects, Sequences) {
            """
            return indexOf(element) >= 0
            """
        }
    }

    val f_indexOf = fn("indexOf(element: T)") {
        include(Iterables, Sequences, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned, Lists)
    } builder {
        doc { "Returns first index of [element], or -1 if the ${f.collection} does not contain element." }
        typeParam("@kotlin.internal.OnlyInputTypes T")
        specialFor(Lists) {
            annotation("""@Suppress("EXTENSION_SHADOWED_BY_MEMBER") // false warning, extension takes precedence in some cases""")
        }
        if (f == ArraysOfPrimitives && primitive!!.isFloatingPoint()) {
            val replacement = "indexOfFirst { it == element }"
            val message = floatingSearchDeprecationMessage(signature, replacement)
            deprecate(Deprecation(message, replacement, warningSince = "1.4", errorSince = "1.6", hiddenSince = "1.7"))
        }
        returns("Int")
        body {
            """
            ${if (f == Iterables) "if (this is List) return this.indexOf(element)" else ""}
            var index = 0
            for (item in this) {
                checkIndexOverflow(index)
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

        specialFor(ArraysOfUnsigned) {
            inlineOnly()
            val signedPrimitiveName = primitive!!.name.drop(1)
            body { "return storage.indexOf(element.to$signedPrimitiveName())" }
        }
    }

    val f_lastIndexOf = fn("lastIndexOf(element: T)") {
        include(Iterables, Sequences, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned, Lists)
    } builder {
        doc { "Returns last index of [element], or -1 if the ${f.collection} does not contain element." }
        typeParam("@kotlin.internal.OnlyInputTypes T")
        specialFor(Lists) {
            annotation("""@Suppress("EXTENSION_SHADOWED_BY_MEMBER") // false warning, extension takes precedence in some cases""")
        }
        if (f == ArraysOfPrimitives && primitive!!.isFloatingPoint()) {
            val replacement = "indexOfLast { it == element }"
            val message = floatingSearchDeprecationMessage(signature, replacement)
            deprecate(Deprecation(message, replacement, warningSince = "1.4", errorSince = "1.6", hiddenSince = "1.7"))
        }
        returns("Int")
        body {
            """
            ${if (f == Iterables) "if (this is List) return this.lastIndexOf(element)" else ""}
            var lastIndex = -1
            var index = 0
            for (item in this) {
                checkIndexOverflow(index)
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

        specialFor(ArraysOfUnsigned) {
            inlineOnly()
            val signedPrimitiveName = primitive!!.name.drop(1)
            body { "return storage.lastIndexOf(element.to$signedPrimitiveName())" }
        }
    }

    val f_indexOfFirst = fn("indexOfFirst(predicate: (T) -> Boolean)") {
        includeDefault()
        include(CharSequences, Lists, ArraysOfUnsigned)
    } builder {
        inline()

        doc { "Returns index of the first ${f.element} matching the given [predicate], or -1 if the ${f.collection} does not contain such ${f.element}." }
        returns("Int")
        body {
            """
            var index = 0
            for (item in this) {
                ${if (f != Lists) "checkIndexOverflow(index)" else ""}
                if (predicate(item))
                    return index
                index++
            }
            return -1
            """.lines().filterNot { it.isBlank() }.joinToString("\n")
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

        specialFor(ArraysOfUnsigned) {
            inlineOnly()
            body { "return storage.indexOfFirst { predicate(it.to${primitive!!.name}()) }" }
        }
    }

    val f_indexOfLast = fn("indexOfLast(predicate: (T) -> Boolean)") {
        includeDefault()
        include(CharSequences, Lists, ArraysOfUnsigned)
    } builder {
        inline()

        doc { "Returns index of the last ${f.element} matching the given [predicate], or -1 if the ${f.collection} does not contain such ${f.element}." }
        returns("Int")
        body {
            """
            var lastIndex = -1
            var index = 0
            for (item in this) {
                checkIndexOverflow(index)
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

        specialFor(ArraysOfUnsigned) {
            inlineOnly()
            body { "return storage.indexOfLast { predicate(it.to${primitive!!.name}()) }" }
        }
    }

    val f_elementAt = fn("elementAt(index: Int)") {
        includeDefault()
        include(CharSequences, Lists, ArraysOfUnsigned)
    } builder {
        val index = '$' + "index"
        doc { "Returns ${f.element.prefixWithArticle()} at the given [index] or throws an [IndexOutOfBoundsException] if the [index] is out of bounds of this ${f.collection}." }
        sample("samples.collections.Collections.Elements.elementAt")
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

        specialFor(CharSequences, Lists, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned) {
            inlineOnly()
            body { "return get(index)" }
        }

        specialFor(CharSequences, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned) {
            on(Platform.Common) {
                inline(Inline.No)
            }
            on(Platform.JS) {
                inline(Inline.No)

                val size = f.code.size
                body {
                    """
                    return elementAtOrElse(index) { throw IndexOutOfBoundsException("index: $index, $size: $$size}") }
                    """
                }
            }
        }
    }

    val f_elementAtOrElse = fn("elementAtOrElse(index: Int, defaultValue: (Int) -> T)") {
        includeDefault()
        include(CharSequences, Lists, ArraysOfUnsigned)
    } builder {
        doc { "Returns ${f.element.prefixWithArticle()} at the given [index] or the result of calling the [defaultValue] function if the [index] is out of bounds of this ${f.collection}." }
        sample("samples.collections.Collections.Elements.elementAtOrElse")
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
        specialFor(CharSequences, Lists, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned) {
            inlineOnly()
            body {
                """
                return if (index >= 0 && index <= lastIndex) get(index) else defaultValue(index)
                """
            }
        }
    }

    val f_getOrElse = fn("getOrElse(index: Int, defaultValue: (Int) -> T)") {
        include(CharSequences, Lists, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        doc { "Returns ${f.element.prefixWithArticle()} at the given [index] or the result of calling the [defaultValue] function if the [index] is out of bounds of this ${f.collection}." }
        returns("T")
        inlineOnly()
        body {
            """
            return if (index >= 0 && index <= lastIndex) get(index) else defaultValue(index)
            """
        }
    }

    val f_elementAtOrNull = fn("elementAtOrNull(index: Int)") {
        includeDefault()
        include(CharSequences, Lists, ArraysOfUnsigned)
    } builder {
        doc { "Returns ${f.element.prefixWithArticle()} at the given [index] or `null` if the [index] is out of bounds of this ${f.collection}." }
        sample("samples.collections.Collections.Elements.elementAtOrNull")
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
        specialFor(CharSequences, Lists, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned) {
            inlineOnly()
            body { "return this.getOrNull(index)" }
        }
    }

    val f_getOrNull = fn("getOrNull(index: Int)") {
        include(CharSequences, Lists, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        doc { "Returns ${f.element.prefixWithArticle()} at the given [index] or `null` if the [index] is out of bounds of this ${f.collection}." }
        sample("samples.collections.Collections.Elements.getOrNull")
        returns("T?")
        body {
            """
            return if (index >= 0 && index <= lastIndex) get(index) else null
            """
        }
    }

    val f_first = fn("first()") {
        includeDefault()
        include(CharSequences, Lists, ArraysOfUnsigned)
        include(ProgressionsOfPrimitives, PrimitiveType.rangePrimitives)
    } builder {
        specialFor(ProgressionsOfPrimitives) {
            since("1.7")
        }
        doc { "Returns the first ${f.element}." }
        throws("NoSuchElementException", "if the ${f.collection} is empty.")
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
        body(CharSequences, Lists, ArraysOfObjects, ArraysOfPrimitives) {
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
        body(ProgressionsOfPrimitives) {
            """
            if (isEmpty())
                throw NoSuchElementException("Progression ${'$'}this is empty.")
            return this.first
            """
        }

        specialFor(ArraysOfUnsigned) {
            inlineOnly()
            body { "return storage.first().to${primitive!!.name}()" }
        }
    }

    val f_firstOrNull = fn("firstOrNull()") {
        includeDefault()
        include(CharSequences, Lists, ArraysOfUnsigned)
        include(ProgressionsOfPrimitives, PrimitiveType.rangePrimitives)
    } builder {
        specialFor(ProgressionsOfPrimitives) {
            since("1.7")
        }
        doc { "Returns the first ${f.element}, or `null` if the ${f.collection} is empty." }
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
        body(CharSequences, Lists, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned) {
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
        body(ProgressionsOfPrimitives) {
            """
            return if (isEmpty()) null else this.first
            """
        }
    }

    val f_first_predicate = fn("first(predicate: (T) -> Boolean)") {
        includeDefault()
        include(CharSequences, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { """Returns the first ${f.element} matching the given [predicate].
        @throws [NoSuchElementException] if no such ${f.element} is found.""" }
        returns("T")

        body {
            """
            for (element in this) if (predicate(element)) return element
            throw NoSuchElementException("${f.doc.collection.capitalize()} contains no ${f.doc.element} matching the predicate.")
            """
        }
    }

    val f_firstOrNull_predicate = fn("firstOrNull(predicate: (T) -> Boolean)") {
        includeDefault()
        include(CharSequences, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { "Returns the first ${f.element} matching the given [predicate], or `null` if ${f.element} was not found." }
        returns("T?")
        body {
            """
            for (element in this) if (predicate(element)) return element
            return null
            """
        }
    }

    val f_find = fn("find(predicate: (T) -> Boolean)") {
        includeDefault()
        include(CharSequences, ArraysOfUnsigned)
    } builder {
        inline(Inline.Only)
        doc { "Returns the first ${f.element} matching the given [predicate], or `null` if no such ${f.element} was found." }
        sample("samples.collections.Collections.Elements.find")
        returns("T?")
        body { "return firstOrNull(predicate)"}
    }

    private val Family.sampleClass: String
        get() = when (this) {
            Strings, CharSequences -> "samples.text.Strings"
            else -> "samples.collections.Collections.Elements"
        }

    val f_last = fn("last()") {
        includeDefault()
        include(CharSequences, Lists, ArraysOfUnsigned)
        include(ProgressionsOfPrimitives, PrimitiveType.rangePrimitives)
    } builder {
        specialFor(ProgressionsOfPrimitives) {
            since("1.7")
        }
        doc { "Returns the last ${f.element}." }
        throws("NoSuchElementException", "if the ${f.collection} is empty.")
        sample("${f.sampleClass}.last")
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
        body(CharSequences, Lists, ArraysOfObjects, ArraysOfPrimitives) {
            """
            if (isEmpty())
                throw NoSuchElementException("${f.doc.collection.capitalize()} is empty.")
            return this[lastIndex]
            """
        }
        body(ProgressionsOfPrimitives) {
            """
            if (isEmpty())
                throw NoSuchElementException("Progression ${'$'}this is empty.")
            return this.last
            """
        }

        specialFor(ArraysOfUnsigned) {
            inlineOnly()
            body { "return storage.last().to${primitive!!.name}()" }
        }
    }

    val f_lastOrNull = fn("lastOrNull()") {
        includeDefault()
        include(Lists, CharSequences, ArraysOfUnsigned)
        include(ProgressionsOfPrimitives, PrimitiveType.rangePrimitives)
    } builder {
        specialFor(ProgressionsOfPrimitives) {
            since("1.7")
        }
        doc { "Returns the last ${f.element}, or `null` if the ${f.collection} is empty." }
        sample("${f.sampleClass}.last")
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
        body(Lists, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned) {
            """
            return if (isEmpty()) null else this[size - 1]
            """
        }
        body(ProgressionsOfPrimitives) {
            """
            return if (isEmpty()) null else this.last
            """
        }
    }

    val f_last_predicate = fn("last(predicate: (T) -> Boolean)") {
        includeDefault()
        include(Lists, CharSequences, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { "Returns the last ${f.element} matching the given [predicate]." }
        throws("NoSuchElementException", "if no such ${f.element} is found.")
        sample("${f.sampleClass}.last")
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
            if (!found) throw NoSuchElementException("${f.doc.collection.capitalize()} contains no ${f.doc.element} matching the predicate.")
            @Suppress("UNCHECKED_CAST")
            return last as T
            """
        }

        body(CharSequences, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned) {
            """
            for (index in this.indices.reversed()) {
                val element = this[index]
                if (predicate(element)) return element
            }
            throw NoSuchElementException("${f.doc.collection.capitalize()} contains no ${f.doc.element} matching the predicate.")
            """
        }
        body(Lists) {
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

    val f_lastOrNull_predicate = fn("lastOrNull(predicate: (T) -> Boolean)") {
        includeDefault()
        include(Lists, CharSequences, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { "Returns the last ${f.element} matching the given [predicate], or `null` if no such ${f.element} was found." }
        sample("${f.sampleClass}.last")
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

        body(CharSequences, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned) {
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

    val f_findLast = fn("findLast(predicate: (T) -> Boolean)") {
        includeDefault()
        include(Lists, CharSequences, ArraysOfUnsigned)
    } builder {
        inline(Inline.Only)
        doc { "Returns the last ${f.element} matching the given [predicate], or `null` if no such ${f.element} was found." }
        sample("samples.collections.Collections.Elements.find")
        returns("T?")
        body { "return lastOrNull(predicate)"}
    }

    val f_single = fn("single()") {
        includeDefault()
        include(Lists, CharSequences, ArraysOfUnsigned)
    } builder {
        doc { "Returns the single ${f.element}, or throws an exception if the ${f.collection} is empty or has more than one ${f.element}." }
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
        body(CharSequences, Lists, ArraysOfObjects, ArraysOfPrimitives) {
            """
            return when (${f.code.size}) {
                0 -> throw NoSuchElementException("${f.doc.collection.capitalize()} is empty.")
                1 -> this[0]
                else -> throw IllegalArgumentException("${f.doc.collection.capitalize()} has more than one element.")
            }
            """
        }

        specialFor(ArraysOfUnsigned) {
            inlineOnly()
            body { "return storage.single().to${primitive!!.name}()" }
        }
    }

    val f_singleOrNull = fn("singleOrNull()") {
        includeDefault()
        include(Lists, CharSequences, ArraysOfUnsigned)
    } builder {
        doc { "Returns single ${f.element}, or `null` if the ${f.collection} is empty or has more than one ${f.element}." }
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
        body(Lists, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned) {
            """
            return if (size == 1) this[0] else null
            """
        }
    }

    val f_single_predicate = fn("single(predicate: (T) -> Boolean)") {
        includeDefault()
        include(CharSequences, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { "Returns the single ${f.element} matching the given [predicate], or throws exception if there is no or more than one matching ${f.element}." }
        returns("T")

        body {
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
            @Suppress("UNCHECKED_CAST")
            return single as T
            """
        }
    }

    val f_singleOrNull_predicate = fn("singleOrNull(predicate: (T) -> Boolean)") {
        includeDefault()
        include(CharSequences, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { "Returns the single ${f.element} matching the given [predicate], or `null` if ${f.element} was not found or more than one ${f.element} was found." }
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

    val f_random = fn("random()") {
        include(Collections, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned, CharSequences, RangesOfPrimitives)
    } builder {
        since("1.3")
        inlineOnly()
        returns("T")
        doc {
            """
            Returns a random ${f.element} from this ${f.collection}.

            @throws ${if (f == RangesOfPrimitives) "IllegalArgumentException" else "NoSuchElementException"} if this ${f.collection} is empty.
            """
        }
        body {
            """return random(Random)"""
        }
    }

    val f_randomOrNull = fn("randomOrNull()") {
        include(Collections, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned, CharSequences, RangesOfPrimitives)
    } builder {
        since("1.4")
        wasExperimental("ExperimentalStdlibApi")
        inlineOnly()
        returns("T?")
        doc {
            """
            Returns a random ${f.element} from this ${f.collection}, or `null` if this ${f.collection} is empty.
            """
        }
        body {
            """return randomOrNull(Random)"""
        }
    }

    val f_random_random = fn("random(random: Random)") {
        include(Collections, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned, CharSequences, RangesOfPrimitives)
    } builder {
        since("1.3")
        returns("T")
        doc {
            """
            Returns a random ${f.element} from this ${f.collection} using the specified source of randomness.

            @throws ${if (f == RangesOfPrimitives) "IllegalArgumentException" else "NoSuchElementException"} if this ${f.collection} is empty.
            """
        }
        body {
            """
            if (isEmpty())
                throw NoSuchElementException("${f.doc.collection.capitalize()} is empty.")
            return elementAt(random.nextInt(size))
            """
        }
        specialFor(ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned, CharSequences) {
            body {
                """
                if (isEmpty())
                    throw NoSuchElementException("${f.doc.collection.capitalize()} is empty.")
                return get(random.nextInt(${f.code.size}))
                """
            }
        }
        specialFor(RangesOfPrimitives) {
            body {
                val expr = when (primitive) {
                    PrimitiveType.Char -> "nextInt(first.code, last.code + 1).toChar()"
                    else -> "next$primitive(this)"
                }
                """
                try {
                    return random.$expr
                } catch(e: IllegalArgumentException) {
                    throw NoSuchElementException(e.message)
                }
                """
            }
        }
    }

    val f_randomOrNull_random = fn("randomOrNull(random: Random)") {
        include(Collections, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned, CharSequences, RangesOfPrimitives)
    } builder {
        since("1.4")
        wasExperimental("ExperimentalStdlibApi")
        returns("T?")
        doc {
            """
            Returns a random ${f.element} from this ${f.collection} using the specified source of randomness, or `null` if this ${f.collection} is empty.
            """
        }
        body {
            """
            if (isEmpty())
                return null
            return elementAt(random.nextInt(size))
            """
        }
        specialFor(ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned, CharSequences) {
            body {
                val size = if (family == CharSequences) "length" else "size"
                """
                if (isEmpty())
                    return null
                return get(random.nextInt($size))
                """
            }
        }
        specialFor(RangesOfPrimitives) {
            body {
                val expr = when (primitive) {
                    PrimitiveType.Char -> "nextInt(first.code, last.code + 1).toChar()"
                    else -> "next$primitive(this)"
                }
                """
                if (isEmpty())
                    return null
                return random.$expr
                """
            }
        }
    }

    val f_components = (1..5).map { n ->
        fn("component$n()") {
            include(Lists, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
        } builder {
            operator(true)
            inlineOnly()
            fun getOrdinal(n: Int) = n.toString() + when (n) {
                1 -> "st"
                2 -> "nd"
                3 -> "rd"
                else -> "th"
            }

            val condition = "the size of this ${f.doc.collection} is less than $n"
            doc {
                """
                Returns ${getOrdinal(n)} *element* from the ${f.collection}.
            
                Throws an [IndexOutOfBoundsException] if $condition.
                """
            }
            specialFor(ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned) {
                doc {
                    """
                    Returns ${getOrdinal(n)} *element* from the ${f.collection}.
            
                    If $condition, throws an [IndexOutOfBoundsException] except in Kotlin/JS 
                    where the behavior is unspecified.
                    """
                }
            }
            returns("T")
            body { "return get(${n-1})" }
        }
    }

    val f_firstNotNullOfOrNull = fn("firstNotNullOfOrNull(transform: (T) -> R?)") {
        include(Iterables, Sequences, Maps, CharSequences, ArraysOfObjects)
    } builder {
        inlineOnly()
        since("1.5")
        typeParam("R : Any")
        returns("R?")

        sample("samples.collections.Collections.Transformations.firstNotNullOf")

        doc {
            """
            Returns the first non-null value produced by [transform] function being applied to ${f.element.pluralize()} of this ${f.collection} in iteration order,
            or `null` if no non-null value was produced.
            """
        }
        body {
            """
            for (element in this) {
                val result = transform(element)
                if (result != null) {
                    return result
                }
            }
            return null
            """
        }
    }

    val f_firstNotNullOf = fn("firstNotNullOf(transform: (T) -> R?)") {
        include(Iterables, Sequences, Maps, CharSequences, ArraysOfObjects)
    } builder {
        inlineOnly()
        since("1.5")
        typeParam("R : Any")
        returns("R")

        sample("samples.collections.Collections.Transformations.firstNotNullOf")

        doc {
            """
            Returns the first non-null value produced by [transform] function being applied to ${f.element.pluralize()} of this ${f.collection} in iteration order,
            or throws [NoSuchElementException] if no non-null value was produced.
            """
        }
        body {
            """
            return firstNotNullOfOrNull(transform) ?: throw NoSuchElementException("No element of the ${f.collection} was transformed to a non-null value.")
            """
        }
    }

}

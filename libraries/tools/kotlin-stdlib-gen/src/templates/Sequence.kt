/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package templates

import templates.Family.*

object SequenceOps : TemplateGroupBase() {

    val f_asIterable = fn("asIterable()") {
        include(Iterables, ArraysOfObjects, ArraysOfPrimitives, Sequences, CharSequences, Maps)
    } builder {
        doc { "Creates an [Iterable] instance that wraps the original ${f.collection} returning its ${f.element.pluralize()} when being iterated." }
        returns("Iterable<T>")
        body {
            """
            ${ when(f) {
                ArraysOfObjects, ArraysOfPrimitives -> "if (isEmpty()) return emptyList()"
                CharSequences -> "if (this is String && isEmpty()) return emptyList()"
                else -> ""
            }}
            return Iterable { this.iterator() }
            """
        }

        specialFor(Iterables, Maps) { inlineOnly() }
        specialFor(Iterables) {
            doc { "Returns this collection as an [Iterable]." }
            body { "return this" }
        }

        body(Maps) { "return entries" }
    }

    val f_asSequence = fn("asSequence()") {
        includeDefault()
        include(CharSequences, Maps)
    } builder {
        doc {
            """
            Creates a [Sequence] instance that wraps the original ${f.collection} returning its ${f.element.pluralize()} when being iterated.
            """
        }
        if (f in listOf(ArraysOfPrimitives, ArraysOfObjects, Iterables))
            sample("samples.collections.Sequences.Building.sequenceFrom${f.doc.collection.capitalize()}")

        returns("Sequence<T>")
        body {
            """
            ${ when(f) {
                ArraysOfObjects, ArraysOfPrimitives -> "if (isEmpty()) return emptySequence()"
                CharSequences -> "if (this is String && isEmpty()) return emptySequence()"
                else -> ""
            }}
            return Sequence { this.iterator() }
            """
        }

        body(Maps) { "return entries.asSequence()" }

        specialFor(Sequences) {
            doc { "Returns this sequence as a [Sequence]."}
            inlineOnly()
            body { "return this" }
        }
    }
}

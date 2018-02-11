/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

            ${if (f in listOf(ArraysOfPrimitives, ArraysOfObjects, Iterables)) "@sample samples.collections.Sequences.Building.sequenceFrom${f.doc.collection.capitalize()}" else ""}
            """
        }
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

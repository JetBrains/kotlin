/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package templates

import templates.Family.*
import templates.SequenceClass.*

object Guards : TemplateGroupBase() {
    private val THIS = "\$this"

    val f_requireNoNulls = fn("requireNoNulls()") {
        include(Iterables, Sequences, InvariantArraysOfObjects, Lists)
    } builder {
        doc { "Returns an original collection containing all the non-`null` elements, throwing an [IllegalArgumentException] if there are any `null` elements." }
        specialFor(Sequences) {
            doc {
                """
                Returns a sequence containing all the elements of this sequence, each guaranteed to be non-`null`.
                Throws an [IllegalArgumentException] if a `null` element is encountered while iterating the returned sequence.
                """
            }
        }
        sequenceClassification(intermediate, stateless)
        typeParam("T : Any")
        toNullableT = true
        returns("SELF")
        body {
            """
            for (element in this) {
                if (element == null) {
                    throw IllegalArgumentException("null element found in $THIS.")
                }
            }
            @Suppress("UNCHECKED_CAST")
            return this as SELF
            """
        }
        body(Sequences) {
            """
            return map { it ?: throw IllegalArgumentException("null element found in $THIS.") }
            """
        }
    }
}

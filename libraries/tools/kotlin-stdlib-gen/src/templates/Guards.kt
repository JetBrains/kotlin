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
import templates.SequenceClass.*

object Guards : TemplateGroupBase() {
    private val THIS = "\$this"

    val f_requireNoNulls = fn("requireNoNulls()") {
        include(Iterables, Sequences, InvariantArraysOfObjects, Lists)
    } builder {
        doc { "Returns an original collection containing all the non-`null` elements, throwing an [IllegalArgumentException] if there are any `null` elements." }
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

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

object StringJoinOps : TemplateGroupBase() {

    val f_joinTo = fn("joinTo(buffer: A, separator: CharSequence = \", \", prefix: CharSequence = \"\", postfix: CharSequence = \"\", limit: Int = -1, truncated: CharSequence = \"...\", transform: ((T) -> CharSequence)? = null)") {
        includeDefault()
    } builder {
        doc {
            """
            Appends the string from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.

            If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
            elements will be appended, followed by the [truncated] string (which defaults to "...").
            """
        }
        sequenceClassification(terminal)
        typeParam("A : Appendable")
        returns("A")
        body {
            """
            buffer.append(prefix)
            var count = 0
            for (element in this) {
                if (++count > 1) buffer.append(separator)
                if (limit < 0 || count <= limit) {
                    buffer.appendElement(element, transform)
                } else break
            }
            if (limit >= 0 && count > limit) buffer.append(truncated)
            buffer.append(postfix)
            return buffer
            """
        }
        body(ArraysOfPrimitives) {
            """
            buffer.append(prefix)
            var count = 0
            for (element in this) {
                if (++count > 1) buffer.append(separator)
                if (limit < 0 || count <= limit) {
                    if (transform != null)
                        buffer.append(transform(element))
                    else
                        buffer.append(${if (primitive == PrimitiveType.Char) "element" else "element.toString()"})
                } else break
            }
            if (limit >= 0 && count > limit) buffer.append(truncated)
            buffer.append(postfix)
            return buffer
            """
        }
    }

    val f_joinToString = fn("joinToString(separator: CharSequence = \", \", prefix: CharSequence = \"\", postfix: CharSequence = \"\", limit: Int = -1, truncated: CharSequence = \"...\", transform: ((T) -> CharSequence)? = null)") {
        includeDefault()
    } builder {
        doc {
            """
            Creates a string from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.

            If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
            elements will be appended, followed by the [truncated] string (which defaults to "...").
            """
        }
        sequenceClassification(terminal)

        returns("String")
        body {
            """
            return joinTo(StringBuilder(), separator, prefix, postfix, limit, truncated, transform).toString()
            """
        }
    }
}

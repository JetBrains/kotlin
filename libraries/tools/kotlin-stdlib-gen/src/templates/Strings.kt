/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
            
            @return the [buffer] argument with appended elements.
            """
        }
        annotation("@IgnorableReturnValue")
        sample("samples.collections.Collections.Transformations.joinTo")
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
        sample("samples.collections.Collections.Transformations.joinToString")
        sequenceClassification(terminal)

        returns("String")
        body {
            """
            return joinTo(StringBuilder(), separator, prefix, postfix, limit, truncated, transform).toString()
            """
        }
    }
}

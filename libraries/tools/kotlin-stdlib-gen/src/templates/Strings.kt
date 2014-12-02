package templates

import templates.Family.*

fun strings(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("joinTo(buffer: A, separator: String = \", \", prefix: String = \"\", postfix: String = \"\", limit: Int = -1, truncated: String = \"...\")") {
        doc {
            """
            Appends the string from all the elements separated using the *separator* and using the given *prefix* and *postfix* if supplied

            If a collection could be huge you can specify a non-negative value of *limit* which will only show a subset of the collection then it will
            a special *truncated* separator (which defaults to "...")
            """
        }
        typeParam("A : Appendable")
        returns { "A" }
        body {
            """
            buffer.append(prefix)
            var count = 0
            for (element in this) {
                if (++count > 1) buffer.append(separator)
                if (limit < 0 || count <= limit) {
                    val text = if (element == null) "null" else element.toString()
                    buffer.append(text)
                } else break
            }
            if (limit >= 0 && count > limit) buffer.append(truncated)
            buffer.append(postfix)
            return buffer
            """
        }
        exclude(Strings)
        body(ArraysOfPrimitives) {
            """
            buffer.append(prefix)
            var count = 0
            for (element in this) {
                if (++count > 1) buffer.append(separator)
                if (limit < 0 || count <= limit) {
                    val text = element.toString()
                    buffer.append(text)
                } else break
            }
            if (limit >= 0 && count > limit) buffer.append(truncated)
            buffer.append(postfix)
            return buffer
            """
        }
    }

    templates add f("joinToString(separator: String = \", \", prefix: String = \"\", postfix: String = \"\", limit: Int = -1, truncated: String = \"...\")") {
        doc {
            """
            Creates a string from all the elements separated using the *separator* and using the given *prefix* and *postfix* if supplied.

            If a collection could be huge you can specify a non-negative value of *limit* which will only show a subset of the collection then it will
            a special *truncated* separator (which defaults to "..."
            """
        }

        exclude(Strings)
        returns("String")
        body {
            """
            return joinTo(StringBuilder(), separator, prefix, postfix, limit, truncated).toString()
            """
        }
    }

    return templates
}
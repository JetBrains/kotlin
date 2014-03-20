package templates

import templates.Family.*

fun strings(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("appendString(buffer: Appendable, separator: String = \", \", prefix: String =\"\", postfix: String = \"\", limit: Int = -1, truncated: String = \"...\")") {
        doc {
            """
            Appends the string from all the elements separated using the *separator* and using the given *prefix* and *postfix* if supplied

            If a collection could be huge you can specify a non-negative value of *limit* which will only show a subset of the collection then it will
            a special *truncated* separator (which defaults to "..."
            """
        }
        returns { "Unit" }
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
            """
        }
    }

    templates add f("makeString(separator: String = \", \", prefix: String = \"\", postfix: String = \"\", limit: Int = -1, truncated: String = \"...\")") {
        doc {
            """
            Creates a string from all the elements separated using the *separator* and using the given *prefix* and *postfix* if supplied.

            If a collection could be huge you can specify a non-negative value of *limit* which will only show a subset of the collection then it will
            a special *truncated* separator (which defaults to "..."
            """
        }

        returns("String")
        body {
            """
            val buffer = StringBuilder()
            appendString(buffer, separator, prefix, postfix, limit, truncated)
            return buffer.toString()
            """
        }
    }

    return templates
}
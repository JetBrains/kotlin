package org.jetbrains.dokka.base.parsers

import org.jetbrains.dokka.model.doc.*

abstract class Parser {

    abstract fun parseStringToDocNode(extractedString: String): DocTag

    abstract fun preparse(text: String): String

    open fun parse(text: String): DocumentationNode =
        DocumentationNode(jkdocToListOfPairs(preparse(text)).map { (tag, content) -> parseTagWithBody(tag, content) })

    open fun parseTagWithBody(tagName: String, content: String): TagWrapper =
        when (tagName) {
            "description" -> Description(parseStringToDocNode(content))
            "author" -> Author(parseStringToDocNode(content))
            "version" -> Version(parseStringToDocNode(content))
            "since" -> Since(parseStringToDocNode(content))
            "see" -> See(
                parseStringToDocNode(content.substringAfter(' ')),
                content.substringBefore(' '),
                null
            )
            "param" -> Param(
                parseStringToDocNode(content.substringAfter(' ')),
                content.substringBefore(' ')
            )
            "property" -> Property(
                parseStringToDocNode(content.substringAfter(' ')),
                content.substringBefore(' ')
            )
            "return" -> Return(parseStringToDocNode(content))
            "constructor" -> Constructor(parseStringToDocNode(content))
            "receiver" -> Receiver(parseStringToDocNode(content))
            "throws", "exception" -> Throws(
                parseStringToDocNode(content.substringAfter(' ')),
                content.substringBefore(' '),
                null
            )
            "deprecated" -> Deprecated(parseStringToDocNode(content))
            "sample" -> Sample(
                parseStringToDocNode(content.substringAfter(' ')),
                content.substringBefore(' ')
            )
            "suppress" -> Suppress(parseStringToDocNode(content))
            else -> CustomTagWrapper(parseStringToDocNode(content), tagName)
        }

    private fun jkdocToListOfPairs(javadoc: String): List<Pair<String, String>> =
        "description $javadoc"
            .split("\n@")
            .map { content ->
                val contentWithEscapedAts = content.replace("\\@", "@")
                val (tag, body) = contentWithEscapedAts.split(" ", limit = 2)
                tag to body
            }
}

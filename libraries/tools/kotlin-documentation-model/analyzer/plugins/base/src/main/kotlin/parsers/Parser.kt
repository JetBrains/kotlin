package org.jetbrains.dokka.base.parsers

import org.jetbrains.dokka.model.doc.*

abstract class Parser {

    abstract fun parseStringToDocNode(extractedString: String): DocTag

    abstract fun preparse(text: String): String

    fun parse(text: String): DocumentationNode {

        val list = jkdocToListOfPairs(preparse(text))

        val mappedList: List<TagWrapper> = list.map {
            when (it.first) {
                "description" -> Description(parseStringToDocNode(it.second))
                "author" -> Author(parseStringToDocNode(it.second))
                "version" -> Version(parseStringToDocNode(it.second))
                "since" -> Since(parseStringToDocNode(it.second))
                "see" -> See(parseStringToDocNode(it.second.substringAfter(' ')), it.second.substringBefore(' '), null)
                "param" -> Param(parseStringToDocNode(it.second.substringAfter(' ')), it.second.substringBefore(' '))
                "property" -> Property(
                    parseStringToDocNode(it.second.substringAfter(' ')),
                    it.second.substringBefore(' ')
                )
                "return" -> Return(parseStringToDocNode(it.second))
                "constructor" -> Constructor(parseStringToDocNode(it.second))
                "receiver" -> Receiver(parseStringToDocNode(it.second))
                "throws", "exception" -> Throws(
                    parseStringToDocNode(it.second.substringAfter(' ')),
                    it.second.substringBefore(' ')
                )
                "deprecated" -> Deprecated(parseStringToDocNode(it.second))
                "sample" -> Sample(parseStringToDocNode(it.second.substringAfter(' ')), it.second.substringBefore(' '))
                "suppress" -> Suppress(parseStringToDocNode(it.second))
                else -> CustomTagWrapper(parseStringToDocNode(it.second), it.first)
            }
        }
        return DocumentationNode(mappedList)
    }

    private fun jkdocToListOfPairs(javadoc: String): List<Pair<String, String>> =
        "description $javadoc"
            .split("\n@")
            .map {
                it.substringBefore(' ') to it.substringAfter(' ')
            }
}

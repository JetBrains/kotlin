/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.markdown.jb

import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.model.doc.*

@InternalDokkaApi
public abstract class Parser {

    public abstract fun parseStringToDocNode(extractedString: String): DocTag

    protected abstract fun preparse(text: String): String

    public open fun parse(text: String): DocumentationNode =
        DocumentationNode(extractTagsToListOfPairs(preparse(text)).map { (tag, content) -> parseTagWithBody(tag, content) })

    protected open fun parseTagWithBody(tagName: String, content: String): TagWrapper =
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

    /**
     * KDoc parser from Kotlin compiler relies on a comment asterisk
     * So there is a mini parser here
     * TODO: at least to adapt [org.jetbrains.kotlin.kdoc.lexer.KDocLexer] to analyze KDoc without the asterisks and use it here
     */
    private fun extractTagsToListOfPairs(text: String): List<Pair<String, String>> =
        "description $text"
            .extractKDocSections()
            .map { content ->
                val contentWithEscapedAts = content.replace("\\@", "@")
                val (tag, body) = contentWithEscapedAts.split(" ", limit = 2)
                tag to body
            }

    /**
     * Ignore a doc tag inside code spans and blocks
     * @see org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
     */
    private fun CharSequence.extractKDocSections(delimiter: String = "\n@"): List<String> {
        var countOfBackticks = 0
        var countOfTildes = 0
        var countOfBackticksInOpeningFence = 0
        var countOfTildesInOpeningFence = 0

        var isInCode = false
        val result = mutableListOf<String>()
        var rangeStart = 0
        var rangeEnd = 0
        var currentOffset = 0
        while (currentOffset < length) {

            when (get(currentOffset)) {
                '`' -> {
                    countOfBackticks++
                    countOfTildes = 0
                }
                '~' -> {
                    countOfTildes++
                    countOfBackticks = 0
                }
                else -> {
                    if (isInCode) {
                        // The closing code fence must be at least as long as the opening fence
                        if(countOfBackticks >= countOfBackticksInOpeningFence
                                || countOfTildes >= countOfTildesInOpeningFence)
                            isInCode = false
                    } else {
                        // as per CommonMark spec, there can be any number of backticks for a code span, not only one or three
                        if (countOfBackticks > 0) {
                            isInCode = true
                            countOfBackticksInOpeningFence = countOfBackticks
                            countOfTildesInOpeningFence = Int.MAX_VALUE
                        }
                        // tildes are only for a code block, not code span
                        if (countOfTildes >= 3) {
                            isInCode = true
                            countOfTildesInOpeningFence = countOfTildes
                            countOfBackticksInOpeningFence = Int.MAX_VALUE
                        }
                    }
                    countOfTildes = 0
                    countOfBackticks = 0
                }
            }
            if (!isInCode && startsWith(delimiter, currentOffset)) {
                result.add(substring(rangeStart, rangeEnd))
                currentOffset += delimiter.length
                rangeStart = currentOffset
                rangeEnd = currentOffset
                continue
            }

            ++rangeEnd
            ++currentOffset
        }
        result.add(substring(rangeStart, rangeEnd))
        return result
    }

}

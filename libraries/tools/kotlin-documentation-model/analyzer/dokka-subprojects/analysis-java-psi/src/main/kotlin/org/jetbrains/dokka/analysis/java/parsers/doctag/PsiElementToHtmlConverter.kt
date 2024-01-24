/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.parsers.doctag

import com.intellij.lexer.JavaDocTokenTypes
import com.intellij.psi.*
import com.intellij.psi.impl.source.javadoc.PsiDocParamRef
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.javadoc.PsiDocTagValue
import com.intellij.psi.javadoc.PsiDocToken
import com.intellij.psi.javadoc.PsiInlineDocTag
import org.jetbrains.dokka.analysis.java.doccomment.DocumentationContent
import org.jetbrains.dokka.analysis.java.JavadocTag
import org.jetbrains.dokka.analysis.java.doccomment.PsiDocumentationContent
import org.jetbrains.dokka.analysis.java.parsers.CommentResolutionContext
import org.jetbrains.dokka.analysis.java.util.*
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.utilities.htmlEscape

private const val UNRESOLVED_PSI_ELEMENT = "UNRESOLVED_PSI_ELEMENT"

private data class HtmlParserState(
    val currentJavadocTag: JavadocTag?,
    val previousElement: PsiElement? = null,
    val openPreTags: Int = 0,
    val closedPreTags: Int = 0
)

private data class HtmlParsingResult(val newState: HtmlParserState, val parsedLine: String? = null) {
    constructor(tag: JavadocTag?) : this(HtmlParserState(tag))

    operator fun plus(other: HtmlParsingResult): HtmlParsingResult {
        return HtmlParsingResult(
            newState = other.newState,
            parsedLine = listOfNotNull(parsedLine, other.parsedLine).joinToString(separator = "")
        )
    }
}

internal class PsiElementToHtmlConverter(
    private val inheritDocTagResolver: InheritDocTagResolver
) {
    private val preOpeningTagRegex = "<pre(\\s+.*)?>".toRegex()
    private val preClosingTagRegex = "</pre>".toRegex()

    fun convert(
        psiElements: Iterable<PsiElement>,
        docTagParserContext: DocTagParserContext,
        commentResolutionContext: CommentResolutionContext
    ): String? {
        return WithContext(docTagParserContext, commentResolutionContext)
            .convert(psiElements)
    }

    private inner class WithContext(
        private val docTagParserContext: DocTagParserContext,
        private val commentResolutionContext: CommentResolutionContext
    ) {
        fun convert(psiElements: Iterable<PsiElement>): String? {
            val parsingResult =
                psiElements.fold(HtmlParsingResult(commentResolutionContext.tag)) { resultAccumulator, psiElement ->
                    resultAccumulator + parseHtml(psiElement, resultAccumulator.newState)
                }
            return parsingResult.parsedLine?.trim()
        }

        private fun parseHtml(psiElement: PsiElement, state: HtmlParserState): HtmlParsingResult =
            when (psiElement) {
                is PsiReference -> psiElement.children.fold(HtmlParsingResult(state)) { acc, e ->
                    acc + parseHtml(e, acc.newState)
                }
                else -> parseHtmlOfSimpleElement(psiElement, state)
            }

        private fun parseHtmlOfSimpleElement(psiElement: PsiElement, state: HtmlParserState): HtmlParsingResult {
            val text = psiElement.text

            val openPre = state.openPreTags + preOpeningTagRegex.findAll(text).count()
            val closedPre = state.closedPreTags + preClosingTagRegex.findAll(text).count()
            val isInsidePre = openPre > closedPre

            val parsed = when (psiElement) {
                is PsiInlineDocTag -> psiElement.toHtml(state.currentJavadocTag)
                is PsiDocParamRef -> psiElement.toDocumentationLinkString()
                is PsiDocTagValue, is LeafPsiElement -> {
                    psiElement.stringifyElementAsText(isInsidePre, state.previousElement)
                }
                else -> null
            }
            val previousElement = if (text.trim() == "") state.previousElement else psiElement
            return HtmlParsingResult(
                state.copy(
                    previousElement = previousElement,
                    closedPreTags = closedPre,
                    openPreTags = openPre
                ), parsed
            )
        }

        /**
         * Inline tags can be met in the middle of some text. Example of an inline tag usage:
         *
         * ```java
         * Use the {@link #getComponentAt(int, int) getComponentAt} method.
         * ```
         */
        private fun PsiInlineDocTag.toHtml(javadocTag: JavadocTag?): String? =
            when (this.name) {
                "link", "linkplain" -> this.referenceElement()
                    ?.toDocumentationLinkString(this.dataElements.filterIsInstance<PsiDocToken>().joinToString(" ") {
                        it.stringifyElementAsText(keepFormatting = false).orEmpty()
                    })

                "code" -> "<code data-inline>${dataElementsAsText(this)}</code>"
                "literal" -> "<literal>${dataElementsAsText(this)}</literal>"
                "index" -> "<index>${this.children.filterIsInstance<PsiDocTagValue>().joinToString { it.text }}</index>"
                "inheritDoc" -> {
                    val inheritDocContent = inheritDocTagResolver.resolveContent(commentResolutionContext)
                    val html = inheritDocContent?.fold(HtmlParsingResult(javadocTag)) { result, content ->
                            result + content.toInheritDocHtml(result.newState, docTagParserContext)
                        }?.parsedLine.orEmpty()
                    html
                }

                else -> this.text
            }

        private fun DocumentationContent.toInheritDocHtml(
            parserState: HtmlParserState,
            docTagParserContext: DocTagParserContext
        ): HtmlParsingResult {
            // TODO [beresnev] comment
            return if (this is PsiDocumentationContent) {
                parseHtml(this.psiElement, parserState)
            } else {
                HtmlParsingResult(parserState, inheritDocTagResolver.convertToHtml(this, docTagParserContext))
            }
        }

        private fun dataElementsAsText(tag: PsiInlineDocTag): String {
            return tag.dataElements.joinToString("") {
                it.stringifyElementAsText(keepFormatting = true).orEmpty()
            }.htmlEscape()
        }

        private fun PsiElement.toDocumentationLinkString(label: String = ""): String {
            val driId = reference?.resolve()?.takeIf { it !is PsiParameter }?.let {
                val dri = DRI.from(it)
                val id = docTagParserContext.store(dri)
                id
            } ?: UNRESOLVED_PSI_ELEMENT // TODO [beresnev] log this somewhere maybe?

            // TODO [beresnev] data-dri into a constant
            return """<a data-dri="${driId.htmlEscape()}">${label.ifBlank { defaultLabel().text }}</a>"""
        }
    }
}

private fun PsiElement.stringifyElementAsText(keepFormatting: Boolean, previousElement: PsiElement? = null) =
    if (keepFormatting) {
        /*
        For values in the <pre> tag we try to keep formatting, so only the leading space is trimmed,
        since it is there because it separates this line from the leading asterisk
         */
        text.let {
            if (((prevSibling as? PsiDocToken)?.isLeadingAsterisk() == true || (prevSibling as? PsiDocToken)?.isTagName() == true) && it.firstOrNull() == ' ')
                it.drop(1) else it
        }.let {
            if ((nextSibling as? PsiDocToken)?.isLeadingAsterisk() == true) it.dropLastWhile { it == ' ' } else it
        }
    } else {
        /*
        Outside of the <pre> we would like to trim everything from the start and end of a line since
        javadoc doesn't care about it.
         */
        text.let {
            if ((prevSibling as? PsiDocToken)?.isLeadingAsterisk() == true && text.isNotBlank() && previousElement !is PsiInlineDocTag) it?.trimStart() else it
        }?.let {
            if ((nextSibling as? PsiDocToken)?.isLeadingAsterisk() == true && text.isNotBlank()) it.trimEnd() else it
        }?.let {
            if (shouldHaveSpaceAtTheEnd()) "$it " else it
        }
    }

private fun PsiDocToken.isLeadingAsterisk() = tokenType == JavaDocTokenType.DOC_COMMENT_LEADING_ASTERISKS

private fun PsiDocToken.isTagName() = tokenType == JavaDocTokenType.DOC_TAG_NAME

/**
 * We would like to know if we need to have a space after a this tag
 *
 * The space is required when:
 *  - tag spans multiple lines, between every line we would need a space
 *
 *  We wouldn't like to render a space if:
 *  - tag is followed by an end of comment
 *  - after a tag there is another tag (eg. multiple @author tags)
 *  - they end with an html tag like: <a href="...">Something</a> since then the space will be displayed in the following text
 *  - next line starts with a <p> or <pre> token
 */
private fun PsiElement.shouldHaveSpaceAtTheEnd(): Boolean {
    val siblings = siblings(withItself = false).toList().filterNot { it.text.trim() == "" }
    val nextNotEmptySibling = (siblings.firstOrNull() as? PsiDocToken)
    val furtherNotEmptySibling =
        (siblings.drop(1).firstOrNull { it is PsiDocToken && !it.isLeadingAsterisk() } as? PsiDocToken)
    val lastHtmlTag = text.trim().substringAfterLast("<")
    val endsWithAnUnclosedTag = lastHtmlTag.endsWith(">") && !lastHtmlTag.startsWith("</")

    return (nextSibling as? PsiWhiteSpace)?.text?.startsWith("\n ") == true &&
            (getNextSiblingIgnoringWhitespace() as? PsiDocToken)?.tokenType != JavaDocTokenTypes.INSTANCE.commentEnd() &&
            nextNotEmptySibling?.isLeadingAsterisk() == true &&
            furtherNotEmptySibling?.tokenType == JavaDocTokenTypes.INSTANCE.commentData() &&
            !endsWithAnUnclosedTag
}



/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.j2k

import com.intellij.ide.highlighter.HtmlFileType
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.javadoc.PsiDocTag
import com.intellij.psi.javadoc.PsiDocToken
import com.intellij.psi.javadoc.PsiInlineDocTag
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlText
import com.intellij.psi.xml.XmlTokenType
import java.util.Stack

object DocCommentConverter {
    fun convertDocComment(docComment: PsiDocComment): String {
        val html = StringBuilder {
            appendJavadocElements(docComment.getDescriptionElements())

            for (tag in docComment.getTags()) {
                when (tag.getName()) {
                    "deprecated" -> continue
                    "see" -> append("@see ${convertJavadocLink(tag.content())}\n")
                    else -> appendJavadocElements(tag.getChildren()).append("\n")
                }
            }
        }.toString()

        if (html.trim().isEmpty() && docComment.findTagByName("deprecated") != null) {
            // @deprecated was the only content of the doc comment; we can drop the comment
            return ""
        }

        val htmlFile = PsiFileFactory.getInstance(docComment.getProject()).createFileFromText(
                "javadoc.html", HtmlFileType.INSTANCE, html)
        val htmlToMarkdownConverter = HtmlToMarkdownConverter()
        htmlFile.accept(htmlToMarkdownConverter)
        return htmlToMarkdownConverter.result
    }

    private fun StringBuilder.appendJavadocElements(elements: Array<PsiElement>): StringBuilder {
        elements.forEach {
            if (it is PsiInlineDocTag) {
                append(convertInlineDocTag(it))
            }
            else {
                append(it.getText())
            }
        }
        return this
    }

    private fun convertInlineDocTag(tag: PsiInlineDocTag) = when(tag.getName()) {
        "code", "literal" -> {
            val text = tag.getDataElements().map { it.getText() }.join("")
            val escaped = StringUtil.escapeXml(text.trimLeading())
            if (tag.getName() == "code") "<code>$escaped</code>" else escaped
        }

        "link", "linkplain" -> {
            val valueElement = tag.linkElement()
            val labelText = tag.getDataElements().firstOrNull { it is PsiDocToken }?.getText() ?: ""
            val kdocLink = convertJavadocLink(valueElement?.getText())
            val linkText = if (labelText.isEmpty()) kdocLink else StringUtil.escapeXml(labelText)
            "<a docref=\"$kdocLink\">$linkText</a>"
        }

        else -> tag.getText()
    }

    private fun convertJavadocLink(link: String?): String =
        if (link != null) link.substringBefore('(').replace('#', '.') else ""

    private fun PsiDocTag.linkElement(): PsiElement? =
            getValueElement() ?: getDataElements().firstOrNull { it !is PsiWhiteSpace }

    private class HtmlToMarkdownConverter() : XmlRecursiveElementVisitor() {
        private enum class ListType { Ordered; Unordered }
        data class MarkdownSpan(val prefix: String, val suffix: String) {
            class object {
                val Empty = MarkdownSpan("", "")

                fun wrap(text: String) = MarkdownSpan(text, text)
                fun prefix(text: String) = MarkdownSpan(text, "")
            }
        }


        val result: String
            get() = markdownBuilder.toString()

        private val markdownBuilder = StringBuilder("/**")
        private var afterLineBreak = false
        private var whitespaceIsPartOfText = true
        private var currentListType = ListType.Unordered

        override fun visitWhiteSpace(space: PsiWhiteSpace) {
            super.visitWhiteSpace(space)

            if (whitespaceIsPartOfText) {
                appendPendingText()
                markdownBuilder.append(space.getText())
                if (space.textContains('\n')) {
                    afterLineBreak = true
                }
            }
        }

        override fun visitElement(element: PsiElement) {
            super.visitElement(element)

            val tokenType = element.getNode().getElementType()
            if (tokenType == XmlTokenType.XML_DATA_CHARACTERS || tokenType == XmlTokenType.XML_CHAR_ENTITY_REF) {
                appendPendingText()
                markdownBuilder.append(element.getText())
            }
        }

        override fun visitXmlTag(tag: XmlTag) {
            withWhitespaceAsPartOfText(false) {
                val oldListType = currentListType
                val atLineStart = afterLineBreak
                appendPendingText()
                val (openingMarkdown, closingMarkdown) = getMarkdownForTag(tag, atLineStart)
                markdownBuilder.append(openingMarkdown)

                super.visitXmlTag(tag)

                markdownBuilder.append(closingMarkdown)
                currentListType = oldListType
            }
        }

        override fun visitXmlText(text: XmlText) {
            withWhitespaceAsPartOfText(true) {
                super.visitXmlText(text)
            }
        }

        private inline fun withWhitespaceAsPartOfText(newValue: Boolean, block: () -> Unit) {
            val oldValue = whitespaceIsPartOfText
            whitespaceIsPartOfText = newValue
            try {
                block()
            }
            finally {
                whitespaceIsPartOfText = oldValue
            }
        }

        private fun getMarkdownForTag(tag: XmlTag, atLineStart: Boolean): MarkdownSpan = when(tag.getName()) {
            "b", "strong" -> MarkdownSpan.wrap("**")

            "p" -> if (atLineStart) MarkdownSpan.prefix("\n * ") else MarkdownSpan.prefix("\n *\n *")

            "i", "em" -> MarkdownSpan.wrap("*")

            "s", "del" -> MarkdownSpan.wrap("~~")

            "code" -> MarkdownSpan.wrap("`")

            "a" -> {
                if (tag.getAttributeValue("docref") != null) {
                    val docRef = tag.getAttributeValue("docref")
                    val innerText = tag.getValue().getText()
                    if (docRef == innerText) MarkdownSpan("[", "]") else MarkdownSpan("[", "][$docRef]")
                }
                else {
                    MarkdownSpan("[", "](${tag.getAttributeValue("href")})")
                }
            }

            "ul" -> { currentListType = ListType.Unordered; MarkdownSpan.Empty }

            "ol" -> { currentListType = ListType.Ordered; MarkdownSpan.Empty }

            "li" -> if (currentListType == ListType.Unordered) MarkdownSpan.prefix(" * ") else MarkdownSpan.prefix(" 1. ")

            else -> MarkdownSpan.Empty
        }

        private fun appendPendingText() {
            if (afterLineBreak ) {
                markdownBuilder.append(" * ")
                afterLineBreak = false
            }
        }

        override fun visitXmlFile(file: XmlFile) {
            super.visitXmlFile(file)

            markdownBuilder.append(" */")
        }
    }
}

fun PsiDocTag.content(): String =
    getChildren()
            .dropWhile { it.getNode().getElementType() == JavaDocTokenType.DOC_TAG_NAME }
            .dropWhile { it is PsiWhiteSpace }
            .filterNot { it.getNode().getElementType() == JavaDocTokenType.DOC_COMMENT_LEADING_ASTERISKS }
            .map { it.getText() }
            .join("")

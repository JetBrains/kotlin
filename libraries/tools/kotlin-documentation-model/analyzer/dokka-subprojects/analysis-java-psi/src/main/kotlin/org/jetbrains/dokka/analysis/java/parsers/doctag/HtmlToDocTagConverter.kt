/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.parsers.doctag

import org.jetbrains.dokka.analysis.markdown.jb.parseHtmlEncodedWithNormalisedSpaces
import org.jetbrains.dokka.model.doc.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Comment
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

internal class HtmlToDocTagConverter(
    private val docTagParserContext: DocTagParserContext
) {
    fun convertToDocTag(html: String): List<DocTag> {
        return Jsoup.parseBodyFragment(html)
            .body()
            .childNodes()
            .flatMap { convertHtmlNode(it) }
    }

    private fun convertHtmlNode(node: Node, keepFormatting: Boolean = false): List<DocTag> = when (node) {
        is TextNode -> (if (keepFormatting) {
            node.wholeText.takeIf { it.isNotBlank() }?.let { listOf(Text(body = it)) }
        } else {
            node.wholeText.parseHtmlEncodedWithNormalisedSpaces(renderWhiteCharactersAsSpaces = true)
        }).orEmpty()
        is Comment -> listOf(Text(body = node.outerHtml(), params = DocTag.contentTypeParam("html")))
        is Element -> createBlock(node, keepFormatting)
        else -> emptyList()
    }

    private fun createBlock(element: Element, keepFormatting: Boolean = false): List<DocTag> {
        val tagName = element.tagName()
        val children = element.childNodes()
            .flatMap { convertHtmlNode(it, keepFormatting = keepFormatting || tagName == "pre" || tagName == "code") }

        fun ifChildrenPresent(operation: () -> DocTag): List<DocTag> {
            return if (children.isNotEmpty()) listOf(operation()) else emptyList()
        }
        return when (tagName) {
            "blockquote" -> ifChildrenPresent { BlockQuote(children) }
            "p" -> ifChildrenPresent { P(children) }
            "b" -> ifChildrenPresent { B(children) }
            "strong" -> ifChildrenPresent { Strong(children) }
            "index" -> listOf(Index(children))
            "i" -> ifChildrenPresent { I(children) }
            "img" -> listOf(
                Img(
                    children,
                    element.attributes().associate { (if (it.key == "src") "href" else it.key) to it.value })
            )
            "em" -> listOf(Em(children))
            "code" -> ifChildrenPresent { if(keepFormatting) CodeBlock(children) else CodeInline(children) }
            "pre" -> if(children.size == 1) {
                when(children.first()) {
                    is CodeInline -> listOf(CodeBlock(children.first().children))
                    is CodeBlock -> listOf(children.first())
                    else -> listOf(Pre(children))
                }
            } else {
                listOf(Pre(children))
            }
            "ul" -> ifChildrenPresent { Ul(children) }
            "ol" -> ifChildrenPresent { Ol(children) }
            "li" -> listOf(Li(children))
            "dl" -> ifChildrenPresent { Dl(children) }
            "dt" -> listOf(Dt(children))
            "dd" -> listOf(Dd(children))
            "a" -> listOf(createLink(element, children))
            "table" -> ifChildrenPresent { Table(children) }
            "tr" -> ifChildrenPresent { Tr(children) }
            "td" -> listOf(Td(children))
            "thead" -> listOf(THead(children))
            "tbody" -> listOf(TBody(children))
            "tfoot" -> listOf(TFoot(children))
            "caption" -> ifChildrenPresent { Caption(children) }
            "inheritdoc" -> {
                // TODO [beresnev] describe how it works
                val id = element.attr("id")
                val section = docTagParserContext.getDocumentationNode(id)
                val parsed = section?.children?.flatMap { it.root.children }.orEmpty()
                if(parsed.size == 1 && parsed.first() is P){
                    parsed.first().children
                } else {
                    parsed
                }
            }
            "h1" -> ifChildrenPresent { H1(children) }
            "h2" -> ifChildrenPresent { H2(children) }
            "h3" -> ifChildrenPresent { H3(children) }
            "var" -> ifChildrenPresent { Var(children) }
            "u" -> ifChildrenPresent { U(children) }
            else -> listOf(Text(body = element.ownText()))
        }
    }

    private fun createLink(element: Element, children: List<DocTag>): DocTag {
        return when {
            element.hasAttr("docref") ->
                A(children, params = mapOf("docref" to element.attr("docref")))
            element.hasAttr("href") ->
                A(children, params = mapOf("href" to element.attr("href")))
            element.hasAttr("data-dri") && docTagParserContext.getDri(element.attr("data-dri")) != null -> {
                val referencedDriId = element.attr("data-dri")
                DocumentationLink(
                    dri = docTagParserContext.getDri(referencedDriId)
                        ?: error("docTagParserContext.getDri is null, TODO"), // TODO [beresnev] handle
                    children = children
                )
            }
            else -> Text(body = children.filterIsInstance<Text>().joinToString { it.body })
        }
    }
}

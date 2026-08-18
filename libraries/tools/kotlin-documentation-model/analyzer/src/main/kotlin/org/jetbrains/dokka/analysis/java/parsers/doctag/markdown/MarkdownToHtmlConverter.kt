/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.parsers.doctag.markdown

import org.intellij.markdown.flavours.MarkdownFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.LinkMap
import org.intellij.markdown.parser.MarkdownParser
import java.net.URI

/**
 * Converts Markdown-formatted text to HTML using a specified [flavour descriptor][org.intellij.markdown.flavours.MarkdownFlavourDescriptor].
 */
internal class MarkdownToHtmlConverter(
    private val flavourDescriptor: MarkdownFlavourDescriptor
) {
    /**
     * Converts the given Markdown-formatted string into HTML.
     *
     * @param markdownText The Markdown-formatted input string to be converted.
     * @param server An optional base server URL for resolving relative links within the Markdown content.
     * @return The HTML representation of the provided Markdown content.
     */
    fun convertMarkdownToHtml(markdownText: String, server: String? = null): String {
        val parsedTree = MarkdownParser(flavourDescriptor).buildMarkdownTreeFromString(markdownText)
        val providers = flavourDescriptor.createHtmlGeneratingProviders(
            linkMap = LinkMap.buildLinkMap(parsedTree, markdownText),
            baseURI = server?.let { URI(it) }
        )

        return HtmlGenerator(markdownText, parsedTree, providers, false).generateHtml()
    }
}

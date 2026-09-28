/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.parsers.doctag.markdown

import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.GeneratingProvider
import org.intellij.markdown.html.URI
import org.intellij.markdown.parser.LinkMap
import org.intellij.markdown.parser.MarkerProcessorFactory

internal class JavaDocMarkdownFlavourDescriptor : GFMFlavourDescriptor(
    useSafeLinks = true, absolutizeAnchorLinks = false, makeHttpsAutoLinks = true
) {
    override fun createHtmlGeneratingProviders(
        linkMap: LinkMap,
        baseURI: URI?
    ): Map<IElementType, GeneratingProvider> =
        // With MarkdownElementTypes.MARKDOWN_FILE the content will be wrapped into `<body>`, which we don't need
        super.createHtmlGeneratingProviders(linkMap, baseURI) - MarkdownElementTypes.MARKDOWN_FILE

    override val markerProcessorFactory: MarkerProcessorFactory
        get() = JavaDocMarkerProcessorFactory
}

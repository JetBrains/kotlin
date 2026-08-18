/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.parsers.doctag.markdown

import org.intellij.markdown.flavours.commonmark.CommonMarkMarkerProcessor
import org.intellij.markdown.flavours.gfm.table.GitHubTableMarkerProvider
import org.intellij.markdown.parser.MarkerProcessor
import org.intellij.markdown.parser.MarkerProcessorFactory
import org.intellij.markdown.parser.ProductionHolder
import org.intellij.markdown.parser.constraints.CommonMarkdownConstraints
import org.intellij.markdown.parser.markerblocks.MarkerBlockProvider
import org.intellij.markdown.parser.markerblocks.providers.AtxHeaderProvider
import org.intellij.markdown.parser.markerblocks.providers.BlockQuoteProvider
import org.intellij.markdown.parser.markerblocks.providers.CodeFenceProvider
import org.intellij.markdown.parser.markerblocks.providers.HorizontalRuleProvider
import org.intellij.markdown.parser.markerblocks.providers.HtmlBlockProvider
import org.intellij.markdown.parser.markerblocks.providers.ListMarkerProvider
import org.intellij.markdown.parser.markerblocks.providers.SetextHeaderProvider

/**
 * Marker processor taking account of changes brought to Common Markdown by the JEP-467
 * Namely: no indented code blocks, tables from GitHub Markdown
 */
internal class JavaDocMarkerProcessor(productionHolder: ProductionHolder) : CommonMarkMarkerProcessor(
    productionHolder,
    CommonMarkdownConstraints.BASE
) {
    override fun getMarkerBlockProviders(): List<MarkerBlockProvider<StateInfo>> {
        return listOf(
            HorizontalRuleProvider(),
            CodeFenceProvider(),
            SetextHeaderProvider(),
            BlockQuoteProvider(),
            ListMarkerProvider(),
            AtxHeaderProvider(),
            HtmlBlockProvider(),
            GitHubTableMarkerProvider()
        )
    }
}

internal object JavaDocMarkerProcessorFactory : MarkerProcessorFactory {
    override fun createMarkerProcessor(productionHolder: ProductionHolder): MarkerProcessor<*> {
        return JavaDocMarkerProcessor(productionHolder)
    }
}

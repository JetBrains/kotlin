/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.parsers.doctag

import com.intellij.psi.PsiElement
import com.intellij.psi.javadoc.PsiDocTag
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.analysis.java.parsers.CommentResolutionContext
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.plugability.DokkaContext

/**
 * Parses [PsiElement] of [PsiDocTag] into Dokka's [DocTag]
 */
internal class PsiDocTagParser(
    private val context: DokkaContext,
    private val inheritDocTagResolver: InheritDocTagResolver
) {
    fun parse(
        psiElements: Iterable<PsiElement>,
        commentResolutionContext: CommentResolutionContext,
        sourceSet: DokkaSourceSet
    ): List<DocTag> = parse(asParagraph = false, psiElements, commentResolutionContext, sourceSet)

    fun parseAsParagraph(
        psiElements: Iterable<PsiElement>,
        commentResolutionContext: CommentResolutionContext,
        sourceSet: DokkaSourceSet
    ): List<DocTag> = parse(asParagraph = true, psiElements, commentResolutionContext, sourceSet)

    private fun parse(
        asParagraph: Boolean,
        psiElements: Iterable<PsiElement>,
        commentResolutionContext: CommentResolutionContext,
        sourceSet: DokkaSourceSet
    ): List<DocTag> {
        val docTagParserContext = DocTagParserContext()

        val psiToHtmlConverter = PsiElementToHtmlConverter(inheritDocTagResolver, sourceSet, context.logger)

        val elementsHtml = psiToHtmlConverter.convert(psiElements, docTagParserContext, commentResolutionContext)
            ?: return emptyList()

        val htmlToDocTagConverter = HtmlToDocTagConverter(docTagParserContext)
        val html = if (asParagraph) "<p>$elementsHtml</p>" else elementsHtml
        return htmlToDocTagConverter.convertToDocTag(html)
    }
}

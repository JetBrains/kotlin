/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.parsers.doctag

import com.intellij.psi.PsiElement
import com.intellij.psi.javadoc.PsiDocTag
import org.jetbrains.dokka.analysis.java.parsers.CommentResolutionContext
import org.jetbrains.dokka.model.doc.*

/**
 * Parses [PsiElement] of [PsiDocTag] into Dokka's [DocTag]
 */
internal class PsiDocTagParser(
    private val inheritDocTagResolver: InheritDocTagResolver
) {
    fun parse(
        psiElements: Iterable<PsiElement>,
        commentResolutionContext: CommentResolutionContext
    ): List<DocTag> = parse(asParagraph = false, psiElements, commentResolutionContext)

    fun parseAsParagraph(
        psiElements: Iterable<PsiElement>,
        commentResolutionContext: CommentResolutionContext
    ): List<DocTag> = parse(asParagraph = true, psiElements, commentResolutionContext)

    private fun parse(
        asParagraph: Boolean,
        psiElements: Iterable<PsiElement>,
        commentResolutionContext: CommentResolutionContext
    ): List<DocTag> {
        val docTagParserContext = DocTagParserContext()

        val psiToHtmlConverter = PsiElementToHtmlConverter(inheritDocTagResolver)
        val elementsHtml = psiToHtmlConverter.convert(psiElements, docTagParserContext, commentResolutionContext)
            ?: return emptyList()

        val htmlToDocTagConverter = HtmlToDocTagConverter(docTagParserContext)
        val html = if (asParagraph) "<p>$elementsHtml</p>" else elementsHtml
        return htmlToDocTagConverter.convertToDocTag(html)
    }
}

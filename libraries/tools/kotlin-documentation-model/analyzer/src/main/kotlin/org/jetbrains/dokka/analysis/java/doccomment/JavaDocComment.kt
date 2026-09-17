/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.doccomment

import com.intellij.psi.PsiElement
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.javadoc.PsiDocTag
import org.jetbrains.dokka.analysis.java.*
import org.jetbrains.dokka.analysis.java.util.contentElementsWithSiblingIfNeeded
import org.jetbrains.dokka.analysis.java.util.getKotlinFqName
import org.jetbrains.dokka.analysis.java.util.hasTag
import org.jetbrains.dokka.analysis.java.util.resolveToElement
import org.jetbrains.dokka.utilities.firstIsInstanceOrNull

internal class JavaDocComment(val comment: PsiDocComment) : DocComment {
    override fun hasTag(tag: JavadocTag): Boolean {
        return when (tag) {
            is ThrowingExceptionJavadocTag -> hasTag(tag)
            else -> comment.hasTag(tag)
        }
    }

    private fun hasTag(tag: ThrowingExceptionJavadocTag): Boolean =
        comment.hasTag(tag) && comment.resolveTag(tag).firstIsInstanceOrNull<PsiDocTag>()
            ?.resolveToElement()
            ?.getKotlinFqName() == tag.exceptionQualifiedName

    override fun resolveTag(tag: JavadocTag): List<DocumentationContent> {
        return when (tag) {
            is ParamJavadocTag -> resolveParamTag(tag)
            is ThrowingExceptionJavadocTag -> resolveThrowingTag(tag)
            else -> comment.resolveTag(tag).map { PsiDocumentationContent(it, tag) }
        }
    }

    private fun resolveParamTag(tag: ParamJavadocTag): List<DocumentationContent> {
        val resolvedParamElements = comment.resolveTag(tag)
            .filterIsInstance<PsiDocTag>()
            .map { it.contentElementsWithSiblingIfNeeded() }
            .firstOrNull { it.firstOrNull()?.text == tag.paramName }.orEmpty()

        return resolvedParamElements
            .withoutReferenceLink()
            .map { PsiDocumentationContent(it, tag) }
    }

    private fun resolveThrowingTag(tag: ThrowingExceptionJavadocTag): List<DocumentationContent> {
        val resolvedElements = comment.resolveTag(tag)
            .flatMap {
                when (it) {
                    is PsiDocTag -> it.contentElementsWithSiblingIfNeeded()
                    else -> listOf(it)
                }
            }

        return resolvedElements
            .withoutReferenceLink()
            .map { PsiDocumentationContent(it, tag) }
    }

    private fun PsiDocComment.resolveTag(tag: JavadocTag): List<PsiElement> {
        return when (tag) {
            DescriptionJavadocTag -> this.descriptionElements.toList()
            else -> this.findTagsByName(tag.name).toList()
        }
    }

    private fun List<PsiElement>.withoutReferenceLink(): List<PsiElement> = drop(1)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as JavaDocComment

        if (comment != other.comment) return false

        return true
    }

    override fun hashCode(): Int {
        return comment.hashCode()
    }
}

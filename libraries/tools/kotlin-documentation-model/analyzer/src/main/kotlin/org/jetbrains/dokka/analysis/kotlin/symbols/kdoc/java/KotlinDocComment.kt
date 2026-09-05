/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.java

import org.jetbrains.dokka.analysis.java.*
import org.jetbrains.dokka.analysis.java.doccomment.DocComment
import org.jetbrains.dokka.analysis.java.doccomment.DocumentationContent
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.KtElement

internal class ResolveDocContext(val ktElement: KtElement)

internal class KotlinDocComment(
    val comment: KDocTag,
    val resolveDocContext: ResolveDocContext
) : DocComment {

    private val tagsWithContent: List<KDocTag> = comment.children.mapNotNull { (it as? KDocTag) }

    override fun hasTag(tag: JavadocTag): Boolean {
        return when (tag) {
            is DescriptionJavadocTag -> comment.getContent().isNotEmpty()
            is ThrowingExceptionJavadocTag -> tagsWithContent.any { it.hasException(tag) }
            else -> tagsWithContent.any { it.text.startsWith("@${tag.name}") }
        }
    }

    private fun KDocTag.hasException(tag: ThrowingExceptionJavadocTag) =
        text.startsWith("@${tag.name}") && getSubjectName() == tag.exceptionQualifiedName

    override fun resolveTag(tag: JavadocTag): List<DocumentationContent> {
        return when (tag) {
            is DescriptionJavadocTag -> listOf(DescriptorDocumentationContent(resolveDocContext, comment, tag))
            is ParamJavadocTag -> {
                val resolvedContent = resolveGeneric(tag)
                listOf(resolvedContent[tag.paramIndex])
            }

            is ThrowsJavadocTag -> resolveThrowingException(tag)
            is ExceptionJavadocTag -> resolveThrowingException(tag)
            else -> resolveGeneric(tag)
        }
    }

    private fun resolveThrowingException(tag: ThrowingExceptionJavadocTag): List<DescriptorDocumentationContent> {
        val exceptionName = tag.exceptionQualifiedName ?: return resolveGeneric(tag)

        return comment.children
            .filterIsInstance<KDocTag>()
            .filter { it.name == tag.name && it.getSubjectName() == exceptionName }
            .map { DescriptorDocumentationContent(resolveDocContext, it, tag) }
    }

    private fun resolveGeneric(tag: JavadocTag): List<DescriptorDocumentationContent> {
        return comment.children.mapNotNull { element ->
            if (element is KDocTag && element.name == tag.name) {
                DescriptorDocumentationContent(resolveDocContext, element, tag)
            } else {
                null
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KotlinDocComment

        if (comment != other.comment) return false
        //if (resolveDocContext.name != other.resolveDocContext.name) return false
        if (tagsWithContent != other.tagsWithContent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = comment.hashCode()
       // result = 31 * result + resolveDocContext.name.hashCode()
        result = 31 * result + tagsWithContent.hashCode()
        return result
    }
}

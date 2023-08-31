/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.parsers.doctag

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.javadoc.PsiDocComment
import org.jetbrains.dokka.analysis.java.*
import org.jetbrains.dokka.analysis.java.doccomment.*
import org.jetbrains.dokka.analysis.java.doccomment.JavaDocComment
import org.jetbrains.dokka.analysis.java.parsers.CommentResolutionContext

internal class InheritDocTagResolver(
    private val docCommentFactory: DocCommentFactory,
    private val docCommentFinder: DocCommentFinder,
    private val contentProviders: List<InheritDocTagContentProvider>
) {
    internal fun convertToHtml(content: DocumentationContent, docTagParserContext: DocTagParserContext): String? {
        return contentProviders
            .firstOrNull { it.canConvert(content) }
            ?.convertToHtml(content, docTagParserContext)
    }

    internal fun resolveContent(context: CommentResolutionContext): List<DocumentationContent>? {
        val javadocTag = context.tag ?: return null

        return when (javadocTag) {
            is ThrowingExceptionJavadocTag -> {
                javadocTag.exceptionQualifiedName?.let { _ ->
                    resolveThrowsTag(
                        javadocTag,
                        context.comment,
                    )
                } ?: return null
            }
            is ParamJavadocTag -> resolveParamTag(context.comment, javadocTag)
            is DeprecatedJavadocTag -> resolveGenericTag(context.comment, DescriptionJavadocTag)
            is SeeJavadocTag -> emptyList()
            else -> resolveGenericTag(context.comment, javadocTag)
        }
    }

    private fun resolveGenericTag(currentElement: PsiDocComment, tag: JavadocTag): List<DocumentationContent> {
        val docComment = when (val owner = currentElement.owner) {
            is PsiClass -> lowestClassWithTag(owner, tag)
            is PsiMethod -> lowestMethodWithTag(owner, tag)
            else -> null
        }
        return docComment?.resolveTag(tag)?.flatMap {
            it.resolveSiblings()
        }.orEmpty()
    }

    /**
     * Main resolution point for exception like tags
     *
     * This should be used only with [ThrowsJavadocTag] or [ExceptionJavadocTag] as their resolution path should be the same
     */
    private fun resolveThrowsTag(
        tag: ThrowingExceptionJavadocTag,
        currentElement: PsiDocComment,
    ): List<DocumentationContent> {
        val closestDocsWithThrows =
            (currentElement.owner as? PsiMethod)?.let { method -> lowestMethodsWithTag(method, tag) }
                .orEmpty().firstOrNull {
                    docCommentFinder.findClosestToElement(it)?.hasTag(tag) == true
                } ?: return emptyList()

        return docCommentFactory.fromElement(closestDocsWithThrows)
            ?.resolveTag(tag)
            ?: emptyList()
    }

    private fun resolveParamTag(
        currentElement: PsiDocComment,
        paramTag: ParamJavadocTag,
    ): List<DocumentationContent> {
        val parameterIndex = paramTag.paramIndex

        val methods = (currentElement.owner as? PsiMethod)
            ?.let { method -> lowestMethodsWithTag(method, paramTag) }
            .orEmpty()

        return methods.flatMap {
            if (parameterIndex >= it.parameterList.parametersCount || parameterIndex < 0) {
                return@flatMap emptyList()
            }

            val closestTag = docCommentFinder.findClosestToElement(it)
            val hasTag = closestTag?.hasTag(paramTag) ?: false
            closestTag?.takeIf { hasTag }?.resolveTag(ParamJavadocTag(it, "", parameterIndex)) ?: emptyList()
        }
    }

    //if we are in psi class javadoc only inherits docs from classes and not from interfaces
    private fun lowestClassWithTag(baseClass: PsiClass, javadocTag: JavadocTag): DocComment? =
        baseClass.superClass?.let {
            docCommentFinder.findClosestToElement(it)?.takeIf { tag -> tag.hasTag(javadocTag) } ?: lowestClassWithTag(
                it,
                javadocTag
            )
        }

    private fun lowestMethodWithTag(
        baseMethod: PsiMethod,
        javadocTag: JavadocTag,
    ): DocComment? {
        val methodsWithTag = lowestMethodsWithTag(baseMethod, javadocTag).firstOrNull()
        return methodsWithTag?.let {
            it.docComment?.let { JavaDocComment(it) } ?: docCommentFinder.findClosestToElement(it)
        }
    }

    private fun lowestMethodsWithTag(baseMethod: PsiMethod, javadocTag: JavadocTag): List<PsiMethod> =
        baseMethod.findSuperMethods().filter { docCommentFinder.findClosestToElement(it)?.hasTag(javadocTag) == true }
}

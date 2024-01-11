/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.parsers

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.JavaDocElementType
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.javadoc.PsiDocTag

import org.jetbrains.dokka.analysis.java.*
import org.jetbrains.dokka.analysis.java.doccomment.DocComment
import org.jetbrains.dokka.analysis.java.doccomment.JavaDocComment
import org.jetbrains.dokka.analysis.java.parsers.doctag.PsiDocTagParser
import org.jetbrains.dokka.analysis.java.util.*
import org.jetbrains.dokka.analysis.kotlin.markdown.MARKDOWN_ELEMENT_FILE_NAME
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.model.doc.Deprecated

internal class JavaPsiDocCommentParser(
    private val psiDocTagParser: PsiDocTagParser,
) : DocCommentParser {

    override fun canParse(docComment: DocComment): Boolean {
        return docComment is JavaDocComment
    }

    override fun parse(docComment: DocComment, context: PsiNamedElement): DocumentationNode {
        val javaDocComment = docComment as JavaDocComment
        return parsePsiDocComment(javaDocComment.comment, context)
    }

    internal fun parsePsiDocComment(docComment: PsiDocComment, context: PsiNamedElement): DocumentationNode {
        val description = listOfNotNull(docComment.getDescription())
        val tags = docComment.tags.mapNotNull { tag ->
            parseDocTag(tag, docComment, context)
        }
        return DocumentationNode(description + tags)
    }

    private fun PsiDocComment.getDescription(): Description? {
        val docTags = psiDocTagParser.parseAsParagraph(
            psiElements = descriptionElements.asIterable(),
            commentResolutionContext = CommentResolutionContext(this, DescriptionJavadocTag),
        )
        return docTags.takeIf { it.isNotEmpty() }?.let {
            Description(wrapTagIfNecessary(it))
        }
    }

    private fun parseDocTag(tag: PsiDocTag, docComment: PsiDocComment, analysedElement: PsiNamedElement): TagWrapper? {
        return when (tag.name) {
            ParamJavadocTag.name -> parseParamTag(tag, docComment, analysedElement)
            ThrowsJavadocTag.name, ExceptionJavadocTag.name -> parseThrowsTag(tag, docComment)
            ReturnJavadocTag.name -> parseReturnTag(tag, docComment)
            SinceJavadocTag.name -> parseSinceTag(tag, docComment)
            AuthorJavadocTag.name -> parseAuthorTag(tag, docComment)
            SeeJavadocTag.name -> parseSeeTag(tag, docComment)
            DeprecatedJavadocTag.name -> parseDeprecatedTag(tag, docComment)
            else -> emptyTagWrapper(tag, docComment)
        }
    }

    private fun parseParamTag(
        tag: PsiDocTag,
        docComment: PsiDocComment,
        analysedElement: PsiNamedElement
    ): TagWrapper? {
        val paramName = tag.dataElements.firstOrNull()?.text.orEmpty()
        val paramIndex = when (analysedElement) {
            // for functions `@param` can be used with both generics and arguments
            //  if it's for generics,
            //  then `paramName` will be in the form of `<T>`, where `T` is a type parameter name
            is PsiMethod -> when {
                paramName.startsWith('<') -> {
                    val pName = paramName.removeSurrounding("<", ">")
                    analysedElement.typeParameters.indexOfFirst { it.name == pName }
                }

                else -> analysedElement.parameterList.parameters.indexOfFirst { it.name == paramName }
            }

            // for classes `@param` can be used with generics and `record` components
            is PsiClass -> when {
                paramName.startsWith('<') -> {
                    val pName = paramName.removeSurrounding("<", ">")
                    analysedElement.typeParameters.indexOfFirst { it.name == pName }
                }

                else -> analysedElement.recordComponents.indexOfFirst { it.name == paramName }
            }

            // if `@param` tag is on any other element - ignore it
            else -> return null
        }

        val docTags = psiDocTagParser.parseAsParagraph(
            psiElements = tag.contentElementsWithSiblingIfNeeded().drop(1),
            commentResolutionContext = CommentResolutionContext(
                comment = docComment,
                tag = ParamJavadocTag(paramName, paramIndex)
            )
        )
        return Param(root = wrapTagIfNecessary(docTags), name = paramName)
    }

    private fun parseThrowsTag(
        tag: PsiDocTag,
        docComment: PsiDocComment
    ): Throws {
        val resolvedElement = tag.resolveToElement()
        val exceptionAddress = resolvedElement?.let { DRI.from(it) }

        /* we always would like to have a fully qualified name as name,
         * because it will be used as a display name later and we would like to have those unified
         * even if documentation states shortened version
         * Only if dri search fails we should use the provided phrase (since then we are not able to get a fq name)
         */
        val fullyQualifiedExceptionName =
            resolvedElement?.getKotlinFqName() ?: tag.dataElements.firstOrNull()?.text.orEmpty()

        val javadocTag = when (tag.name) {
            ThrowsJavadocTag.name -> ThrowsJavadocTag(fullyQualifiedExceptionName)
            ExceptionJavadocTag.name -> ExceptionJavadocTag(fullyQualifiedExceptionName)
            else -> throw IllegalArgumentException("Expected @throws or @exception")
        }

        val docTags = psiDocTagParser.parseAsParagraph(
            psiElements = tag.dataElements.drop(1),
            commentResolutionContext = CommentResolutionContext(
                comment = docComment,
                tag = javadocTag
            ),
        )
        return Throws(
            root = wrapTagIfNecessary(docTags),
            name = fullyQualifiedExceptionName,
            exceptionAddress = exceptionAddress
        )
    }

    private fun parseReturnTag(
        tag: PsiDocTag,
        docComment: PsiDocComment
    ): Return {
        val docTags = psiDocTagParser.parseAsParagraph(
            psiElements = tag.contentElementsWithSiblingIfNeeded(),
            commentResolutionContext = CommentResolutionContext(comment = docComment, tag = ReturnJavadocTag),
        )
        return Return(root = wrapTagIfNecessary(docTags))
    }

    private fun parseSinceTag(
        tag: PsiDocTag,
        docComment: PsiDocComment
    ): Since {
        val docTags = psiDocTagParser.parseAsParagraph(
            psiElements = tag.contentElementsWithSiblingIfNeeded(),
            commentResolutionContext = CommentResolutionContext(comment = docComment, tag = ReturnJavadocTag),
        )
        return Since(root = wrapTagIfNecessary(docTags))
    }

    private fun parseAuthorTag(
        tag: PsiDocTag,
        docComment: PsiDocComment
    ): Author {
        // TODO [beresnev] see what the hell this is
        // Workaround: PSI returns first word after @author tag as a `DOC_TAG_VALUE_ELEMENT`,
        // then the rest as a `DOC_COMMENT_DATA`, so for `Name Surname` we get them parted
        val docTags = psiDocTagParser.parseAsParagraph(
            psiElements = tag.contentElementsWithSiblingIfNeeded(),
            commentResolutionContext = CommentResolutionContext(comment = docComment, tag = AuthorJavadocTag),
        )
        return Author(root = wrapTagIfNecessary(docTags))
    }

    private fun parseSeeTag(
        tag: PsiDocTag,
        docComment: PsiDocComment
    ): See {
        val referenceElement = tag.referenceElement()
        val fullyQualifiedSeeReference = tag.resolveToElement()?.getKotlinFqName()
            ?: referenceElement?.text.orEmpty().removePrefix("#")

        val context = CommentResolutionContext(comment = docComment, tag = SeeJavadocTag(fullyQualifiedSeeReference))
        val docTags = psiDocTagParser.parseAsParagraph(
            psiElements = tag.dataElements.dropWhile {
                it is PsiWhiteSpace || it.isDocReferenceHolder() || it == referenceElement
            },
            commentResolutionContext = context,
        )

        return See(
            root = wrapTagIfNecessary(docTags),
            name = fullyQualifiedSeeReference,
            address = referenceElement?.toDocumentationLink(context = context)?.dri
        )
    }

    private fun PsiElement.isDocReferenceHolder(): Boolean {
        return (this as? LazyParseablePsiElement)?.elementType == JavaDocElementType.DOC_REFERENCE_HOLDER
    }

    private fun parseDeprecatedTag(
        tag: PsiDocTag,
        docComment: PsiDocComment
    ): Deprecated {
        val docTags = psiDocTagParser.parseAsParagraph(
            tag.contentElementsWithSiblingIfNeeded(),
            CommentResolutionContext(comment = docComment, tag = DeprecatedJavadocTag),
        )
        return Deprecated(root = wrapTagIfNecessary(docTags))
    }

    private fun wrapTagIfNecessary(tags: List<DocTag>): CustomDocTag {
        val isFile = (tags.singleOrNull() as? CustomDocTag)?.name == MARKDOWN_ELEMENT_FILE_NAME
        return if (isFile) {
            tags.first() as CustomDocTag
        } else {
            CustomDocTag(tags, name = MARKDOWN_ELEMENT_FILE_NAME)
        }
    }

    // Wrapper for unsupported tags https://github.com/Kotlin/dokka/issues/1618
    private fun emptyTagWrapper(
        tag: PsiDocTag,
        docComment: PsiDocComment,
    ): CustomTagWrapper {
        val docTags = psiDocTagParser.parseAsParagraph(
            psiElements = tag.contentElementsWithSiblingIfNeeded(),
            commentResolutionContext = CommentResolutionContext(docComment, null),
        )
        return CustomTagWrapper(
            root = wrapTagIfNecessary(docTags),
            name = tag.name
        )
    }

    private fun PsiElement.toDocumentationLink(labelElement: PsiElement? = null, context: CommentResolutionContext): DocumentationLink? {
        val resolvedElement = this.resolveToGetDri() ?: return null
        val label = labelElement ?: defaultLabel()
        val docTags = psiDocTagParser.parse(listOfNotNull(label), context)
        return DocumentationLink(dri = DRI.from(resolvedElement), children = docTags)
    }
}

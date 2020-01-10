/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.kotlin.kaptlite.kdoc

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin

object KDocParsingHelper {
    enum class DeclarationKind {
        CLASS, METHOD, FIELD
    }

    fun getKDocComment(kind: DeclarationKind, origin: JvmDeclarationOrigin, bindingContext: BindingContext): String? {
        val psiElement = origin.element as? KtDeclaration ?: return null
        val descriptor = origin.descriptor
        val docComment = psiElement.docComment ?: return null

        if (descriptor is ConstructorDescriptor && psiElement is KtClassOrObject) {
            // We don't want the class comment to be duplicated on <init>()
            return null
        }

        if (kind == DeclarationKind.METHOD
            && psiElement is KtProperty
            && descriptor is PropertyAccessorDescriptor
            && bindingContext[BindingContext.BACKING_FIELD_REQUIRED, descriptor.correspondingProperty] == true
        ) {
            // Do not place documentation on backing field and property accessors
            return null
        }

        if (kind == DeclarationKind.FIELD && psiElement is KtObjectDeclaration && descriptor == null) {
            // Do not write KDoc on object instance field
            return null
        }

        return escapeNestedComments(extractCommentText(docComment))
    }

    private fun escapeNestedComments(text: String): String {
        val result = StringBuilder()

        var index = 0
        var commentLevel = 0

        while (index < text.length) {
            val currentChar = text[index]
            fun nextChar() = text.getOrNull(index + 1)

            if (currentChar == '/' && nextChar() == '*') {
                commentLevel++
                index++
                result.append("/ *")
            } else if (currentChar == '*' && nextChar() == '/') {
                commentLevel = maxOf(0, commentLevel - 1)
                index++
                result.append("* /")
            } else {
                result.append(currentChar)
            }

            index++
        }

        return result.toString()
    }

    private fun extractCommentText(docComment: KDoc): String {
        return buildString {
            docComment.accept(object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    if (element is LeafPsiElement) {
                        if (element.isKDocLeadingAsterisk()) {
                            val indent = takeLastWhile { it == ' ' || it == '\t' }.length
                            if (indent > 0) {
                                delete(length - indent, length)
                            }
                        } else if (!element.isKDocStart() && !element.isKDocEnd()) {
                            append(element.text)
                        }
                    }

                    super.visitElement(element)
                }
            })
        }.trimIndent().trim()
    }

    private fun LeafPsiElement.isKDocStart() = elementType == KDocTokens.START
    private fun LeafPsiElement.isKDocEnd() = elementType == KDocTokens.END
    private fun LeafPsiElement.isKDocLeadingAsterisk() = elementType == KDocTokens.LEADING_ASTERISK
}
/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter.trailingComma

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiUtilCore
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.utils.addToStdlib.cast

fun trailingCommaIsAllowedOnCallSite(): Boolean = Registry.`is`("kotlin.formatter.allowTrailingCommaOnCallSite")

private val TYPES_WITH_TRAILING_COMMA_ON_DECLARATION_SITE = TokenSet.create(
    KtNodeTypes.TYPE_PARAMETER_LIST,
    KtNodeTypes.DESTRUCTURING_DECLARATION,
    KtNodeTypes.WHEN_ENTRY,
    KtNodeTypes.FUNCTION_LITERAL,
    KtNodeTypes.VALUE_PARAMETER_LIST,
)

private val TYPES_WITH_TRAILING_COMMA_ON_CALL_SITE = TokenSet.create(
    KtNodeTypes.COLLECTION_LITERAL_EXPRESSION,
    KtNodeTypes.TYPE_ARGUMENT_LIST,
    KtNodeTypes.INDICES,
    KtNodeTypes.VALUE_ARGUMENT_LIST,
)

private val TYPES_WITH_TRAILING_COMMA = TokenSet.orSet(
    TYPES_WITH_TRAILING_COMMA_ON_DECLARATION_SITE,
    TYPES_WITH_TRAILING_COMMA_ON_CALL_SITE,
)

fun UserDataHolder.addTrailingCommaIsAllowedForThis(): Boolean {
    val type = elementType(this) ?: return false
    return type in TYPES_WITH_TRAILING_COMMA_ON_DECLARATION_SITE ||
            trailingCommaIsAllowedOnCallSite() && type in TYPES_WITH_TRAILING_COMMA_ON_CALL_SITE
}

fun KotlinCodeStyleSettings.addTrailingCommaIsAllowedFor(element: UserDataHolder): Boolean =
    ALLOW_TRAILING_COMMA && element.addTrailingCommaIsAllowedForThis()

private fun elementType(userDataHolder: UserDataHolder): IElementType? = when (userDataHolder) {
    is ASTNode -> PsiUtilCore.getElementType(userDataHolder)
    is PsiElement -> PsiUtilCore.getElementType(userDataHolder)
    else -> null
}

fun ASTNode.canAddTrailingComma(): Boolean = psi?.canAddTrailingComma() == true
fun PsiElement.canAddTrailingComma(): Boolean =
    if (this is KtWhenEntry && (isElse || parent.cast<KtWhenExpression>().leftParenthesis == null))
        false
    else
        canAddTrailingComma(this)

private fun canAddTrailingComma(userDataHolder: UserDataHolder): Boolean = elementType(userDataHolder) in TYPES_WITH_TRAILING_COMMA

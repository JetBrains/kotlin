package com.jetbrains.kotlin.structuralsearch.impl.matcher

import com.google.common.collect.ImmutableBiMap
import com.intellij.psi.PsiComment
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.expressions.OperatorConventions

fun getCommentText(comment: PsiComment): String {
    return when (comment.tokenType) {
        KtTokens.EOL_COMMENT -> comment.text.drop(2)
        KtTokens.BLOCK_COMMENT -> comment.text.drop(2).dropLast(2)
        else -> ""
    }
}

private val BINARY_EXPR_OP_NAMES = ImmutableBiMap.builder<KtSingleValueToken, Name>()
    .putAll(OperatorConventions.ASSIGNMENT_OPERATIONS)
    .putAll(OperatorConventions.BINARY_OPERATION_NAMES)
    .build()

fun IElementType.binaryExprOpName(): Name? = BINARY_EXPR_OP_NAMES[this]
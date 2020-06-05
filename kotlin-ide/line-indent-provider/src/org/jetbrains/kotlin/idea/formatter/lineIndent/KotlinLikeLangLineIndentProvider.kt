/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter.lineIndent

import com.intellij.formatting.Indent
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.lineIndent.LineIndentProvider
import com.intellij.psi.impl.source.codeStyle.SemanticEditorPosition
import com.intellij.psi.impl.source.codeStyle.lineIndent.IndentCalculator
import com.intellij.psi.impl.source.codeStyle.lineIndent.JavaLikeLangLineIndentProvider
import com.intellij.psi.impl.source.codeStyle.lineIndent.JavaLikeLangLineIndentProvider.JavaLikeElement.*
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.formatter.lineIndent.KotlinLikeLangLineIndentProvider.KotlinElement.*
import org.jetbrains.kotlin.lexer.KtTokens

abstract class KotlinLikeLangLineIndentProvider : JavaLikeLangLineIndentProvider() {
    abstract fun indentionSettings(project: Project): KotlinIndentationAdjuster

    override fun mapType(tokenType: IElementType): SemanticEditorPosition.SyntaxElement? = SYNTAX_MAP[tokenType]

    override fun isSuitableForLanguage(language: Language): Boolean = language.isKindOf(KotlinLanguage.INSTANCE)

    private fun debugInfo(currentPosition: SemanticEditorPosition): String {
        val after = currentPosition.after()
        val before = currentPosition.before()
        val chars = currentPosition.chars
        fun print(position: SemanticEditorPosition, next: SemanticEditorPosition? = null) = "${position.currElement} =>\n'${
            if (position.isAtEnd)
                "end"
            else
                chars.subSequence(position.startOffset, next?.takeIf { !it.isAtEnd }?.startOffset ?: chars.length)
        }'"

        return "==\nbefore ${
            print(before, currentPosition)
        }\ncurr ${
            print(currentPosition, after)
        }\nafter ${
            print(after)
        }\n=="
    }

    override fun getLineIndent(project: Project, editor: Editor, language: Language?, offset: Int): String? {
        // HACK: TODO: KT-34566 investigate this hack (necessary for [org.jetbrains.kotlin.idea.editor.Kotlin.MultilineStringEnterHandler])
        return if (offset > 0 && getPosition(editor, offset - 1).isAt(RegularStringPart))
            LineIndentProvider.DO_NOT_ADJUST
        else
            super.getLineIndent(project, editor, language, offset)
    }

    override fun getIndent(project: Project, editor: Editor, language: Language?, offset: Int): IndentCalculator? {
        val factory = IndentCalculatorFactory(project, editor)
        val currentPosition = getPosition(editor, offset)
        if (!currentPosition.matchesRule { it.isAt(Whitespace) && it.isAtMultiline }) return null

        // ~~~ TESTING ~~~
//        println(debugInfo(currentPosition))
        // ~~~ TESTING ~~~

        val before = currentPosition.beforeOptionalMix(*WHITE_SPACE_OR_COMMENT_BIT_SET)
        val after = currentPosition.afterOptionalMix(*WHITE_SPACE_OR_COMMENT_BIT_SET)
        when {
            before.isAt(TemplateEntryOpen) -> {
                val indent = if (!currentPosition.hasLineBreaksAfter(offset) && after.isAt(TemplateEntryClose))
                    Indent.getNoneIndent()
                else
                    Indent.getNormalIndent()

                return factory.createIndentCalculator(indent, before.startOffset)
            }

            before.isAtAnyOf(TryKeyword) || before.isFinallyKeyword() -> return factory.createIndentCalculator(
                Indent.getNoneIndent(),
                IndentCalculator.LINE_BEFORE,
            )

            after.isAt(TemplateEntryClose) -> {
                val indent = if (currentPosition.hasEmptyLineAfter(offset)) Indent.getNormalIndent() else Indent.getNoneIndent()
                after.moveBeforeParentheses(TemplateEntryOpen, TemplateEntryClose)
                return factory.createIndentCalculator(indent, after.startOffset)
            }
        }

        return before.controlFlowStatementBefore()?.let { controlFlowKeywordPosition ->
            val indent = when {
                controlFlowKeywordPosition.similarToCatchKeyword() -> if (before.isAt(RightParenthesis)) Indent.getNoneIndent() else Indent.getNormalIndent()
                after.isAt(LeftParenthesis) -> Indent.getContinuationIndent()
                after.isAtAnyOf(BlockOpeningBrace, Arrow) || controlFlowKeywordPosition.isWhileInsideDoWhile() -> Indent.getNoneIndent()
                else -> Indent.getNormalIndent()
            }

            factory.createIndentCalculator(indent, IndentCalculator.LINE_BEFORE)
        }
    }

    private fun SemanticEditorPosition.isWhileInsideDoWhile(): Boolean {
        if (!isAt(WhileKeyword)) return false
        with(copy()) {
            moveBefore()
            var whileKeywordLevel = 1
            while (!isAtEnd) when {
                isAt(BlockOpeningBrace) -> return false
                isAt(DoKeyword) -> {
                    if (--whileKeywordLevel == 0) return true
                    moveBefore()
                }
                isAt(WhileKeyword) -> {
                    ++whileKeywordLevel
                    moveBefore()
                }
                isAt(BlockClosingBrace) -> moveBeforeParentheses(BlockOpeningBrace, BlockClosingBrace)
                else -> moveBefore()
            }
        }

        return false
    }

    private fun SemanticEditorPosition.controlFlowStatementBefore(): SemanticEditorPosition? = with(copy()) {
        if (isAt(BlockOpeningBrace)) moveBeforeIgnoringWhiteSpaceOrComment()

        if (isControlFlowKeyword()) return this
        if (!moveBeforeParenthesesIfPossible()) return null

        return takeIf { isControlFlowKeyword() }
    }

    private fun SemanticEditorPosition.isControlFlowKeyword(): Boolean =
        currElement in CONTROL_FLOW_KEYWORDS || isCatchKeyword() || isFinallyKeyword()

    private fun SemanticEditorPosition.similarToCatchKeyword(): Boolean = textOfCurrentPosition() == KtTokens.CATCH_KEYWORD.value

    private fun SemanticEditorPosition.isCatchKeyword(): Boolean = with(copy()) {
        // try-catch-*-catch
        do {
            if (!isAt(KtTokens.IDENTIFIER)) return false
            if (!similarToCatchKeyword()) return false

            moveBeforeIgnoringWhiteSpaceOrComment()
            if (!moveBeforeBlockIfPossible()) return false

            if (isAt(TryKeyword)) return true

            if (!moveBeforeParenthesesIfPossible()) return false
        } while (!isAtEnd)

        return false
    }

    private fun SemanticEditorPosition.isFinallyKeyword(): Boolean {
        if (!isAt(KtTokens.IDENTIFIER)) return false
        if (textOfCurrentPosition() != KtTokens.FINALLY_KEYWORD.value) return false
        with(copy()) {
            moveBeforeIgnoringWhiteSpaceOrComment()
            if (!moveBeforeBlockIfPossible()) return false

            // try-finally
            if (isAt(TryKeyword)) return true

            if (!moveBeforeParenthesesIfPossible()) return false

            // try-catch-finally
            return isCatchKeyword()
        }
    }

    private fun SemanticEditorPosition.moveBeforeBlockIfPossible(): Boolean = moveBeforeParenthesesIfPossible(
        leftParenthesis = BlockOpeningBrace,
        rightParenthesis = BlockClosingBrace
    )

    private fun SemanticEditorPosition.moveBeforeParenthesesIfPossible(): Boolean = moveBeforeParenthesesIfPossible(
        leftParenthesis = LeftParenthesis,
        rightParenthesis = RightParenthesis
    )

    private fun SemanticEditorPosition.moveBeforeParenthesesIfPossible(
        leftParenthesis: SemanticEditorPosition.SyntaxElement,
        rightParenthesis: SemanticEditorPosition.SyntaxElement,
    ): Boolean {
        if (!isAt(rightParenthesis)) return false

        moveBeforeParentheses(leftParenthesis, rightParenthesis)
        moveBeforeIfThisIsWhiteSpaceOrComment()
        return true
    }

    private fun SemanticEditorPosition.moveBeforeIfThisIsWhiteSpaceOrComment() = moveBeforeOptionalMix(*WHITE_SPACE_OR_COMMENT_BIT_SET)

    private fun SemanticEditorPosition.moveBeforeIgnoringWhiteSpaceOrComment() {
        moveBefore()
        moveBeforeIfThisIsWhiteSpaceOrComment()
    }

    private enum class KotlinElement : SemanticEditorPosition.SyntaxElement {
        TemplateEntryOpen,
        TemplateEntryClose,
        Arrow,
        WhenKeyword,
        WhileKeyword,
        RegularStringPart,
        KDoc,
    }

    companion object {
        private val SYNTAX_MAP: Map<IElementType, SemanticEditorPosition.SyntaxElement> = hashMapOf(
            KtTokens.WHITE_SPACE to Whitespace,
            KtTokens.LONG_TEMPLATE_ENTRY_START to TemplateEntryOpen,
            KtTokens.LONG_TEMPLATE_ENTRY_END to TemplateEntryClose,
            KtTokens.EOL_COMMENT to LineComment,
            KtTokens.BLOCK_COMMENT to BlockComment,
            KtTokens.DOC_COMMENT to KDoc,
            KtTokens.ARROW to Arrow,
            KtTokens.LBRACE to BlockOpeningBrace,
            KtTokens.RBRACE to BlockClosingBrace,
            KtTokens.LPAR to LeftParenthesis,
            KtTokens.RPAR to RightParenthesis,
            KtTokens.IF_KEYWORD to IfKeyword,
            KtTokens.ELSE_KEYWORD to ElseKeyword,
            KtTokens.WHEN_KEYWORD to WhenKeyword,
            KtTokens.TRY_KEYWORD to TryKeyword,
            KtTokens.WHILE_KEYWORD to WhileKeyword,
            KtTokens.DO_KEYWORD to DoKeyword,
            KtTokens.FOR_KEYWORD to ForKeyword,
            KtTokens.REGULAR_STRING_PART to RegularStringPart,
            KtTokens.LBRACKET to ArrayOpeningBracket,
            KtTokens.RBRACKET to ArrayClosingBracket,
        )

        private val CONTROL_FLOW_KEYWORDS: HashSet<SemanticEditorPosition.SyntaxElement> = hashSetOf(
            WhenKeyword,
            IfKeyword,
            ElseKeyword,
            DoKeyword,
            WhileKeyword,
            ForKeyword,
            TryKeyword,
        )

        private val WHITE_SPACE_OR_COMMENT_BIT_SET: Array<SemanticEditorPosition.SyntaxElement> = arrayOf(
            Whitespace,
            LineComment,
            BlockComment,
        )
    }
}

private fun JavaLikeLangLineIndentProvider.IndentCalculatorFactory.createIndentCalculator(
    indent: Indent?,
    baseLineOffset: Int
): IndentCalculator? = createIndentCalculator(indent) { baseLineOffset }

private fun SemanticEditorPosition.textOfCurrentPosition(): String =
    if (isAtEnd) "" else chars.subSequence(startOffset, after().startOffset).toString()
/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.api.impl

import com.intellij.platform.syntax.lexer.performLexing
import com.intellij.platform.syntax.parser.SyntaxTreeBuilder
import com.intellij.platform.syntax.parser.SyntaxTreeBuilderFactory
import com.intellij.platform.syntax.parser.prepareProduction
import com.intellij.platform.syntax.util.lexer.LexerBase
import org.jetbrains.kotlin.kmp.lexer.KDocLexer
import org.jetbrains.kotlin.kmp.lexer.KDocTokens
import org.jetbrains.kotlin.kmp.lexer.KotlinLexer
import org.jetbrains.kotlin.kmp.lexer.KtTokens
import org.jetbrains.kotlin.kmp.parser.AbstractParser
import org.jetbrains.kotlin.kmp.parser.KDocLinkParser
import org.jetbrains.kotlin.kmp.parser.KDocParser
import org.jetbrains.kotlin.kmp.parser.KotlinParser
import org.jetbrains.kotlin.kmp.utils.Stack
import kotlin.script.experimental.api.ast.SyntaxElement

fun parseImpl(isScript: Boolean, text: String): SyntaxElement =
    parseToScriptParseElement(text, 0, KotlinLexer(), KotlinParser(isScript, false))

private fun parseToScriptParseElement(
    charSequence: CharSequence,
    start: Int,
    lexer: LexerBase,
    parser: AbstractParser,
): SyntaxElement {
    val syntaxTreeBuilder = SyntaxTreeBuilderFactory.builder(
        charSequence,
        performLexing(charSequence, lexer, cancellationProvider = null, logger = null),
        whitespaces = parser.whitespaces,
        comments = parser.comments,
    ).withStartOffset(start)
        .withWhitespaceOrCommentBindingPolicy(parser.whitespaceOrCommentBindingPolicy)
        .build()

    parser.parse(syntaxTreeBuilder)

    return convertToScriptParseElement(syntaxTreeBuilder, start)
}

private fun convertToScriptParseElement(builder: SyntaxTreeBuilder, start: Int): SyntaxElement {
    val productions = prepareProduction(builder).productionMarkers
    val tokens = builder.tokens

    val childrenStack = Stack<MutableList<SyntaxElement>>().apply {
        push(mutableListOf())
    }
    var prevTokenIndex = 0
    var lastErrorTokenIndex = -1

    fun MutableList<SyntaxElement>.appendLeafElements(lastTokenIndex: Int) {
        for (leafTokenIndex in prevTokenIndex until lastTokenIndex) {
            val tokenType = tokens.getTokenType(leafTokenIndex)!!
            val tokenStart = tokens.getTokenStart(leafTokenIndex) + start
            val tokenEnd = tokens.getTokenEnd(leafTokenIndex) + start

            if (tokenStart == tokenEnd) {
                // LightTree and PSI builders ignores empty leaf tokens by default (for instance, `DANGLING_NEWLINE`)
                continue
            }

            when (tokenType) {
                // `MARKDOWN_LINK` only can be encountered inside KDoc
                KDocTokens.MARKDOWN_LINK -> {
                    parseToScriptParseElement(
                        tokens.getTokenText(leafTokenIndex)!!,
                        tokenStart,
                        KotlinLexer(),
                        KDocLinkParser,
                    )
                }
                KtTokens.DOC_COMMENT -> {
                    parseToScriptParseElement(
                        tokens.getTokenText(leafTokenIndex)!!,
                        tokenStart,
                        KDocLexer(),
                        KDocParser,
                    )
                }
                else -> {
                    SyntaxElement.Term(
                        tokenType,
                        tokenStart, tokenEnd,
                        tokens.getTokenText(leafTokenIndex)!!,
                    )
                }
            }.let {
                add(it)
            }
        }
        prevTokenIndex = lastTokenIndex
    }

    for (productionIndex in 0 until productions.size) {
        val production = productions.getMarker(productionIndex)

        when {
            productions.isDoneMarker(productionIndex) -> {
                val lastChildren = childrenStack.pop()
                val children = if (production.isCollapsed()) {
                    // Ignore collapsed elements
                    prevTokenIndex = production.getEndTokenIndex()
                    emptyList()
                } else {
                    lastChildren.also { it.appendLeafElements(production.getEndTokenIndex()) }
                }

                // Here is the extension point to implement custom logic on finishing an element (for instance, creating a FIR node).
                // We have a parent element type, its children, and a previously initialized state from start marker.
                // Also, if such a bottom-up conversion is complicated, here we can initialize just an ordinary "super-light" tree
                // and convert it later in a builder like existing PSI/LightTree builders.
                // In addition, here we can skip whitespace or comment tokens that could improve performance a bit.

                childrenStack.peek().add(
                    SyntaxElement.Node(
                        production,
                        children,
                    )
                )
            }

            production.isErrorMarker() -> {
                val errorTokenIndex = production.getStartTokenIndex()
                if (errorTokenIndex == lastErrorTokenIndex) {
                    // Prevent inserting of duplicated error elements (obey `PsiBuilderImpl.prepareLightTree` implementation)
                    continue
                } else {
                    lastErrorTokenIndex = errorTokenIndex
                }
                childrenStack.peek().let {
                    it.appendLeafElements(errorTokenIndex)
                    it.add(
                        SyntaxElement.Node(
                            production,
                            emptyList(), // No children `isErrorMarker` is true only on leaf elements
                        )
                    )
                }
            }

            else -> {
                // Start marker

                // Here is the extension point to implement custom logic on starting visiting an element.
                // For instance, initialize some state during converting to FIR.
                // Element type is known, it's `production.getNodeType()`

                childrenStack.peek().appendLeafElements(production.getStartTokenIndex())
                childrenStack.push(mutableListOf())
            }
        }
    }

    return childrenStack.pop().single()
}

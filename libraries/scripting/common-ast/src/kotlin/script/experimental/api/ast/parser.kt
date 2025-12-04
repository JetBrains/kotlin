/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.api.ast

import com.intellij.platform.syntax.SyntaxElementType
import com.intellij.platform.syntax.parser.SyntaxTreeBuilder
import org.jetbrains.kotlin.KtSourceFileLinesMapping
import org.jetbrains.kotlin.kmp.lexer.KotlinLexer
import org.jetbrains.kotlin.kmp.parser.KotlinParser
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.impl.parseImpl

fun parseToAst(script: SourceCode): ParseNode<out Element> = parseImpl(true, script.text)

data class ParsingOptions(
    val retainComments: Boolean = false,
)

abstract class SyntaxElement<out T>(
    val name: String,
    val start: Int,
    val end: Int,
    val syntaxElement: T?,
    val children: List<SyntaxElement<T>>,
) {
    companion object {
        const val WRAPPER_SYNTAX_ELEMENT_NAME = "WRAPPER"
    }

    val isWrapper: Boolean
        get() = syntaxElement == null

    abstract val isErrorElement: Boolean

    fun countSyntaxElements(): SyntaxElementStats {
        var syntaxElementNumber = if (isWrapper) 0 else 1L // Count all nodes except wrappers, not only leaf ones
        var hasErrorElement = isErrorElement

        children.forEach {
            val (number, syntaxError) = it.countSyntaxElements()
            syntaxElementNumber += number
            hasErrorElement = hasErrorElement || syntaxError
        }

        return SyntaxElementStats(syntaxElementNumber, hasErrorElement)
    }

    data class SyntaxElementStats(val number: Long, val hasErrorElement: Boolean)
}


class ParseNode<T>(name: String, start: Int, end: Int, parseNode: T?, children: List<ParseNode<out T>>) :
    SyntaxElement<T>(name, start, end, parseNode, children) {
    override val isErrorElement: Boolean
        get() = name == "BAD_CHARACTER" || name == "ERROR_ELEMENT"
}

sealed class Element

class ElementToken(val token: SyntaxElementType) : Element()

class ElementNode(val production: SyntaxTreeBuilder.Production) : Element()


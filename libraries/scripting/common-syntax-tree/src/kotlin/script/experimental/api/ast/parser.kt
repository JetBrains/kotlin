/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.api.ast

import com.intellij.platform.syntax.SyntaxElementType
import com.intellij.platform.syntax.parser.SyntaxTreeBuilder
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.impl.parseImpl

fun parseToAst(script: SourceCode): SyntaxElement = parseImpl(true, script.text)

sealed class SyntaxElement {

    abstract val type: SyntaxElementType
    abstract val start: Int
    abstract val end: Int
    abstract val children: List<SyntaxElement>

    class Node(val value: SyntaxTreeBuilder.Production, override val children: List<SyntaxElement>) : SyntaxElement() {
        override val type: SyntaxElementType get() = value.getNodeType()
        override val start: Int get() = value.getStartOffset()
        override val end: Int get() = value.getEndOffset()
    }

    class Term(
        override val type: SyntaxElementType,
        override val start: Int,
        override val end: Int,
        val text: CharSequence
    ) : SyntaxElement() {
        override val children: List<SyntaxElement> get() = emptyList()
    }
}

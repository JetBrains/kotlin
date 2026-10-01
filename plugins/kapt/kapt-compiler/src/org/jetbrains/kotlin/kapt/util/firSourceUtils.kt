/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.util

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.findChildByType
import org.jetbrains.kotlin.diagnostics.findChildrenByType
import org.jetbrains.kotlin.lexer.KtTokens

/** Source ranges of supertypes written as constructor calls, e.g. `Foo()`. */
fun KtSourceElement.superTypeCallEntryRanges(): List<IntRange> {
    val superTypeList = treeStructure.findChildByType(lighterASTNode, KtNodeTypes.SUPER_TYPE_LIST) ?: return emptyList()

    return treeStructure.findChildrenByType(superTypeList, KtNodeTypes.SUPER_TYPE_CALL_ENTRY)
        .map { it.startOffset..it.endOffset }
}

/** Full KDoc text attached to this declaration, delimiters included. */
fun KtSourceElement.kdocText(): String? {
    val docComment = treeStructure.findChildByType(lighterASTNode, KtTokens.DOC_COMMENT)
    // KDoc before modifiers is nested under the modifier list.
        ?: treeStructure.findChildByType(lighterASTNode, KtNodeTypes.MODIFIER_LIST)
            ?.let { treeStructure.findChildByType(it, KtTokens.DOC_COMMENT) }
        ?: return null

    // Works for both light-tree and PSI-backed nodes.
    return treeStructure.toString(docComment).toString()
}

/** Strips KDoc delimiters and leading asterisks from raw KDoc text. */
fun String.kdocBody(): String {
    val withoutStart = removePrefix("/**")
    val withoutEnd = withoutStart.replace(KDOC_END, "")

    return withoutEnd.lineSequence()
        .mapIndexed { index, line -> if (index == 0) line else line.replaceFirst(KDOC_LEADING_ASTERISK, "") }
        .joinToString("\n")
        .trimIndent()
        .trim()
}

private val KDOC_END = Regex("""\*+/$""")

/** Matches the KDoc lexer's whitespace before a leading asterisk. */
private val KDOC_LEADING_ASTERISK = Regex("""^[ \t]*\*+""")

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.util

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.syntax.FirSyntaxComponent
import org.jetbrains.kotlin.fir.syntax.forEachChildOfType
import org.jetbrains.kotlin.fir.syntax.getChild
import org.jetbrains.kotlin.syntax.KotlinSyntaxNodeTypeIds
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds
import org.jetbrains.kotlin.syntax.SyntaxIdSet

/** Source ranges of supertypes written as constructor calls, e.g. `Foo()`. */
context(syntax: FirSyntaxComponent<*>)
fun KtSourceElement.superTypeCallEntryRanges(): List<IntRange> {
    val superTypeList = getChild(KotlinSyntaxNodeTypeIds.SUPER_TYPE_LIST, depth = 1) ?: return emptyList()

    return buildList {
        superTypeList.forEachChildOfType(SUPER_TYPE_CALL_ENTRY, depth = 1) { add(it.startOffset..it.endOffset) }
    }
}

private val SUPER_TYPE_CALL_ENTRY = SyntaxIdSet.of(KotlinSyntaxNodeTypeIds.SUPER_TYPE_CALL_ENTRY)

/** Full KDoc text attached to this declaration, delimiters included. */
context(syntax: FirSyntaxComponent<*>)
fun KtSourceElement.kdocText(): String? {
    val docComment = getChild(KotlinSyntaxTokenIds.DOC_COMMENT, depth = 1)
    // KDoc before modifiers is nested under the modifier list.
        ?: getChild(KotlinSyntaxNodeTypeIds.MODIFIER_LIST, depth = 1)?.getChild(KotlinSyntaxTokenIds.DOC_COMMENT, depth = 1)
        ?: return null

    return docComment.text.toString()
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

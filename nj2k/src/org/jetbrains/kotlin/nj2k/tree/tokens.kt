/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.tree


interface JKNonCodeElement {
    val text: String
}

interface JKSpaceElement : JKNonCodeElement

interface JKCommentElement : JKNonCodeElement

fun JKCommentElement.isMultiline() =
    text.startsWith("/*")

fun JKCommentElement.isSingleline() =
    text.startsWith("//")


interface JKNonCodeElementsListOwner {
    var leftNonCodeElements: List<JKNonCodeElement>
    var rightNonCodeElements: List<JKNonCodeElement>
}

fun JKNonCodeElementsListOwner.takeNonCodeElementsFrom(other: JKNonCodeElementsListOwner) {
    leftNonCodeElements = other.leftNonCodeElements
    rightNonCodeElements = other.rightNonCodeElements
}


fun JKNonCodeElementsListOwner.appendNonCodeElementsFrom(other: JKNonCodeElementsListOwner) {
    leftNonCodeElements = other.leftNonCodeElements + leftNonCodeElements
    rightNonCodeElements = rightNonCodeElements + other.rightNonCodeElements
}

fun List<JKNonCodeElement>.dropSpacesAtBegining() =
    dropWhile { it is JKSpaceElement }

fun JKTreeElement.commentsFromInside(): List<JKCommentElement> {
    val comments = mutableListOf<JKCommentElement>()
    fun recurse(element: JKTreeElement): JKTreeElement {
        comments += (element.leftNonCodeElements + element.rightNonCodeElements).filterIsInstance<JKCommentElement>()
        return applyRecursive(element, ::recurse)
    }
    applyRecursive(this, ::recurse)
    return comments
}

inline fun <reified T : JKNonCodeElementsListOwner> T.withNonCodeElementsFrom(other: JKNonCodeElementsListOwner): T =
    also { it.takeNonCodeElementsFrom(other) }

fun JKNonCodeElementsListOwner.clearNonCodeElements() {
    leftNonCodeElements = emptyList()
    rightNonCodeElements = emptyList()
}

interface JKTokenElement : JKNonCodeElementsListOwner {
    val text: String
}

fun JKNonCodeElementsListOwner.containsNewLine(): Boolean =
    (leftNonCodeElements + rightNonCodeElements).any {
        it is JKSpaceElement && '\n' in it.text
    }
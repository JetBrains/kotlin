/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.tree

import org.jetbrains.kotlin.utils.SmartList


interface JKNonCodeElement {
    val text: String
}

class JKSpaceElement(override val text: String) : JKNonCodeElement
class JKCommentElement(override val text: String) : JKNonCodeElement

class JKTokenElementImpl(override val text: String) : JKTokenElement {
    override val leftNonCodeElements: MutableList<JKNonCodeElement> = SmartList()
    override val rightNonCodeElements: MutableList<JKNonCodeElement> = SmartList()
}

interface JKNonCodeElementsListOwner {
    val leftNonCodeElements: MutableList<JKNonCodeElement>
    val rightNonCodeElements: MutableList<JKNonCodeElement>
}

fun JKNonCodeElementsListOwner.takeNonCodeElementsFrom(other: JKNonCodeElementsListOwner) {
    leftNonCodeElements += other.leftNonCodeElements
    rightNonCodeElements += other.rightNonCodeElements
}

inline fun <reified T : JKNonCodeElementsListOwner> T.withNonCodeElementsFrom(other: JKNonCodeElementsListOwner): T =
    also { it.takeNonCodeElementsFrom(other) }

inline fun <reified T : JKNonCodeElementsListOwner> List<T>.withNonCodeElementsFrom(other: JKNonCodeElementsListOwner): List<T> =
    also {
        if (isNotEmpty()) {
            it.first().leftNonCodeElements += other.leftNonCodeElements
            it.last().rightNonCodeElements += other.rightNonCodeElements
        }
    }

fun JKNonCodeElementsListOwner.clearNonCodeElements() {
    leftNonCodeElements.clear()
    rightNonCodeElements.clear()
}

interface JKTokenElement : JKNonCodeElementsListOwner {
    val text: String
}

fun JKNonCodeElementsListOwner.containsNewLine(): Boolean =
    (leftNonCodeElements + rightNonCodeElements).any {
        it is JKSpaceElement && '\n' in it.text
    }
/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.CommentsAndSpaces
import com.intellij.psi.PsiElement

fun <TElement: Element> TElement.assignPrototype(prototype: PsiElement?): TElement {
    assignPrototypeInfos(if (prototype != null) listOf(PrototypeInfo(prototype, true)) else listOf())
    return this
}

fun <TElement: Element> TElement.assignPrototypes(prototypes: List<PsiElement>, inheritBlankLinesBefore: Boolean): TElement {
    assignPrototypeInfos(prototypes.map { PrototypeInfo(it, inheritBlankLinesBefore) })
    return this
}

fun <TElement: Element> TElement.assignPrototypesFrom(element: Element): TElement {
    assignPrototypeInfos(element.prototypes)
    return this
}

data class PrototypeInfo(val element: PsiElement, val inheritBlankLinesBefore: Boolean)

abstract class Element {
    public var prototypes: List<PrototypeInfo> = listOf()
        private set

    public fun assignPrototypeInfos(prototypes: List<PrototypeInfo>) {
        this.prototypes = prototypes
    }

    public fun toKotlin(commentsAndSpaces: CommentsAndSpaces): String {
        return if (isEmpty) // do not insert comment and spaces for empty elements to avoid multiple blank lines
            ""
        else
            commentsAndSpaces.wrapElement({ toKotlinImpl(commentsAndSpaces) }, prototypes)
    }

    protected abstract fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces): String

    public open val isEmpty: Boolean get() = false

    object Empty : Element() {
        override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces) = ""
        override val isEmpty: Boolean get() = true
    }
}

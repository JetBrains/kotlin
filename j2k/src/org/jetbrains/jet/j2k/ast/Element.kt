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

import org.jetbrains.jet.j2k.*
import com.intellij.psi.PsiElement

fun <TElement: Element> TElement.assignPrototype(prototype: PsiElement?, inheritance: CommentsAndSpacesInheritance = CommentsAndSpacesInheritance()): TElement {
    prototypes = if (prototype != null) listOf(PrototypeInfo(prototype, inheritance)) else listOf()
    return this
}

fun <TElement: Element> TElement.assignPrototypes(prototypes: List<PsiElement>, inheritance: CommentsAndSpacesInheritance): TElement {
    this.prototypes = prototypes.map { PrototypeInfo(it, inheritance) }
    return this
}

fun <TElement: Element> TElement.assignNoPrototype(): TElement {
    prototypes = listOf()
    return this
}

fun <TElement: Element> TElement.assignPrototypesFrom(element: Element, inheritance: CommentsAndSpacesInheritance? = null): TElement {
    prototypes = element.prototypes
    if (inheritance != null) {
        prototypes = prototypes?.map { PrototypeInfo(it.element, inheritance) }
    }
    createdAt = element.createdAt
    return this
}

data class PrototypeInfo(val element: PsiElement, val commentsAndSpacesInheritance: CommentsAndSpacesInheritance)

data class CommentsAndSpacesInheritance(val blankLinesBefore: Boolean = true,
                                        val commentsBefore: Boolean = true,
                                        val commentsAfter: Boolean = true,
                                        val commentsInside: Boolean = true)

fun Element.canonicalCode(): String {
    val builder = CodeBuilder(null)
    builder.append(this)
    return builder.result
}

abstract class Element {
    public var prototypes: List<PrototypeInfo>? = null
      set(value) {
          // no prototypes assigned to empty elements because they can be singleton instances (and they are not needed anyway)
          if (isEmpty) {
              $prototypes = listOf()
              return
          }
          $prototypes = value
      }

    public var createdAt: String = "Element creation stacktraces turned off. Uncomment initializer of Element.createdAt." //Exception().getStackTrace().joinToString("\n")

    /** This method should not be used anywhere except for CodeBuilder! Use CodeBuilder.append instead. */
    public abstract fun generateCode(builder: CodeBuilder)

    public open fun postGenerateCode(builder: CodeBuilder) { }

    public open val isEmpty: Boolean get() = false

    object Empty : Element() {
        override fun generateCode(builder: CodeBuilder) { }
        override val isEmpty: Boolean get() = true
    }
}

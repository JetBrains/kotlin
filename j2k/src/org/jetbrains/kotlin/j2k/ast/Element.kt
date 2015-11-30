/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.j2k.ast

import org.jetbrains.kotlin.j2k.*
import com.intellij.psi.PsiElement

fun <TElement: Element> TElement.assignPrototype(prototype: PsiElement?, inheritance: CommentsAndSpacesInheritance = CommentsAndSpacesInheritance()): TElement {
    prototypes = if (prototype != null) listOf(PrototypeInfo(prototype, inheritance)) else listOf()
    return this
}

fun <TElement: Element> TElement.assignPrototypes(vararg prototypes: PrototypeInfo): TElement {
    this.prototypes = prototypes.asList()
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

enum class SpacesInheritance {
    NONE, BLANK_LINES_ONLY, LINE_BREAKS
}

data class CommentsAndSpacesInheritance(
        val spacesBefore: SpacesInheritance = SpacesInheritance.BLANK_LINES_ONLY,
        val commentsBefore: Boolean = true,
        val commentsAfter: Boolean = true,
        val commentsInside: Boolean = true
) {
    companion object {
        val NO_SPACES = CommentsAndSpacesInheritance(spacesBefore = SpacesInheritance.NONE)
        val LINE_BREAKS = CommentsAndSpacesInheritance(spacesBefore = SpacesInheritance.LINE_BREAKS)
    }
}

fun Element.canonicalCode(): String {
    val builder = CodeBuilder(null, EmptyDocCommentConverter)
    builder.append(this)
    return builder.resultText
}

abstract class Element {
    public var prototypes: List<PrototypeInfo>? = null
        set(value) {
            // do not assign prototypes to singleton instances
            if (canBeSingleton) {
                field = listOf()
                return
            }
            field = value
        }

    protected open val canBeSingleton: Boolean
        get() = isEmpty

    public var createdAt: String?
            = if (saveCreationStacktraces)
                  Exception().stackTrace.joinToString("\n")
              else
                  null

    /** This method should not be used anywhere except for CodeBuilder! Use CodeBuilder.append instead. */
    public abstract fun generateCode(builder: CodeBuilder)

    public open fun postGenerateCode(builder: CodeBuilder) { }

    public open val isEmpty: Boolean get() = false

    object Empty : Element() {
        override fun generateCode(builder: CodeBuilder) { }
        override val isEmpty: Boolean get() = true
    }

    companion object {
        var saveCreationStacktraces = false
    }
}

// this class should never be created directly - Converter.deferredElement() should be used!
class DeferredElement<TResult : Element>(
        private val generator: (CodeConverter) -> TResult,
        public val converterState: Converter.PersonalState
) : Element() {

    private var result: TResult? = null

    init {
        assignNoPrototype()
    }

    // need to override it to not use isEmpty
    override val canBeSingleton: Boolean
        get() = false

    public fun unfold(codeConverter: CodeConverter) {
        assert(result == null)
        result = generator(codeConverter)
    }

    override fun generateCode(builder: CodeBuilder) {
        resultNotNull.generateCode(builder)
    }

    override val isEmpty: Boolean
        get() = resultNotNull.isEmpty

    private val resultNotNull: TResult
        get() {
            assert(result != null) { "No code generated for deferred element $this. Possible reason is that it has been created directly instead of Converter.lazyElement() call." }
            return result!!
        }
}


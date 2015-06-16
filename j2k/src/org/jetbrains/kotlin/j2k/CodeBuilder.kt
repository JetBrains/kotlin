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

package org.jetbrains.kotlin.j2k

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.util.SmartList
import org.jetbrains.kotlin.j2k.ast.CommentsAndSpacesInheritance
import org.jetbrains.kotlin.j2k.ast.Element
import org.jetbrains.kotlin.j2k.ast.SpacesInheritance
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import java.util.ArrayList
import java.util.HashSet
import java.util.LinkedHashSet
import kotlin.platform.platformName

fun<T> CodeBuilder.append(generators: Collection<() -> T>, separator: String, prefix: String = "", suffix: String = ""): CodeBuilder {
    if (generators.isNotEmpty()) {
        append(prefix)
        var first = true
        for (generator in generators) {
            if (!first) {
                append(separator)
            }
            generator()
            first = false
        }
        append(suffix)
    }
    return this
}

platformName("appendElements")
fun CodeBuilder.append(elements: Collection<Element>, separator: String, prefix: String = "", suffix: String = ""): CodeBuilder {
    return append(elements.filter { !it.isEmpty }.map { { append(it) } }, separator, prefix, suffix)
}

class ElementCreationStackTraceRequiredException : RuntimeException()

class CodeBuilder(private val topElement: PsiElement?) {
    private val builder = StringBuilder()
    private var endOfLineCommentAtEnd = false

    private val commentsAndSpacesUsed = HashSet<PsiElement>()

    private val imports = LinkedHashSet<FqName>()

    public fun append(text: String): CodeBuilder
            = append(text, false)

    public fun addImport(fqName: FqName) {
        imports.add(fqName)
    }

    private fun appendCommentOrWhiteSpace(element: PsiElement) {
        if (element is PsiDocComment) {
            append(DocCommentConverter.convertDocComment(element), false)
        }
        else {
            append(element.getText()!!, element.isEndOfLineComment())
        }
    }

    private fun append(text: String, endOfLineComment: Boolean = false): CodeBuilder {
        if (text.isEmpty()) {
            assert(!endOfLineComment)
            return this
        }

        if (endOfLineCommentAtEnd) {
            if (text[0] != '\n' && text[0] != '\r') builder.append('\n')
            endOfLineCommentAtEnd = false
        }

        builder.append(text)
        endOfLineCommentAtEnd = endOfLineComment
        return this
    }

    public val resultText: String
        get() = builder.toString()

    public val importsToAdd: Set<FqName>
        get() = imports

    public fun append(element: Element): CodeBuilder {
        if (element.isEmpty) return this // do not insert comment and spaces for empty elements to avoid multiple blank lines

        if (element.prototypes == null && topElement != null) {
            if (element.createdAt == null) {
                throw ElementCreationStackTraceRequiredException()
            }
            else {
                val s = "Element $element has no prototypes assigned.\n" +
                        "Use Element.assignPrototype() or Element.assignNoPrototype().\n" +
                        "Element created at:\n${element.createdAt}"
                throw RuntimeException(s)
            }
        }

        if (topElement == null || topElement is PsiCompiledElement || element.prototypes!!.isEmpty()) {
            element.generateCode(this)
            element.postGenerateCode(this)
            return this
        }

        val notInsideElements = HashSet<PsiElement>()
        var prefix = Prefix.Empty
        var postfix = emptyList<PsiElement>()
        for ((prototype, inheritance) in element.prototypes!!) {
            assert(prototype !is PsiComment)
            assert(prototype !is PsiWhiteSpace)
            if (!topElement.isAncestor(prototype)) continue
            prefix += collectPrefixElements(prototype, inheritance, notInsideElements)
            postfix += collectPostfixElements(prototype, inheritance, notInsideElements)
        }

        if (prefix.lineBreaksBefore > 0) {
            val lineBreaksToAdd = prefix.lineBreaksBefore - builder.trailingLineBreakCount()
            for (_ in 1..lineBreaksToAdd) {
                append("\n", false)
            }
        }

        prefix.elements.forEach { appendCommentOrWhiteSpace(it) }

        element.generateCode(this)

        // scan for all comments inside which are not yet used in the text and put them here to not loose any comment from code
        for ((prototype, inheritance) in element.prototypes!!) {
            if (inheritance.commentsInside) {
                prototype.accept(object : JavaRecursiveElementVisitor(){
                    override fun visitComment(comment: PsiComment) {
                        if (comment !in notInsideElements && commentsAndSpacesUsed.add(comment)) {
                            appendCommentOrWhiteSpace(comment)
                        }
                    }
                })
            }
        }

        postfix.forEach { appendCommentOrWhiteSpace(it) }

        element.postGenerateCode(this)

        return this
    }

    private data class Prefix(val elements: List<PsiElement>, val lineBreaksBefore: Int) {
        fun plus(other: Prefix): Prefix {
            return when {
                isEmpty() -> other
                other.isEmpty() -> this
                else -> Prefix(elements + other.elements, Math.max(lineBreaksBefore, other.lineBreaksBefore))
            }
        }

        private fun isEmpty() = elements.isEmpty() && lineBreaksBefore == 0

        companion object {
            val Empty = Prefix(emptyList(), 0)
        }
    }

    private fun collectPrefixElements(
            element: PsiElement,
            inheritance: CommentsAndSpacesInheritance,
            notInsideElements: MutableSet<PsiElement>
    ): Prefix {
        val before = SmartList<PsiElement>().collectCommentsAndSpacesBefore(element)
        val atStart = SmartList<PsiElement>().collectCommentsAndSpacesAtStart(element)
        notInsideElements.addAll(atStart)

        if (inheritance.spacesBefore == SpacesInheritance.NONE && !inheritance.commentsBefore) return Prefix.Empty

        val firstSpace = before.lastOrNull() as? PsiWhiteSpace
        var lineBreaks = 0
        if (firstSpace != null) {
            lineBreaks = firstSpace.lineBreakCount()
            when (inheritance.spacesBefore) {
                SpacesInheritance.NONE -> lineBreaks = 0

                SpacesInheritance.LINE_BREAKS -> commentsAndSpacesUsed.add(firstSpace)

                SpacesInheritance.BLANK_LINES_ONLY -> {
                    commentsAndSpacesUsed.add(firstSpace)
                    if (lineBreaks == 1) lineBreaks = 0
                }
            }
        }

        if (!inheritance.commentsBefore) { // take only whitespace
            return Prefix(emptyList(), lineBreaks)
        }

        if (firstSpace != null) {
            before.remove(before.lastIndex)
        }

        val elements = before.reverse() + atStart
        commentsAndSpacesUsed.addAll(elements)
        return Prefix(elements, lineBreaks)
    }

    private fun collectPostfixElements(
            element: PsiElement,
            inheritance: CommentsAndSpacesInheritance,
            notInsideElements: MutableSet<PsiElement>
    ): List<PsiElement> {
        val atEnd = SmartList<PsiElement>().collectCommentsAndSpacesAtEnd(element)
        notInsideElements.addAll(atEnd)

        if (!inheritance.commentsAfter) return emptyList()

        val after = SmartList<PsiElement>().collectCommentsAndSpacesAfter(element)
        if (after.isNotEmpty()) {
            val last = after.last()
            if (last is PsiWhiteSpace) {
                after.remove(after.lastIndex)
            }
        }

        val result = atEnd.reverse() + after
        commentsAndSpacesUsed.addAll(result)
        return result
    }

    private fun MutableList<PsiElement>.collectCommentsAndSpacesBefore(element: PsiElement): MutableList<PsiElement> {
        if (element == topElement) return this

        val prev = element.getPrevSibling()
        if (prev != null) {
            if (prev.isCommentOrSpace()) {
                if (prev !in commentsAndSpacesUsed) {
                    add(prev)
                    collectCommentsAndSpacesBefore(prev)
                }
            }
            else if (prev.isEmptyElement()){
                collectCommentsAndSpacesBefore(prev)
            }
        }
        else {
            collectCommentsAndSpacesBefore(element.getParent()!!)
        }
        return this
    }

    private fun MutableList<PsiElement>.collectCommentsAndSpacesAfter(element: PsiElement): MutableList<PsiElement> {
        if (element == topElement) return this

        val next = element.getNextSibling()
        if (next != null) {
            if (next.isCommentOrSpace()) {
                if (next is PsiWhiteSpace && next.hasLineBreaks()) return this // do not attach anything on next line after element
                if (next !in commentsAndSpacesUsed) {
                    add(next)
                    collectCommentsAndSpacesAfter(next)
                }
            }
            else if (next.isEmptyElement()){
                collectCommentsAndSpacesAfter(next)
            }
        }
        else {
            collectCommentsAndSpacesAfter(element.getParent()!!)
        }
        return this
    }

    private fun MutableList<PsiElement>.collectCommentsAndSpacesAtStart(element: PsiElement): MutableList<PsiElement> {
        var child = element.getFirstChild()
        while(child != null) {
            if (child.isCommentOrSpace()) {
                if (child !in commentsAndSpacesUsed) add(child) else break
            }
            else if (!child.isEmptyElement()) {
                collectCommentsAndSpacesAtStart(child)
                break
            }
            child = child.getNextSibling()
        }
        return this
    }

    private fun MutableList<PsiElement>.collectCommentsAndSpacesAtEnd(element: PsiElement): MutableList<PsiElement> {
        var child = element.getLastChild()
        while(child != null) {
            if (child.isCommentOrSpace()) {
                if (child !in commentsAndSpacesUsed) add(child) else break
            }
            else if (!child.isEmptyElement()) {
                collectCommentsAndSpacesAtEnd(child)
                break
            }
            child = child.getPrevSibling()
        }
        return this
    }

    private companion object {
        fun<T> List<T>.plus(other: List<T>): List<T> {
            when {
                isEmpty() -> return other

                other.isEmpty() -> return this

                else -> {
                    val result = ArrayList<T>(size() + other.size())
                    result.addAll(this)
                    result.addAll(other)
                    return result
                }
            }
        }

        fun<T> List<T>.reverse(): List<T> {
            return if (size() <= 1)
                this
            else
                (this as Iterable<T>).reverse()
        }

        fun PsiElement.isCommentOrSpace() = this is PsiComment || this is PsiWhiteSpace

        fun PsiElement.isEndOfLineComment() = this is PsiComment && getTokenType() == JavaTokenType.END_OF_LINE_COMMENT

        fun PsiElement.isEmptyElement() = getFirstChild() == null && getTextLength() == 0

        fun PsiWhiteSpace.lineBreakCount() = StringUtil.getLineBreakCount(getText()!!)

        fun PsiWhiteSpace.hasLineBreaks() = StringUtil.containsLineBreak(getText()!!)

        fun CharSequence.trailingLineBreakCount(): Int {
            val index = ((length()-1 downTo 0).firstOrNull { val c = this[it]; c != '\n' && c != '\r' } ?: -1) + 1
            if (index == length()) return 0
            return StringUtil.getLineBreakCount(subSequence(index, length()))
        }
    }
}


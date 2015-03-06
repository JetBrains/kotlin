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
import org.jetbrains.kotlin.j2k.ast.CommentsAndSpacesInheritance
import org.jetbrains.kotlin.j2k.ast.Element
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import java.util.ArrayList
import java.util.HashSet
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

    public fun append(text: String): CodeBuilder
            = append(text, false)

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

    public val result: String
        get() = builder.toString()

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

        if (topElement == null || element.prototypes!!.isEmpty()) {
            element.generateCode(this)
            element.postGenerateCode(this)
            return this
        }

        val notInsideElements = HashSet<PsiElement>()
        val prefixElements = ArrayList<PsiElement>(1)
        val postfixElements = ArrayList<PsiElement>(1)
        for ((prototype, inheritance) in element.prototypes!!) {
            assert(prototype !is PsiComment)
            assert(prototype !is PsiWhiteSpace)
            if (!topElement.isAncestor(prototype)) continue
            prefixElements.collectPrefixElements(prototype, inheritance, notInsideElements)
            postfixElements.collectPostfixElements(prototype, inheritance, notInsideElements)
        }

        commentsAndSpacesUsed.addAll(prefixElements)
        commentsAndSpacesUsed.addAll(postfixElements)

        for ((i, e) in prefixElements.withIndex()) {
            if (i == 0 && e is PsiWhiteSpace) {
                val blankLines = e.newLinesCount() - 1
                for (_ in 1..blankLines) {
                    append("\n", false)
                }
            }
            else {
                appendCommentOrWhiteSpace(e)
            }
        }

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

        postfixElements.forEach { appendCommentOrWhiteSpace(it) }

        element.postGenerateCode(this)

        return this
    }

    private fun MutableList<PsiElement>.collectPrefixElements(element: PsiElement,
                                                              inheritance: CommentsAndSpacesInheritance,
                                                              notInsideElements: MutableSet<PsiElement>) {
        val before = ArrayList<PsiElement>(1).collectCommentsAndSpacesBefore(element)
        val atStart = ArrayList<PsiElement>(1).collectCommentsAndSpacesAtStart(element)
        notInsideElements.addAll(atStart)

        if (!inheritance.blankLinesBefore && !inheritance.commentsBefore) return

        val firstSpace = before.lastOrNull() as? PsiWhiteSpace
        if (!inheritance.commentsBefore) { // take only first whitespace
            if (firstSpace != null) {
                add(firstSpace)
            }
            return
        }

        if (!inheritance.blankLinesBefore && firstSpace != null) {
            before.remove(before.lastIndex)
        }

        addAll(before.reverse())
        addAll(atStart)
    }

    private fun MutableList<PsiElement>.collectPostfixElements(element: PsiElement, inheritance: CommentsAndSpacesInheritance, notInsideElements: MutableSet<PsiElement>) {
        val atEnd = ArrayList<PsiElement>(1).collectCommentsAndSpacesAtEnd(element)
        notInsideElements.addAll(atEnd)

        if (!inheritance.commentsAfter) return

        val after = ArrayList<PsiElement>(1).collectCommentsAndSpacesAfter(element)
        if (after.isNotEmpty()) {
            val last = after.last()
            if (last is PsiWhiteSpace) {
                after.remove(after.lastIndex)
            }
        }

        addAll(atEnd.reverse())
        addAll(after)
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
                if (next is PsiWhiteSpace && next.hasNewLines()) return this // do not attach anything on next line after element
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
            if (child!!.isCommentOrSpace()) {
                if (child !in commentsAndSpacesUsed) add(child!!) else break
            }
            else if (!child!!.isEmptyElement()) {
                collectCommentsAndSpacesAtStart(child!!)
                break
            }
            child = child!!.getNextSibling()
        }
        return this
    }

    private fun MutableList<PsiElement>.collectCommentsAndSpacesAtEnd(element: PsiElement): MutableList<PsiElement> {
        var child = element.getLastChild()
        while(child != null) {
            if (child!!.isCommentOrSpace()) {
                if (child !in commentsAndSpacesUsed) add(child!!) else break
            }
            else if (!child!!.isEmptyElement()) {
                collectCommentsAndSpacesAtEnd(child!!)
                break
            }
            child = child!!.getPrevSibling()
        }
        return this
    }

    private fun PsiElement.isCommentOrSpace() = this is PsiComment || this is PsiWhiteSpace

    private fun PsiElement.isEndOfLineComment() = this is PsiComment && getTokenType() == JavaTokenType.END_OF_LINE_COMMENT

    private fun PsiElement.isEmptyElement() = getFirstChild() == null && getTextLength() == 0

    private fun PsiWhiteSpace.newLinesCount() = StringUtil.getLineBreakCount(getText()!!)

    private fun PsiWhiteSpace.hasNewLines() = StringUtil.containsLineBreak(getText()!!)
}


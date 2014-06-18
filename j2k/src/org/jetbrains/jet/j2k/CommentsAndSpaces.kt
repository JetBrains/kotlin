/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.j2k

import com.intellij.psi.*
import java.util.HashSet
import org.jetbrains.jet.lang.psi.psiUtil.isAncestor
import java.util.ArrayList
import org.jetbrains.jet.j2k.ast.PrototypeInfo

class CommentsAndSpaces(private val topElement: PsiElement?) {
    private val commentsAndSpacesUsed = HashSet<PsiElement>()

    public fun wrapElement(elementToKotlin: () -> String, prototypes: List<PrototypeInfo>): String {
        if (prototypes.isEmpty() || topElement == null) return elementToKotlin()

        val prefix = StringBuilder()
        val postfix = StringBuilder()
        for ((element, inheritBlankLinesBefore) in prototypes) {
            assert(element !is PsiComment)
            assert(element !is PsiWhiteSpace)
            assert(topElement.isAncestor(element))
            prefix.collectPrefix(element, inheritBlankLinesBefore)
            postfix.collectPostfix(element)
        }

        return prefix.toString() + elementToKotlin() + postfix.toString()
    }

    private fun StringBuilder.collectPrefix(element: PsiElement, allowBlankLinesBefore: Boolean) {
        val atStart = ArrayList<PsiElement>(2).addCommentsAndSpacesAtStart(element)

        val before = ArrayList<PsiElement>(2).addCommentsAndSpacesBefore(element)
        if (before.isNotEmpty()) {
            val last = before.last()
            if (last is PsiWhiteSpace) {
                if (allowBlankLinesBefore) {
                    val blankLines = last.newLinesCount() - 1
                    if (blankLines > 0) {
                        append("\n") // insert at maximum one blank line
                        commentsAndSpacesUsed.add(last)
                    }
                }
                before.remove(before.size - 1)
            }
        }

        if (before.isEmpty() && atStart.isEmpty()) return

        for (e in before.reverse() + atStart) {
            append(e.getText())
            commentsAndSpacesUsed.add(e)
        }
    }

    private fun StringBuilder.collectPostfix(element: PsiElement) {
        val atEnd = ArrayList<PsiElement>(2).addCommentsAndSpacesAtEnd(element)

        val after = ArrayList<PsiElement>(2).addCommentsAndSpacesAfter(element)
        if (after.isNotEmpty()) {
            val last = after.last()
            if (last is PsiWhiteSpace) {
                after.remove(after.size - 1)
            }
        }

        if (after.isEmpty() && atEnd.isEmpty()) return

        for (e in atEnd.reverse() + after) {
            append(e.getText())
            commentsAndSpacesUsed.add(e)
        }
    }

    private fun MutableList<PsiElement>.addCommentsAndSpacesBefore(element: PsiElement): MutableList<PsiElement> {
        if (element == topElement) return this

        val prev = element.getPrevSibling()
        if (prev != null) {
            if (prev.isCommentOrSpace()) {
                if (prev !in commentsAndSpacesUsed) {
                    add(prev)
                    addCommentsAndSpacesBefore(prev)
                }
            }
            else if (prev.isEmptyElement()){
                addCommentsAndSpacesBefore(prev)
            }
        }
        else {
            addCommentsAndSpacesBefore(element.getParent()!!)
        }
        return this
    }

    private fun MutableList<PsiElement>.addCommentsAndSpacesAfter(element: PsiElement): MutableList<PsiElement> {
        if (element == topElement) return this

        val next = element.getNextSibling()
        if (next != null) {
            if (next.isCommentOrSpace()) {
                if (next is PsiWhiteSpace && next.hasNewLines()) return this // do not attach anything on next line after element
                if (next !in commentsAndSpacesUsed) {
                    add(next)
                    addCommentsAndSpacesAfter(next)
                }
            }
            else if (next.isEmptyElement()){
                addCommentsAndSpacesAfter(next)
            }
        }
        else {
            addCommentsAndSpacesAfter(element.getParent()!!)
        }
        return this
    }

    private fun MutableList<PsiElement>.addCommentsAndSpacesAtStart(element: PsiElement): MutableList<PsiElement> {
        var child = element.getFirstChild()
        while(child != null) {
            if (child!!.isCommentOrSpace()) {
                if (child !in commentsAndSpacesUsed) add(child!!) else break
            }
            else if (!child!!.isEmptyElement()) {
                addCommentsAndSpacesAtStart(child!!)
                break
            }
            child = child!!.getNextSibling()
        }
        return this
    }

    private fun MutableList<PsiElement>.addCommentsAndSpacesAtEnd(element: PsiElement): MutableList<PsiElement> {
        var child = element.getLastChild()
        while(child != null) {
            if (child!!.isCommentOrSpace()) {
                if (child !in commentsAndSpacesUsed) add(child!!) else break
            }
            else if (!child!!.isEmptyElement()) {
                addCommentsAndSpacesAtEnd(child!!)
                break
            }
            child = child!!.getPrevSibling()
        }
        return this
    }

    private fun PsiElement.isCommentOrSpace() = this is PsiComment || this is PsiWhiteSpace

    private fun PsiElement.isEmptyElement() = getFirstChild() == null && getTextLength() == 0

    private fun PsiWhiteSpace.newLinesCount() = getText()!!.count { it == '\n' } //TODO: this is not correct!!

    private fun PsiWhiteSpace.hasNewLines() = getText()!!.any { it == '\n' || it == '\r' }

    class object {
        val None: CommentsAndSpaces = CommentsAndSpaces(null)
    }
}


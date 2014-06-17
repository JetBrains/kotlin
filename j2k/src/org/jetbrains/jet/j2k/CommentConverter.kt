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
import java.util.ArrayList
import org.jetbrains.jet.j2k.ast.Element

class CommentConverter(private val topElement: PsiElement?) {
    private enum class CommentPlacement {
        Before
        SameLineBefore
        SameLineAfter
        After
    }

    private class CommentInfo(val comment: PsiComment) {
        val startOffset: Int
        val endOffset: Int
        var isAttached = false

        {
            val range = comment.getTextRange()!!
            startOffset = range.getStartOffset()
            endOffset = range.getEndOffset()
        }
    }

    private val allComments: List<CommentInfo> = run {
        val list = ArrayList<CommentInfo>()
        topElement?.accept(object : JavaRecursiveElementVisitor(){
            override fun visitComment(comment: PsiComment) {
                list.add(CommentInfo(comment))
            }
        })
        list
    }

    /*
    public fun toKotlin(elementToKotlin: () -> String, originalElements: List<PsiElement>): String {

    }
    */

    class object {
        val Dummy: CommentConverter = CommentConverter(null)
    }
}

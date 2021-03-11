/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.uast.kotlin.internal

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.uast.UComment
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UParameter

interface KotlinUElementWithComments : UElement {

    override val comments: List<UComment>
        get() {
            val psi = sourcePsi ?: return emptyList()
            val childrenComments = psi.allChildren.filterIsInstance<PsiComment>().map { UComment(it, this) }.toList()
            if (this !is UExpression &&
                this !is UParameter     // fun (/* prior */ a: Int) <-- /* prior */ is on the level of VALUE_PARAM_LIST
            )
                return childrenComments
            val childrenAndSiblingComments = childrenComments +
                    psi.nearestCommentSibling(forward = true)?.let { listOf(UComment(it, this)) }.orEmpty() +
                    psi.nearestCommentSibling(forward = false)?.let { listOf(UComment(it, this)) }.orEmpty()
            val parent = psi.parent as? KtValueArgument ?: return childrenAndSiblingComments

            return childrenAndSiblingComments +
                    parent.nearestCommentSibling(forward = true)?.let { listOf(UComment(it, this)) }.orEmpty() +
                    parent.nearestCommentSibling(forward = false)?.let { listOf(UComment(it, this)) }.orEmpty()
        }

    private fun PsiElement.nearestCommentSibling(forward: Boolean): PsiComment? {
        var sibling = if (forward) nextSibling else prevSibling
        while (sibling is PsiWhiteSpace && !sibling.text.contains('\n')) {
            sibling = if (forward) sibling.nextSibling else sibling.prevSibling
        }
        return sibling as? PsiComment
    }

}

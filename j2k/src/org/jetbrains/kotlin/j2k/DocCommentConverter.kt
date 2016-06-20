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

import com.intellij.psi.JavaDocTokenType
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.javadoc.PsiDocTag

interface DocCommentConverter {
    fun convertDocComment(docComment: PsiDocComment): String
}

object EmptyDocCommentConverter: DocCommentConverter {
    override fun convertDocComment(docComment: PsiDocComment) = docComment.text
}

fun PsiDocTag.content(): String =
        children
                .dropWhile { it?.node?.elementType == JavaDocTokenType.DOC_TAG_NAME }
                .dropWhile { it is PsiWhiteSpace }
                .filterNot { it?.node?.elementType == JavaDocTokenType.DOC_COMMENT_LEADING_ASTERISKS }
                .joinToString("") { it.text }

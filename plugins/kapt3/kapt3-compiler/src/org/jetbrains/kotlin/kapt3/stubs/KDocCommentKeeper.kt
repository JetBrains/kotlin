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

package org.jetbrains.kotlin.kapt3.stubs

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.sun.tools.javac.parser.Tokens
import com.sun.tools.javac.tree.DCTree
import com.sun.tools.javac.tree.DocCommentTable
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.kapt3.KaptContext
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.org.objectweb.asm.tree.FieldNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode

class KDocCommentKeeper(private val kaptContext: KaptContext<*>) {
    val docCommentTable: DocCommentTable = KaptDocCommentTable()

    fun saveKDocComment(tree: JCTree, node: Any) {
        val origin = kaptContext.origins[node] ?: return
        val psiElement = origin.element as? KtDeclaration ?: return
        val descriptor = origin.descriptor
        val docComment = psiElement.docComment ?: return

        if (descriptor is ConstructorDescriptor && psiElement is KtClassOrObject) {
            // We don't want the class comment to be duplicated on <init>()
            return
        }

        if (node is MethodNode
            && psiElement is KtProperty
            && descriptor is PropertyAccessorDescriptor
            && kaptContext.bindingContext[BindingContext.BACKING_FIELD_REQUIRED, descriptor.correspondingProperty] == true
        ) {
            // Do not place the smae documentation on backing field and property accessors
            return
        }

        if (node is FieldNode && psiElement is KtObjectDeclaration && descriptor == null) {
            // Do not write KDoc on object instance field
            return
        }

        docCommentTable.putComment(tree, KDocComment(extractCommentText(docComment)))
    }

    private fun extractCommentText(docComment: KDoc): String {
        return docComment.children.dropWhile { it is PsiWhiteSpace || it.isKDocStart() }
                .dropLastWhile { it is PsiWhiteSpace || it.isKDocEnd() }
                .joinToString("") { it.text }
    }

    private fun PsiElement.isKDocStart() = this is LeafPsiElement && elementType == KDocTokens.START
    private fun PsiElement.isKDocEnd() = this is LeafPsiElement && elementType == KDocTokens.END
}

private class KDocComment(val body: String) : Tokens.Comment {
    override fun getSourcePos(index: Int) = -1
    override fun getStyle() = Tokens.Comment.CommentStyle.JAVADOC
    override fun getText() = body
    override fun isDeprecated() = false
}

private class KaptDocCommentTable : DocCommentTable {
    private val table = mutableMapOf<JCTree, Tokens.Comment>()

    override fun hasComment(tree: JCTree) = tree in table
    override fun getComment(tree: JCTree) = table[tree]
    override fun getCommentText(tree: JCTree) = getComment(tree)?.text

    override fun getCommentTree(tree: JCTree): DCTree.DCDocComment? = null

    override fun putComment(tree: JCTree, c: Tokens.Comment) {
        table.put(tree, c)
    }
}
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
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.sun.tools.javac.parser.Tokens
import com.sun.tools.javac.tree.DCTree
import com.sun.tools.javac.tree.DocCommentTable
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.TreeScanner
import org.jetbrains.kotlin.kapt3.base.KaptContext
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.org.objectweb.asm.Opcodes

abstract class AbstractKDocCommentKeeper<T: KaptContext>(protected val kaptContext: T) {
    private val docCommentTable = KaptDocCommentTable()

    fun getDocTable(file: JCTree.JCCompilationUnit): DocCommentTable {
        val map = docCommentTable.takeIf { it.map.isNotEmpty() } ?: return docCommentTable

        // Enum values with doc comments are rendered incorrectly in javac pretty print,
        // so we delete the comments.
        file.accept(object : TreeScanner() {
            var removeComments = false

            override fun visitVarDef(def: JCTree.JCVariableDecl) {
                if (!removeComments && (def.modifiers.flags and Opcodes.ACC_ENUM.toLong()) != 0L) {
                    map.removeComment(def)

                    removeComments = true
                    super.visitVarDef(def)
                    removeComments = false
                    return
                }

                super.visitVarDef(def)
            }

            override fun scan(tree: JCTree?) {
                if (removeComments && tree != null) {
                    map.removeComment(tree)
                }

                super.scan(tree)
            }
        })

        return docCommentTable
    }

    protected fun saveKDocComment(tree: JCTree, comment: KDoc) {
        docCommentTable.putComment(tree, KDocComment(escapeNestedComments(extractCommentText(comment))))
    }

    private fun escapeNestedComments(text: String): String {
        val result = StringBuilder()

        var index = 0
        var commentLevel = 0

        while (index < text.length) {
            val currentChar = text[index]
            fun nextChar() = text.getOrNull(index + 1)

            if (currentChar == '/' && nextChar() == '*') {
                commentLevel++
                index++
                result.append("/ *")
            } else if (currentChar == '*' && nextChar() == '/') {
                commentLevel = maxOf(0, commentLevel - 1)
                index++
                result.append("* /")
            } else {
                result.append(currentChar)
            }

            index++
        }

        return result.toString()
    }

    private fun extractCommentText(docComment: KDoc): String {
        return buildString {
            docComment.accept(object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    if (element is LeafPsiElement) {
                        if (element.isKDocLeadingAsterisk()) {
                            val indent = takeLastWhile { it == ' ' || it == '\t' }.length
                            if (indent > 0) {
                                delete(length - indent, length)
                            }
                        } else if (!element.isKDocStart() && !element.isKDocEnd()) {
                            append(element.text)
                        }
                    }

                    super.visitElement(element)
                }
            })
        }.trimIndent().trim()
    }

    private fun LeafPsiElement.isKDocStart() = elementType == KDocTokens.START
    private fun LeafPsiElement.isKDocEnd() = elementType == KDocTokens.END
    private fun LeafPsiElement.isKDocLeadingAsterisk() = elementType == KDocTokens.LEADING_ASTERISK
}

private class KDocComment(val body: String) : Tokens.Comment {
    override fun getSourcePos(index: Int) = -1
    override fun getStyle() = Tokens.Comment.CommentStyle.JAVADOC
    override fun getText() = body
    override fun isDeprecated() = false
}

private class KaptDocCommentTable(map: Map<JCTree, Tokens.Comment> = emptyMap()) : DocCommentTable {
    private val table = map.toMutableMap()

    val map: Map<JCTree, Tokens.Comment>
        get() = table

    override fun hasComment(tree: JCTree) = tree in table
    override fun getComment(tree: JCTree) = table[tree]
    override fun getCommentText(tree: JCTree) = getComment(tree)?.text

    override fun getCommentTree(tree: JCTree): DCTree.DCDocComment? = null

    override fun putComment(tree: JCTree, c: Tokens.Comment) {
        table[tree] = c
    }

    fun removeComment(tree: JCTree) {
        table.remove(tree)
    }
}
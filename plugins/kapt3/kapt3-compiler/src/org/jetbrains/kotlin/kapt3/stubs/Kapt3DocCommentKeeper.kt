/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.kapt3.KaptContextForStubGeneration
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.FieldNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode

internal class Kapt3DocCommentKeeper(private val kaptContext: KaptContextForStubGeneration) {
    private val docCommentTable = KaptDocCommentTable()

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
            // Do not place documentation on backing field and property accessors
            return
        }

        if (node is FieldNode && psiElement is KtObjectDeclaration && descriptor == null) {
            // Do not write KDoc on object instance field
            return
        }

        saveKDocComment(tree, docComment)
    }

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
        docCommentTable.putComment(tree, KDocComment(extractComment(comment)))
    }
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

fun extractComment(comment: KDoc) = escapeNestedComments(extractCommentText(comment))


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
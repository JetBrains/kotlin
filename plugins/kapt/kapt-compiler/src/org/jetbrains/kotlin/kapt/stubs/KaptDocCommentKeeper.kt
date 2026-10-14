/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.stubs

import com.sun.tools.javac.parser.Tokens
import com.sun.tools.javac.tree.DCTree
import com.sun.tools.javac.tree.DocCommentTable
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.TreeScanner
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.kapt.KaptContextForStubGeneration
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.kapt.util.kdocBody
import org.jetbrains.kotlin.kapt.util.kdocText
import org.jetbrains.org.objectweb.asm.Opcodes

internal class KaptDocCommentKeeper(private val kaptContext: KaptContextForStubGeneration) {
    private val docCommentTable = KaptDocCommentTable()

    fun saveKDocComment(tree: JCTree, node: Any) {
        val origin = kaptContext.origins[node] ?: return
        val source = kaptContext.firSourceOf(origin.declaration) ?: return

        if (origin.declaration is IrConstructor && source.elementType in CLASS_LIKE_ELEMENT_TYPES) {
            // Do not copy class KDoc to an implicit constructor.
            return
        }

        val docComment = source.kdocText() ?: return
        docCommentTable.putComment(tree, KDocComment(extractComment(docComment)))
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

}

internal val CLASS_LIKE_ELEMENT_TYPES = setOf(KtNodeTypes.CLASS, KtNodeTypes.OBJECT_DECLARATION)

private class KDocComment(val body: String) : Tokens.Comment {
    override fun getSourcePos(index: Int) = -1
    override fun getStyle() = Tokens.Comment.CommentStyle.JAVADOC
    override fun getText() = body
    override fun isDeprecated() = false
}

private class KaptDocCommentTable(map: Map<JCTree, Tokens.Comment> = emptyMap()) : DocCommentTable {
    val map: Map<JCTree, Tokens.Comment>
        field = map.toMutableMap()

    override fun hasComment(tree: JCTree) = tree in map
    override fun getComment(tree: JCTree) = map[tree]
    override fun getCommentText(tree: JCTree) = getComment(tree)?.text

    override fun getCommentTree(tree: JCTree): DCTree.DCDocComment? = null

    override fun putComment(tree: JCTree, c: Tokens.Comment) {
        map[tree] = c
    }

    fun removeComment(tree: JCTree) {
        map.remove(tree)
    }
}

fun extractComment(comment: String) = escapeNestedComments(comment.kdocBody())


private fun escapeNestedComments(text: String): String {
    val result = StringBuilder()

    var index = 0
    var commentLevel = 0

    while (index < text.length) {
        val currentChar = text[index]
        fun nextChar() = text.getOrNull(index + 1)

        when (currentChar) {
            '/' if nextChar() == '*' -> {
                commentLevel++
                index++
                result.append("/ *")
            }
            '*' if nextChar() == '/' -> {
                commentLevel = maxOf(0, commentLevel - 1)
                index++
                result.append("* /")
            }
            else -> {
                result.append(currentChar)
            }
        }

        index++
    }

    return result.toString()
}

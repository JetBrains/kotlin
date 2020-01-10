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

import com.sun.tools.javac.parser.Tokens
import com.sun.tools.javac.tree.DCTree
import com.sun.tools.javac.tree.DocCommentTable
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.TreeScanner
import org.jetbrains.kotlin.kapt3.KaptContextForStubGeneration
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.jetbrains.org.objectweb.asm.tree.FieldNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.kotlin.kaptlite.kdoc.KDocParsingHelper

class KDocCommentKeeper(private val kaptContext: KaptContextForStubGeneration) {
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

    fun saveKDocComment(tree: JCTree, node: Any) {
        val origin = kaptContext.origins[node] ?: return
        val kind = when (node) {
            is ClassNode -> KDocParsingHelper.DeclarationKind.CLASS
            is MethodNode -> KDocParsingHelper.DeclarationKind.METHOD
            is FieldNode -> KDocParsingHelper.DeclarationKind.FIELD
            else -> return
        }

        val text = KDocParsingHelper.getKDocComment(kind, origin, kaptContext.bindingContext) ?: return
        docCommentTable.putComment(tree, KDocComment(text))
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
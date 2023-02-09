/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.sun.tools.javac.parser.Tokens
import com.sun.tools.javac.tree.DCTree
import com.sun.tools.javac.tree.DocCommentTable
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.TreeScanner
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightMember
import org.jetbrains.kotlin.asJava.elements.KtLightParameter
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.light.classes.symbol.isFieldForObjectInstance
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.org.objectweb.asm.Opcodes

// TODO: unify with KDocCommentKeeper
context(Kapt4ContextForStubGeneration)
class Kapt4KDocCommentKeeper(private val analysisSession: KtAnalysisSession) {
    private val docCommentTable = Kapt4DocCommentTable()

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

    fun saveKDocComment(tree: JCTree, psiElement: PsiElement) {
        val ktElement = psiElement.extractOriginalKtDeclaration<KtDeclaration>() ?: return
        val docComment =
            when {
                ktElement is KtProperty ->
                    // Do not place documentation on property accessors of a property with a backing field
                    ktElement.docComment.takeIf { psiElement is PsiField }
                ktElement.docComment == null && ktElement is KtPropertyAccessor -> ktElement.property.docComment
                else -> ktElement.docComment
            } ?: return

        if (psiElement is PsiMethod && psiElement.isConstructor && ktElement is KtClassOrObject) {
            // We don't want the class comment to be duplicated on <init>()
            return
        }

        if (psiElement is PsiField && psiElement.isFieldForObjectInstance) {
            // Do not write KDoc on object instance field
            return
        }

        docCommentTable.putComment(tree, KDocComment(escapeNestedComments(extractCommentText(docComment))))
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


private class Kapt4DocCommentTable(map: Map<JCTree, Tokens.Comment> = emptyMap()) : DocCommentTable {
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

private class KDocComment(val body: String) : Tokens.Comment {
    override fun getSourcePos(index: Int) = -1
    override fun getStyle() = Tokens.Comment.CommentStyle.JAVADOC
    override fun getText() = body
    override fun isDeprecated() = false
}

inline fun <reified T : KtDeclaration> PsiElement.extractOriginalKtDeclaration(): T? {
    // This when is needed to avoid recursion
    val elementToExtract = when (this) {
        is KtLightParameter -> when (kotlinOrigin) {
            null -> method
            else -> return kotlinOrigin as? T
        }
        else -> this
    }

    return when (elementToExtract) {
        is KtLightMember<*> -> {
            val origin = elementToExtract.lightMemberOrigin
            origin?.auxiliaryOriginalElement ?: origin?.originalElement ?: elementToExtract.kotlinOrigin
        }
        is KtLightElement<*, *> -> elementToExtract.kotlinOrigin
        else -> null
    } as? T
}

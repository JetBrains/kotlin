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

package org.jetbrains.kotlin.j2k

import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl
import org.jetbrains.kotlin.j2k.ast.CommentsAndSpacesInheritance
import org.jetbrains.kotlin.j2k.ast.Modifiers
import org.jetbrains.kotlin.j2k.ast.assignPrototype
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.*

class JavaToJKTreeBuilder {

    private class ExpressionTreeVisitor : JavaElementVisitor() {

        private var resultExpression: JKExpression? = null

        override fun visitLiteralExpression(expression: PsiLiteralExpression?) {
            if (expression is PsiLiteralExpressionImpl) {

                when (expression.literalElementType) {
                    JavaTokenType.STRING_LITERAL ->
                        resultExpression = JKJavaStringLiteralExpressionImpl(expression.innerText!!)
                }
            }
        }

        fun result(): JKExpression = resultExpression!!
    }

    private object ClassTreeMapper {
        fun PsiClass.toJK(): JKClass{
            return JKClassImpl(with(ModifierMapper){modifierList.toJK()}, JKNameIdentifierImpl(this.name!!))
        }
    }

    private object ModifierMapper {
        fun PsiModifierList?.toJK(): JKModifierList {

            val modifierList = JKModifierListImpl()
            if (this == null) return modifierList

            PsiModifier.MODIFIERS
                    .filter { hasExplicitModifier(it) }
                    .mapTo(modifierList.modifiers) { modifierToJK(it) }

            return modifierList
        }

        fun modifierToJK(name: String): JKModifier = when (name) {
            PsiModifier.PUBLIC -> JKJavaAccessModifierImpl(JKJavaAccessModifier.AccessModifierType.PUBLIC)
            PsiModifier.PRIVATE -> JKJavaAccessModifierImpl(JKJavaAccessModifier.AccessModifierType.PRIVATE)
            PsiModifier.PROTECTED -> JKJavaAccessModifierImpl(JKJavaAccessModifier.AccessModifierType.PROTECTED)
            else -> TODO("Not yet supported")
        }
    }

    private class ElementVisitor : JavaElementVisitor() {

        override fun visitElement(element: PsiElement) {
            element.acceptChildren(this)
        }

        var currentClass: JKClass? = null

        override fun visitClass(aClass: PsiClass) {

            val name = JKNameIdentifierImpl(aClass.name!!)
            val modifierList = with(ModifierMapper) {
                aClass.modifierList.toJK()
            }

            currentClass = JKClassImpl(modifierList, name)
            super.visitClass(aClass)
        }

        override fun visitField(field: PsiField) {
            val initializer = field.initializer?.buildTreeForExpression()

            val type = JKJavaTypeIdentifierImpl(field.type.canonicalText)
            val name = JKNameIdentifierImpl(field.name!!)

            val modifierList = with(ModifierMapper) {
                field.modifierList.toJK()
            }

            currentClass!!.declarations += JKJavaFieldImpl(modifierList, type, name, initializer)
            super.visitField(field)
        }

    }


    fun buildTree(psi: PsiElement): JKElement? {
        assert(psi.language.`is`(JavaLanguage.INSTANCE)) { "Unable to build JK Tree using Java Visitor for language ${psi.language}" }
        val elementVisitor = ElementVisitor()
        psi.accept(elementVisitor)
        return elementVisitor.currentClass
    }

    companion object {
        private fun PsiExpression.buildTreeForExpression(): JKExpression =
                JavaToJKTreeBuilder.ExpressionTreeVisitor().apply { accept(this) }.result()
    }
}


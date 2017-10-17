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
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.*

class JavaToJKTreeBuilder {

    private object ExpressionTreeMapper {
        fun PsiExpression.toJK(): JKExpression {
            when (this) {
                is PsiBinaryExpression -> {
                    return toJK()
                }
                is PsiPrefixExpression -> {
                    return toJK()
                }
                is PsiPostfixExpression -> {
                    return toJK()
                }
                is PsiLiteralExpression -> {
                    return toJK()
                }
                is PsiCallExpression -> {
                    return toJK()
                }
            }
            throw RuntimeException("Not supported")
        }

        fun PsiBinaryExpression.toJK(): JKExpression {
            return JKBinaryExpressionImpl(lOperand.toJK(), rOperand?.toJK(), operationSign.toJK())
        }

        fun PsiLiteralExpression.toJK(): JKExpression {
            if (this !is PsiLiteralExpressionImpl) {
                throw RuntimeException("Not supported")
            }
            return when (this.literalElementType) {
                JavaTokenType.STRING_LITERAL -> JKJavaStringLiteralExpressionImpl(innerText!!)
                else -> throw RuntimeException("Not supported")
            }
        }

        fun PsiJavaToken.toJK(): JKOperatorIdentifier = when (tokenType) {
            JavaTokenType.PLUS -> JKJavaOperatorIdentifierImpl.PLUS
            JavaTokenType.MINUS -> JKJavaOperatorIdentifierImpl.MINUS
            else -> throw RuntimeException("Not supported")
        }

        fun PsiPrefixExpression.toJK(): JKExpression {
            return JKPrefixExpressionImpl(operand?.toJK(), operationSign.toJK())
        }

        fun PsiPostfixExpression.toJK(): JKExpression {
            return JKPostfixExpressionImpl(operand.toJK(), operationSign.toJK())
        }

        fun PsiCallExpression.toJK(): JKExpression {
            return JKJavaCallExpressionImpl(resolveMethod().toJK(), argumentList.toJK())
        }

        fun PsiExpressionList?.toJK(): JKExpressionList {
            this ?: return JKExpressionListImpl(emptyArray())
            return JKExpressionListImpl(expressions.map { it.toJK() }.toTypedArray())
        }

        fun PsiMethod?.toJK(): JKJavaMethodReference {
            return JKJavaMethodReferenceImpl()
        }
    }

    private object DeclarationMapper {

        fun PsiClass.toJK(): JKClass {
            val classKind: JKClass.ClassKind = when {
                isAnnotationType -> JKClass.ClassKind.ANNOTATION
                isEnum -> JKClass.ClassKind.ENUM
                isInterface -> JKClass.ClassKind.INTERFACE
                else -> JKClass.ClassKind.CLASS
            }
            return JKClassImpl(with(ModifierMapper) { modifierList.toJK() }, JKNameIdentifierImpl(this.name!!), classKind)
        }


        fun PsiField.toJK(): JKJavaField {
            val initializer = this.initializer?.buildTreeForExpression()

            val type = JKJavaTypeIdentifierImpl(this.type.canonicalText)
            val name = JKNameIdentifierImpl(this.name!!)

            val modifierList = with(ModifierMapper) {
                modifierList.toJK()
            }

            return JKJavaFieldImpl(modifierList, type, name, initializer)
        }

        fun PsiMethod.toJK(): JKJavaMethod {
            val modifierList = with(ModifierMapper) { modifierList.toJK() }
            val name = JKNameIdentifierImpl(name)
            val valueArgumentList = this.parameterList.parameters.map { it -> it.toJK() }
            val block = body?.toJK()
            return JKJavaMethodImpl(modifierList, name, valueArgumentList, block)
        }

        fun PsiMember.toJK(): JKDeclaration? = when (this) {
            is PsiField -> this.toJK()
            is PsiMethod -> this.toJK()
            else -> null
        }

        fun PsiParameter.toJK(): JKValueArgumentImpl {
            //TODO implement PsiType mapping for type name
            return JKValueArgumentImpl(JKJavaTypeIdentifierImpl("this.type.getCanonicalText()"), this.name!!)
        }

        fun PsiCodeBlock.toJK(): JKBlock {
            //TODO block mapping
            return JKBlockImpl(listOf(JKStringLiteralExpressionImpl("")))
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

            PsiModifier.ABSTRACT -> JKJavaModifierImpl(JKJavaModifier.JavaModifierType.ABSTRACT)
            PsiModifier.FINAL -> JKJavaModifierImpl(JKJavaModifier.JavaModifierType.FINAL)
            PsiModifier.NATIVE -> JKJavaModifierImpl(JKJavaModifier.JavaModifierType.NATIVE)
            PsiModifier.STATIC -> JKJavaModifierImpl(JKJavaModifier.JavaModifierType.STATIC)
            PsiModifier.STRICTFP -> JKJavaModifierImpl(JKJavaModifier.JavaModifierType.STRICTFP)
            PsiModifier.SYNCHRONIZED -> JKJavaModifierImpl(JKJavaModifier.JavaModifierType.SYNCHRONIZED)
            PsiModifier.TRANSIENT -> JKJavaModifierImpl(JKJavaModifier.JavaModifierType.TRANSIENT)
            PsiModifier.VOLATILE -> JKJavaModifierImpl(JKJavaModifier.JavaModifierType.VOLATILE)

            else -> TODO("Not yet supported")
        }
    }

    private class ElementVisitor : JavaElementVisitor() {

        override fun visitElement(element: PsiElement) {
            element.acceptChildren(this)
        }

        var resultElement: JKElement? = null

        override fun visitClass(aClass: PsiClass) {
            resultElement = with(DeclarationMapper) { aClass.toJK() }
        }

        override fun visitField(field: PsiField) {
            resultElement = with(DeclarationMapper) { field.toJK() }
        }

    }


    fun buildTree(psi: PsiElement): JKElement? {
        assert(psi.language.`is`(JavaLanguage.INSTANCE)) { "Unable to build JK Tree using Java Visitor for language ${psi.language}" }
        val elementVisitor = ElementVisitor()
        psi.accept(elementVisitor)
        return elementVisitor.resultElement
    }
}


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
import com.intellij.psi.impl.source.tree.ChildRole
import com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl
import com.intellij.psi.impl.source.tree.java.PsiNewExpressionImpl
import com.intellij.psi.impl.source.tree.java.PsiReferenceExpressionImpl
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
                is PsiMethodCallExpression -> {
                    return toJK()
                }
                is PsiReferenceExpression -> {
                    return toJK()
                }
                is PsiNewExpression -> {
                    return toJK()
                }
                is PsiArrayAccessExpression -> {
                    return toJK()
                }
                is PsiTypeCastExpression -> {
                    return toJK()
                }
                is PsiParenthesizedExpression -> {
                    return toJK()
                }
            }
            throw RuntimeException("Not supported")
        }

        fun PsiBinaryExpression.toJK(): JKExpression {
            return JKBinaryExpressionImpl(lOperand.toJK(), rOperand?.toJK(), operationSign.toJK())
        }

        fun PsiLiteralExpression.toJK(): JKLiteralExpression {
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

        fun PsiMethodCallExpression.toJK(): JKExpression {
            val method = methodExpression as PsiReferenceExpressionImpl
            val identifier = (method.referenceNameElement as PsiIdentifier).convertMethodReference()
            val call = JKJavaMethodCallExpressionImpl(identifier, argumentList.toJK())
            return if (method.findChildByRole(ChildRole.DOT) != null) {
                JKQualifiedExpressionImpl((method.qualifier as PsiExpression).toJK(), JKJavaQualificationIdentifierImpl.DOT, call)
            }
            else {
                call
            }
        }

        fun PsiReferenceExpression.toJK(): JKExpression {
            val impl = this as PsiReferenceExpressionImpl
            val identifier = (impl.referenceNameElement as PsiIdentifier).convertFieldReference()
            val access = JKJavaFieldAccessExpressionImpl(identifier)
            return if (impl.findChildByRole(ChildRole.DOT) != null) {
                JKQualifiedExpressionImpl((impl.qualifier as PsiExpression).toJK(), JKJavaQualificationIdentifierImpl.DOT, access)
            }
            else {
                access
            }
        }

        fun PsiNewExpression.toJK(): JKExpression {
            assert(this is PsiNewExpressionImpl)
            if ((this as PsiNewExpressionImpl).findChildByRole(ChildRole.LBRACKET) != null) {
                val arrayInitializer = arrayInitializer
                if (arrayInitializer != null) {
                    return JKJavaNewArrayImpl(arrayInitializer.initializers.map { toJK() })
                }
                else {
                    val dimensions = mutableListOf<PsiLiteralExpression?>()
                    var child = firstChild
                    while (child != null) {
                        if (child.node.elementType == JavaTokenType.LBRACKET) {
                            child = child.nextSibling
                            if (child.node.elementType == JavaTokenType.RBRACKET) {
                                dimensions.add(null)
                            }
                            else {
                                dimensions.add(child as PsiLiteralExpression?)
                            }
                        }
                        child = child.nextSibling
                    }
                    return JKJavaNewEmptyArrayImpl(dimensions.map { it?.toJK() })
                }
            }
            return JKJavaNewExpressionImpl((classReference as PsiIdentifier).convertClassReference(), argumentList.toJK())
        }

        fun PsiArrayAccessExpression.toJK(): JKExpression {
            return JKArrayAccessExpressionImpl(arrayExpression.toJK(), indexExpression?.toJK())
        }

        fun PsiTypeCastExpression.toJK(): JKExpression {
            return JKTypeCastExpressionImpl(operand?.toJK(), castType?.convertTypeReference())
        }

        fun PsiParenthesizedExpression.toJK(): JKExpression {
            return JKParenthesizedExpressionImpl(expression?.toJK())
        }

        fun PsiExpressionList?.toJK(): JKExpressionList {
            return JKExpressionListImpl(this?.expressions?.map { it.toJK() }?.toTypedArray() ?: emptyArray())
        }

        fun PsiIdentifier.convertMethodReference(): JKJavaMethodReference {

            return JKJavaMethodReferenceImpl()
        }

        fun PsiIdentifier.convertFieldReference(): JKJavaFieldReference {
            return JKJavaFieldReferenceImpl()
        }

        fun PsiIdentifier.convertClassReference(): JKJavaClassReference {
            return JKJavaClassReferenceImpl()
        }

        fun PsiTypeElement.convertTypeReference(): JKTypeReference {
            return JKTypeReferenceImpl()
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
            return JKClassImpl(with(ModifierMapper) { modifierList.toJK() }, JKNameIdentifierImpl(this.name!!), classKind).apply {
                declarations = children.mapNotNull { ElementVisitor().apply { it.accept(this) }.resultElement as? JKDeclaration }
            }
        }


        fun PsiField.toJK(): JKJavaField {

            val initializer = with(ExpressionTreeMapper) {
                initializer?.toJK()
            }

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

            modifierList.modifiers = PsiModifier.MODIFIERS
                    .filter { hasExplicitModifier(it) }
                    .map { modifierToJK(it) }

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

        var resultElement: JKElement? = null

        override fun visitClass(aClass: PsiClass) {
            resultElement = with(DeclarationMapper) { aClass.toJK() }
        }

        override fun visitField(field: PsiField) {
            resultElement = with(DeclarationMapper) { field.toJK() }
        }

        override fun visitFile(file: PsiFile) {
            file.acceptChildren(this)
        }
    }


    fun buildTree(psi: PsiElement): JKElement? {
        assert(psi.language.`is`(JavaLanguage.INSTANCE)) { "Unable to build JK Tree using Java Visitor for language ${psi.language}" }
        val elementVisitor = ElementVisitor()
        psi.accept(elementVisitor)
        return elementVisitor.resultElement
    }
}


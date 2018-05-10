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
import org.jetbrains.kotlin.j2k.tree.conveersionCache.JKMultiverseClass
import org.jetbrains.kotlin.j2k.tree.conveersionCache.ReferenceTargetProvider
import org.jetbrains.kotlin.j2k.tree.impl.*


class JavaToJKTreeBuilder : ReferenceTargetProvider {
    override fun resolveClassReference(clazz: PsiClass): JKClass {
        val name = clazz.qualifiedName ?: TODO()
        return universe[name] ?: multiverse[name] ?: run {
            val clazz = JKMultiverseClass(JKNameIdentifierImpl(name), mutableListOf(), JKClass.ClassKind.CLASS, JKModifierListImpl())
            multiverse[name] = clazz
            clazz
        }
    }

    override fun resolveClassReference(identifier: String): JKClass {
        return universe[identifier] ?: multiverse[identifier] ?: run {
            val clazz =
                JKMultiverseClass(JKNameIdentifierImpl(identifier), mutableListOf(), JKClass.ClassKind.CLASS, JKModifierListImpl())
            multiverse[identifier] = clazz
            clazz
        }
    }

    override fun putUniverseClass(clazz: JKClass) {
        universe[clazz.name.name] = clazz
    }

    override fun putMultiverseClass(clazz: JKMultiverseClass) {
        multiverse[clazz.name.name] = clazz
    }

    private val universe = mutableMapOf<String, JKClass>()
    private val multiverse = mutableMapOf<String, JKMultiverseClass>()

    private val expressionTreeMapper = ExpressionTreeMapper()

    private val declarationMapper = DeclarationMapper(expressionTreeMapper)

    private val modifierMapper = ModifierMapper()

    private inner class ExpressionTreeMapper {
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
            return JKBinaryExpressionImpl(lOperand.toJK(), rOperand?.toJK() ?: TODO(), operationSign.toJK())
        }

        fun PsiLiteralExpression.toJK(): JKLiteralExpression {
            if (this !is PsiLiteralExpressionImpl) {
                throw RuntimeException("Not supported")
            }
            return JKJavaLiteralExpressionImpl(
                innerText!!, when (this.literalElementType) {
                    JavaTokenType.STRING_LITERAL -> JKLiteralExpression.LiteralType.STRING
                    JavaTokenType.TRUE_KEYWORD, JavaTokenType.FALSE_KEYWORD -> JKLiteralExpression.LiteralType.BOOLEAN
                    JavaTokenType.NULL_KEYWORD -> JKLiteralExpression.LiteralType.NULL
                    else -> throw RuntimeException("Not supported")
                }
            )
        }

        fun PsiJavaToken.toJK(): JKOperator = when (tokenType) {
            JavaTokenType.PLUS -> JKJavaOperatorImpl.PLUS
            JavaTokenType.MINUS -> JKJavaOperatorImpl.MINUS
            else -> throw RuntimeException("Not supported")
        }

        fun PsiPrefixExpression.toJK(): JKExpression {
            return JKPrefixExpressionImpl(operand?.toJK() ?: TODO(), operationSign.toJK())
        }

        fun PsiPostfixExpression.toJK(): JKExpression {
            return JKPostfixExpressionImpl(operand.toJK(), operationSign.toJK())
        }

        fun PsiMethodCallExpression.toJK(): JKExpression {
            val method = methodExpression as PsiReferenceExpressionImpl

            val identifier = method.resolve().convertReference()
            val call = JKJavaMethodCallExpressionImpl(identifier as JKMethodReference, argumentList.toJK())
            return if (method.findChildByRole(ChildRole.DOT) != null) {
                JKQualifiedExpressionImpl((method.qualifier as PsiExpression).toJK(), JKJavaQualifierImpl.DOT, call)
            } else {
                call
            }
        }

        fun PsiReferenceExpression.toJK(): JKExpression {
            val impl = this as PsiReferenceExpressionImpl
            val identifier = impl.resolve().convertReference() as JKFieldReference
            val access = JKJavaFieldAccessExpressionImpl(identifier)
            return if (impl.findChildByRole(ChildRole.DOT) != null) {
                JKQualifiedExpressionImpl((impl.qualifier as PsiExpression).toJK(), JKJavaQualifierImpl.DOT, access)
            } else {
                access
            }
        }

        fun PsiNewExpression.toJK(): JKExpression {
            assert(this is PsiNewExpressionImpl)
            if ((this as PsiNewExpressionImpl).findChildByRole(ChildRole.LBRACKET) != null) {
                val arrayInitializer = arrayInitializer
                if (arrayInitializer != null) {
                    return JKJavaNewArrayImpl(arrayInitializer.initializers.map { toJK() })
                } else {
                    val dimensions = mutableListOf<PsiLiteralExpression?>()
                    var child = firstChild
                    while (child != null) {
                        if (child.node.elementType == JavaTokenType.LBRACKET) {
                            child = child.nextSibling
                            if (child.node.elementType == JavaTokenType.RBRACKET) {
                                dimensions.add(null)
                            } else {
                                dimensions.add(child as PsiLiteralExpression?)
                            }
                        }
                        child = child.nextSibling
                    }
                    return JKJavaNewEmptyArrayImpl(dimensions.map { it?.toJK() })
                }
            }
            return JKJavaNewExpressionImpl(classReference?.resolve().convertReference() as JKClassReference, argumentList.toJK())
        }

        fun PsiArrayAccessExpression.toJK(): JKExpression {
            return JKArrayAccessExpressionImpl(arrayExpression.toJK(), indexExpression?.toJK() ?: TODO())
        }

        fun PsiTypeCastExpression.toJK(): JKExpression {
            return JKTypeCastExpressionImpl(operand?.toJK() ?: TODO(), castType?.type?.toJK() ?: TODO())
        }

        fun PsiParenthesizedExpression.toJK(): JKExpression {
            return JKParenthesizedExpressionImpl(expression?.toJK() ?: TODO())
        }

        fun PsiExpressionList?.toJK(): JKExpressionList {
            return JKExpressionListImpl(this?.expressions?.map { it.toJK() } ?: emptyList())
        }

        fun PsiElement?.convertReference(): JKReference = when (this) {
            is PsiMethod -> convertMethodReference()
            is PsiField -> convertFieldReference()
            is PsiClass -> convertClassReference()
            else -> throw Exception("Invalid PSI")
        }

        fun PsiMethod.convertMethodReference(): JKMethodReference {
            val clazz = this@JavaToJKTreeBuilder.resolveClassReference(containingClass ?: TODO())
            return JKMethodReferenceImpl(
                this@JavaToJKTreeBuilder.resolveMethodReference(clazz, this),
                if (clazz is JKMultiverseClass) JKReference.JKReferenceType.U2M else JKReference.JKReferenceType.U2U
            )
        }

        fun PsiField.convertFieldReference(): JKFieldReference {
            val clazz = this@JavaToJKTreeBuilder.resolveClassReference(containingClass ?: TODO())
            return JKFieldReferenceImpl(
                this@JavaToJKTreeBuilder.resolveFieldReference(clazz, this),
                if (clazz is JKMultiverseClass) JKReference.JKReferenceType.U2M else JKReference.JKReferenceType.U2U
            )
        }

        fun PsiClass.convertClassReference(): JKClassReference {
            val clazz = this@JavaToJKTreeBuilder.resolveClassReference(this)
            return JKClassReferenceImpl(
                clazz, if (clazz is JKMultiverseClass) JKReference.JKReferenceType.U2M else JKReference.JKReferenceType.U2U
            )
        }

        fun PsiType.toJK(): JKType {
            return when (this) {
                is PsiClassType -> JKClassTypeImpl(resolve()!!.convertClassReference(), parameters.map { it.toJK() })
                is PsiArrayType -> JKJavaArrayTypeImpl(componentType.toJK())
                is PsiPrimitiveType -> when (presentableText) {
                    "int" -> JKJavaPrimitiveTypeImpl.INT
                    "byte" -> JKJavaPrimitiveTypeImpl.BYTE
                    "boolean" -> JKJavaPrimitiveTypeImpl.BOOLEAN
                    else -> throw Exception("Invalid PSI")
                }
                else -> throw Exception("Invalid PSI")
            }
        }

    }

    private inner class DeclarationMapper(val expressionTreeMapper: ExpressionTreeMapper) {
        fun PsiClass.toJK(): JKUniverseClass {
            val classKind: JKClass.ClassKind = when {
                isAnnotationType -> JKClass.ClassKind.ANNOTATION
                isEnum -> JKClass.ClassKind.ENUM
                isInterface -> JKClass.ClassKind.INTERFACE
                else -> JKClass.ClassKind.CLASS
            }
            return JKClassImpl(with(modifierMapper) { modifierList.toJK() }, JKNameIdentifierImpl(name!!), classKind).apply {
                declarations =
                        children.mapNotNull { ElementVisitor(this@DeclarationMapper).apply { it.accept(this) }.resultElement as? JKUniverseDeclaration }
            }
        }

        fun PsiField.toJK(): JKJavaField {
            return JKJavaFieldImpl(with(modifierMapper) { modifierList.toJK() }, with(expressionTreeMapper) { type.toJK() },
                                   JKNameIdentifierImpl(this.name), with(expressionTreeMapper) { initializer?.toJK() } ?: TODO())
        }

        fun PsiMethod.toJK(): JKJavaMethod {
            return JKJavaMethodImpl(
                with(modifierMapper) { modifierList.toJK() }, JKNameIdentifierImpl(name),
                parameterList.parameters.map { it -> it.toJK() }, body?.toJK() ?: TODO()
            )
        }

        fun PsiMember.toJK(): JKDeclaration? = when (this) {
            is PsiField -> this.toJK()
            is PsiMethod -> this.toJK()
            else -> null
        }

        fun PsiParameter.toJK(): JKValueArgumentImpl {
            return JKValueArgumentImpl(with(expressionTreeMapper) { type.toJK() }, name!!)
        }

        fun PsiCodeBlock.toJK(): JKBlock {
            return JKBlockImpl(statements.map { it.toJK() })
        }

        fun PsiStatement.toJK(): JKStatement {
            if (this is PsiExpressionStatement) {
                return JKExpressionStatementImpl(with(expressionTreeMapper) { expression.toJK() })
            }
            TODO()
        }
    }

    private inner class ModifierMapper {
        fun PsiModifierList?.toJK(): JKModifierList = JKModifierListImpl(
            if (this == null) emptyList()
            else PsiModifier.MODIFIERS.filter { hasExplicitModifier(it) }.map { modifierToJK(it) }.toMutableList()
        )

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

    private inner class ElementVisitor(val declarationMapper: DeclarationMapper) : JavaElementVisitor() {

        var resultElement: JKTreeElement? = null

        override fun visitClass(aClass: PsiClass) {
            resultElement = with(declarationMapper) { aClass.toJK() }
        }

        override fun visitField(field: PsiField) {
            resultElement = with(declarationMapper) { field.toJK() }
        }

        override fun visitMethod(method: PsiMethod) {
            resultElement = with(declarationMapper) { method.toJK() }
        }

        override fun visitFile(file: PsiFile) {
            file.acceptChildren(this)
        }
    }


    fun buildTree(psi: PsiElement): JKTreeElement? {
        assert(psi.language.`is`(JavaLanguage.INSTANCE)) { "Unable to build JK Tree using Java Visitor for language ${psi.language}" }
        val elementVisitor = ElementVisitor(declarationMapper)
        psi.accept(elementVisitor)
        return elementVisitor.resultElement
    }
}


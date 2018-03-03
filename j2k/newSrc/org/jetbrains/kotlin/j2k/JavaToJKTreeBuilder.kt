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
import java.util.*


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
            val clazz = JKMultiverseClass(JKNameIdentifierImpl(identifier), mutableListOf(), JKClass.ClassKind.CLASS, JKModifierListImpl())
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

            val identifier = method.resolve().convertReference()
            val call = JKJavaMethodCallExpressionImpl(identifier as JKMethodReference, argumentList.toJK())
            return if (method.findChildByRole(ChildRole.DOT) != null) {
                JKQualifiedExpressionImpl((method.qualifier as PsiExpression).toJK(), JKJavaQualificationIdentifierImpl.DOT, call)
            }
            else {
                call
            }
        }

        fun PsiReferenceExpression.toJK(): JKExpression {
            val impl = this as PsiReferenceExpressionImpl
            val identifier = impl.resolve().convertReference()
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
            return JKJavaNewExpressionImpl(classReference?.resolve().convertReference() as JKClassReference, argumentList.toJK())
        }

        fun PsiArrayAccessExpression.toJK(): JKExpression {
            return JKArrayAccessExpressionImpl(arrayExpression.toJK(), indexExpression?.toJK())
        }

        fun PsiTypeCastExpression.toJK(): JKExpression {
            return JKTypeCastExpressionImpl(operand?.toJK(), castType?.toJK())
        }

        fun PsiParenthesizedExpression.toJK(): JKExpression {
            return JKParenthesizedExpressionImpl(expression?.toJK())
        }

        fun PsiExpressionList?.toJK(): JKExpressionList {
            return JKExpressionListImpl(this?.expressions?.map { it.toJK() }?.toTypedArray() ?: emptyArray())
        }

        fun PsiElement?.convertReference(): JKReference = when(this){
            is PsiMethod -> convertMethodReference()
            is PsiField -> convertFieldReference()
            is PsiClass -> convertType()
            else -> throw Exception("Invalid PSI")
        }

        fun PsiMethod.convertMethodReference(): JKJavaMethodReference {
            val clazz = this@JavaToJKTreeBuilder.resolveClassReference(containingClass ?: TODO())
            return JKMethodReferenceImpl(clazz, this@JavaToJKTreeBuilder.resolveMethodReference(clazz, this))
        }

        fun PsiField.convertFieldReference(): JKJavaFieldReference {
            val clazz = this@JavaToJKTreeBuilder.resolveClassReference(containingClass ?: TODO())
            return JKFieldReferenceImpl(clazz, this@JavaToJKTreeBuilder.resolveFieldReference(clazz, this))
        }

        fun PsiClass.convertType(): JKJavaClassReference {
            return JKClassReferenceImpl(this@JavaToJKTreeBuilder.resolveClassReference(this))
        }

        fun PsiTypeElement.toJK(): JKType? {
            return innermostComponentReferenceElement?.convertType()
        }

        fun PsiJavaCodeReferenceElement.convertType(): JKType {
            val classReference = JKClassReferenceImpl(this@JavaToJKTreeBuilder.resolveClassReference(this.resolve() as? PsiClass ?: TODO()))
            val typeArguments = ArrayList<JKType>()
            parameterList?.typeParameterElements?.forEach { it ->
                kotlin.run {
                    val type = it.toJK()
                    if (type != null) typeArguments.add(type)
                }
            }
            return JKTypeImpl(classReference, typeArguments)
        }

    }

    private inner class DeclarationMapper(val expressionTreeMapper: ExpressionTreeMapper) {

        val declarationMapper = this

        fun PsiClass.toJK(): JKClass {
            val classKind: JKClass.ClassKind = when {
                isAnnotationType -> JKClass.ClassKind.ANNOTATION
                isEnum -> JKClass.ClassKind.ENUM
                isInterface -> JKClass.ClassKind.INTERFACE
                else -> JKClass.ClassKind.CLASS
            }
            return JKClassImpl(with(modifierMapper) { modifierList.toJK() }, JKNameIdentifierImpl(this.name!!), classKind).apply {
                declarations = children.mapNotNull { ElementVisitor(declarationMapper).apply { it.accept(this) }.resultElement as? JKDeclaration }
            }
        }


        fun PsiField.toJK(): JKJavaField {

            val initializer = with(expressionTreeMapper) {
                initializer?.toJK()
            }

            val type = JKJavaTypeIdentifierImpl(this.type.canonicalText)
            val name = JKNameIdentifierImpl(this.name!!)

            val modifierList = with(modifierMapper) {
                modifierList.toJK()
            }

            return JKJavaFieldImpl(modifierList, type, name, initializer)
        }

        fun PsiMethod.toJK(): JKJavaMethod {
            val modifierList = with(modifierMapper) { modifierList.toJK() }
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

    private inner class ElementVisitor(val declarationMapper: DeclarationMapper) : JavaElementVisitor() {

        var resultElement: JKElement? = null

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


    fun buildTree(psi: PsiElement): JKElement? {
        assert(psi.language.`is`(JavaLanguage.INSTANCE)) { "Unable to build JK Tree using Java Visitor for language ${psi.language}" }
        val elementVisitor = ElementVisitor(declarationMapper)
        psi.accept(elementVisitor)
        return elementVisitor.resultElement
    }
}


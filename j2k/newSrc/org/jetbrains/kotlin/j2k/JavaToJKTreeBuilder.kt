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

import com.intellij.lang.jvm.JvmAnnotatedElement
import com.intellij.psi.*
import com.intellij.psi.JavaTokenType.SUPER_KEYWORD
import com.intellij.psi.JavaTokenType.THIS_KEYWORD
import com.intellij.psi.impl.source.tree.ChildRole
import com.intellij.psi.impl.source.tree.java.*
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiUtil
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.idea.j2k.content
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.JKLiteralExpression.LiteralType.*
import org.jetbrains.kotlin.j2k.tree.impl.*
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


class JavaToJKTreeBuilder(
    var symbolProvider: JKSymbolProvider,
    private val converterServices: NewJavaToKotlinServices
) {

    private val expressionTreeMapper = ExpressionTreeMapper()

    val referenceSearcher: ReferenceSearcher = converterServices.oldServices.referenceSearcher

    private val declarationMapper = DeclarationMapper(expressionTreeMapper)

    private fun PsiJavaFile.toJK(): JKFile =
        JKFileImpl(
            packageStatement?.toJK() ?: JKPackageDeclarationImpl(JKNameIdentifierImpl("")),
            importList?.importStatements?.map { it.toJK() }.orEmpty(),
            with(declarationMapper) { classes.map { it.toJK() } }
        )

   private fun PsiPackageStatement.toJK(): JKPackageDeclaration =
        JKPackageDeclarationImpl(JKNameIdentifierImpl(packageName))

    private fun PsiImportStatement.toJK(): JKImportStatementImpl {
        val target = resolve()
        val rawName = text.substringAfter("import").substringBeforeLast(";").trim()
        val name =
            if (target is KtLightClassForFacade) rawName.replaceAfterLast('.', "*")
            else rawName
        return JKImportStatementImpl(JKNameIdentifierImpl(name))
    }


    private inner class ExpressionTreeMapper {
        fun PsiExpression?.toJK(): JKExpression {
            return when (this) {
                null -> JKStubExpressionImpl()
                is PsiBinaryExpression -> toJK()
                is PsiPrefixExpression -> toJK()
                is PsiPostfixExpression -> toJK()
                is PsiLiteralExpression -> toJK()
                is PsiMethodCallExpression -> toJK()
                is PsiReferenceExpression -> toJK()
                is PsiNewExpression -> toJK()
                is PsiArrayAccessExpression -> toJK()
                is PsiTypeCastExpression -> toJK()
                is PsiParenthesizedExpression -> toJK()
                is PsiAssignmentExpression -> toJK()
                is PsiInstanceOfExpression -> toJK()
                is PsiThisExpression ->
                    JKThisExpressionImpl(
                        qualifier?.referenceName?.let { JKLabelTextImpl(JKNameIdentifierImpl(it)) } ?: JKLabelEmptyImpl()
                    )
                is PsiSuperExpression ->
                    JKSuperExpressionImpl(
                        qualifier?.referenceName?.let { JKLabelTextImpl(JKNameIdentifierImpl(it)) } ?: JKLabelEmptyImpl()
                    )
                is PsiConditionalExpression -> JKIfElseExpressionImpl(
                    condition.toJK(), thenExpression.toJK(), elseExpression.toJK()
                )
                is PsiPolyadicExpression -> JKJavaPolyadicExpressionImpl(
                    operands.map { it.toJK() },
                    Array(operands.lastIndex) { getTokenBeforeOperand(operands[it + 1]) }.map { it?.tokenType?.toJK() ?: TODO() }
                )
                is PsiArrayInitializerExpression -> toJK()
                is PsiLambdaExpression -> toJK()
                is PsiClassObjectAccessExpressionImpl -> toJK()
                else -> {
                    throw RuntimeException("Not supported: ${this::class}")
                }
            }.also {
                if (this != null) (it as PsiOwner).psi = this
            }
        }

        fun PsiClassObjectAccessExpressionImpl.toJK(): JKClassLiteralExpression {
            val type = operand.toJK().type.updateNullabilityRecursively(Nullability.NotNull)
            return JKClassLiteralExpressionImpl(
                JKTypeElementImpl(type),
                when (type) {
                    is JKJavaPrimitiveType -> JKClassLiteralExpression.LiteralType.JAVA_PRIMITIVE_CLASS
                    is JKJavaVoidType -> JKClassLiteralExpression.LiteralType.JAVA_VOID_TYPE
                    else -> JKClassLiteralExpression.LiteralType.JAVA_CLASS
                }
            )
        }

        fun PsiInstanceOfExpression.toJK(): JKKtIsExpression =
            JKKtIsExpressionImpl(operand.toJK(), checkType?.toJK() ?: JKTypeElementImpl(JKNoTypeImpl))


        fun PsiAssignmentExpression.toJK(): JKJavaAssignmentExpression {
            return JKJavaAssignmentExpressionImpl(
                lExpression.toJK() as? JKAssignableExpression ?: error("Its possible? ${lExpression.toJK().prettyDebugPrintTree()}"),
                rExpression.toJK(),
                operationSign.tokenType.toJK()
            )
        }

        fun PsiBinaryExpression.toJK(): JKExpression {
            val token = when (operationSign.tokenType) {
                JavaTokenType.EQEQ, JavaTokenType.NE ->
                    when {
                        canKeepEqEq(lOperand, rOperand) -> operationSign.tokenType
                        operationSign.tokenType == JavaTokenType.EQEQ -> KtTokens.EQEQEQ
                        else -> KtTokens.EXCLEQEQEQ
                    }
                else -> operationSign.tokenType
            }
            return JKBinaryExpressionImpl(lOperand.toJK(), rOperand.toJK(), token.toJK())
        }

        fun PsiLiteralExpression.toJK(): JKLiteralExpression {
            require(this is PsiLiteralExpressionImpl)

            return when (this.literalElementType) {
                JavaTokenType.NULL_KEYWORD -> JKNullLiteral()
                JavaTokenType.TRUE_KEYWORD -> JKBooleanLiteral(true)
                JavaTokenType.FALSE_KEYWORD -> JKBooleanLiteral(false)
                JavaTokenType.STRING_LITERAL -> JKJavaLiteralExpressionImpl(text, STRING)
                JavaTokenType.CHARACTER_LITERAL -> JKJavaLiteralExpressionImpl(text, CHAR)
                JavaTokenType.INTEGER_LITERAL -> JKJavaLiteralExpressionImpl(text, INT)
                JavaTokenType.LONG_LITERAL -> JKJavaLiteralExpressionImpl(text, LONG)
                JavaTokenType.FLOAT_LITERAL -> JKJavaLiteralExpressionImpl(text, FLOAT)
                JavaTokenType.DOUBLE_LITERAL -> JKJavaLiteralExpressionImpl(text, DOUBLE)
                else -> error("Unknown literal element type: ${this.literalElementType}")
            }
        }

        fun IElementType.toJK(): JKOperator = JKJavaOperatorImpl.tokenToOperator[this] ?: error("Unsupported token-type: $this")

        fun PsiPrefixExpression.toJK(): JKExpression {
            return JKPrefixExpressionImpl(operand.toJK(), operationSign.tokenType.toJK()).let {
                if (it.operator.token.text in listOf("+", "-") && it.expression is JKLiteralExpression) {
                    JKJavaLiteralExpressionImpl(
                        it.operator.token.text + (it.expression as JKLiteralExpression).literal,
                        (it.expression as JKLiteralExpression).type
                    )
                } else it
            }
        }

        fun PsiPostfixExpression.toJK(): JKExpression {
            return JKPostfixExpressionImpl(operand.toJK(), operationSign.tokenType.toJK())
        }

        fun PsiLambdaExpression.toJK(): JKExpression {
            return JKLambdaExpressionImpl(
                body.let {
                    when (it) {
                        is PsiExpression -> JKExpressionStatementImpl(it.toJK())
                        is PsiCodeBlock -> JKBlockStatementImpl(with(declarationMapper) { it.toJK() })
                        else -> JKBlockStatementImpl(JKBodyStub)
                    }
                },
                with(declarationMapper) { parameterList.parameters.map { it.toJK() } }
            )
        }

        private fun JKExpression.qualified(qualifier: JKExpression?) =
            if (qualifier != null) {
                JKQualifiedExpressionImpl(qualifier, JKJavaQualifierImpl.DOT, this)
            } else this

        //TODO mostly copied from old j2k, refactor
        fun PsiMethodCallExpression.toJK(): JKExpression {
            val arguments = argumentList.toJK()
            val typeArguments = typeArgumentList.toJK()
            val qualifier = methodExpression.qualifierExpression?.toJK()
            val target = methodExpression.resolve()
            val symbol = symbolProvider.provideSymbol<JKMethodSymbol>(methodExpression as PsiReferenceExpressionImpl)

            return when {
                methodExpression.referenceNameElement is PsiKeyword -> {
                    val callee = when ((methodExpression.referenceNameElement as PsiKeyword).tokenType) {
                        SUPER_KEYWORD -> JKSuperExpressionImpl()
                        THIS_KEYWORD -> JKThisExpressionImpl(JKLabelEmptyImpl())
                        else -> error("Unknown keyword in callee position")
                    }
                    JKDelegationConstructorCallImpl(symbol, callee, arguments)
                }

                target is KtLightMethod -> {
                    val origin = target.kotlinOrigin
                    when (origin) {
                        is KtNamedFunction -> {
                            if (origin.isExtensionDeclaration()) {
                                val receiver = arguments.expressions.firstOrNull()
                                JKJavaMethodCallExpressionImpl(
                                    symbolProvider.provideDirectSymbol(origin) as JKMethodSymbol,
                                    arguments.also { it.expressions = it.expressions.drop(1) },
                                    typeArguments
                                ).qualified(receiver)
                            } else {
                                JKJavaMethodCallExpressionImpl(
                                    symbolProvider.provideDirectSymbol(origin) as JKMethodSymbol,
                                    arguments,
                                    typeArguments
                                ).qualified(qualifier)
                            }
                        }
                        is KtProperty, is KtPropertyAccessor, is KtParameter -> {
                            val property =
                                if (origin is KtPropertyAccessor) origin.parent as KtProperty
                                else origin as KtNamedDeclaration
                            val parameterCount = target.parameterList.parameters.size
                            val propertyAccessExpression =
                                JKFieldAccessExpressionImpl(symbolProvider.provideDirectSymbol(property) as JKFieldSymbol)
                            val isExtension = property.isExtensionDeclaration()
                            val isTopLevel = origin.getStrictParentOfType<KtClassOrObject>() == null
                            val propertyAccess = if (isTopLevel) {
                                if (isExtension) JKQualifiedExpressionImpl(
                                    arguments.expressions.first().detached(arguments),
                                    JKJavaQualifierImpl.DOT,
                                    propertyAccessExpression
                                )
                                else propertyAccessExpression
                            } else propertyAccessExpression.qualified(qualifier) as JKAssignableExpression

                            when (if (isExtension) parameterCount - 1 else parameterCount) {
                                0 /* getter */ ->
                                    propertyAccess

                                1 /* setter */ -> {
                                    val argument = (arguments.expressions[if (isExtension) 1 else 0]).detached(arguments)
                                    JKJavaAssignmentExpressionImpl(
                                        propertyAccess,
                                        argument,
                                        JKJavaOperatorImpl.tokenToOperator[JavaTokenType.EQ]!!
                                    )
                                }
                                else -> TODO()
                            }
                        }

                        else -> TODO()
                    }
                }

                target is PsiMethod ->
                    JKJavaMethodCallExpressionImpl(symbol, arguments, typeArguments)
                        .qualified(qualifier)
                else ->
                    JKJavaMethodCallExpressionImpl(symbol, arguments, typeArguments)
                        .qualified(qualifier)
            }
        }

        fun PsiReferenceExpression.toJK(): JKExpression {
            val symbol = symbolProvider.provideSymbol(this)
            val access = when (symbol) {
                is JKClassSymbol -> JKClassAccessExpressionImpl(symbol)
                is JKFieldSymbol -> JKFieldAccessExpressionImpl(symbol)
                else -> TODO()
            }
            return qualifierExpression?.let { JKQualifiedExpressionImpl(it.toJK(), JKJavaQualifierImpl.DOT, access) } ?: access
        }

        fun PsiArrayInitializerExpression.toJK(): JKExpression {
            return JKJavaNewArrayImpl(
                initializers.map { it.toJK() },
                JKTypeElementImpl(type?.toJK(symbolProvider).safeAs<JKJavaArrayType>()?.type ?: JKContextType)
            )
        }

        fun PsiNewExpression.toJK(): JKExpression {
            require(this is PsiNewExpressionImpl)
            val newExpression =
                if (findChildByRole(ChildRole.LBRACKET) != null) {
                    arrayInitializer?.toJK() ?: run {
                        val dimensions = mutableListOf<PsiExpression?>()
                        var child = firstChild
                        while (child != null) {
                            if (child.node.elementType == JavaTokenType.LBRACKET) {
                                child = child.nextSibling
                                dimensions += if (child.node.elementType == JavaTokenType.RBRACKET) {
                                    null
                                } else {
                                    child as PsiExpression? //TODO
                                }
                            }
                            child = child.nextSibling
                        }
                        JKJavaNewEmptyArrayImpl(
                            dimensions.map { it?.toJK() ?: JKStubExpressionImpl() },
                            JKTypeElementImpl(generateSequence(type?.toJK(symbolProvider)) { it.safeAs<JKJavaArrayType>()?.type }.last())
                        ).also {
                            it.psi = this
                        }
                    }
                } else {
                    val classSymbol =
                        classOrAnonymousClassReference?.resolve()?.let {
                            symbolProvider.provideDirectSymbol(it) as JKClassSymbol
                        } ?: JKUnresolvedClassSymbol(classOrAnonymousClassReference!!.text)
                    val typeArgumentList =
                        this.typeArgumentList.toJK()
                            .takeIf { it.typeArguments.isNotEmpty() }
                            ?: classOrAnonymousClassReference
                                ?.typeParameters
                                ?.let { typeParameters ->
                                    JKTypeArgumentListImpl(typeParameters.map { JKTypeElementImpl(it.toJK(symbolProvider)) })
                                } ?: JKTypeArgumentListImpl()
                    JKJavaNewExpressionImpl(
                        classSymbol,
                        argumentList.toJK(),
                        typeArgumentList,
                        with(declarationMapper) { anonymousClass?.createClassBody() } ?: JKEmptyClassBodyImpl()
                    )
                }
            return qualifier?.let { JKQualifiedExpressionImpl(it.toJK(), JKJavaQualifierImpl.DOT, newExpression) } ?: newExpression
        }

        fun PsiReferenceParameterList.toJK(): JKTypeArgumentList =
            JKTypeArgumentListImpl(this.typeArguments.map { JKTypeElementImpl(it.toJK(symbolProvider)) })


        fun PsiArrayAccessExpression.toJK(): JKExpression {
            return JKArrayAccessExpressionImpl(arrayExpression.toJK(), indexExpression?.toJK() ?: TODO())
        }

        fun PsiTypeCastExpression.toJK(): JKExpression {
            return JKTypeCastExpressionImpl(operand?.toJK() ?: TODO(), castType?.toJK() ?: TODO())
        }

        fun PsiParenthesizedExpression.toJK(): JKExpression {
            return JKParenthesizedExpressionImpl(expression?.toJK() ?: TODO())
        }

        fun PsiExpressionList?.toJK(): JKExpressionList {
            return JKExpressionListImpl(this?.expressions?.map { it.toJK() } ?: emptyList())
        }

        fun PsiTypeElement.toJK(): JKTypeElement {
            return JKTypeElementImpl(type.toJK(symbolProvider)).also {
                (it as PsiOwner).psi = this
            }
        }
    }

    private inner class DeclarationMapper(val expressionTreeMapper: ExpressionTreeMapper) {
        fun PsiTypeParameterList.toJK(): JKTypeParameterList =
            JKTypeParameterListImpl(typeParameters.map { it.toJK() })

        fun PsiTypeParameter.toJK(): JKTypeParameter =
            JKTypeParameterImpl(JKNameIdentifierImpl(name!!),
                                extendsListTypes.map { JKTypeElementImpl(it.toJK(symbolProvider, Nullability.Default)) })

        fun PsiClass.toJK(): JKClass {
            val classKind: JKClass.ClassKind = when {
                isAnnotationType -> JKClass.ClassKind.ANNOTATION
                isEnum -> JKClass.ClassKind.ENUM
                isInterface -> JKClass.ClassKind.INTERFACE
                else -> JKClass.ClassKind.CLASS
            }

            fun PsiReferenceList.mapTypes() =
                this.referencedTypes.map { with(expressionTreeMapper) { JKTypeElementImpl(it.toJK(symbolProvider, Nullability.Default)) } }

            val implTypes = this.implementsList?.mapTypes().orEmpty()
            val extensionType = this.extendsList?.mapTypes().orEmpty()
            return JKClassImpl(
                JKNameIdentifierImpl(name!!),
                JKInheritanceInfoImpl(extensionType, implTypes),
                classKind,
                typeParameterList?.toJK() ?: JKTypeParameterListImpl(),
                createClassBody(),
                annotationList(),
                modifiers(),
                visibility(),
                modality()
            ).also { jkClassImpl ->
                jkClassImpl.psi = this
                symbolProvider.provideUniverseSymbol(this, jkClassImpl)
            }
        }

        fun PsiClass.createClassBody() =
            JKClassBodyImpl(
                children.mapNotNull {
                    when (it) {
                        is PsiEnumConstant -> it.toJK()
                        is PsiClass -> it.toJK()
                        is PsiMethod -> it.toJK()
                        is PsiField -> it.toJK()
                        is PsiClassInitializer -> it.toJK()
                        else -> null
                    }
                }
            )

        fun PsiClassInitializer.toJK() =
            JKKtInitDeclarationImpl(body.toJK())

        fun PsiEnumConstant.toJK(): JKEnumConstant =
            JKEnumConstantImpl(
                JKNameIdentifierImpl(name),
                with(expressionTreeMapper) { argumentList.toJK() },
                initializingClass?.createClassBody() ?: JKEmptyClassBodyImpl(),
                JKTypeElementImpl(JKClassTypeImpl(symbolProvider.provideDirectSymbol(containingClass!!) as JKClassSymbol, emptyList()))
            ).also {
                symbolProvider.provideUniverseSymbol(this, it)
                it.psi = this
            }

        fun PsiMember.modality() =
            when {
                modifierList == null -> Modality.OPEN
                hasModifierProperty(PsiModifier.FINAL) -> Modality.FINAL
                hasModifierProperty(PsiModifier.ABSTRACT) -> Modality.ABSTRACT
                else -> Modality.OPEN
            }

        fun PsiMember.visibility() =
            when {
                modifierList == null -> Visibility.PACKAGE_PRIVATE
                hasModifierProperty(PsiModifier.PACKAGE_LOCAL) -> Visibility.PACKAGE_PRIVATE
                hasModifierProperty(PsiModifier.PRIVATE) -> Visibility.PRIVATE
                hasModifierProperty(PsiModifier.PROTECTED) -> handleProtectedVisibility()
                hasModifierProperty(PsiModifier.PUBLIC) -> Visibility.PUBLIC
                else -> Visibility.PACKAGE_PRIVATE
            }

        private fun PsiMember.handleProtectedVisibility(): Visibility {
            val originalClass = containingClass ?: return Visibility.PROTECTED
            // Search for usages only in Java because java-protected member cannot be used in Kotlin from same package
            val usages = referenceSearcher.findUsagesForExternalCodeProcessing(this, true, false)

            return if (usages.any { !allowProtected(it.element, this, originalClass) })
                Visibility.PUBLIC
            else Visibility.PROTECTED
        }

        private fun allowProtected(element: PsiElement, member: PsiMember, originalClass: PsiClass): Boolean {
            if (element.parent is PsiNewExpression && member is PsiMethod && member.isConstructor) {
                // calls to for protected constructors are allowed only within same class or as super calls
                return element.parentsWithSelf.contains(originalClass)
            }

            return element.parentsWithSelf.filterIsInstance<PsiClass>().any { accessContainingClass ->
                if (!InheritanceUtil.isInheritorOrSelf(accessContainingClass, originalClass, true)) return@any false

                if (element !is PsiReferenceExpression) return@any true

                val qualifierExpression = element.qualifierExpression ?: return@any true

                // super.foo is allowed if 'foo' is protected
                if (qualifierExpression is PsiSuperExpression) return@any true

                val receiverType = qualifierExpression.type ?: return@any true
                val resolvedClass = PsiUtil.resolveGenericsClassInType(receiverType).element ?: return@any true

                // receiver type should be subtype of containing class
                InheritanceUtil.isInheritorOrSelf(resolvedClass, accessContainingClass, true)
            }
        }


        fun PsiMember.modifiers() =
            if (modifierList == null) emptyList()
            else
                PsiModifier.MODIFIERS
                    .filter { this.modifierList!!.hasExplicitModifier(it) }
                    .mapNotNull {
                        when (it) {
                            PsiModifier.NATIVE -> ExtraModifier.NATIVE
                            PsiModifier.STATIC -> ExtraModifier.STATIC
                            PsiModifier.STRICTFP -> ExtraModifier.STRICTFP
                            PsiModifier.SYNCHRONIZED -> ExtraModifier.SYNCHRONIZED
                            PsiModifier.TRANSIENT -> ExtraModifier.TRANSIENT
                            PsiModifier.VOLATILE -> ExtraModifier.VOLATILE

                            PsiModifier.PROTECTED -> null
                            PsiModifier.PUBLIC -> null
                            PsiModifier.PRIVATE -> null
                            PsiModifier.FINAL -> null
                            PsiModifier.ABSTRACT -> null

                            else -> TODO("Not yet supported")
                        }
                    }


        fun PsiField.toJK(): JKJavaField {
            return JKJavaFieldImpl(
                with(expressionTreeMapper) { typeElement?.toJK() } ?: JKTypeElementImpl(JKNoTypeImpl),
                JKNameIdentifierImpl(name),
                with(expressionTreeMapper) { initializer.toJK() },
                annotationList(),
                modifiers(),
                visibility(),
                modality(),
                Mutability.UNKNOWN
            ).also {
                symbolProvider.provideUniverseSymbol(this, it)
                it.psi = this
            }
        }

        fun <T> T.annotationList(): JKAnnotationList where T : JvmAnnotatedElement, T: PsiDocCommentOwner {
            val plainAnnotations = annotations.map { it.toJK() }
            val deprecatedAnnotation = docComment?.deprecatedAnnotation() ?: return JKAnnotationListImpl(plainAnnotations)
            return JKAnnotationListImpl(
                plainAnnotations.mapNotNull { annotation ->
                    if (annotation.classSymbol.fqName == "java.lang.Deprecated") null else annotation
                } + deprecatedAnnotation
            )
        }

        fun PsiAnnotation.toJK(): JKAnnotation =
            JKAnnotationImpl(
                symbolProvider.provideSymbol(nameReferenceElement!!),
                with(expressionTreeMapper) {
                    JKExpressionListImpl(parameterList.attributes.map { (it.value as? PsiExpression).toJK() })
                }
            )

        fun PsiDocComment.deprecatedAnnotation(): JKAnnotation? =
            findTagByName("deprecated")?.let { tag ->
                JKAnnotationImpl(
                    symbolProvider.provideByFqName("kotlin.Deprecated"),
                    JKExpressionListImpl(
                        stringLiteral(tag.content(), symbolProvider)
                    )
                )
            }


        fun PsiMethod.toJK(): JKJavaMethod {
            return JKJavaMethodImpl(
                with(expressionTreeMapper) {
                    returnTypeElement?.toJK()
                            ?: JKTypeElementImpl(JKJavaVoidType).takeIf { isConstructor }
                            ?: TODO()
                },
                JKNameIdentifierImpl(name),
                parameterList.parameters.map { it.toJK() },
                body?.toJK() ?: JKBodyStub,
                typeParameterList?.toJK() ?: JKTypeParameterListImpl(),
                annotationList(),
                throwsList.referencedTypes.map { JKTypeElementImpl(it.toJK(symbolProvider)) },
                modifiers(),
                visibility(),
                modality()
            ).also {
                it.psi = this
                symbolProvider.provideUniverseSymbol(this, it)
            }
        }

        fun PsiParameter.toJK(): JKParameter {
            val rawType = with(expressionTreeMapper) { typeElement?.toJK() } ?: JKTypeElementImpl(JKNoTypeImpl)
            val type =
                if (isVarArgs && rawType.type is JKJavaArrayType) JKTypeElementImpl((rawType.type as JKJavaArrayType).type)
                else rawType
            return JKParameterImpl(
                type,
                JKNameIdentifierImpl(name!!),
                isVarArgs
            ).also {
                symbolProvider.provideUniverseSymbol(this, it)
                it.psi = this
            }
        }

        fun PsiCodeBlock.toJK(): JKBlock {
            return JKBlockImpl(statements.map { it.toJK() })
        }

        fun PsiLocalVariable.toJK(): JKLocalVariable =
            JKLocalVariableImpl(
                with(expressionTreeMapper) { typeElement.toJK() },
                JKNameIdentifierImpl(this.name ?: TODO()),
                with(expressionTreeMapper) { initializer.toJK() },
                if (hasModifierProperty(PsiModifier.FINAL)) Mutability.IMMUTABLE else Mutability.UNKNOWN
            ).also { i ->
                symbolProvider.provideUniverseSymbol(this, i)
                i.psi = this
            }

        fun PsiStatement?.toJK(): JKStatement {
            return when (this) {
                null -> JKExpressionStatementImpl(JKStubExpressionImpl())
                is PsiExpressionStatement -> JKExpressionStatementImpl(with(expressionTreeMapper) { expression.toJK() })
                is PsiReturnStatement -> JKReturnStatementImpl(with(expressionTreeMapper) { returnValue.toJK() })
                is PsiDeclarationStatement ->
                    JKDeclarationStatementImpl(declaredElements.map {
                        when (it) {
                            is PsiClass -> it.toJK()
                            is PsiLocalVariable -> it.toJK()
                            else -> TODO(it::class.java.toString())
                        }
                    })
                is PsiAssertStatement ->
                    JKJavaAssertStatementImpl(
                        with(expressionTreeMapper) { assertCondition.toJK() },
                        with(expressionTreeMapper) { assertDescription?.toJK() } ?: JKStubExpressionImpl())
                is PsiIfStatement ->
                    if (elseElement == null)
                        JKIfStatementImpl(with(expressionTreeMapper) { condition.toJK() }, thenBranch.toJK())
                    else
                        JKIfElseStatementImpl(with(expressionTreeMapper) { condition.toJK() }, thenBranch.toJK(), elseBranch.toJK())

                is PsiForStatement -> JKJavaForLoopStatementImpl(
                    initialization.toJK(),
                    with(expressionTreeMapper) { condition.toJK() },
                    when (update) {
                        is PsiExpressionListStatement ->
                            (update as PsiExpressionListStatement).expressionList.expressions.map {
                                JKExpressionStatementImpl(with(expressionTreeMapper) { it.toJK() })
                            }
                        else -> listOf(update.toJK())
                    },
                    body.toJK()
                )
                is PsiForeachStatement ->
                    JKForInStatementImpl(
                        iterationParameter.toJK(),
                        with(expressionTreeMapper) { iteratedValue?.toJK() ?: JKStubExpressionImpl() },
                        body?.toJK() ?: blockStatement()
                    )
                is PsiBlockStatement -> JKBlockStatementImpl(codeBlock.toJK())
                is PsiWhileStatement -> JKWhileStatementImpl(with(expressionTreeMapper) { condition.toJK() }, body.toJK())
                is PsiDoWhileStatement -> JKDoWhileStatementImpl(body.toJK(), with(expressionTreeMapper) { condition.toJK() })

                is PsiSwitchStatement -> {
                    val cases = mutableListOf<JKJavaSwitchCase>()
                    for (statement in body?.statements.orEmpty()) {
                        when (statement) {
                            is PsiSwitchLabelStatement ->
                                cases += if (statement.isDefaultCase)
                                    JKJavaDefaultSwitchCaseImpl(emptyList())
                                else
                                    JKJavaLabelSwitchCaseImpl(
                                        with(expressionTreeMapper) { statement.caseValue.toJK() },
                                        emptyList()
                                    )
                            else ->
                                //TODO Handle case then there is no last case
                                cases.lastOrNull()?.also { it.statements = it.statements + statement.toJK() }
                        }
                    }
                    JKJavaSwitchStatementImpl(with(expressionTreeMapper) { expression.toJK() }, cases)
                }
                is PsiBreakStatement -> {
                    if (labelIdentifier != null)
                        JKBreakWithLabelStatementImpl(JKNameIdentifierImpl(labelIdentifier!!.text))
                    else
                        JKBreakStatementImpl()
                }
                is PsiContinueStatement -> {
                    val label = labelIdentifier?.let {
                        JKLabelTextImpl(JKNameIdentifierImpl(it.text))
                    } ?: JKLabelEmptyImpl()
                    JKContinueStatementImpl(label)
                }
                is PsiLabeledStatement -> {
                    val (labels, statement) = collectLabels()
                    JKLabeledStatementImpl(statement.toJK(), labels.map { JKNameIdentifierImpl(it.text) })
                }
                is PsiEmptyStatement -> JKEmptyStatementImpl()
                is PsiThrowStatement ->
                    JKJavaThrowStatementImpl(with(expressionTreeMapper) { exception.toJK() })
                is PsiTryStatement ->
                    JKJavaTryStatementImpl(
                        resourceList?.toList()?.map { (it as PsiLocalVariable).toJK() }.orEmpty(),
                        tryBlock?.toJK() ?: JKBodyStub,
                        finallyBlock?.toJK() ?: JKBodyStub,
                        catchSections.map { it.toJK() }
                    )
                else -> TODO("for ${this::class}")
            }.also {
                if (this != null) (it as PsiOwner).psi = this
            }
        }

        fun PsiCatchSection.toJK(): JKJavaTryCatchSection =
            JKJavaTryCatchSectionImpl(parameter?.toJK()!!, catchBlock?.toJK() ?: JKBodyStub)
                .also { it.psi = this }
    }

    //TODO better way than generateSequence.last??
    fun PsiLabeledStatement.collectLabels(): Pair<List<PsiIdentifier>, PsiStatement> =
        generateSequence(emptyList<PsiIdentifier>() to this as PsiStatement) { (labels, statement) ->
            if (statement !is PsiLabeledStatementImpl) return@generateSequence null
            (labels + statement.labelIdentifier) to statement.statement!!
        }.last()


    fun buildTree(psi: PsiElement): JKTreeElement? =
        when (psi) {
            is PsiJavaFile -> psi.toJK()
            else -> error("Cannot convert non-java file")
        }

}


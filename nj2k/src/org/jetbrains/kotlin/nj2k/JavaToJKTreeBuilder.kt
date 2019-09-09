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

package org.jetbrains.kotlin.nj2k

import com.intellij.lang.jvm.JvmModifier
import com.intellij.psi.*
import com.intellij.psi.JavaTokenType.SUPER_KEYWORD
import com.intellij.psi.JavaTokenType.THIS_KEYWORD
import com.intellij.psi.impl.source.tree.ChildRole
import com.intellij.psi.impl.source.tree.CompositeElement
import com.intellij.psi.impl.source.tree.java.PsiClassObjectAccessExpressionImpl
import com.intellij.psi.impl.source.tree.java.PsiLabeledStatementImpl
import com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl
import com.intellij.psi.impl.source.tree.java.PsiNewExpressionImpl
import com.intellij.psi.infos.MethodCandidateInfo
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.idea.caches.lightClasses.KtLightClassForDecompiledDeclaration
import org.jetbrains.kotlin.idea.j2k.IdeaDocCommentConverter
import org.jetbrains.kotlin.idea.j2k.content
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.j2k.ReferenceSearcher
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.nj2k.symbols.*
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.JKLiteralExpression.LiteralType.*
import org.jetbrains.kotlin.nj2k.types.*


import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


class JavaToJKTreeBuilder constructor(
    private val symbolProvider: JKSymbolProvider,
    private val typeFactory: JKTypeFactory,
    converterServices: NewJavaToKotlinServices,
    private val importStorage: ImportStorage
) {
    private fun PsiType?.toJK(): JKType {
        if (this == null) return JKNoType
        return typeFactory.fromPsiType(this)
    }

    private val expressionTreeMapper = ExpressionTreeMapper()

    private val referenceSearcher: ReferenceSearcher = converterServices.oldServices.referenceSearcher

    private val declarationMapper = DeclarationMapper(expressionTreeMapper)

    private fun PsiJavaFile.toJK(): JKFile =
        JKFile(
            packageStatement?.toJK() ?: JKPackageDeclaration(JKNameIdentifier("")),
            importList.toJK(),
            with(declarationMapper) { classes.map { it.toJK() } }
        )

    private fun PsiImportList?.toJK(): JKImportList =
        JKImportList(this?.allImportStatements?.mapNotNull { it.toJK() }.orEmpty())

    private fun PsiPackageStatement.toJK(): JKPackageDeclaration =
        JKPackageDeclaration(JKNameIdentifier(packageName))
            .also {
                it.assignNonCodeElements(this)
            }


    private fun PsiImportStatementBase.toJK(): JKImportStatement? {
        val target = resolve()
        val rawName = (importReference?.canonicalText ?: return null) + if (isOnDemand) ".*" else ""
        val name =
            target.safeAs<KtLightElement<*, *>>()?.kotlinOrigin?.getKotlinFqName()?.asString()
                ?: target.safeAs<KtLightClass>()?.containingFile?.safeAs<KtFile>()?.packageFqName?.asString()?.let { "$it.*" }
                ?: target.safeAs<KtLightClassForFacade>()?.fqName?.parent()?.asString()?.let { "$it.*" }
                ?: target.safeAs<KtLightClassForDecompiledDeclaration>()?.fqName?.parent()?.asString()?.let { "$it.*" }
                ?: rawName

        return JKImportStatement(JKNameIdentifier(name))
            .also {
                it.assignNonCodeElements(this)
            }
    }

    private fun PsiIdentifier?.toJK(): JKNameIdentifier =
        this?.let {
            JKNameIdentifier(it.text).also {
                it.assignNonCodeElements(this)
            }
        } ?: JKNameIdentifier("")


    private inner class ExpressionTreeMapper {
        fun PsiExpression?.toJK(): JKExpression {
            return when (this) {
                null -> JKStubExpression()
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
                    JKThisExpression(
                        qualifier?.referenceName?.let { JKLabelText(JKNameIdentifier(it)) } ?: JKLabelEmpty(),
                        type.toJK()
                    )
                is PsiSuperExpression ->
                    JKSuperExpression(
                        qualifier?.referenceName?.let { JKLabelText(JKNameIdentifier(it)) } ?: JKLabelEmpty(),
                        type.toJK()
                    )
                is PsiConditionalExpression -> JKIfElseExpression(
                    condition.toJK(),
                    thenExpression.toJK(),
                    elseExpression.toJK(),
                    type.toJK()
                )
                is PsiPolyadicExpression -> {
                    val token = JKOperatorToken.fromElementType(operationTokenType)
                    val type = type?.toJK() ?: typeFactory.types.nullableAny
                    val jkOperands = operands.map { it.toJK().parenthesizeIfBinaryExpression() }
                    jkOperands.reduce { acc, operand ->
                        JKBinaryExpression(acc, operand, JKKtOperatorImpl(token, type))
                    }.let { folded ->
                        if (jkOperands.any { it.containsNewLine() }) folded.parenthesize()
                        else folded
                    }
                }
                is PsiArrayInitializerExpression -> toJK()
                is PsiLambdaExpression -> toJK()
                is PsiClassObjectAccessExpressionImpl -> toJK()
                else -> throwCanNotConvertError()
            }.also {
                if (this != null) {
                    (it as PsiOwner).psi = this
                    it.assignNonCodeElements(this)
                }
            }
        }

        fun PsiClassObjectAccessExpressionImpl.toJK(): JKClassLiteralExpression {
            val type = operand.type.toJK().updateNullabilityRecursively(Nullability.NotNull)
            return JKClassLiteralExpression(
                JKTypeElement(type),
                when (type) {
                    is JKJavaPrimitiveType -> JKClassLiteralExpression.ClassLiteralType.JAVA_PRIMITIVE_CLASS
                    is JKJavaVoidType -> JKClassLiteralExpression.ClassLiteralType.JAVA_VOID_TYPE
                    else -> JKClassLiteralExpression.ClassLiteralType.JAVA_CLASS
                }
            ).also {
                it.assignNonCodeElements(this)
            }
        }

        fun PsiInstanceOfExpression.toJK(): JKIsExpression =
            JKIsExpression(operand.toJK(), JKTypeElement(checkType?.type?.toJK() ?: JKNoType))
                .also {
                    it.assignNonCodeElements(this)
                }

        fun PsiAssignmentExpression.toJK(): JKJavaAssignmentExpression {
            return JKJavaAssignmentExpression(
                lExpression.toJK(),
                rExpression.toJK(),
                createOperator(operationSign.tokenType, type)
            ).also {
                it.assignNonCodeElements(this)
            }
        }

        fun PsiBinaryExpression.toJK(): JKExpression {
            val token = when (operationSign.tokenType) {
                JavaTokenType.EQEQ, JavaTokenType.NE ->
                    when {
                        canKeepEqEq(lOperand, rOperand) -> JKOperatorToken.fromElementType(operationSign.tokenType)
                        operationSign.tokenType == JavaTokenType.EQEQ -> JKOperatorToken.fromElementType(KtTokens.EQEQEQ)
                        else -> JKOperatorToken.fromElementType(KtTokens.EXCLEQEQEQ)
                    }
                else -> JKOperatorToken.fromElementType(operationSign.tokenType)
            }
            return JKBinaryExpression(
                lOperand.toJK(),
                rOperand.toJK(),
                JKKtOperatorImpl(
                    token,
                    type?.toJK() ?: typeFactory.types.nullableAny
                )
            ).also {
                it.assignNonCodeElements(this)
            }
        }

        fun PsiLiteralExpression.toJK(): JKLiteralExpression {
            require(this is PsiLiteralExpressionImpl)

            return when (literalElementType) {
                JavaTokenType.NULL_KEYWORD -> JKLiteralExpression("null", NULL)
                JavaTokenType.TRUE_KEYWORD -> JKLiteralExpression("true", BOOLEAN)
                JavaTokenType.FALSE_KEYWORD -> JKLiteralExpression("false", BOOLEAN)
                JavaTokenType.STRING_LITERAL -> JKLiteralExpression(text, STRING)
                JavaTokenType.CHARACTER_LITERAL -> JKLiteralExpression(text, CHAR)
                JavaTokenType.INTEGER_LITERAL -> JKLiteralExpression(text, INT)
                JavaTokenType.LONG_LITERAL -> JKLiteralExpression(text, LONG)
                JavaTokenType.FLOAT_LITERAL -> JKLiteralExpression(text, FLOAT)
                JavaTokenType.DOUBLE_LITERAL -> JKLiteralExpression(text, DOUBLE)
                else -> error("Unknown literal element type: ${this.literalElementType}")
            }.also {
                it.assignNonCodeElements(this)
            }
        }

        private fun createOperator(elementType: IElementType, type: PsiType?) =
            JKKtOperatorImpl(
                JKOperatorToken.fromElementType(elementType),
                type?.toJK() ?: typeFactory.types.nullableAny
            )

        fun PsiPrefixExpression.toJK(): JKExpression = when (operationSign.tokenType) {
            JavaTokenType.TILDE -> operand.toJK().callOn(symbolProvider.provideMethodSymbol("kotlin.Int.inv"))
            else -> JKPrefixExpression(operand.toJK(), createOperator(operationSign.tokenType, type))
        }.also {
            it.assignNonCodeElements(this)
        }

        fun PsiPostfixExpression.toJK(): JKExpression =
            JKPostfixExpression(operand.toJK(), createOperator(operationSign.tokenType, type)).also {
                it.assignNonCodeElements(this)
            }

        fun PsiLambdaExpression.toJK(): JKExpression {
            return JKLambdaExpression(
                body.let {
                    when (it) {
                        is PsiExpression -> JKExpressionStatement(it.toJK())
                        is PsiCodeBlock -> JKBlockStatement(with(declarationMapper) { it.toJK() })
                        else -> JKBlockStatement(JKBodyStub)
                    }
                },
                with(declarationMapper) { parameterList.parameters.map { it.toJK() } },
                functionalType()
            ).also {
                it.assignNonCodeElements(this)
            }
        }

        private fun PsiMethodCallExpression.getExplicitTypeArguments(): PsiReferenceParameterList {
            if (typeArguments.isNotEmpty()) return typeArgumentList

            val resolveResult = resolveMethodGenerics()
            if (resolveResult is MethodCandidateInfo && resolveResult.isApplicable) {
                val method = resolveResult.element
                if (method.isConstructor || !method.hasTypeParameters()) return typeArgumentList
            }

            return FixTypeArguments.addTypeArguments(this, null)
                ?.safeAs<PsiMethodCallExpression>()
                ?.typeArgumentList
                ?: typeArgumentList
        }

        //TODO mostly copied from old j2k, refactor
        fun PsiMethodCallExpression.toJK(): JKExpression {
            val arguments = argumentList
            val typeArguments = getExplicitTypeArguments().toJK()
            val qualifier = methodExpression.qualifierExpression?.toJK()
            val target = methodExpression.resolve()
            val symbol = target?.let {
                symbolProvider.provideDirectSymbol(it)
            } ?: JKUnresolvedMethod(methodExpression, typeFactory)

            return when {
                methodExpression.referenceNameElement is PsiKeyword -> {
                    val callee = when ((methodExpression.referenceNameElement as PsiKeyword).tokenType) {
                        SUPER_KEYWORD -> JKSuperExpression(JKLabelEmpty(), JKNoType)
                        THIS_KEYWORD -> JKThisExpression(JKLabelEmpty(), JKNoType)
                        else -> throwCanNotConvertError("unknown keyword in callee position")
                    }
                    JKDelegationConstructorCall(symbol as JKMethodSymbol, callee, arguments.toJK())
                }

                target is KtLightMethod -> {
                    val origin = target.kotlinOrigin
                    when (origin) {
                        is KtNamedFunction -> {
                            if (origin.isExtensionDeclaration()) {
                                val receiver = arguments.expressions.firstOrNull()?.toJK()?.parenthesizeIfBinaryExpression()
                                origin.fqName?.also { importStorage.addImport(it) }
                                JKCallExpressionImpl(
                                    symbolProvider.provideDirectSymbol(origin) as JKMethodSymbol,
                                    arguments.expressions.drop(1).map { it.toJK() }.toArgumentList(),
                                    typeArguments
                                ).qualified(receiver)
                            } else {
                                origin.fqName?.also { importStorage.addImport(it) }
                                JKCallExpressionImpl(
                                    symbolProvider.provideDirectSymbol(origin) as JKMethodSymbol,
                                    arguments.toJK(),
                                    typeArguments
                                ).qualified(qualifier)
                            }
                        }
                        is KtProperty, is KtPropertyAccessor, is KtParameter -> {
                            origin.getKotlinFqName()?.also { importStorage.addImport(it) }
                            val property =
                                if (origin is KtPropertyAccessor) origin.parent as KtProperty
                                else origin as KtNamedDeclaration
                            val parameterCount = target.parameterList.parameters.size
                            val propertyAccessExpression =
                                JKFieldAccessExpression(symbolProvider.provideDirectSymbol(property) as JKFieldSymbol)
                            val isExtension = property.isExtensionDeclaration()
                            val isTopLevel = origin.getStrictParentOfType<KtClassOrObject>() == null
                            val propertyAccess = if (isTopLevel) {
                                if (isExtension) JKQualifiedExpression(
                                    arguments.expressions.first().toJK(),
                                    propertyAccessExpression
                                )
                                else propertyAccessExpression
                            } else propertyAccessExpression.qualified(qualifier)

                            when (if (isExtension) parameterCount - 1 else parameterCount) {
                                0 /* getter */ ->
                                    propertyAccess

                                1 /* setter */ -> {
                                    val argument = (arguments.expressions[if (isExtension) 1 else 0]).toJK()
                                    JKJavaAssignmentExpression(
                                        propertyAccess,
                                        argument,
                                        createOperator(JavaTokenType.EQ, type)//TODO correct type
                                    )
                                }
                                else -> throwCanNotConvertError("expected getter or setter call")
                            }
                        }

                        else -> {
                            JKCallExpressionImpl(
                                JKMultiverseMethodSymbol(target, typeFactory),
                                arguments.toJK(),
                                typeArguments
                            ).qualified(qualifier)
                        }
                    }
                }

                symbol is JKMethodSymbol ->
                    JKCallExpressionImpl(symbol, arguments.toJK(), typeArguments)
                        .qualified(qualifier)
                symbol is JKFieldSymbol ->
                    JKFieldAccessExpression(symbol).qualified(qualifier)
                else -> throwCanNotConvertError("unexpected symbol ${symbol::class}")
            }.also {
                it.assignNonCodeElements(this)
            }
        }

        fun PsiFunctionalExpression.functionalType(): JKTypeElement =
            functionalInterfaceType
                ?.takeUnless { type ->
                    type.safeAs<PsiClassType>()?.parameters?.any { it is PsiCapturedWildcardType } == true
                }?.takeUnless { type ->
                    type.isKotlinFunctionalType
                }?.toJK()
                ?.asTypeElement() ?: JKTypeElement(JKNoType)

        fun PsiMethodReferenceExpression.toJK(): JKMethodReferenceExpression {
            val symbol = symbolProvider.provideSymbolForReference<JKSymbol>(this).let { symbol ->
                when {
                    symbol.isUnresolved && isConstructor -> JKUnresolvedClassSymbol(qualifier?.text ?: text, typeFactory)
                    symbol.isUnresolved && !isConstructor -> JKUnresolvedMethod(referenceName ?: text, typeFactory)
                    else -> symbol
                }
            }

            return JKMethodReferenceExpression(
                qualifierExpression?.toJK() ?: JKStubExpression(),
                symbol,
                functionalType(),
                isConstructor
            )
        }

        fun PsiReferenceExpression.toJK(): JKExpression {
            if (this is PsiMethodReferenceExpression) return toJK()
            val target = resolve()
            if (target is KtLightClassForFacade
                || target is KtLightClassForDecompiledDeclaration
            ) return JKStubExpression()
            if (target is KtLightField
                && target.name == "INSTANCE"
                && target.containingClass.kotlinOrigin is KtObjectDeclaration
            ) {
                return qualifierExpression?.toJK() ?: JKStubExpression()
            }

            val symbol = symbolProvider.provideSymbolForReference<JKSymbol>(this)
            return when (symbol) {
                is JKClassSymbol -> JKClassAccessExpression(symbol)
                is JKFieldSymbol -> JKFieldAccessExpression(symbol)
                is JKPackageSymbol -> JKPackageAccessExpression(symbol)
                else -> throwCanNotConvertError("unexpected symbol ${symbol::class}")
            }.qualified(qualifierExpression?.toJK()).also {
                it.assignNonCodeElements(this)
            }
        }

        fun PsiArrayInitializerExpression.toJK(): JKExpression {
            return JKJavaNewArray(
                initializers.map { it.toJK() },
                JKTypeElement(type?.toJK().safeAs<JKJavaArrayType>()?.type ?: JKContextType)
            ).also {
                it.assignNonCodeElements(this)
            }
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
                        JKJavaNewEmptyArray(
                            dimensions.map { it?.toJK() ?: JKStubExpression() },
                            JKTypeElement(generateSequence(type?.toJK()) { it.safeAs<JKJavaArrayType>()?.type }.last())
                        ).also {
                            it.psi = this
                        }
                    }
                } else {
                    val classSymbol =
                        classOrAnonymousClassReference?.resolve()?.let {
                            symbolProvider.provideDirectSymbol(it) as JKClassSymbol
                        } ?: JKUnresolvedClassSymbol(
                            classOrAnonymousClassReference?.referenceName ?: throwCanNotConvertError(),
                            typeFactory
                        )
                    val typeArgumentList =
                        this.typeArgumentList.toJK()
                            .takeIf { it.typeArguments.isNotEmpty() }
                            ?: classOrAnonymousClassReference
                                ?.typeParameters
                                ?.let { typeParameters ->
                                    JKTypeArgumentList(typeParameters.map { JKTypeElement(it.toJK()) })
                                } ?: JKTypeArgumentList()
                    JKNewExpression(
                        classSymbol,
                        argumentList?.toJK() ?: JKArgumentList(),
                        typeArgumentList,
                        with(declarationMapper) { anonymousClass?.createClassBody() } ?: JKClassBody(),
                        anonymousClass != null
                    )
                }
            return newExpression.qualified(qualifier?.toJK())
        }

        fun PsiReferenceParameterList.toJK(): JKTypeArgumentList =
            JKTypeArgumentList(typeArguments.map { JKTypeElement(it.toJK()) })
                .also {
                    it.assignNonCodeElements(this)
                }


        fun PsiArrayAccessExpression.toJK(): JKExpression =
            arrayExpression.toJK()
                .callOn(
                    symbolProvider.provideMethodSymbol("kotlin.Array.get"),
                    arguments = listOf(indexExpression?.toJK() ?: JKStubExpression())
                ).also {
                    it.assignNonCodeElements(this)
                }


        fun PsiTypeCastExpression.toJK(): JKExpression {
            return JKTypeCastExpression(
                operand?.toJK() ?: throwCanNotConvertError(),
                castType?.type?.toJK()?.asTypeElement() ?: throwCanNotConvertError()
            ).also {
                it.assignNonCodeElements(this)
            }
        }

        fun PsiParenthesizedExpression.toJK(): JKExpression {
            return JKParenthesizedExpression(expression.toJK())
                .also {
                    it.assignNonCodeElements(this)
                }
        }

        fun PsiExpressionList.toJK(): JKArgumentList {
            val jkExpressions = expressions.map { it.toJK() }
            return ((parent as? PsiCall)?.resolveMethod()
                ?.let { method ->
                    val lastExpressionType = expressions.lastOrNull()?.type
                    if (jkExpressions.size == method.parameterList.parameters.size
                        && method.parameterList.parameters.getOrNull(jkExpressions.lastIndex)?.isVarArgs == true
                        && lastExpressionType is PsiArrayType
                    ) {
                        val staredExpression =
                            JKPrefixExpression(
                                jkExpressions.last(),
                                JKKtSpreadOperator(lastExpressionType.toJK())
                            ).withNonCodeElementsFrom(jkExpressions.last())
                        jkExpressions.dropLast(1) + staredExpression
                    } else jkExpressions
                } ?: jkExpressions)
                .toArgumentList()
                .also {
                    it.assignNonCodeElements(this)
                }
        }

    }

    private inner class DeclarationMapper(val expressionTreeMapper: ExpressionTreeMapper) {

        fun PsiTypeParameterList.toJK(): JKTypeParameterList =
            JKTypeParameterList(typeParameters.map { it.toJK() })
                .also {
                    it.assignNonCodeElements(this)
                }

        fun PsiTypeParameter.toJK(): JKTypeParameter =
            JKTypeParameter(
                nameIdentifier.toJK(),
                extendsListTypes.map { JKTypeElement(it.toJK()) }
            ).also {
                symbolProvider.provideUniverseSymbol(this, it)
                it.assignNonCodeElements(this)
            }

        fun PsiClass.toJK(): JKClass =
            JKClass(
                nameIdentifier.toJK(),
                inheritanceInfo(),
                classKind(),
                typeParameterList?.toJK() ?: JKTypeParameterList(),
                createClassBody(),
                annotationList(this),
                otherModifiers(),
                visibility(),
                modality()
            ).also { klass ->
                klass.psi = this
                symbolProvider.provideUniverseSymbol(this, klass)
                klass.assignNonCodeElements(this)
            }


        fun PsiClass.inheritanceInfo(): JKInheritanceInfo {
            val implTypes = implementsList?.referencedTypes?.map { JKTypeElement(it.toJK()) }.orEmpty()
            val extensionType = extendsList?.referencedTypes?.map { JKTypeElement(it.toJK()) }.orEmpty()
            return JKInheritanceInfo(extensionType, implTypes)
                .also {
                    if (implementsList != null) {
                        it.assignNonCodeElements(implementsList!!)
                    }
                }
        }

        fun PsiClass.createClassBody() =
            JKClassBody(
                children.mapNotNull {
                    when (it) {
                        is PsiEnumConstant -> it.toJK()
                        is PsiClass -> it.toJK()
                        is PsiAnnotationMethod -> it.toJK()
                        is PsiMethod -> it.toJK()
                        is PsiField -> it.toJK()
                        is PsiClassInitializer -> it.toJK()
                        else -> null
                    }
                }
            ).also {
                it.leftBrace.assignNonCodeElements(lBrace)
                it.rightBrace.assignNonCodeElements(rBrace)
            }

        fun PsiClassInitializer.toJK(): JKDeclaration = when {
            hasModifier(JvmModifier.STATIC) -> JKJavaStaticInitDeclaration(body.toJK())
            else -> JKKtInitDeclaration(body.toJK())
        }.also {
            it.assignNonCodeElements(this)
        }


        fun PsiEnumConstant.toJK(): JKEnumConstant =
            JKEnumConstant(
                nameIdentifier.toJK(),
                with(expressionTreeMapper) { argumentList?.toJK() ?: JKArgumentList() },
                initializingClass?.createClassBody() ?: JKClassBody(),
                JKTypeElement(
                    JKClassType(
                        symbolProvider.provideDirectSymbol(containingClass ?: throwCanNotConvertError()) as JKClassSymbol,
                        emptyList()
                    )
                )
            ).also {
                symbolProvider.provideUniverseSymbol(this, it)
                it.psi = this
                it.assignNonCodeElements(this)
            }


        fun PsiMember.modality() =
            modality { ast, psi -> ast.assignNonCodeElements(psi) }

        fun PsiMember.otherModifiers() =
            modifierList?.children?.mapNotNull { child ->
                if (child !is PsiKeyword) return@mapNotNull null
                when (child.text) {
                    PsiModifier.NATIVE -> OtherModifier.NATIVE
                    PsiModifier.STATIC -> OtherModifier.STATIC
                    PsiModifier.STRICTFP -> OtherModifier.STRICTFP
                    PsiModifier.SYNCHRONIZED -> OtherModifier.SYNCHRONIZED
                    PsiModifier.TRANSIENT -> OtherModifier.TRANSIENT
                    PsiModifier.VOLATILE -> OtherModifier.VOLATILE

                    else -> null
                }?.let {
                    JKOtherModifierElement(it).withAssignedNonCodeElements(child)
                }
            }.orEmpty()


        private fun PsiMember.visibility(): JKVisibilityModifierElement =
            visibility(referenceSearcher) { ast, psi -> ast.assignNonCodeElements(psi) }

        fun PsiField.toJK(): JKField {
            return JKField(
                JKTypeElement(type.toJK()).withAssignedNonCodeElements(typeElement),
                nameIdentifier.toJK(),
                with(expressionTreeMapper) { initializer.toJK() },
                annotationList(this),
                otherModifiers(),
                visibility(),
                modality(),
                JKMutabilityModifierElement(Mutability.UNKNOWN)
            ).also {
                symbolProvider.provideUniverseSymbol(this, it)
                it.psi = this
                it.assignNonCodeElements(this)
            }
        }

        fun <T : PsiModifierListOwner> T.annotationList(docCommentOwner: PsiDocCommentOwner?): JKAnnotationList {
            val plainAnnotations = annotations.map { it.cast<PsiAnnotation>().toJK() }
            val deprecatedAnnotation = docCommentOwner?.docComment?.deprecatedAnnotation() ?: return JKAnnotationList(plainAnnotations)
            return JKAnnotationList(
                plainAnnotations.mapNotNull { annotation ->
                    if (annotation.classSymbol.fqName == "java.lang.Deprecated") null else annotation
                } + deprecatedAnnotation
            )
        }

        fun PsiAnnotation.toJK(): JKAnnotation =
            JKAnnotation(
                symbolProvider.provideSymbolForReference<JKSymbol>(
                    nameReferenceElement ?: throwCanNotConvertError()
                ).safeAs<JKClassSymbol>()
                    ?: JKUnresolvedClassSymbol(nameReferenceElement?.text ?: throwCanNotConvertError(), typeFactory),
                parameterList.attributes.map { parameter ->
                    if (parameter.nameIdentifier != null) {
                        JKAnnotationNameParameter(
                            parameter.value?.toJK() ?: JKStubExpression(),
                            JKNameIdentifier(parameter.name ?: throwCanNotConvertError())
                        )
                    } else {
                        JKAnnotationParameterImpl(
                            parameter.value?.toJK() ?: JKStubExpression()
                        )
                    }
                }
            ).also {
                it.assignNonCodeElements(this)
            }

        fun PsiDocComment.deprecatedAnnotation(): JKAnnotation? =
            findTagByName("deprecated")?.let { tag ->
                JKAnnotation(
                    symbolProvider.provideClassSymbol("kotlin.Deprecated"),
                    listOf(
                        JKAnnotationParameterImpl(stringLiteral(tag.content(), typeFactory))
                    )
                )
            }

        private fun PsiAnnotationMemberValue.toJK(): JKAnnotationMemberValue =
            when (this) {
                is PsiExpression -> with(expressionTreeMapper) { toJK() }
                is PsiAnnotation -> toJK()
                is PsiArrayInitializerMemberValue ->
                    JKKtAnnotationArrayInitializerExpression(initializers.map { it.toJK() })
                else -> throwCanNotConvertError()
            }.also {
                it.assignNonCodeElements(this)
            }

        fun PsiAnnotationMethod.toJK(): JKJavaAnnotationMethod =
            JKJavaAnnotationMethod(
                returnType?.toJK()?.asTypeElement()
                    ?: JKTypeElement(JKJavaVoidType).takeIf { isConstructor }
                    ?: throwCanNotConvertError("type of PsiAnnotationMethod can not be retrieved"),
                nameIdentifier.toJK(),
                defaultValue?.toJK() ?: JKStubExpression(),
                otherModifiers(),
                visibility(),
                modality()
            ).also {
                it.psi = this
                symbolProvider.provideUniverseSymbol(this, it)
                it.assignNonCodeElements(this)
            }


        fun PsiMethod.toJK(): JKMethod {
            return JKMethodImpl(
                returnType?.toJK()?.asTypeElement()
                    ?: JKTypeElement(JKJavaVoidType).takeIf { isConstructor }
                    ?: throwCanNotConvertError("type of PsiAnnotationMethod can not be retrieved"),
                nameIdentifier.toJK(),
                parameterList.parameters.map { it.toJK() },
                body?.toJK() ?: JKBodyStub,
                typeParameterList?.toJK() ?: JKTypeParameterList(),
                annotationList(this),
                throwsList.referencedTypes.map { JKTypeElement(it.toJK()) },
                otherModifiers(),
                visibility(),
                modality()
            ).also { jkMethod ->
                jkMethod.psi = this
                symbolProvider.provideUniverseSymbol(this, jkMethod)
                parameterList.node
                    ?.safeAs<CompositeElement>()
                    ?.also {
                        jkMethod.leftParen.assignNonCodeElements(it.findChildByRoleAsPsiElement(ChildRole.LPARENTH))
                        jkMethod.rightParen.assignNonCodeElements(it.findChildByRoleAsPsiElement(ChildRole.RPARENTH))
                    }
            }.withAssignedNonCodeElements(this)
        }

        fun PsiParameter.toJK(): JKParameter {
            val rawType = type.toJK()
            val type =
                if (isVarArgs && rawType is JKJavaArrayType) JKTypeElement(rawType.type)
                else rawType.asTypeElement()
            return JKParameter(
                type,
                nameIdentifier.toJK(),
                isVarArgs,
                annotationList = annotationList(null)
            ).also {
                symbolProvider.provideUniverseSymbol(this, it)
                it.psi = this
                it.assignNonCodeElements(this)
            }
        }

        fun PsiCodeBlock.toJK(): JKBlock =
            JKBlockImpl(statements.map { it.toJK() })
                .withAssignedNonCodeElements(this)
                .also {
                    it.leftBrace.assignNonCodeElements(lBrace)
                    it.rightBrace.assignNonCodeElements(rBrace)
                }


        fun PsiLocalVariable.toJK(): JKLocalVariable =
            JKLocalVariable(
                JKTypeElement(type.toJK()).withAssignedNonCodeElements(typeElement),
                nameIdentifier.toJK(),
                with(expressionTreeMapper) { initializer.toJK() },
                JKMutabilityModifierElement(
                    if (hasModifierProperty(PsiModifier.FINAL)) Mutability.IMMUTABLE
                    else Mutability.UNKNOWN
                ),
                annotationList(null)
            ).also { i ->
                symbolProvider.provideUniverseSymbol(this, i)
                i.psi = this
            }.also {
                it.assignNonCodeElements(this)
            }

        fun PsiStatement?.toJK(): JKStatement {
            return when (this) {
                null -> JKExpressionStatement(JKStubExpression())
                is PsiExpressionStatement -> JKExpressionStatement(with(expressionTreeMapper) { expression.toJK() })
                is PsiReturnStatement -> JKReturnStatement(with(expressionTreeMapper) { returnValue.toJK() })
                is PsiDeclarationStatement ->
                    JKDeclarationStatement(declaredElements.map {
                        when (it) {
                            is PsiClass -> it.toJK()
                            is PsiLocalVariable -> it.toJK()
                            else -> it.throwCanNotConvertError()
                        }
                    })
                is PsiAssertStatement ->
                    JKJavaAssertStatement(
                        with(expressionTreeMapper) { assertCondition.toJK() },
                        with(expressionTreeMapper) { assertDescription?.toJK() } ?: JKStubExpression())
                is PsiIfStatement ->
                    with(expressionTreeMapper) {
                        JKIfElseStatement(condition.toJK(), thenBranch.toJK(), elseBranch.toJK())
                    }


                is PsiForStatement -> JKJavaForLoopStatement(
                    initialization.toJK(),
                    with(expressionTreeMapper) { condition.toJK() },
                    when (update) {
                        is PsiExpressionListStatement ->
                            (update as PsiExpressionListStatement).expressionList.expressions.map {
                                JKExpressionStatement(with(expressionTreeMapper) { it.toJK() })
                            }
                        else -> listOf(update.toJK())
                    },
                    body.toJK()
                )
                is PsiForeachStatement ->
                    JKForInStatement(
                        iterationParameter.toJK(),
                        with(expressionTreeMapper) { iteratedValue?.toJK() ?: JKStubExpression() },
                        body?.toJK() ?: blockStatement()
                    )
                is PsiBlockStatement -> JKBlockStatement(codeBlock.toJK())
                is PsiWhileStatement -> JKWhileStatement(with(expressionTreeMapper) { condition.toJK() }, body.toJK())
                is PsiDoWhileStatement -> JKDoWhileStatement(body.toJK(), with(expressionTreeMapper) { condition.toJK() })

                is PsiSwitchStatement -> {
                    val cases = mutableListOf<JKJavaSwitchCase>()
                    for (statement in body?.statements.orEmpty()) {
                        when (statement) {
                            is PsiSwitchLabelStatement ->
                                cases += when {
                                    statement.isDefaultCase -> JKJavaDefaultSwitchCase(emptyList())
                                    else -> JKJavaLabelSwitchCase(
                                        with(expressionTreeMapper) { statement.caseValue.toJK() },
                                        emptyList()
                                    )
                                }.withAssignedNonCodeElements(statement)
                            else ->
                                cases.lastOrNull()?.also { it.statements = it.statements + statement.toJK() }
                                    ?: run {
                                        cases += JKJavaLabelSwitchCase(
                                            JKStubExpression(),
                                            listOf(statement.toJK())
                                        )
                                    }
                        }
                    }
                    JKJavaSwitchStatement(with(expressionTreeMapper) { expression.toJK() }, cases)
                }
                is PsiBreakStatement ->
                    JKBreakStatement(labelIdentifier?.let { JKLabelText(JKNameIdentifier(it.text)) } ?: JKLabelEmpty())
                is PsiContinueStatement -> {
                    val label = labelIdentifier?.let {
                        JKLabelText(JKNameIdentifier(it.text))
                    } ?: JKLabelEmpty()
                    JKContinueStatement(label)
                }
                is PsiLabeledStatement -> {
                    val (labels, statement) = collectLabels()
                    JKLabeledExpression(statement.toJK(), labels.map { JKNameIdentifier(it.text) }).asStatement()
                }
                is PsiEmptyStatement -> JKEmptyStatement()
                is PsiThrowStatement ->
                    JKJavaThrowStatement(with(expressionTreeMapper) { exception.toJK() })
                is PsiTryStatement ->
                    JKJavaTryStatement(
                        resourceList?.toList()?.map { (it as PsiLocalVariable).toJK() }.orEmpty(),
                        tryBlock?.toJK() ?: JKBodyStub,
                        finallyBlock?.toJK() ?: JKBodyStub,
                        catchSections.map { it.toJK() }
                    )
                is PsiSynchronizedStatement ->
                    JKJavaSynchronizedStatement(
                        with(expressionTreeMapper) { lockExpression?.toJK() } ?: JKStubExpression(),
                        body?.toJK() ?: JKBodyStub
                    )
                else -> throwCanNotConvertError()
            }.also {
                if (this != null) {
                    (it as PsiOwner).psi = this
                    it.assignNonCodeElements(this)
                }
            }
        }

        fun PsiCatchSection.toJK(): JKJavaTryCatchSection =
            JKJavaTryCatchSection(
                parameter?.toJK() ?: throwCanNotConvertError(),
                catchBlock?.toJK() ?: JKBodyStub
            ).also {
                it.psi = this
                it.assignNonCodeElements(this)
            }
    }

    fun PsiLabeledStatement.collectLabels(): Pair<List<PsiIdentifier>, PsiStatement> =
        generateSequence(emptyList<PsiIdentifier>() to this as PsiStatement) { (labels, statement) ->
            if (statement !is PsiLabeledStatementImpl) return@generateSequence null
            (labels + statement.labelIdentifier) to (statement.statement ?: throwCanNotConvertError())
        }.last()


    fun buildTree(psi: PsiElement): JKTreeRoot? =
        when (psi) {
            is PsiJavaFile -> psi.toJK()
            is PsiExpression -> with(expressionTreeMapper) { psi.toJK() }
            is PsiStatement -> with(declarationMapper) { psi.toJK() }
            is PsiClass -> with(declarationMapper) { psi.toJK() }
            is PsiField -> with(declarationMapper) { psi.toJK() }
            is PsiMethod -> with(declarationMapper) { psi.toJK() }
            is PsiAnnotation -> with(declarationMapper) { psi.toJK() }
            is PsiImportList -> psi.toJK()
            is PsiImportStatementBase -> psi.toJK()
            is PsiJavaCodeReferenceElement ->
                if (psi.parent is PsiReferenceList) {
                    val factory = JavaPsiFacade.getInstance(psi.project).elementFactory
                    val type = factory.createType(psi)
                    JKTypeElement(type.toJK().updateNullabilityRecursively(Nullability.NotNull))
                } else null
            else -> null
        }?.let { JKTreeRoot(it) }

    private val tokenCache = mutableMapOf<PsiElement, JKNonCodeElement>()

    private fun PsiElement.collectNonCodeElements(): Pair<List<JKNonCodeElement>, List<JKNonCodeElement>> {
        fun PsiElement.toToken(): JKNonCodeElement? {
            if (this in tokenCache) return tokenCache.getValue(this)
            val token = when {
                this is PsiDocComment ->
                    JKCommentElement(IdeaDocCommentConverter.convertDocComment(this))
                this is PsiComment -> JKCommentElement(text)
                this is PsiWhiteSpace -> JKSpaceElement(text)
                text == ";" -> null
                text == "" -> null
                else -> error("Token should be either token or whitespace")
            } ?: return null
            tokenCache[this] = token
            return token
        }

        fun Sequence<PsiElement>.toNonCodeElements(): List<JKNonCodeElement> =
            takeWhile { it is PsiComment || it is PsiWhiteSpace || it.text == ";" }
                .mapNotNull { it.toToken() }
                .toList()

        fun PsiElement.isNonCodeElement() =
            this is PsiComment || this is PsiWhiteSpace || text == ";" || text == ""

        fun PsiElement.nextNonCodeElements() =
            generateSequence(nextSibling) { it.nextSibling }
                .takeWhile { it.isNonCodeElement() }

        fun PsiElement.prevNonCodeElements() =
            generateSequence(prevSibling) { it.prevSibling }
                .takeWhile { it.isNonCodeElement() }


        fun PsiElement.nextNonCodeElementsWithParent(): Sequence<JKNonCodeElement> {
            val innerElements = nextNonCodeElements()
            return (if (innerElements.lastOrNull()?.nextSibling == null && this is PsiKeyword)
                innerElements + parent?.nextNonCodeElements().orEmpty()
            else innerElements).mapNotNull { it.toToken() }
        }

        fun PsiElement.prevNonCodeElementsWithParent(): Sequence<JKNonCodeElement> {
            val innerElements = prevNonCodeElements()
            return (if (innerElements.firstOrNull()?.prevSibling == null && this is PsiKeyword)
                innerElements + parent?.prevNonCodeElements().orEmpty()
            else innerElements).mapNotNull { it.toToken() }
        }


        val leftInnerTokens = children.asSequence().toNonCodeElements().reversed()
        val rightInnerTokens =
            if (children.isEmpty()) emptyList()
            else generateSequence(children.last()) { it.prevSibling }
                .toNonCodeElements()
                .reversed()


        return (leftInnerTokens + prevNonCodeElementsWithParent()).reversed() to
                (rightInnerTokens + nextNonCodeElementsWithParent())
    }

    private fun JKNonCodeElementsListOwner.assignNonCodeElements(psi: PsiElement?) {
        if (psi == null) return
        val (leftTokens, rightTokens) = psi.collectNonCodeElements()
        this.leftNonCodeElements += leftTokens
        this.rightNonCodeElements += rightTokens
    }

    private inline fun <reified T : JKNonCodeElementsListOwner> T.withAssignedNonCodeElements(psi: PsiElement?): T =
        also { it.assignNonCodeElements(psi) }

    private fun PsiElement.throwCanNotConvertError(message: String? = null): Nothing {
        error("Cannot convert the following Java element ${this::class} with text `$text`" + message?.let { " due to `$it`" }.orEmpty())
    }

}


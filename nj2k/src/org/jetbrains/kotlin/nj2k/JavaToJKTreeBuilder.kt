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
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.JKLiteralExpression.LiteralType.*
import org.jetbrains.kotlin.nj2k.tree.impl.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


class JavaToJKTreeBuilder constructor(
    var symbolProvider: JKSymbolProvider,
    converterServices: NewJavaToKotlinServices,
    private val importStorage: ImportStorage
) {

    private val expressionTreeMapper = ExpressionTreeMapper()

    private val referenceSearcher: ReferenceSearcher = converterServices.oldServices.referenceSearcher

    private val declarationMapper = DeclarationMapper(expressionTreeMapper)

    private fun PsiJavaFile.toJK(): JKFile =
        JKFileImpl(
            packageStatement?.toJK() ?: JKPackageDeclarationImpl(JKNameIdentifierImpl("")),
            importList.toJK(),
            with(declarationMapper) { classes.map { it.toJK() } }
        )

    private fun PsiImportList?.toJK(): JKImportList =
        JKImportListImpl(this?.allImportStatements?.mapNotNull { it.toJK() }.orEmpty())

    private fun PsiPackageStatement.toJK(): JKPackageDeclaration =
        JKPackageDeclarationImpl(JKNameIdentifierImpl(packageName))
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

        return JKImportStatementImpl(JKNameIdentifierImpl(name))
            .also {
                it.assignNonCodeElements(this)
            }
    }

    private fun PsiIdentifier?.toJK(): JKNameIdentifier =
        this?.let {
            JKNameIdentifierImpl(it.text).also {
                it.assignNonCodeElements(this)
            }
        } ?: JKNameIdentifierImpl("")


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
                if (this != null) {
                    (it as PsiOwner).psi = this
                    it.assignNonCodeElements(this)
                }
            }
        }

        fun PsiClassObjectAccessExpressionImpl.toJK(): JKClassLiteralExpression {
            val type = operand.type.toJK(symbolProvider).updateNullabilityRecursively(Nullability.NotNull)
            return JKClassLiteralExpressionImpl(
                JKTypeElementImpl(type),
                when (type) {
                    is JKJavaPrimitiveType -> JKClassLiteralExpression.LiteralType.JAVA_PRIMITIVE_CLASS
                    is JKJavaVoidType -> JKClassLiteralExpression.LiteralType.JAVA_VOID_TYPE
                    else -> JKClassLiteralExpression.LiteralType.JAVA_CLASS
                }
            ).also {
                it.assignNonCodeElements(this)
            }
        }

        fun PsiInstanceOfExpression.toJK(): JKKtIsExpression =
            JKKtIsExpressionImpl(operand.toJK(), JKTypeElementImpl(checkType?.type?.toJK(symbolProvider) ?: JKNoTypeImpl))
                .also {
                    it.assignNonCodeElements(this)
                }

        fun PsiAssignmentExpression.toJK(): JKJavaAssignmentExpression {
            return JKJavaAssignmentExpressionImpl(
                lExpression.toJK() as? JKAssignableExpression ?: error("Its possible? ${lExpression.toJK().prettyDebugPrintTree()}"),
                rExpression.toJK(),
                operationSign.tokenType.toJK()
            ).also {
                it.assignNonCodeElements(this)
            }
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
                .also {
                    it.assignNonCodeElements(this)
                }
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
            }.also {
                it.assignNonCodeElements(this)
            }
        }

        fun IElementType.toJK(): JKOperator = JKJavaOperatorImpl.tokenToOperator[this] ?: error("Unsupported token-type: $this")

        fun PsiPrefixExpression.toJK(): JKExpression {
            return JKPrefixExpressionImpl(operand.toJK(), operationSign.tokenType.toJK()).also {
                it.assignNonCodeElements(this)
            }
        }

        fun PsiPostfixExpression.toJK(): JKExpression {
            return JKPostfixExpressionImpl(operand.toJK(), operationSign.tokenType.toJK()).also {
                it.assignNonCodeElements(this)
            }
        }

        fun PsiLambdaExpression.toJK(): JKExpression {
            return JKLambdaExpressionImpl(
                body.let {
                    when (it) {
                        is PsiExpression -> JKExpressionStatementImpl(it.toJK())
                        is PsiCodeBlock -> JKBlockStatementImpl(with(declarationMapper) { it.toJK() })
                        else -> JKBlockStatementImpl(JKBodyStubImpl)
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
            } ?: JKUnresolvedMethod(methodExpression)

            return when {
                methodExpression.referenceNameElement is PsiKeyword -> {
                    val callee = when ((methodExpression.referenceNameElement as PsiKeyword).tokenType) {
                        SUPER_KEYWORD -> JKSuperExpressionImpl()
                        THIS_KEYWORD -> JKThisExpressionImpl(JKLabelEmptyImpl())
                        else -> error("Unknown keyword in callee position")
                    }
                    JKDelegationConstructorCallImpl(symbol as JKMethodSymbol, callee, arguments.toJK())
                }

                target is KtLightMethod -> {
                    val origin = target.kotlinOrigin
                    when (origin) {
                        is KtNamedFunction -> {
                            if (origin.isExtensionDeclaration()) {
                                val receiver = arguments.expressions.firstOrNull()?.toJK()?.parenthesizeIfBinaryExpression()
                                origin.fqName?.also { importStorage.addImport(it) }
                                JKJavaMethodCallExpressionImpl(
                                    symbolProvider.provideDirectSymbol(origin) as JKMethodSymbol,
                                    arguments.expressions.drop(1).map { it.toJK() }.toArgumentList(),
                                    typeArguments
                                ).qualified(receiver)
                            } else {
                                origin.fqName?.also { importStorage.addImport(it) }
                                JKJavaMethodCallExpressionImpl(
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
                                JKFieldAccessExpressionImpl(symbolProvider.provideDirectSymbol(property) as JKFieldSymbol)
                            val isExtension = property.isExtensionDeclaration()
                            val isTopLevel = origin.getStrictParentOfType<KtClassOrObject>() == null
                            val propertyAccess = if (isTopLevel) {
                                if (isExtension) JKQualifiedExpressionImpl(
                                    arguments.expressions.first().toJK(),
                                    JKJavaQualifierImpl.DOT,
                                    propertyAccessExpression
                                )
                                else propertyAccessExpression
                            } else propertyAccessExpression.qualified(qualifier) as JKAssignableExpression

                            when (if (isExtension) parameterCount - 1 else parameterCount) {
                                0 /* getter */ ->
                                    propertyAccess

                                1 /* setter */ -> {
                                    val argument = (arguments.expressions[if (isExtension) 1 else 0]).toJK()
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

                symbol is JKMethodSymbol ->
                    JKJavaMethodCallExpressionImpl(symbol, arguments.toJK(), typeArguments)
                        .qualified(qualifier)
                symbol is JKFieldSymbol ->
                    JKFieldAccessExpressionImpl(symbol).qualified(qualifier)
                else -> TODO(text)
            }.also {
                it.assignNonCodeElements(this)
            }
        }

        fun PsiFunctionalExpression.functionalType(): JKTypeElement =
            functionalInterfaceType?.toJK(symbolProvider)?.takeIf { type ->
                type.safeAs<JKClassType>()?.classReference is JKMultiverseClassSymbol
            }?.asTypeElement() ?: JKTypeElementImpl(JKNoTypeImpl)

        fun PsiMethodReferenceExpression.toJK(): JKMethodReferenceExpression {
            val symbol = symbolProvider.provideSymbol<JKNamedSymbol>(this).let { symbol ->
                when {
                    symbol.isUnresolved() && isConstructor -> JKUnresolvedClassSymbol(qualifier?.text ?: text)
                    symbol.isUnresolved() && !isConstructor -> JKUnresolvedMethod(referenceName ?: text)
                    else -> symbol
                }
            }

            return JKMethodReferenceExpressionImpl(
                qualifierExpression?.toJK() ?: JKStubExpressionImpl(),
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
            ) return JKStubExpressionImpl()
            if (target is KtLightField
                && target.name == "INSTANCE"
                && target.containingClass.kotlinOrigin is KtObjectDeclaration
            ) {
                return qualifierExpression?.toJK() ?: JKStubExpressionImpl()
            }

            val symbol = symbolProvider.provideSymbol<JKSymbol>(this)
            return when (symbol) {
                is JKClassSymbol -> JKClassAccessExpressionImpl(symbol)
                is JKFieldSymbol -> JKFieldAccessExpressionImpl(symbol)
                is JKPackageSymbol -> JKPackageAccessExpressionImpl(symbol)
                else -> TODO()
            }.qualified(qualifierExpression?.toJK()).also {
                it.assignNonCodeElements(this)
            }
        }

        fun PsiArrayInitializerExpression.toJK(): JKExpression {
            return JKJavaNewArrayImpl(
                initializers.map { it.toJK() },
                JKTypeElementImpl(type?.toJK(symbolProvider).safeAs<JKJavaArrayType>()?.type ?: JKContextType)
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
                        } ?: JKUnresolvedClassSymbol(classOrAnonymousClassReference?.referenceName!!)
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
                        argumentList?.toJK() ?: JKArgumentListImpl(),
                        typeArgumentList,
                        with(declarationMapper) { anonymousClass?.createClassBody() } ?: JKEmptyClassBodyImpl()
                    )
                }
            return qualifier?.let {
                JKQualifiedExpressionImpl(it.toJK(), JKJavaQualifierImpl.DOT, newExpression)
            } ?: newExpression
        }

        fun PsiReferenceParameterList.toJK(): JKTypeArgumentList =
            JKTypeArgumentListImpl(this.typeArguments.map { JKTypeElementImpl(it.toJK(symbolProvider)) })
                .also {
                    it.assignNonCodeElements(this)
                }


        fun PsiArrayAccessExpression.toJK(): JKExpression {
            return JKArrayAccessExpressionImpl(arrayExpression.toJK(), indexExpression?.toJK() ?: TODO())
                .also {
                    it.assignNonCodeElements(this)
                }
        }

        fun PsiTypeCastExpression.toJK(): JKExpression {
            return JKTypeCastExpressionImpl(operand?.toJK() ?: TODO(), castType?.type?.toJK(symbolProvider)?.asTypeElement() ?: TODO())
                .also {
                    it.assignNonCodeElements(this)
                }
        }

        fun PsiParenthesizedExpression.toJK(): JKExpression {
            return JKParenthesizedExpressionImpl(expression?.toJK() ?: TODO())
                .also {
                    it.assignNonCodeElements(this)
                }
        }

        fun PsiExpressionList.toJK(): JKArgumentList {
            val jkExpressions = expressions.map { it.toJK() }
            return ((parent as? PsiCall)?.resolveMethod()
                ?.let { method ->
                    if (jkExpressions.size == method.parameterList.parameters.size
                        && method.parameterList.parameters.getOrNull(jkExpressions.lastIndex)?.isVarArgs == true
                        && expressions.lastOrNull()?.type is PsiArrayType
                    ) {
                        val staredExpression =
                            JKPrefixExpressionImpl(
                                jkExpressions.last(),
                                JKKtSpreadOperator
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
            JKTypeParameterListImpl(typeParameters.map { it.toJK() })
                .also {
                    it.assignNonCodeElements(this)
                }

        fun PsiTypeParameter.toJK(): JKTypeParameter =
            JKTypeParameterImpl(
                nameIdentifier.toJK(),
                extendsListTypes.map { JKTypeElementImpl(it.toJK(symbolProvider, Nullability.Default)) }
            ).also {
                it.assignNonCodeElements(this)
            }

        fun PsiClass.toJK(): JKClass =
            JKClassImpl(
                nameIdentifier.toJK(),
                inheritanceInfo(),
                classKind(),
                typeParameterList?.toJK() ?: JKTypeParameterListImpl(),
                createClassBody(),
                annotationList(this),
                otherModifiers(),
                visibility(),
                modality()
            ).also { jkClassImpl ->
                jkClassImpl.psi = this
                symbolProvider.provideUniverseSymbol(this, jkClassImpl)
            }.also {
                it.assignNonCodeElements(this)
            }


        fun PsiClass.inheritanceInfo(): JKInheritanceInfo {
            val implTypes =
                implementsList?.referencedTypes?.map { JKTypeElementImpl(it.toJK(symbolProvider, Nullability.Default)) }.orEmpty()
            val extensionType =
                extendsList?.referencedTypes?.map { JKTypeElementImpl(it.toJK(symbolProvider, Nullability.Default)) }.orEmpty()
            return JKInheritanceInfoImpl(extensionType, implTypes)
                .also {
                    if (implementsList != null) {
                        it.assignNonCodeElements(implementsList!!)
                    }
                }
        }

        fun PsiClass.createClassBody() =
            JKClassBodyImpl(
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

        fun PsiClassInitializer.toJK(): JKDeclaration =
            (if (hasModifier(JvmModifier.STATIC))
                JKJavaStaticInitDeclarationImpl(body.toJK())
            else JKKtInitDeclarationImpl(body.toJK()))
                .also {
                    it.assignNonCodeElements(this)
                }


        fun PsiEnumConstant.toJK(): JKEnumConstant =
            JKEnumConstantImpl(
                nameIdentifier.toJK(),
                with(expressionTreeMapper) { argumentList?.toJK() ?: JKArgumentListImpl() },
                initializingClass?.createClassBody() ?: JKEmptyClassBodyImpl(),
                JKTypeElementImpl(JKClassTypeImpl(symbolProvider.provideDirectSymbol(containingClass!!) as JKClassSymbol, emptyList()))
            ).also {
                symbolProvider.provideUniverseSymbol(this, it)
                it.psi = this
            }.also {
                it.assignNonCodeElements(this)
            }


        fun PsiMember.modality() =
            modality({ ast, psi -> ast.assignNonCodeElements(psi) })

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
                    JKOtherModifierElementImpl(it).withAssignedNonCodeElements(child)
                }
            }.orEmpty()


        private fun PsiMember.visibility(): JKVisibilityModifierElement =
            visibility(referenceSearcher) { ast, psi -> ast.assignNonCodeElements(psi) }

        fun PsiField.toJK(): JKJavaField {
            return JKJavaFieldImpl(
                JKTypeElementImpl(type.toJK(symbolProvider)).withAssignedNonCodeElements(typeElement),
                nameIdentifier.toJK(),
                with(expressionTreeMapper) { initializer.toJK() },
                annotationList(this),
                otherModifiers(),
                visibility(),
                modality(),
                JKMutabilityModifierElementImpl(Mutability.UNKNOWN)
            ).also {
                symbolProvider.provideUniverseSymbol(this, it)
                it.psi = this
            }.also {
                it.assignNonCodeElements(this)
            }
        }

        fun <T : PsiModifierListOwner> T.annotationList(docCommentOwner: PsiDocCommentOwner?): JKAnnotationList {
            val plainAnnotations = annotations.map { it.cast<PsiAnnotation>().toJK() }
            val deprecatedAnnotation = docCommentOwner?.docComment?.deprecatedAnnotation() ?: return JKAnnotationListImpl(plainAnnotations)
            return JKAnnotationListImpl(
                plainAnnotations.mapNotNull { annotation ->
                    if (annotation.classSymbol.fqName == "java.lang.Deprecated") null else annotation
                } + deprecatedAnnotation
            )
        }

        fun PsiAnnotation.toJK(): JKAnnotation =
            JKAnnotationImpl(
                symbolProvider.provideSymbol<JKSymbol>(nameReferenceElement!!).safeAs<JKClassSymbol>()
                    ?: JKUnresolvedClassSymbol(nameReferenceElement!!.text),
                parameterList.attributes.map { parameter ->
                    if (parameter.nameIdentifier != null) {
                        JKAnnotationNameParameterImpl(
                            parameter.value?.toJK() ?: JKStubExpressionImpl(),
                            JKNameIdentifierImpl(parameter.name!!)
                        )
                    } else {
                        JKAnnotationParameterImpl(
                            parameter.value?.toJK() ?: JKStubExpressionImpl()
                        )
                    }
                }
            ).also {
                it.assignNonCodeElements(this)
            }

        fun PsiDocComment.deprecatedAnnotation(): JKAnnotation? =
            findTagByName("deprecated")?.let { tag ->
                JKAnnotationImpl(
                    symbolProvider.provideClassSymbol("kotlin.Deprecated"),
                    listOf(
                        JKAnnotationParameterImpl(stringLiteral(tag.content(), symbolProvider))
                    )
                )
            }

        private fun PsiAnnotationMemberValue.toJK(): JKAnnotationMemberValue =
            when (this) {
                is PsiExpression -> with(expressionTreeMapper) { toJK() }
                is PsiAnnotation -> toJK()
                is PsiArrayInitializerMemberValue ->
                    JKKtAnnotationArrayInitializerExpressionImpl(
                        initializers.map { it.toJK() }
                    )
                else -> TODO(this::class.toString())
            }.also {
                it.assignNonCodeElements(this)
            }

        fun PsiAnnotationMethod.toJK(): JKJavaAnnotationMethod =
            JKJavaAnnotationMethodImpl(
                returnType?.toJK(symbolProvider)?.asTypeElement()
                    ?: JKTypeElementImpl(JKJavaVoidType).takeIf { isConstructor }
                    ?: TODO(),
                nameIdentifier.toJK(),
                defaultValue?.toJK() ?: JKStubExpressionImpl()
            ).also {
                it.psi = this
                symbolProvider.provideUniverseSymbol(this, it)
            }.also {
                it.assignNonCodeElements(this)
            }


        fun PsiMethod.toJK(): JKJavaMethod {
            return JKJavaMethodImpl(
                returnType?.toJK(symbolProvider)?.asTypeElement()
                    ?: JKTypeElementImpl(JKJavaVoidType).takeIf { isConstructor }
                    ?: TODO(),
                nameIdentifier.toJK(),
                parameterList.parameters.map { it.toJK() },
                body?.toJK() ?: JKBodyStubImpl,
                typeParameterList?.toJK() ?: JKTypeParameterListImpl(),
                annotationList(this),
                throwsList.referencedTypes.map { JKTypeElementImpl(it.toJK(symbolProvider)) },
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
            val rawType = type.toJK(symbolProvider)
            val type =
                if (isVarArgs && rawType is JKJavaArrayType) JKTypeElementImpl(rawType.type)
                else rawType.asTypeElement()
            return JKParameterImpl(
                type,
                nameIdentifier.toJK(),
                isVarArgs,
                annotationList = annotationList(null)
            ).also {
                symbolProvider.provideUniverseSymbol(this, it)
                it.psi = this
            }.also {
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
            JKLocalVariableImpl(
                JKTypeElementImpl(type.toJK(symbolProvider)).withAssignedNonCodeElements(typeElement),
                nameIdentifier.toJK(),
                with(expressionTreeMapper) { initializer.toJK() },
                JKMutabilityModifierElementImpl(
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
                                    JKJavaDefaultSwitchCaseImpl(emptyList()).withAssignedNonCodeElements(statement)
                                else
                                    JKJavaLabelSwitchCaseImpl(
                                        with(expressionTreeMapper) { statement.caseValue.toJK() },
                                        emptyList()
                                    ).withAssignedNonCodeElements(statement)
                            else ->
                                cases.lastOrNull()?.also { it.statements = it.statements + statement.toJK() }
                                    ?: run {
                                        cases += JKJavaLabelSwitchCaseImpl(
                                            JKStubExpressionImpl(),
                                            listOf(statement.toJK())
                                        )
                                    }
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
                    JKLabeledStatementImpl(statement.toJK(), labels.map { JKNameIdentifierImpl(it.text) }).asStatement()
                }
                is PsiEmptyStatement -> JKEmptyStatementImpl()
                is PsiThrowStatement ->
                    JKJavaThrowStatementImpl(with(expressionTreeMapper) { exception.toJK() })
                is PsiTryStatement ->
                    JKJavaTryStatementImpl(
                        resourceList?.toList()?.map { (it as PsiLocalVariable).toJK() }.orEmpty(),
                        tryBlock?.toJK() ?: JKBodyStubImpl,
                        finallyBlock?.toJK() ?: JKBodyStubImpl,
                        catchSections.map { it.toJK() }
                    )
                is PsiSynchronizedStatement ->
                    JKJavaSynchronizedStatementImpl(
                        with(expressionTreeMapper) { lockExpression?.toJK() } ?: JKStubExpressionImpl(),
                        body?.toJK() ?: JKBodyStubImpl
                    )
                else -> TODO("for ${this::class}")
            }.also {
                if (this != null) {
                    (it as PsiOwner).psi = this
                    it.assignNonCodeElements(this)
                }
            }
        }

        fun PsiCatchSection.toJK(): JKJavaTryCatchSection =
            JKJavaTryCatchSectionImpl(parameter?.toJK()!!, catchBlock?.toJK() ?: JKBodyStubImpl)
                .also {
                    it.psi = this
                }.also {
                    it.assignNonCodeElements(this)
                }
    }

    //TODO better way than generateSequence.last??
    fun PsiLabeledStatement.collectLabels(): Pair<List<PsiIdentifier>, PsiStatement> =
        generateSequence(emptyList<PsiIdentifier>() to this as PsiStatement) { (labels, statement) ->
            if (statement !is PsiLabeledStatementImpl) return@generateSequence null
            (labels + statement.labelIdentifier) to statement.statement!!
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
                    JKTypeElementImpl(type.toJK(symbolProvider).updateNullabilityRecursively(Nullability.NotNull))
                } else null
            else -> null
        }?.let { JKTreeRootImpl(it) }

    private val tokenCache = mutableMapOf<PsiElement, JKNonCodeElement>()

    private fun PsiElement.collectNonCodeElements(): Pair<List<JKNonCodeElement>, List<JKNonCodeElement>> {
        fun PsiElement.toToken(): JKNonCodeElement? {
            if (this in tokenCache) return tokenCache[this]!!
            val token = when {
                this is PsiDocComment ->
                    JKCommentElementImpl(IdeaDocCommentConverter.convertDocComment(this))
                this is PsiComment -> JKCommentElementImpl(text)
                this is PsiWhiteSpace -> JKSpaceElementImpl(text)
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
        this.leftNonCodeElements = leftTokens
        this.rightNonCodeElements = rightTokens
    }

    private inline fun <reified T : JKNonCodeElementsListOwner> T.withAssignedNonCodeElements(psi: PsiElement?): T =
        also { it.assignNonCodeElements(psi) }
}


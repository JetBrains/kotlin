/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.tree

import com.intellij.psi.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaClassDescriptor
import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.JKSymbolProvider
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.j2k.conversions.resolveFqName
import org.jetbrains.kotlin.j2k.kotlinTypeByName
import org.jetbrains.kotlin.j2k.tree.impl.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtTypeElement
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

fun JKExpression.type(context: ConversionContext): JKType? =
    when (this) {
        is JKLiteralExpression -> type.toJkType(context.symbolProvider)
        is JKOperatorExpression -> {
            if (operator !is JKKtOperatorImpl) {
                error("Cannot get type of ${operator::class}, it should be first converted to KtOperator")
            }
            val operatorSymbol = (operator as JKKtOperatorImpl).methodSymbol
            if (operatorSymbol.name == "compareTo") {
                kotlinTypeByName("kotlin.Boolean", context.symbolProvider)
            } else operatorSymbol.returnType
        }
        is JKMethodCallExpression -> identifier.returnType
        is JKFieldAccessExpressionImpl -> identifier.fieldType
        is JKQualifiedExpressionImpl -> this.selector.type(context)
        is JKKtThrowExpression -> kotlinTypeByName(KotlinBuiltIns.FQ_NAMES.nothing.asString(), context.symbolProvider)
        is JKClassAccessExpression -> null
        is JKJavaNewExpression -> JKClassTypeImpl(classSymbol)
        is JKKtIsExpression -> kotlinTypeByName(KotlinBuiltIns.FQ_NAMES._boolean.asString(), context.symbolProvider)
        is JKParenthesizedExpression -> expression.type(context)
        is JKTypeCastExpression -> type.type
        is JKThisExpression -> null// TODO return actual type
        is JKSuperExpression -> null// TODO return actual type
        is JKStubExpression -> null
        is JKIfElseExpression -> thenBranch.type(context)// TODO return actual type
        is JKArrayAccessExpression ->
            (expression.type(context) as? JKParametrizedType)?.parameters?.lastOrNull()
        is JKClassLiteralExpression -> {
            val symbol = when (literalType) {
                JKClassLiteralExpression.LiteralType.KOTLIN_CLASS ->
                    context.symbolProvider.provideByFqName<JKClassSymbol>("kotlin.reflect.KClass")
                JKClassLiteralExpression.LiteralType.JAVA_CLASS,
                JKClassLiteralExpression.LiteralType.JAVA_PRIMITIVE_CLASS, JKClassLiteralExpression.LiteralType.JAVA_VOID_TYPE ->
                    context.symbolProvider.provideByFqName("java.lang.Class")
            }
            JKClassTypeImpl(symbol, listOf(classType.type), Nullability.NotNull)
        }

        else -> TODO(this::class.java.toString())
    }

fun ClassId.toKtClassType(
    symbolProvider: JKSymbolProvider,
    nullability: Nullability = Nullability.Default,
    context: PsiElement = symbolProvider.symbolsByPsi.keys.first()
): JKType {
    val typeSymbol = symbolProvider.provideDirectSymbol(resolveFqName(this, context)!!) as JKClassSymbol
    return JKClassTypeImpl(typeSymbol, emptyList(), nullability)
}

fun PsiType.toJK(symbolProvider: JKSymbolProvider, nullability: Nullability = Nullability.Default): JKType {
    return when (this) {
        is PsiClassType -> {
            val target = resolve()
            val parameters = parameters.map { it.toJK(symbolProvider, nullability) }
            when (target) {
                null ->
                    JKClassTypeImpl(JKUnresolvedClassSymbol(rawType().canonicalText), parameters, nullability)
                is PsiTypeParameter ->
                    JKTypeParameterTypeImpl(target.name!!)
                else -> {
                    JKClassTypeImpl(
                        target.let { symbolProvider.provideDirectSymbol(it) as JKClassSymbol },
                        parameters,
                        nullability
                    )
                }
            }
        }
        is PsiArrayType -> JKJavaArrayTypeImpl(componentType.toJK(symbolProvider, nullability), nullability)
        is PsiPrimitiveType -> JKJavaPrimitiveTypeImpl.KEYWORD_TO_INSTANCE[presentableText]
            ?: error("Invalid primitive type $presentableText")
        is PsiDisjunctionType ->
            JKJavaDisjunctionTypeImpl(disjunctions.map { it.toJK(symbolProvider) })
        is PsiWildcardType ->
            when {
                isExtends ->
                    JKVarianceTypeParameterTypeImpl(
                        JKVarianceTypeParameterType.Variance.OUT,
                        extendsBound.toJK(symbolProvider)
                    )
                isSuper ->
                    JKVarianceTypeParameterTypeImpl(
                        JKVarianceTypeParameterType.Variance.IN,
                        superBound.toJK(symbolProvider)
                    )
                else -> JKStarProjectionTypeImpl()
            }
        else -> throw Exception("Invalid PSI ${this::class.java}")
    }
}

fun JKType.isSubtypeOf(other: JKType, symbolProvider: JKSymbolProvider): Boolean =
    other.toKtType(symbolProvider)
        ?.let { otherType -> this.toKtType(symbolProvider)?.isSubtypeOf(otherType) } == true


fun KtTypeElement.toJK(symbolProvider: JKSymbolProvider): JKType? {
    return when (this) {
        is KtUserType -> {
            val qualifiedName = qualifier?.text?.let { it + "." }.orEmpty() + referencedName
            val typeParameters = typeArguments.map { it.typeReference?.typeElement?.toJK(symbolProvider) }
            if (typeParameters.any { it == null }) return null
            val fqName = resolveFqName(ClassId.fromString(qualifiedName), this) ?: return null
            val symbol = symbolProvider.provideDirectSymbol(fqName) as? JKClassSymbol ?: return null
            JKClassTypeImpl(symbol, typeParameters as List<JKType>)
        }
        is KtNullableType ->
            innerType?.toJK(symbolProvider)?.updateNullability(Nullability.Nullable)
        else -> null
    }
}

fun JKType.toKtType(symbolProvider: JKSymbolProvider): KotlinType? =
    when (this) {
        is JKClassType -> classReference.toKtType()
        is JKJavaPrimitiveType ->
            kotlinTypeByName(
                jvmPrimitiveType.primitiveType.typeFqName.asString(),
                symbolProvider
            ).toKtType(symbolProvider)
        else -> null
//        else -> TODO(this::class.java.toString())
    }


fun JKClassSymbol.toKtType(): KotlinType? {
    val classDescriptor = when (this) {
        is JKMultiverseKtClassSymbol -> {
            val bindingContext = target.analyze()
            bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, target] as ClassDescriptor
        }
        is JKMultiverseClassSymbol ->
            target.getJavaClassDescriptor()
        is JKUniverseClassSymbol ->
            target.psi<PsiClass>()?.getJavaClassDescriptor()//TODO null in case of a fake package
        else -> TODO(this::class.java.toString())
    }
    return classDescriptor?.defaultType
}

inline fun <reified T : JKType> T.updateNullability(newNullability: Nullability): T =
    if (nullability == newNullability) this
    else when (this) {
        is JKTypeParameterTypeImpl -> JKTypeParameterTypeImpl(name, newNullability)
        is JKClassTypeImpl -> JKClassTypeImpl(classReference, parameters, newNullability)
        is JKNoType -> this
        is JKJavaVoidType -> this
        is JKJavaPrimitiveType -> this
        is JKJavaArrayType -> JKJavaArrayTypeImpl(type, newNullability)
        is JKContextType -> JKContextType
        is JKJavaDisjunctionType -> this
        else -> TODO(this::class.toString())
    } as T

fun <T : JKType> T.updateNullabilityRecursively(newNullability: Nullability): T =
    if (nullability == newNullability) this
    else when (this) {
        is JKTypeParameterTypeImpl -> JKTypeParameterTypeImpl(name, newNullability)
        is JKClassTypeImpl ->
            JKClassTypeImpl(
                classReference,
                parameters.map { it.updateNullabilityRecursively(newNullability) },
                newNullability
            )
        is JKNoType -> this
        is JKJavaVoidType -> this
        is JKJavaPrimitiveType -> this
        is JKJavaArrayType -> JKJavaArrayTypeImpl(type.updateNullabilityRecursively(newNullability), newNullability)
        is JKContextType -> JKContextType
        is JKJavaDisjunctionType -> this
        else -> TODO(this::class.toString())
    } as T

fun JKJavaMethod.returnTypeNullability(context: ConversionContext): Nullability =
    context.typeFlavorCalculator.methodNullability(psi()!!)

fun JKType.isCollectionType(symbolProvider: JKSymbolProvider): Boolean {
    if (this !is JKClassType) return false
    val collectionType = JKClassTypeImpl(symbolProvider.provideByFqName("java.util.Collection"), emptyList())
    return this.isSubtypeOf(collectionType, symbolProvider)
}

fun JKType.isStringType(): Boolean =
    (this as? JKClassType)?.classReference?.name == "String"

fun JKLiteralExpression.LiteralType.toPrimitiveType(): JKJavaPrimitiveType? =
    when (this) {
        JKLiteralExpression.LiteralType.CHAR -> JKJavaPrimitiveTypeImpl.CHAR
        JKLiteralExpression.LiteralType.BOOLEAN -> JKJavaPrimitiveTypeImpl.BOOLEAN
        JKLiteralExpression.LiteralType.INT -> JKJavaPrimitiveTypeImpl.INT
        JKLiteralExpression.LiteralType.LONG -> JKJavaPrimitiveTypeImpl.LONG
        JKLiteralExpression.LiteralType.FLOAT -> JKJavaPrimitiveTypeImpl.FLOAT
        JKLiteralExpression.LiteralType.DOUBLE -> JKJavaPrimitiveTypeImpl.DOUBLE
        JKLiteralExpression.LiteralType.STRING -> null
        JKLiteralExpression.LiteralType.NULL -> null
    }

fun JKJavaPrimitiveType.toLiteralType(): JKLiteralExpression.LiteralType? =
    when (this) {
        JKJavaPrimitiveTypeImpl.CHAR -> JKLiteralExpression.LiteralType.CHAR
        JKJavaPrimitiveTypeImpl.BOOLEAN -> JKLiteralExpression.LiteralType.BOOLEAN
        JKJavaPrimitiveTypeImpl.INT -> JKLiteralExpression.LiteralType.INT
        JKJavaPrimitiveTypeImpl.LONG -> JKLiteralExpression.LiteralType.LONG
        JKJavaPrimitiveTypeImpl.CHAR -> JKLiteralExpression.LiteralType.CHAR
        JKJavaPrimitiveTypeImpl.DOUBLE -> JKLiteralExpression.LiteralType.DOUBLE
        JKJavaPrimitiveTypeImpl.FLOAT -> JKLiteralExpression.LiteralType.FLOAT
        else -> null
    }


fun JKType.asPrimitiveType(): JKJavaPrimitiveType? =
    if (this is JKJavaPrimitiveType) this
    else when ((this as? JKClassType)?.classReference?.fqName) {
        KotlinBuiltIns.FQ_NAMES._char.asString(), CommonClassNames.JAVA_LANG_CHARACTER -> JKJavaPrimitiveTypeImpl.CHAR
        KotlinBuiltIns.FQ_NAMES._boolean.asString(), CommonClassNames.JAVA_LANG_BOOLEAN -> JKJavaPrimitiveTypeImpl.BOOLEAN
        KotlinBuiltIns.FQ_NAMES._int.asString(), CommonClassNames.JAVA_LANG_INTEGER -> JKJavaPrimitiveTypeImpl.INT
        KotlinBuiltIns.FQ_NAMES._long.asString(), CommonClassNames.JAVA_LANG_LONG -> JKJavaPrimitiveTypeImpl.LONG
        KotlinBuiltIns.FQ_NAMES._float.asString(), CommonClassNames.JAVA_LANG_FLOAT -> JKJavaPrimitiveTypeImpl.FLOAT
        KotlinBuiltIns.FQ_NAMES._double.asString(), CommonClassNames.JAVA_LANG_DOUBLE -> JKJavaPrimitiveTypeImpl.DOUBLE
        KotlinBuiltIns.FQ_NAMES._byte.asString(), CommonClassNames.JAVA_LANG_BYTE -> JKJavaPrimitiveTypeImpl.BYTE
        KotlinBuiltIns.FQ_NAMES._short.asString(), CommonClassNames.JAVA_LANG_SHORT -> JKJavaPrimitiveTypeImpl.SHORT
        else -> null
    }

fun JKJavaPrimitiveType.isNumberType() =
    this == JKJavaPrimitiveTypeImpl.INT ||
            this == JKJavaPrimitiveTypeImpl.LONG ||
            this == JKJavaPrimitiveTypeImpl.FLOAT ||
            this == JKJavaPrimitiveTypeImpl.DOUBLE

inline fun <reified T : JKType> T.addTypeParametersToRawProjectionType(typeParameter: JKType): T =
    if (this is JKClassType && parameters.isEmpty()) {
        val resolvedClass = classReference.target
        val parametersCount =
            when (resolvedClass) {
                is PsiClass -> resolvedClass.typeParameters.size
                is KtClass -> resolvedClass.typeParameters.size
                else -> 0
            }
        val typeParameters = List(parametersCount) { typeParameter }
        JKClassTypeImpl(
            classReference,
            typeParameters,
            nullability
        ) as T
    } else this

fun JKClassSymbol.isArrayType(): Boolean =
    fqName in
            JKJavaPrimitiveTypeImpl.KEYWORD_TO_INSTANCE.values
                .filterIsInstance<JKJavaPrimitiveType>()
                .map { PrimitiveType.valueOf(it.jvmPrimitiveType.name).arrayTypeFqName.asString() } +
            KotlinBuiltIns.FQ_NAMES.array.asString()


fun JKType.arrayInnerType(): JKType? =
    when (this) {
        is JKJavaArrayType -> type
        is JKClassType ->
            if (this.classReference.isArrayType()) this.parameters.singleOrNull()
            else null
        else -> null
    }

val namesOfPrimitiveTypes by lazy {
    KotlinBuiltIns.FQ_NAMES.primitiveTypeShortNames.map { it.identifier.decapitalize() }
}
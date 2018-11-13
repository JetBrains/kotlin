/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.tree

import com.intellij.psi.*
import com.intellij.util.reverse
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.JKSymbolProvider
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.j2k.conversions.resolveFqName
import org.jetbrains.kotlin.j2k.tree.impl.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtTypeElement
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

import org.jetbrains.kotlin.idea.caches.resolve.util.*
import org.jetbrains.kotlin.j2k.kotlinTypeByName

fun JKExpression.type(context: ConversionContext): JKType =
    when (this) {
        is JKLiteralExpression -> type.toJkType(context.symbolProvider)
        is JKBinaryExpression -> {
            val operatorSymbol = (operator as JKKtOperatorImpl).methodSymbol
            if (operatorSymbol.name == "compareTo") {
                kotlinTypeByName("kotlin.Boolean", context.symbolProvider)
            } else operatorSymbol.returnType
        }
        is JKPostfixExpressionImpl ->
            (operator as JKKtOperatorImpl).methodSymbol.returnType
        is JKPrefixExpression ->
            (operator as JKKtOperatorImpl).methodSymbol.returnType
        is JKMethodCallExpression -> identifier.returnType
        is JKFieldAccessExpressionImpl -> identifier.fieldType
        is JKQualifiedExpressionImpl -> this.selector.type(context)
        is JKKtThrowExpression -> kotlinTypeByName(KotlinBuiltIns.FQ_NAMES.nothing.asString(), context.symbolProvider)
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
                    JKUnresolvedClassType(rawType().canonicalText, parameters, nullability)
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
    toKtType(symbolProvider).isSubtypeOf(other.toKtType(symbolProvider))

fun KtTypeElement.toJK(symbolProvider: JKSymbolProvider): JKType =
    when (this) {
        is KtUserType -> {
            val qualifiedName = qualifier?.text?.let { it + "." }.orEmpty() + referencedName
            val typeParameters = typeArguments.map { it.typeReference!!.typeElement!!.toJK(symbolProvider) }
            val symbol = symbolProvider.provideDirectSymbol(
                resolveFqName(ClassId.fromString(qualifiedName), this)!!
            ) as JKClassSymbol

            JKClassTypeImpl(symbol, typeParameters)
        }
        else -> TODO(this::class.java.toString())
    }

fun JKType.toKtType(symbolProvider: JKSymbolProvider): KotlinType =
    when (this) {
        is JKClassType -> classReference!!.toKtType(symbolProvider)
        is JKJavaPrimitiveType ->
            kotlinTypeByName(
                jvmPrimitiveType.primitiveType.typeFqName.asString(),
                symbolProvider
            ).toKtType(symbolProvider)
        else -> TODO(this::class.java.toString())
    }


fun JKClassSymbol.toKtType(symbolProvider: JKSymbolProvider): KotlinType {
    val classDescriptor = when (this) {
        is JKMultiverseKtClassSymbol -> {
            val bindingContext = target.analyze()
            bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, target] as ClassDescriptor
        }
        is JKMultiverseClassSymbol ->
            target.getJavaClassDescriptor()!!
        is JKUniverseClassSymbol ->
            (symbolProvider.symbolsByPsi.reverse()[target] as PsiClass).getJavaClassDescriptor()!!
        else -> TODO(this::class.java.toString())
    }
    return classDescriptor.defaultType
}

fun JKType.updateNullability(newNullability: Nullability): JKType =
    if (nullability == newNullability) this
    else when (this) {
        is JKTypeParameterTypeImpl -> JKTypeParameterTypeImpl(name, newNullability)
        is JKClassTypeImpl -> JKClassTypeImpl(classReference, parameters, newNullability)
        is JKUnresolvedClassType -> JKUnresolvedClassType(name, parameters, newNullability)
        is JKNoType -> this
        is JKJavaVoidType -> this
        is JKJavaPrimitiveType -> this
        else -> TODO(this::class.toString())
    }
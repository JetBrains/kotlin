/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiVariable
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.j2k.*
import org.jetbrains.kotlin.j2k.ast.Mutability
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.kotlinTypeByName
import org.jetbrains.kotlin.nj2k.symbols.JKClassSymbol
import org.jetbrains.kotlin.nj2k.symbols.JKUniverseClassSymbol
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.*

class TypeMappingConversion(val context: NewJ2kConverterContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        return when (element) {
            is JKTypeElement -> {
                val newType = element.type.mapType(element)
                JKTypeElementImpl(newType).withNonCodeElementsFrom(element)
            }
            is JKJavaNewExpression -> {
                val newClassSymbol = element.classSymbol.mapClassSymbol(null)
                recurse(
                    JKJavaNewExpressionImpl(
                        newClassSymbol,
                        element::arguments.detached(),
                        element::typeArgumentList.detached().fixTypeArguments(newClassSymbol),
                        element::classBody.detached()
                    ).withNonCodeElementsFrom(element)
                )
            }
            else -> recurse(element)
        }
    }

    private fun JKTypeArgumentList.fixTypeArguments(classSymbol: JKClassSymbol): JKTypeArgumentList {
        if (typeArguments.isNotEmpty()) {
            return JKTypeArgumentListImpl(
                typeArguments.map { typeArgument ->
                    JKTypeElementImpl(typeArgument.type.mapType(null))
                }
            )
        }
        return when (val typeParametersCount = classSymbol.expectedTypeParametersCount()) {
            0 -> this
            else -> JKTypeArgumentListImpl(List(typeParametersCount) {
                JKTypeElementImpl(
                    kotlinTypeByName(
                        KotlinBuiltIns.FQ_NAMES.any.toSafe().asString(),
                        context.symbolProvider,
                        Nullability.Nullable
                    )
                )
            })
        }
    }


    private fun JKType.fixRawType(typeElement: JKTypeElement?) =
        when (typeElement?.parent) {
            is JKClassLiteralExpression -> this
            is JKKtIsExpression ->
                addTypeParametersToRawProjectionType(JKStarProjectionTypeImpl())
                    .updateNullability(Nullability.NotNull)
            is JKTypeCastExpression ->
                addTypeParametersToRawProjectionType(JKStarProjectionTypeImpl())
            else ->
                addTypeParametersToRawProjectionType(
                    JKStarProjectionTypeImpl()
                )
        }

    private fun JKType.mapType(typeElement: JKTypeElement?): JKType =
        when (this) {
            is JKJavaPrimitiveType -> mapPrimitiveType()
            is JKClassType -> mapClassType(typeElement)
            is JKJavaVoidType ->
                kotlinTypeByName(
                    KotlinBuiltIns.FQ_NAMES.unit.toSafe().asString(),
                    context.symbolProvider,
                    Nullability.NotNull
                )
            is JKJavaArrayType ->
                JKClassTypeImpl(
                    context.symbolProvider.provideClassSymbol(type.arrayFqName()),
                    if (type is JKJavaPrimitiveType) emptyList() else listOf(type.mapType(typeElement)),
                    nullability
                )
            is JKVarianceTypeParameterType ->
                JKVarianceTypeParameterTypeImpl(
                    variance,
                    boundType.mapType(null)
                )
            is JKCapturedType -> {
                JKCapturedType(
                    wildcardType.mapType(null) as JKWildCardType,
                    nullability
                )
            }
            else -> this
        }.fixRawType(typeElement)

    private fun JKClassSymbol.mapClassSymbol(typeElement: JKTypeElement?): JKClassSymbol {
        if (this is JKUniverseClassSymbol) return this
        if (typeElement?.parentOfType<JKInheritanceInfo>() != null) return this
        val newFqName = kotlinCollectionClassName()
            ?: kotlinStandardType()
            ?: fqName
        return context.symbolProvider.provideClassSymbol(newFqName)
    }

    private fun JKClassType.mapClassType(typeElement: JKTypeElement?): JKClassType =
        JKClassTypeImpl(
            classReference.mapClassSymbol(typeElement),
            parameters.map { it.mapType(null) },
            nullability
        )

    private fun JKClassSymbol.kotlinCollectionClassName(): String? =
        toKotlinMutableTypesMap[fqName]

    private fun JKClassSymbol.kotlinStandardType(): String? {
        if (isKtFunction(fqName)) return fqName
        return JavaToKotlinClassMap.mapJavaToKotlin(FqName(fqName))?.asString()
    }

    private fun JKJavaPrimitiveType.mapPrimitiveType(): JKClassType {
        val fqName = jvmPrimitiveType.primitiveType.typeFqName
        return JKClassTypeImpl(
            context.symbolProvider.provideClassSymbol(fqName),
            nullability = Nullability.NotNull
        )
    }

    companion object {
        private val ktFunctionRegex = "kotlin\\.jvm\\.functions\\.Function\\d+".toRegex()
        private fun isKtFunction(fqName: String) =
            ktFunctionRegex.matches(fqName)
    }
}
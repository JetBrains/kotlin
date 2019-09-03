/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.j2k.toKotlinMutableTypesMap
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.symbols.JKClassSymbol
import org.jetbrains.kotlin.nj2k.symbols.JKUniverseClassSymbol
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.*
import org.jetbrains.kotlin.nj2k.types.*
import org.jetbrains.kotlin.psi.KtClass

class TypeMappingConversion(context: NewJ2kConverterContext) : RecursiveApplicableConversionBase(context) {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        when (element) {
            is JKTypeElement -> {
                element.type = element.type.mapType(element)
            }
            is JKJavaNewExpression -> {
                val newClassSymbol = element.classSymbol.mapClassSymbol(null)
                return recurse(
                    JKJavaNewExpressionImpl(
                        newClassSymbol,
                        element::arguments.detached(),
                        element::typeArgumentList.detached().fixTypeArguments(newClassSymbol),
                        element::classBody.detached()
                    ).withNonCodeElementsFrom(element)
                )
            }
        }
        return recurse(element)
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
                JKTypeElementImpl(typeFactory.types.nullableAny)
            })
        }
    }


    private fun JKType.fixRawType(typeElement: JKTypeElement?) =
        when (typeElement?.parent) {
            is JKClassLiteralExpression -> this
            is JKKtIsExpression ->
                addTypeParametersToRawProjectionType(JKStarProjectionTypeImpl)
                    .updateNullability(Nullability.NotNull)
            is JKTypeCastExpression ->
                addTypeParametersToRawProjectionType(JKStarProjectionTypeImpl)
            else ->
                addTypeParametersToRawProjectionType(JKStarProjectionTypeImpl)
        }

    private fun JKType.mapType(typeElement: JKTypeElement?): JKType =
        when (this) {
            is JKJavaPrimitiveType -> mapPrimitiveType()
            is JKClassType -> mapClassType(typeElement)
            is JKJavaVoidType -> typeFactory.types.unit

            is JKJavaArrayType ->
                JKClassTypeImpl(
                    symbolProvider.provideClassSymbol(type.arrayFqName()),
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
        return symbolProvider.provideClassSymbol(newFqName)
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
            symbolProvider.provideClassSymbol(fqName),
            nullability = Nullability.NotNull
        )
    }

    private inline fun <reified T : JKType> T.addTypeParametersToRawProjectionType(typeParameter: JKType): T =
        if (this is JKClassType && parameters.isEmpty()) {
            val parametersCount = classReference.expectedTypeParametersCount()
            val typeParameters = List(parametersCount) { typeParameter }
            JKClassTypeImpl(
                classReference,
                typeParameters,
                nullability
            ) as T
        } else this

    private fun JKClassSymbol.expectedTypeParametersCount(): Int =
        when (val resolvedClass = target) {
            is PsiClass -> resolvedClass.typeParameters.size
            is KtClass -> resolvedClass.typeParameters.size
            is JKClass -> resolvedClass.typeParameterList.typeParameters.size
            else -> 0
        }

    companion object {
        private val ktFunctionRegex = "kotlin\\.jvm\\.functions\\.Function\\d+".toRegex()
        private fun isKtFunction(fqName: String) =
            ktFunctionRegex.matches(fqName)
    }
}
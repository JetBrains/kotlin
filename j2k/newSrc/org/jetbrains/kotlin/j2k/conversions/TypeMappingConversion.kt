/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiVariable
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.j2k.*
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject

class TypeMappingConversion(val context: ConversionContext) : RecursiveApplicableConversionBase() {

    private val typeFlavorCalculator = TypeFlavorCalculator(object : TypeFlavorConverterFacade {
        override val referenceSearcher: ReferenceSearcher
            get() = context.converter.converterServices.oldServices.referenceSearcher
        override val javaDataFlowAnalyzerFacade: JavaDataFlowAnalyzerFacade
            get() = context.converter.converterServices.oldServices.javaDataFlowAnalyzerFacade
        override val resolverForConverter: ResolverForConverter
            get() = context.converter.converterServices.oldServices.resolverForConverter

        override fun inConversionScope(element: PsiElement): Boolean = context.inConversionContext(element)

    })

    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        return recurse(
            if (element is JKTypeElement) {
                val newType = refineNullability(mapType(element.type, element), element)
                JKTypeElementImpl(newType)
            } else element
        )
    }

    private fun refineNullability(type: JKType, element: JKTypeElement): JKType {
        if (type.nullability == Nullability.Default && type is JKClassType) {
            val newNullability = calculateNullability(element.parent)
            if (newNullability != type.nullability) {
                return JKClassTypeImpl(type.classReference, type.parameters, newNullability)
            }
        }
        return type
    }

    private fun mapType(type: JKType, element: JKTreeElement): JKType = when (type) {
        is JKJavaPrimitiveType -> mapPrimitiveType(type)
        is JKClassType -> mapClassType(type, element)
        is JKJavaVoidType ->
            kotlinTypeByName(
                KotlinBuiltIns.FQ_NAMES.unit.toSafe().asString(),
                context.symbolProvider,
                Nullability.NotNull
            )
        is JKJavaArrayType -> JKClassTypeImpl(
            context.symbolProvider.provideByFqName(arrayFqName(type.type)),
            if (type.type is JKJavaPrimitiveType) emptyList() else listOf(mapType(type.type, element)),
            type.nullability
        )
        else -> type
    }

    private fun mapClassType(type: JKClassType, element: JKTreeElement): JKClassType {
        val fqName = type.classReference.fqName ?: return type
        val newFqName = JavaToKotlinClassMap.mapJavaToKotlin(FqName(fqName))
            ?: mapCollectionClass(fqName)?.let { ClassId.fromString(it) }
            ?: return type
        return JKClassTypeImpl(
            context.symbolProvider.provideByFqName(newFqName),
            type.parameters.map { mapType(it, element) },
            type.nullability
        )
    }

    private fun mapCollectionClass(fqName: String): String? =
        mapOf("java.util.Collection" to "kotlin.collections.Collection")[fqName]

    private fun mapPrimitiveType(type: JKJavaPrimitiveType): JKClassType {
        val fqName = type.jvmPrimitiveType.primitiveType.typeFqName
        return JKClassTypeImpl(context.symbolProvider.provideByFqName(ClassId.topLevel(fqName)), nullability = Nullability.NotNull)
    }

    private fun calculateNullability(parent: JKElement?): Nullability {
        return when (parent) {
            is JKJavaMethod -> typeFlavorCalculator.methodNullability(parent.psi as PsiMethod)
            is JKJavaField -> typeFlavorCalculator.variableNullability(parent.psi as PsiVariable)
            is JKLocalVariable -> typeFlavorCalculator.variableNullability(parent.psi as PsiVariable)
            else -> Nullability.Default
        }
    }

    private fun arrayFqName(type: JKType): String = if (type is JKJavaPrimitiveType)
        PrimitiveType.valueOf(type.jvmPrimitiveType.name).arrayTypeFqName.asString()
    else KotlinBuiltIns.FQ_NAMES.array.asString()
}
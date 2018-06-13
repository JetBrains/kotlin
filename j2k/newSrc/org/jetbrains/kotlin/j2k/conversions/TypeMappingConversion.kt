/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.JKClassSymbol
import org.jetbrains.kotlin.j2k.tree.impl.JKClassTypeImpl
import org.jetbrains.kotlin.j2k.tree.impl.JKJavaVoidType
import org.jetbrains.kotlin.j2k.tree.impl.JKTypeElementImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.psi.KtClassOrObject

class TypeMappingConversion(val context: ConversionContext) : RecursiveApplicableConversionBase() {

    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        return if (element is JKTypeElement) {
            val type = element.type
            when (type) {
                is JKJavaPrimitiveType -> mapPrimitiveType(type, element)
                is JKClassType -> mapClassType(type, element)
                is JKJavaVoidType -> classTypeByFqName(
                    context.backAnnotator(element), ClassId.fromString("kotlin.Unit"), emptyList(), Nullability.NotNull
                )?.let { JKTypeElementImpl(it) } ?: element
                else -> applyRecursive(element, this::applyToElement)
            }
        } else applyRecursive(element, this::applyToElement)
    }

    private fun classTypeByFqName(
        contextElement: PsiElement?,
        fqName: ClassId,
        parameters: List<JKType>,
        nullability: Nullability = Nullability.Default
    ): JKType? {
        contextElement ?: return null
        val newTarget = resolveFqName(fqName, contextElement) as? KtClassOrObject ?: return null

        return JKClassTypeImpl(context.symbolProvider.provideDirectSymbol(newTarget) as JKClassSymbol, parameters, nullability)
    }

    private fun mapClassType(type: JKClassType, typeElement: JKTypeElement): JKTypeElement {
        val fqNameStr = (type.classReference as? JKClassSymbol)?.fqName ?: return typeElement

        val newFqName = JavaToKotlinClassMap.mapJavaToKotlin(FqName(fqNameStr)) ?: return typeElement

        return classTypeByFqName(context.backAnnotator(typeElement), newFqName, type.parameters, type.nullability)?.let {
            JKTypeElementImpl(it)
        } ?: typeElement
    }

    private fun mapPrimitiveType(type: JKJavaPrimitiveType, typeElement: JKTypeElement): JKTypeElement {
        val fqName = type.jvmPrimitiveType.primitiveType.typeFqName

        val convertedType = classTypeByFqName(
            context.backAnnotator(typeElement),
            ClassId.topLevel(fqName),
            emptyList(),
            nullability = Nullability.NotNull
        ) ?: return typeElement

        return JKTypeElementImpl(convertedType)
    }
}
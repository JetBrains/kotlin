/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.types.JKClassType
import org.jetbrains.kotlin.nj2k.types.JKJavaArrayType
import org.jetbrains.kotlin.nj2k.types.updateNullability


//TODO temporary
class MainFunctionConversion(context: NewJ2kConverterContext) : RecursiveApplicableConversionBase(context) {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKMethod) return recurse(element)
        if (element.isMainFunctionDeclaration()) {
            element.parameters.single().apply {
                val oldType = type.type as JKJavaArrayType
                val oldTypeParameter = oldType.type as JKClassType
                val newType =
                    JKJavaArrayType(
                        oldTypeParameter.updateNullability(Nullability.NotNull),
                        Nullability.NotNull
                    )
                type = JKTypeElement(newType)
            }
            element.annotationList.annotations +=
                JKAnnotation(
                    symbolProvider.provideClassSymbol("kotlin.jvm.JvmStatic"),
                    emptyList()
                )
        }
        return recurse(element)
    }

    private fun JKMethod.isMainFunctionDeclaration(): Boolean {
        val type = parameters.singleOrNull()?.type?.type as? JKJavaArrayType ?: return false
        val typeArgument = type.type as? JKClassType ?: return false
        return name.value == "main" && typeArgument.classReference.name == "String"
    }
}
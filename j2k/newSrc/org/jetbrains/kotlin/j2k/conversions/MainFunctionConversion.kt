/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.*
import org.jetbrains.kotlin.util.collectionUtils.concatInOrder


//TODO temporary
class MainFunctionConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKMethod) return recurse(element)
        if (element.isMainFunctionDeclaration()) {
            element.parameters.single().apply {
                val oldType = type.type as JKJavaArrayType
                val oldTypeParameter = oldType.type as JKClassType
                val newType =
                    JKJavaArrayTypeImpl(
                        oldTypeParameter.updateNullability(Nullability.NotNull),
                        Nullability.NotNull
                    )
                type = JKTypeElementImpl(newType)
            }
            element.annotationList.annotations +=
                    JKAnnotationImpl(
                        context.symbolProvider.provideByFqName("kotlin.jvm.JvmStatic"),
                        JKExpressionListImpl()
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
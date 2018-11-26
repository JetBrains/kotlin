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
        if (element !is JKKtFunction) return recurse(element)
        if (element.isMainFunctionDeclaration()) {
            element.parameters.single().apply {
                val oldType = type.type as JKClassType
                val oldTypeParameter = oldType.parameters.single() as JKClassType
                val newType =
                    JKClassTypeImpl(
                        oldType.classReference,
                        listOf(oldTypeParameter.updateNullability(Nullability.NotNull)),
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

    fun JKKtFunction.isMainFunctionDeclaration(): Boolean {
        val type = parameters.singleOrNull()?.type?.type as? JKClassType ?: return false
        val typeArgument = type.parameters.singleOrNull() as? JKClassType ?: return false
        return name.value == "main" &&
                type.classReference.name == "Array" &&
                typeArgument.classReference.name == "String"
    }
}
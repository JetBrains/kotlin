/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.jvmAnnotation
import org.jetbrains.kotlin.nj2k.tree.JKMethod
import org.jetbrains.kotlin.nj2k.tree.JKTreeElement
import org.jetbrains.kotlin.nj2k.tree.OtherModifier
import org.jetbrains.kotlin.nj2k.tree.hasOtherModifier
import org.jetbrains.kotlin.nj2k.types.JKJavaArrayType
import org.jetbrains.kotlin.nj2k.types.arrayInnerType
import org.jetbrains.kotlin.nj2k.types.isStringType

class MainFunctionConversion(context: NewJ2kConverterContext) : RecursiveApplicableConversionBase(context) {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKMethod) return recurse(element)
        if (element.isMainFunctionDeclaration()) {
            element.parameters.single().apply {
                type.type = JKJavaArrayType(typeFactory.types.string, Nullability.NotNull)
                isVarArgs = false
            }
            element.annotationList.annotations += jvmAnnotation("JvmStatic", symbolProvider)
        }
        return recurse(element)
    }

    private fun JKMethod.isMainFunctionDeclaration(): Boolean {
        if (name.value != "main") return false
        if (!hasOtherModifier(OtherModifier.STATIC)) return false
        val parameter = parameters.singleOrNull() ?: return false
        return when {
            parameter.type.type.arrayInnerType()?.isStringType() == true -> true
            parameter.isVarArgs && parameter.type.type.isStringType() -> true
            else -> false
        }
    }
}
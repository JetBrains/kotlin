/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.throwAnnotation
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.JKKtFunctionImpl
import org.jetbrains.kotlin.nj2k.tree.impl.psi

class JavaMethodToKotlinFunctionConversion(private val context: NewJ2kConverterContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKJavaMethod) return recurse(element)

        element.invalidate()
        val kotlinFunction = JKKtFunctionImpl(
            element.returnType,
            element.name,
            element.parameters,
            element.block,
            element.typeParameterList,
            element.annotationList.also {
                if (element.throwsList.isNotEmpty()) {
                    it.annotations +=
                        throwAnnotation(
                            element.throwsList.map { it.type.updateNullabilityRecursively(Nullability.NotNull) },
                            context.symbolProvider
                        )
                }
            },
            element.otherModifierElements,
            element.visibilityElement,
            element.modalityElement
        ).also {
            it.psi = element.psi
            context.symbolProvider.transferSymbol(it, element)
            it.leftParen.takeNonCodeElementsFrom(element.leftParen)
            it.rightParen.takeNonCodeElementsFrom(element.rightParen)
        }.withNonCodeElementsFrom(element)
        return recurse(kotlinFunction)
    }
}
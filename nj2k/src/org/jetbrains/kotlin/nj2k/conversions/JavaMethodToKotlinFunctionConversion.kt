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

import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.nj2k.externalCodeProcessing.JKMethodData
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.psi
import org.jetbrains.kotlin.nj2k.throwAnnotation
import org.jetbrains.kotlin.nj2k.tree.JKMethodImpl
import org.jetbrains.kotlin.nj2k.tree.JKTreeElement
import org.jetbrains.kotlin.nj2k.types.updateNullabilityRecursively

class JavaMethodToKotlinFunctionConversion(context: NewJ2kConverterContext) : RecursiveApplicableConversionBase(context) {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKMethodImpl) return recurse(element)

        if (element.throwsList.isNotEmpty()) {
            element.annotationList.annotations +=
                throwAnnotation(
                    element.throwsList.map { it.type.updateNullabilityRecursively(Nullability.NotNull) },
                    symbolProvider
                )
        }

        element.psi<PsiMethod>()?.let { psi ->
            context.externalCodeProcessor.addMember(JKMethodData(psi))
        }

        return recurse(element)
    }
}
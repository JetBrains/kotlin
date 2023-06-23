/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.kapt4

import com.intellij.psi.*

internal class ParameterInfo(
    val flags: Long,
    val name: String,
    val type: PsiType,
    val visibleAnnotations: List<PsiAnnotation>,
    val invisibleAnnotations: List<PsiAnnotation>
)

internal fun PsiMethod.getParametersInfo(): List<ParameterInfo> {
    val typeConverter = JvmPsiConversionHelper.getInstance(project)
    return this.parameterList.parameters.map {
        ParameterInfo(0, it.name, typeConverter.convertType(it.type), it.annotations.asList(), emptyList())
    }
}

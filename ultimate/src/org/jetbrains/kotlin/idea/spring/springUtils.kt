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

package org.jetbrains.kotlin.idea.spring

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierListOwner
import com.intellij.spring.CommonSpringModel
import com.intellij.spring.model.jam.utils.JamAnnotationTypeUtil
import com.intellij.spring.model.utils.SpringModelUtils

internal fun PsiModifierListOwner.isAnnotatedWith(annotationFqName: String): Boolean {
    val module = ModuleUtilCore.findModuleForPsiElement(this) ?: return false
    return JamAnnotationTypeUtil.getInstance(module)
            .getAnnotationTypesWithChildren(annotationFqName)
            .mapNotNull { it.qualifiedName }
            .any { AnnotationUtil.isAnnotated(this, it, true) }
}

internal val PsiElement.springModel: CommonSpringModel?
    get() = SpringModelUtils.getInstance().getSpringModel(this)
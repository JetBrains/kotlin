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

package org.jetbrains.kotlin.java.model.internal

import com.intellij.lang.Language
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiModifier.*
import com.intellij.psi.impl.light.LightMethodBuilder

internal class DefaultConstructorPsiMethod(
        clazz: PsiClass,
        language: Language
) : LightMethodBuilder(clazz, language) {
    init {
        val modifier = when {
            clazz.hasModifierProperty(PUBLIC) -> PUBLIC
            clazz.hasModifierProperty(PRIVATE) -> PRIVATE
            clazz.hasModifierProperty(PROTECTED) -> PROTECTED
            else -> PACKAGE_LOCAL
        }
        setModifiers(modifier)
        isConstructor = true
    }
}
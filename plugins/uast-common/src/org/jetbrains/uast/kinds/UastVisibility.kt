/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.jetbrains.uast

import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierListOwner

enum class UastVisibility(val text: String) {
    PUBLIC("public"),
    PRIVATE("private"),
    PROTECTED("protected"), 
    PACKAGE_LOCAL("packageLocal"), 
    LOCAL("local");

    override fun toString() = text
    
    companion object {
        operator fun get(declaration: PsiModifierListOwner): UastVisibility {
            if (declaration.hasModifierProperty(PsiModifier.PUBLIC)) return UastVisibility.PUBLIC
            if (declaration.hasModifierProperty(PsiModifier.PROTECTED)) return UastVisibility.PROTECTED
            if (declaration.hasModifierProperty(PsiModifier.PRIVATE)) return UastVisibility.PRIVATE
            if (declaration is PsiLocalVariable) return UastVisibility.LOCAL
            return UastVisibility.PACKAGE_LOCAL
        }
    }
}
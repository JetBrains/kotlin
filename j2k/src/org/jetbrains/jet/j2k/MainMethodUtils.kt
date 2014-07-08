/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.j2k

import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import java.util.ArrayList
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiType
import com.intellij.psi.PsiArrayType

fun createMainFunction(file: PsiJavaFile): String {
    val classNamesWithMains = ArrayList<Pair<String, PsiMethod>>()
    for (c in file.getClasses()) {
        val main = findMainMethod(c)
        val name = c.getName()
        if (name != null && main != null) {
            classNamesWithMains.add(name to main)
        }
    }

    if (classNamesWithMains.isNotEmpty()) {
        var className = classNamesWithMains.first().first
        return "fun main(args : Array<String>) = $className.main(args)"
    }

    return ""
}

private fun findMainMethod(aClass: PsiClass): PsiMethod?
        = if (isMainClass(aClass)) aClass.findMethodsByName("main", false).firstOrNull { it.isMainMethod() } else null

private fun isMainClass(psiClass: PsiClass): Boolean
        = psiClass !is PsiAnonymousClass &&
            !psiClass.isInterface() &&
            (psiClass.getContainingClass() == null || psiClass.hasModifierProperty(PsiModifier.STATIC))

fun PsiMethod.isMainMethod(): Boolean {
    if (getReturnType() != PsiType.VOID) return false
    if (!hasModifierProperty(PsiModifier.STATIC)) return false
    if (!hasModifierProperty(PsiModifier.PUBLIC)) return false

    val parameters = getParameterList().getParameters()
    if (parameters.size != 1) return false

    val `type` = parameters.single().getType()
    if (`type` !is PsiArrayType) return false

    val componentType = `type`.getComponentType()
    return componentType.equalsToText("java.lang.String")
}

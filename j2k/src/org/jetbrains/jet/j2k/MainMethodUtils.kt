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

import java.text.MessageFormat
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import java.util.ArrayList
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiType
import com.intellij.psi.PsiArrayType

fun createMainFunction(file: PsiFile): String {
    val classNamesWithMains = ArrayList<Pair<String, PsiMethod>>()
    for (c in (file as PsiJavaFile).getClasses()) {
        val main = findMainMethod(c)
        val name = c.getName()
        if (name != null && main != null) {
            classNamesWithMains.add(Pair(name, main))
        }
    }

    if (classNamesWithMains.size() > 0) {
        var className = classNamesWithMains[0].first
        return MessageFormat.format("fun main(args : Array<String>) = {0}.main(args as Array<String?>?)", className)
    }

    return ""

}

private fun findMainMethod(aClass: PsiClass): PsiMethod? {
    if (isMainClass(aClass)) {
        return findMainMethod(aClass.findMethodsByName("main", false))
    }
    return null
}

private fun isMainClass(psiClass: PsiClass): Boolean {
    if (psiClass is PsiAnonymousClass)
        return false

    if (psiClass.isInterface())
        return false

    return psiClass.getContainingClass() == null || psiClass.hasModifierProperty(PsiModifier.STATIC)

}

private fun findMainMethod(mainMethods: Array<PsiMethod>): PsiMethod? {
    return mainMethods.find { isMainMethod(it) }
}

fun isMainMethod(method: PsiMethod): Boolean {
    if (method.getContainingClass() == null)
        return false

    if (PsiType.VOID != method.getReturnType())
        return false

    if (!method.hasModifierProperty(PsiModifier.STATIC))
        return false

    if (!method.hasModifierProperty(PsiModifier.PUBLIC))
        return false

    val parameters = method.getParameterList().getParameters()
    if (parameters.size != 1)
        return false

    val `type` = parameters[0].getType()
    if (`type` !is PsiArrayType)
        return false

    val componentType = `type`.getComponentType()
    return componentType.equalsToText("java.lang.String")
}


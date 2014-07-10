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
import com.intellij.psi.util.PsiMethodUtil

fun createMainFunction(file: PsiJavaFile): String {
    for (`class` in file.getClasses()) {
        val mainMethod = PsiMethodUtil.findMainMethod(`class`)
        if (mainMethod != null) {
            return "fun main(args: Array<String>) = ${`class`.getName()}.${mainMethod.getName()}(args)"
        }
    }
    return ""
}

fun PsiMethod.isMainMethod(): Boolean = PsiMethodUtil.isMainMethod(this)
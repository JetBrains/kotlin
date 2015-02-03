/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.android

import com.intellij.psi.PsiFile
import com.intellij.psi.PsiElement
import com.intellij.openapi.project.Project
import com.intellij.openapi.components.ServiceManager
import com.intellij.psi.PsiField
import com.intellij.psi.PsiClass
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.module.ModuleServiceManager

fun isAndroidSyntheticFile(f: PsiFile?): Boolean {
    return f?.getUserData(AndroidConst.ANDROID_USER_PACKAGE) != null
}

public fun isAndroidSyntheticElement(element: PsiElement?): Boolean {
    return isAndroidSyntheticFile(ApplicationManager.getApplication().runReadAction(Computable {
        element?.getContainingFile()
    }))
}

/*
 * Copyright 2000-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.util

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.builtins.KotlinBuiltInsNames
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.runWithAlternativeResolveEnabled

fun getKotlinJvmRuntimeMarkerClass(project: Project, scope: GlobalSearchScope): PsiClass? {
    return runReadAction {
        project.runWithAlternativeResolveEnabled {
            JavaPsiFacade.getInstance(project).findClass(KotlinBuiltInsNames.FqNames.unit.asString(), scope)
        }
    }
}

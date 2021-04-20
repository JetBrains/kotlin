/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin.internal

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.psi.KtFile

internal val PsiElement.isJvmElement: Boolean
    get() {
        if (allModulesSupportJvm(project)) return true

        val containingFile = containingFile
        if (containingFile is KtFile) {
            return TargetPlatformDetector.getPlatform(containingFile).isJvm()
        }

        return module == null || TargetPlatformDetector.getPlatform(module!!).isJvm()
    }

private fun allModulesSupportJvm(project: Project): Boolean =
    CachedValuesManager.getManager(project)
        .getCachedValue(project) {
            CachedValueProvider.Result.create(
                ModuleManager.getInstance(project).modules.all { module ->
                    TargetPlatformDetector.getPlatform(module).isJvm()
                },
                ProjectRootModificationTracker.getInstance(project)
            )
        }

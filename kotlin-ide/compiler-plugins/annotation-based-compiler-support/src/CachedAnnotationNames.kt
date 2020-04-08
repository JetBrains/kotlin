/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.annotation.plugin.ide

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.psi.KtElement
import java.util.concurrent.ConcurrentMap

fun CachedAnnotationNames.getAnnotationNames(element: KtElement?): List<String> {
    if (element === null) return emptyList()
    val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyList()
    return getNamesForModule(module)
}

class CachedAnnotationNames(project: Project, private val annotationOptionPrefix: String) {

    private val cache: CachedValue<ConcurrentMap<Module, List<String>>> = cachedValue(project) {
        CachedValueProvider.Result.create(
            ContainerUtil.createConcurrentWeakMap<Module, List<String>>(),
            ProjectRootModificationTracker.getInstance(project)
        )
    }

    fun getNamesForModule(module: Module): List<String> {
        return cache.value.getOrPut(module) { module.getSpecialAnnotations(annotationOptionPrefix) }
    }

    private fun <T> cachedValue(project: Project, result: () -> CachedValueProvider.Result<T>): CachedValue<T> {
        return CachedValuesManager.getManager(project).createCachedValue(result, false)
    }
}
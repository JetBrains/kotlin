/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve.util

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.caches.project.*
import org.jetbrains.kotlin.idea.configuration.IdeBuiltInsLoadingState

internal interface ModuleFilters {
    fun sdkFacadeFilter(module: IdeaModuleInfo): Boolean
    fun libraryFacadeFilter(module: IdeaModuleInfo): Boolean
    fun moduleFacadeFilter(module: IdeaModuleInfo): Boolean
}

private object ClassLoaderBuiltInsModuleFilters : ModuleFilters {
    override fun sdkFacadeFilter(module: IdeaModuleInfo): Boolean = module is SdkInfo
    override fun libraryFacadeFilter(module: IdeaModuleInfo): Boolean = module is LibraryInfo
    override fun moduleFacadeFilter(module: IdeaModuleInfo): Boolean = !module.isLibraryClasses()
}

private class DependencyBuiltinsModuleFilters(private val project: Project) : ModuleFilters {
    override fun sdkFacadeFilter(module: IdeaModuleInfo): Boolean =
        module is SdkInfo || module is LibraryInfo && module.isCoreKotlinLibrary(project)

    override fun libraryFacadeFilter(module: IdeaModuleInfo): Boolean =
        module is LibraryInfo && !module.isCoreKotlinLibrary(project)

    override fun moduleFacadeFilter(module: IdeaModuleInfo): Boolean = !module.isLibraryClasses()
}

internal class GlobalFacadeModuleFilters(project: Project) : ModuleFilters {
    private val impl = when (IdeBuiltInsLoadingState.state) {
        IdeBuiltInsLoadingState.IdeBuiltInsLoading.FROM_CLASSLOADER -> ClassLoaderBuiltInsModuleFilters
        IdeBuiltInsLoadingState.IdeBuiltInsLoading.FROM_DEPENDENCIES_JVM -> DependencyBuiltinsModuleFilters(project)
    }

    override fun sdkFacadeFilter(module: IdeaModuleInfo): Boolean = impl.sdkFacadeFilter(module)
    override fun libraryFacadeFilter(module: IdeaModuleInfo): Boolean = impl.libraryFacadeFilter(module)
    override fun moduleFacadeFilter(module: IdeaModuleInfo): Boolean = impl.moduleFacadeFilter(module)
}
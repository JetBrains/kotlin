/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.project

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.JdkOrderEntry
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.libraries.Library
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.caches.project.cacheInvalidatingOnRootModifications
import org.jetbrains.kotlin.idea.util.getProjectJdkTableSafe
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus.checkCanceled
import org.jetbrains.kotlin.types.typeUtil.closure
import java.util.concurrent.ConcurrentHashMap

/** null-platform means that we should get all modules */
fun getModuleInfosFromIdeaModel(project: Project, platform: TargetPlatform? = null): List<IdeaModuleInfo> {
    val ideaModelInfosCache = getIdeaModelInfosCache(project)

    return if (platform != null)
        ideaModelInfosCache.forPlatform(platform)
    else
        ideaModelInfosCache.allModules()
}

fun getIdeaModelInfosCache(project: Project): IdeaModelInfosCache = project.cacheInvalidatingOnRootModifications {
    collectModuleInfosFromIdeaModel(project)
}

class IdeaModelInfosCache(
    private val moduleSourceInfosByModules: MultiMap<Module, ModuleSourceInfo>,
    private val libraryInfosByLibraries: MultiMap<Library, LibraryInfo>,
    private val sdkInfosBySdks: Map<Sdk, SdkInfo>,
) {
    private val resultByPlatform = ConcurrentHashMap<TargetPlatform, List<IdeaModuleInfo>>()

    private val moduleSourceInfos = moduleSourceInfosByModules.values().toList()
    private val libraryInfos = libraryInfosByLibraries.values().toList()
    private val sdkInfos = sdkInfosBySdks.values.toList()

    fun forPlatform(platform: TargetPlatform): List<IdeaModuleInfo> {
        return resultByPlatform.getOrPut(platform) {
            mergePlatformModules(moduleSourceInfos, platform) + libraryInfos + sdkInfos
        }
    }

    fun allModules(): List<IdeaModuleInfo> = moduleSourceInfos + libraryInfos + sdkInfos

    fun getModuleInfosForModule(module: Module): Collection<ModuleSourceInfo> = moduleSourceInfosByModules[module]
    fun getLibraryInfosForLibrary(library: Library): Collection<LibraryInfo> = libraryInfosByLibraries[library]
    fun getSdkInfoForSdk(sdk: Sdk): SdkInfo? = sdkInfosBySdks[sdk]
}

private fun collectModuleInfosFromIdeaModel(
    project: Project
): IdeaModelInfosCache {
    val ideaModules = ModuleManager.getInstance(project).modules.toList()

    //TODO: (module refactoring) include libraries that are not among dependencies of any module
    val ideaLibraries = ideaModules.flatMap {
        ModuleRootManager.getInstance(it).orderEntries.filterIsInstance<LibraryOrderEntry>().map {
            it.library
        }
    }.filterNotNull().toSet()

    val sdksFromModulesDependencies = ideaModules.flatMap {
        ModuleRootManager.getInstance(it).orderEntries.filterIsInstance<JdkOrderEntry>().map {
            it.jdk
        }
    }

    return IdeaModelInfosCache(
        moduleSourceInfosByModules = MultiMap.create<Module, ModuleSourceInfo>().also { moduleInfosByModules ->
            for (module in ideaModules) {
                checkCanceled()
                moduleInfosByModules.putValues(module, module.correspondingModuleInfos())
            }
        },
        libraryInfosByLibraries = MultiMap.create<Library, LibraryInfo>().also { libraryInfosByLibraries ->
            for (library in ideaLibraries) {
                checkCanceled()
                libraryInfosByLibraries.putValues(library, createLibraryInfo(project, library))
            }
        },
        sdkInfosBySdks = (sdksFromModulesDependencies + getAllProjectSdks()).filterNotNull().toSet().associateWith { SdkInfo(project, it) }
    )
}

private fun mergePlatformModules(
    allModules: List<ModuleSourceInfo>,
    platform: TargetPlatform
): List<IdeaModuleInfo> {
    if (platform.isCommon()) return allModules

    val platformModules =
        allModules.flatMap { module ->
            if (module.platform == platform && module.expectedBy.isNotEmpty())
                listOf(module to module.expectedBy)
            else emptyList()
        }.map { (module, expectedBys) ->
            PlatformModuleInfo(module, expectedBys.closure(preserveOrder = true) { it.expectedBy }.toList())
        }

    val rest = allModules - platformModules.flatMap { it.containedModules }
    return rest + platformModules
}

fun getAllProjectSdks(): Array<Sdk> = getProjectJdkTableSafe().allJdks

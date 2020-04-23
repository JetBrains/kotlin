/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.ModuleResolverTracker
import org.jetbrains.kotlin.caches.project.cacheInvalidatingOnRootModifications
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.caches.project.*
import org.jetbrains.kotlin.idea.project.useAnchorServices
import org.jetbrains.kotlin.resolve.AnchorProvider

@State(name = "KotlinIdeAnchorService", storages = [Storage("anchors.xml")])
class KotlinIdeAnchorService(
    val project: Project
) : AnchorProvider(), 
    PersistentStateComponent<KotlinIdeAnchorService.State> {

    data class State(
        var moduleNameToAnchorName: Map<String, String> = emptyMap()
    )

    @JvmField
    var myState: State = State()

    private fun buildMapping(): Map<ModuleInfo, ModuleInfo> {
        val modulesByNames = getModuleInfosFromIdeaModel(project).associateBy { moduleInfo -> 
            when (moduleInfo) {
                is LibraryInfo -> moduleInfo.library.name
                is ModuleSourceInfo -> moduleInfo.module.name
                else -> moduleInfo.name.asString()
            }
        }

        return myState.moduleNameToAnchorName.entries.mapNotNull { (moduleName, anchorName) ->
            val module = modulesByNames[moduleName] ?: return@mapNotNull notFoundModule(moduleName)
            val anchor = modulesByNames[anchorName] ?: return@mapNotNull notFoundModule(moduleName)
            module to anchor
        }.toMap()
    }
    
    private fun notFoundModule(moduleName: String): Nothing? {
        logger<KotlinIdeAnchorService>().warn("Module <${moduleName}> not found in project model")
        return null
    }

    private val moduleToAnchor: Map<ModuleInfo, ModuleInfo>
        get() = project.cacheInvalidatingOnRootModifications {
            buildMapping()
        }

    private fun getAnchorModuleDescriptorIfAny(moduleInfo: ModuleInfo): ModuleDescriptor? {
        val mapped = moduleToAnchor[moduleInfo] ?: return null
        return KotlinCacheService.getInstance(project)
            .getResolutionFacadeByModuleInfo(mapped, mapped.platform)
            ?.moduleDescriptor
    }

    @Synchronized
    override fun getState(): State = myState

    @Synchronized
    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, myState)
    }

    @Synchronized
    override fun getAnchor(moduleDescriptor: ModuleDescriptor): ModuleDescriptor? {
        if (!project.useAnchorServices) return null
        val resolver = ModuleResolverTracker.getInstance(project)
            .findResolverForProjectByModuleDescriptor(moduleDescriptor)
            ?: return null
        return getAnchorModuleDescriptorIfAny(resolver.moduleInfoForModuleDescriptor(moduleDescriptor))
    }
}

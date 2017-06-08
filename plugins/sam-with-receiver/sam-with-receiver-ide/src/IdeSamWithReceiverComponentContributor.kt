/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.samWithReceiver.ide

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.psi.util.CachedValueProvider.*
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.annotation.plugin.ide.getSpecialAnnotations
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.idea.caches.resolve.ModuleProductionSourceInfo
import org.jetbrains.kotlin.idea.caches.resolve.ScriptDependenciesModuleInfo
import org.jetbrains.kotlin.idea.caches.resolve.ScriptModuleInfo
import org.jetbrains.kotlin.load.java.sam.SamWithReceiverResolver
import org.jetbrains.kotlin.samWithReceiver.SamWithReceiverResolverExtension
import org.jetbrains.kotlin.samWithReceiver.SamWithReceiverCommandLineProcessor.Companion.PLUGIN_ID
import org.jetbrains.kotlin.samWithReceiver.SamWithReceiverCommandLineProcessor.Companion.ANNOTATION_OPTION
import java.util.*

class IdeSamWithReceiverComponentContributor(val project: Project) : StorageComponentContainerContributor {
    private companion object {
        val ANNOTATION_OPTION_PREFIX = "plugin:$PLUGIN_ID:${ANNOTATION_OPTION.name}="
    }

    private val cache = CachedValuesManager.getManager(project).createCachedValue({
        Result.create(WeakHashMap<Module, List<String>>(), ProjectRootModificationTracker.getInstance(project))
    }, /* trackValue = */ false)

    private fun getAnnotationsForModule(module: Module): List<String> {
        return cache.value.getOrPut(module) { module.getSpecialAnnotations(ANNOTATION_OPTION_PREFIX) }
    }

    override fun onContainerComposed(container: ComponentProvider, moduleInfo: ModuleInfo?) {
        val annotations = when (moduleInfo) {
            is ScriptModuleInfo -> moduleInfo.scriptDefinition.annotationsForSamWithReceivers
            is ScriptDependenciesModuleInfo -> moduleInfo.scriptModuleInfo?.scriptDefinition?.annotationsForSamWithReceivers
            is ModuleProductionSourceInfo -> getAnnotationsForModule(moduleInfo.module)
            else -> null
        } ?: return

        container.get<SamWithReceiverResolver>().registerExtension(SamWithReceiverResolverExtension(annotations))
    }
}
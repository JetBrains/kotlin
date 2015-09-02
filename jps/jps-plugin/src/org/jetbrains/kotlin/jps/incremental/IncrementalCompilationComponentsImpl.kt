/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.jps.incremental

import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.Module

public class IncrementalCompilationComponentsImpl(
        caches: Map<ModuleBuildTarget, IncrementalCache>,
        private val lookupTracker: LookupTracker
): IncrementalCompilationComponents {
    private val caches = caches.mapKeys { ModuleToModuleBuildTargetAdapter(it.key) }

    override fun getIncrementalCache(target: Module): IncrementalCache =
            caches[target]!!

    override fun getLookupTracker(): LookupTracker = lookupTracker
}

public class ModuleToModuleBuildTargetAdapter(
        private val moduleBuildTarget: ModuleBuildTarget
) : Module {

    override fun getModuleName(): String =
            moduleBuildTarget.id

    override fun getModuleType(): String =
            moduleBuildTarget.targetType.typeId

    override fun getAnnotationsRoots(): List<String> =
            throw UnsupportedOperationException()

    override fun getClasspathRoots(): List<String> =
            throw UnsupportedOperationException()

    override fun getJavaSourceRoots(): List<String> =
            throw UnsupportedOperationException()

    override fun getOutputDirectory(): String =
            throw UnsupportedOperationException()

    override fun getSourceFiles(): List<String> =
            throw UnsupportedOperationException()
}

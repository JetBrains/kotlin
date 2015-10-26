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

import org.jetbrains.jps.builders.*
import org.jetbrains.jps.builders.storage.BuildDataPaths
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.indices.IgnoredFileIndex
import org.jetbrains.jps.indices.ModuleExcludeIndex
import org.jetbrains.jps.model.JpsModel
import java.io.File

private val KOTLIN_LOOKUP_TRACKER = "kotlin-lookup-tracker"

object LOOKUP_TRACKER_TARGET_TYPE : BuildTargetType<LOOKUP_TRACKER_TARGET>(KOTLIN_LOOKUP_TRACKER) {
    override fun computeAllTargets(model: JpsModel): List<LOOKUP_TRACKER_TARGET> = listOf(LOOKUP_TRACKER_TARGET)

    override fun createLoader(model: JpsModel): BuildTargetLoader<LOOKUP_TRACKER_TARGET> =
            object : BuildTargetLoader<LOOKUP_TRACKER_TARGET>() {
                override fun createTarget(targetId: String): LOOKUP_TRACKER_TARGET? = LOOKUP_TRACKER_TARGET
            }
}

object LOOKUP_TRACKER_TARGET : BuildTarget<BuildRootDescriptor>(LOOKUP_TRACKER_TARGET_TYPE) {
    override fun getId(): String? = KOTLIN_LOOKUP_TRACKER
    override fun getPresentableName(): String = KOTLIN_LOOKUP_TRACKER

    override fun computeRootDescriptors(
            model: JpsModel?,
            index: ModuleExcludeIndex?,
            ignoredFileIndex: IgnoredFileIndex?,
            dataPaths: BuildDataPaths?
    ): List<BuildRootDescriptor> = listOf()

    override fun getOutputRoots(context: CompileContext): Collection<File> {
        val dataManager = context.projectDescriptor.dataManager
        val storageRoot = dataManager.dataPaths.dataStorageRoot
        return listOf(File(storageRoot, KOTLIN_LOOKUP_TRACKER))
    }

    override fun findRootDescriptor(rootId: String?, rootIndex: BuildRootIndex?): BuildRootDescriptor? = null

    override fun computeDependencies(
            targetRegistry: BuildTargetRegistry?,
            outputIndex: TargetOutputIndex?
    ): Collection<BuildTarget<*>>? = listOf()
}

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

import org.jetbrains.jps.builders.BuildTarget
import org.jetbrains.jps.builders.storage.BuildDataPaths
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.kotlin.incremental.CacheVersion
import org.jetbrains.kotlin.incremental.dataContainerCacheVersion
import org.jetbrains.kotlin.incremental.normalCacheVersion
import java.io.File


class CacheVersionProvider(private val paths: BuildDataPaths) {
    private val BuildTarget<*>.dataRoot: File
        get() = paths.getTargetDataRoot(this)

    fun normalVersion(target: ModuleBuildTarget): CacheVersion = normalCacheVersion(target.dataRoot)

    fun dataContainerVersion(): CacheVersion = dataContainerCacheVersion(KotlinDataContainerTarget.dataRoot)

    fun allVersions(targets: Iterable<ModuleBuildTarget>): Iterable<CacheVersion> {
        val versions = arrayListOf<CacheVersion>()
        versions.add(dataContainerCacheVersion(KotlinDataContainerTarget.dataRoot))

        for (dataRoot in targets.map { it.dataRoot }) {
            versions.add(normalCacheVersion(dataRoot))
        }

        return versions
    }
}
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

package org.jetbrains.kotlin.jps.incremental

import org.jetbrains.jps.builders.storage.BuildDataPaths
import org.jetbrains.jps.builders.storage.StorageProvider
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.jps.incremental.storage.BuildDataManager
import org.jetbrains.jps.incremental.storage.StorageOwner
import org.jetbrains.kotlin.incremental.IncrementalJvmCache
import org.jetbrains.kotlin.jps.build.KotlinBuilder
import java.io.File

class JpsIncrementalJvmCache(
        target: ModuleBuildTarget,
        paths: BuildDataPaths
) : IncrementalJvmCache(paths.getTargetDataRoot(target), target.outputDir), StorageOwner {
    override fun debugLog(message: String) {
        KotlinBuilder.LOG.debug(message)
    }
}

private class KotlinIncrementalStorageProvider(
        private val target: ModuleBuildTarget,
        private val paths: BuildDataPaths
) : StorageProvider<JpsIncrementalJvmCache>() {

    override fun equals(other: Any?) = other is KotlinIncrementalStorageProvider && target == other.target

    override fun hashCode() = target.hashCode()

    override fun createStorage(targetDataDir: File): JpsIncrementalJvmCache =
            JpsIncrementalJvmCache(target, paths)
}

fun BuildDataManager.getKotlinCache(target: ModuleBuildTarget): JpsIncrementalJvmCache =
        getStorage(target, KotlinIncrementalStorageProvider(target, dataPaths))


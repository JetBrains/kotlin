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

import org.jetbrains.jps.builders.storage.BuildDataPaths
import org.jetbrains.jps.builders.storage.StorageProvider
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.jps.incremental.storage.BuildDataManager
import org.jetbrains.jps.incremental.storage.PathStringDescriptor
import org.jetbrains.jps.incremental.storage.StorageOwner
import org.jetbrains.kotlin.incremental.BasicIncrementalCacheImpl
import org.jetbrains.kotlin.incremental.dumpCollection
import org.jetbrains.kotlin.incremental.storage.BasicStringMap
import org.jetbrains.kotlin.incremental.storage.StringCollectionExternalizer
import org.jetbrains.kotlin.jps.build.KotlinBuilder
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.io.File

val KOTLIN_CACHE_DIRECTORY_NAME = "kotlin"

class IncrementalCacheImpl(
        target: ModuleBuildTarget,
        paths: BuildDataPaths
) : StorageOwner, BasicIncrementalCacheImpl<ModuleBuildTarget>(paths.getTargetDataRoot(target), target.outputDir, target) {

    override fun makeSourceToClassesMap(): SourceToClassesMapInterface = registerMap(PathSourceToClassesMap(SOURCE_TO_CLASSES.storageFile))

    override fun debugLog(message: String) {
        KotlinBuilder.LOG.debug(message)
    }


    protected inner class PathSourceToClassesMap(storageFile: File) : SourceToClassesMapInterface, BasicStringMap<Collection<String>>(storageFile, PathStringDescriptor.INSTANCE, StringCollectionExternalizer) {
        override fun clearOutputsForSource(sourceFile: File) {
            remove(sourceFile.absolutePath)
        }

        override fun add(sourceFile: File, className: JvmClassName) {
            storage.append(sourceFile.absolutePath, className.internalName)
        }

        override operator fun get(sourceFile: File): Collection<JvmClassName> =
                storage[sourceFile.absolutePath].orEmpty().map { JvmClassName.byInternalName(it) }

        override fun dumpValue(value: Collection<String>) = value.dumpCollection()

        override fun clean() {
            storage.keys.forEach { remove(it) }
        }

        private fun remove(path: String) {
            storage.remove(path)
        }
    }
}

private class KotlinIncrementalStorageProvider(
        private val target: ModuleBuildTarget,
        private val paths: BuildDataPaths
) : StorageProvider<IncrementalCacheImpl>() {

    override fun equals(other: Any?) = other is KotlinIncrementalStorageProvider && target == other.target

    override fun hashCode() = target.hashCode()

    override fun createStorage(targetDataDir: File): IncrementalCacheImpl =
            IncrementalCacheImpl(target, paths)
}

fun BuildDataManager.getKotlinCache(target: ModuleBuildTarget): IncrementalCacheImpl =
        getStorage(target, KotlinIncrementalStorageProvider(target, dataPaths))


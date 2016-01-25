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

import com.intellij.openapi.util.io.FileUtil
import gnu.trove.THashSet
import org.jetbrains.jps.builders.storage.BuildDataPaths
import org.jetbrains.jps.builders.storage.StorageProvider
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.jps.incremental.storage.BuildDataManager
import org.jetbrains.jps.incremental.storage.StorageOwner
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.incremental.IncrementalCacheImpl
import org.jetbrains.kotlin.incremental.dumpCollection
import org.jetbrains.kotlin.incremental.storage.BasicMap
import org.jetbrains.kotlin.incremental.storage.BasicStringMap
import org.jetbrains.kotlin.incremental.storage.StringCollectionExternalizer
import org.jetbrains.kotlin.jps.build.KotlinBuilder
import org.jetbrains.kotlin.jps.incremental.storages.PathCollectionExternalizer
import org.jetbrains.kotlin.jps.incremental.storages.PathFunctionPair
import org.jetbrains.kotlin.jps.incremental.storages.PathFunctionPairKeyDescriptor
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.io.File

class JpsIncrementalCacheImpl(
        target: ModuleBuildTarget,
        paths: BuildDataPaths
) : IncrementalCacheImpl<ModuleBuildTarget>(paths.getTargetDataRoot(target), target.outputDir, target), StorageOwner {

    private val dirtyInlineFunctionsMap = registerMap(DirtyInlineFunctionsMap(DIRTY_INLINE_FUNCTIONS.storageFile))
    private val inlinedTo = registerMap(InlineFunctionsFilesMap(INLINED_TO.storageFile))

    override fun registerInline(fromPath: String, jvmSignature: String, toPath: String) {
        if (!IncrementalCompilation.isExperimental()) {
            inlinedTo.add(fromPath, jvmSignature, toPath)
        }
    }

    override fun debugLog(message: String) {
        KotlinBuilder.LOG.debug(message)
    }

    fun getFilesToReinline(): Collection<File> {
        val result = THashSet(FileUtil.PATH_HASHING_STRATEGY)

        for ((className, functions) in dirtyInlineFunctionsMap.getEntries()) {
            val classFilePath = getClassFilePath(className.internalName)

            for (cache in dependentsWithThis) {
                val targetFiles = functions.flatMap { (cache as JpsIncrementalCacheImpl).inlinedTo[classFilePath, it] }
                result.addAll(targetFiles)
            }
        }

        return result.map { File(it) }
    }

    fun cleanDirtyInlineFunctions() {
        dirtyInlineFunctionsMap.clean()
    }

    override fun processChangedInlineFunctions(className: JvmClassName, changedFunctions: Collection<String>) {
        if (changedFunctions.isNotEmpty()) {
            dirtyInlineFunctionsMap.put(className, changedFunctions.toList())
        }
    }

    private inner class DirtyInlineFunctionsMap(storageFile: File) : BasicStringMap<Collection<String>>(storageFile, StringCollectionExternalizer) {
        fun getEntries(): Map<JvmClassName, Collection<String>> =
                storage.keys.toMapBy(JvmClassName::byInternalName) { storage[it]!! }

        fun put(className: JvmClassName, changedFunctions: List<String>) {
            storage[className.internalName] = changedFunctions
        }

        override fun dumpValue(value: Collection<String>) = value.dumpCollection()
    }

    /**
     * Mapping: (sourceFile+inlineFunction)->(targetFiles)
     *
     * Where:
     *  * sourceFile - path to some kotlin source
     *  * inlineFunction - jvmSignature of some inline function in source file
     *  * target files - collection of files inlineFunction has been inlined to
     */
    private inner class InlineFunctionsFilesMap(storageFile: File) : BasicMap<PathFunctionPair, Collection<String>>(storageFile, PathFunctionPairKeyDescriptor, PathCollectionExternalizer) {
        fun add(sourcePath: String, jvmSignature: String, targetPath: String) {
            val key = PathFunctionPair(sourcePath, jvmSignature)
            storage.append(key, targetPath)
        }

        operator fun get(sourcePath: String, jvmSignature: String): Collection<String> {
            val key = PathFunctionPair(sourcePath, jvmSignature)
            return storage[key] ?: emptySet()
        }

        override fun dumpKey(key: PathFunctionPair): String =
                "(${key.path}, ${key.function})"

        override fun dumpValue(value: Collection<String>) =
                value.dumpCollection()
    }

    companion object {
        private val DIRTY_INLINE_FUNCTIONS = "dirty-inline-functions"
        private val INLINED_TO = "inlined-to"
    }
}

private class KotlinIncrementalStorageProvider(
        private val target: ModuleBuildTarget,
        private val paths: BuildDataPaths
) : StorageProvider<JpsIncrementalCacheImpl>() {

    override fun equals(other: Any?) = other is KotlinIncrementalStorageProvider && target == other.target

    override fun hashCode() = target.hashCode()

    override fun createStorage(targetDataDir: File): JpsIncrementalCacheImpl =
            JpsIncrementalCacheImpl(target, paths)
}

fun BuildDataManager.getKotlinCache(target: ModuleBuildTarget): JpsIncrementalCacheImpl =
        getStorage(target, KotlinIncrementalStorageProvider(target, dataPaths))


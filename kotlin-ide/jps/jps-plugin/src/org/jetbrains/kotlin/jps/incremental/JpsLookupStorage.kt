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

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.jps.builders.storage.BuildDataCorruptedException
import org.jetbrains.jps.builders.storage.StorageProvider
import org.jetbrains.jps.incremental.storage.BuildDataManager
import org.jetbrains.jps.incremental.storage.StorageOwner
import org.jetbrains.kotlin.incremental.LookupStorage
import org.jetbrains.kotlin.incremental.storage.FileToPathConverter
import java.io.File
import java.io.IOException

private object LookupStorageLock

class JpsLookupStorageManager(
    private val buildDataManager: BuildDataManager,
    pathConverter: FileToPathConverter
) {
    private val storageProvider = JpsLookupStorageProvider(pathConverter)

    fun cleanLookupStorage(log: Logger) {
        synchronized(LookupStorageLock) {
            try {
                buildDataManager.cleanTargetStorages(KotlinDataContainerTarget)
            } catch (e: IOException) {
                if (!buildDataManager.dataPaths.getTargetDataRoot(KotlinDataContainerTarget).deleteRecursively()) {
                    log.debug("Could not clear lookup storage caches", e)
                }
            }
        }
    }

    fun <T> withLookupStorage(fn: (LookupStorage) -> T): T {
        synchronized(LookupStorageLock) {
            try {
                val lookupStorage = buildDataManager.getStorage(KotlinDataContainerTarget, storageProvider)
                return fn(lookupStorage)
            } catch (e: IOException) {
                throw BuildDataCorruptedException(e)
            }
        }
    }

    private class JpsLookupStorageProvider(
        private val pathConverter: FileToPathConverter
    ) : StorageProvider<JpsLookupStorage>() {
        override fun createStorage(targetDataDir: File): JpsLookupStorage =
            JpsLookupStorage(targetDataDir, pathConverter)
    }

    private class JpsLookupStorage(
        targetDataDir: File,
        pathConverter: FileToPathConverter
    ) : StorageOwner, LookupStorage(targetDataDir, pathConverter)
}

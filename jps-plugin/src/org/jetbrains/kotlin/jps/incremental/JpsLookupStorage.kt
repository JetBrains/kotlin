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
import java.io.File
import java.io.IOException

private object LookupStorageLock

fun BuildDataManager.cleanLookupStorage(log: Logger) {
    synchronized(LookupStorageLock) {
        try {
            cleanTargetStorages(KotlinDataContainerTarget)
        } catch (e: IOException) {
            if (!dataPaths.getTargetDataRoot(KotlinDataContainerTarget).deleteRecursively()) {
                log.debug("Could not clear lookup storage caches", e)
            }
        }
    }
}

fun <T> BuildDataManager.withLookupStorage(fn: (LookupStorage) -> T): T {
    synchronized(LookupStorageLock) {
        try {
            val lookupStorage = getStorage(KotlinDataContainerTarget, JpsLookupStorageProvider)
            return fn(lookupStorage)
        } catch (e: IOException) {
            throw BuildDataCorruptedException(e)
        }
    }
}

private object JpsLookupStorageProvider : StorageProvider<JpsLookupStorage>() {
    override fun createStorage(targetDataDir: File): JpsLookupStorage = JpsLookupStorage(targetDataDir)
}

private class JpsLookupStorage(targetDataDir: File) : StorageOwner, LookupStorage(targetDataDir)

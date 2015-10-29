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

package org.jetbrains.kotlin.jps.incremental.storage

import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.ExternalIntegerKeyDescriptor
import java.io.File

class FilesMap(file: File) : BasicMap<Int, Collection<String>>(file, ExternalIntegerKeyDescriptor(), PATH_COLLECTION_EXTERNALIZER) {
    override fun dumpKey(key: Int): String = key.toString()

    override fun dumpValue(value: Collection<String>): String = value.toString()

    public fun get(hash: Int): Collection<String>? = storage[hash]

    public fun add(path: String): Int {
        val hash = FileUtil.PATH_HASHING_STRATEGY.computeHashCode(path)
        storage.append(hash) { it.writeUTF(path) }
        return hash
    }
}




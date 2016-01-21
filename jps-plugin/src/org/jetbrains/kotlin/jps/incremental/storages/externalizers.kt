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

package org.jetbrains.kotlin.jps.incremental.storages

import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.IOUtil
import com.intellij.util.io.KeyDescriptor
import gnu.trove.THashSet
import org.jetbrains.jps.incremental.storage.PathStringDescriptor
import org.jetbrains.kotlin.incremental.storage.CollectionExternalizer
import java.io.DataInput
import java.io.DataOutput

object PathFunctionPairKeyDescriptor : KeyDescriptor<PathFunctionPair> {
    override fun read(input: DataInput): PathFunctionPair {
        val path = IOUtil.readUTF(input)
        val function = IOUtil.readUTF(input)
        return PathFunctionPair(path, function)
    }

    override fun save(output: DataOutput, value: PathFunctionPair) {
        IOUtil.writeUTF(output, value.path)
        IOUtil.writeUTF(output, value.function)
    }

    override fun getHashCode(value: PathFunctionPair): Int = value.hashCode()

    override fun isEqual(val1: PathFunctionPair, val2: PathFunctionPair): Boolean = val1 == val2
}

object PathCollectionExternalizer : CollectionExternalizer<String>(PathStringDescriptor(), { THashSet(FileUtil.PATH_HASHING_STRATEGY) })

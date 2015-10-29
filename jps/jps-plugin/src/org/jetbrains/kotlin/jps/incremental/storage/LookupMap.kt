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

import java.io.File

class LookupMap(file: File) : BasicMap<IntPair, Collection<String>>(file, INT_PAIR_KEY_DESCRIPTOR, PATH_COLLECTION_EXTERNALIZER) {
    override fun dumpKey(key: IntPair): String = key.toString()

    override fun dumpValue(value: Collection<String>): String = value.toString()

    public fun add(name: String, scope: String, path: String) {
        storage.append(HashPair(name, scope)) { out -> out.writeUTF(path) }
    }

    public fun get(name: String, scope: String): Collection<String>? = storage[HashPair(name, scope)]
}




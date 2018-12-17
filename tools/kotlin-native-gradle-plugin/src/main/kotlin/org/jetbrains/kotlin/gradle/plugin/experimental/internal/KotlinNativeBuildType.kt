/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package org.jetbrains.kotlin.gradle.plugin.experimental.internal

import org.gradle.api.Named
import java.util.*

class KotlinNativeBuildType(
        private val name: String,
        val debuggable: Boolean,
        val optimized: Boolean,
        internal val iosEmbedBitcode: BitcodeEmbeddingMode
) : Named {

    override fun getName() = name

    companion object {
        val DEBUG = KotlinNativeBuildType("debug", true, false, BitcodeEmbeddingMode.MARKER)
        val RELEASE = KotlinNativeBuildType("release", false, true, BitcodeEmbeddingMode.BITCODE)
        val DEFAULT_BUILD_TYPES: Collection<KotlinNativeBuildType> = Arrays.asList(DEBUG, RELEASE)
    }
}

enum class BitcodeEmbeddingMode {
    /** Don't embed LLVM IR bitcode. */
    DISABLE,
    /** Embed LLVM IR bitcode as data. */
    BITCODE,
    /** Embed placeholder LLVM IR data as a marker. */
    MARKER,
}

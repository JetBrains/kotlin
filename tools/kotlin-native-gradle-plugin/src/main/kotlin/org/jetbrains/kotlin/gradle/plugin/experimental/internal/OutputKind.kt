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

import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeBinary
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget

enum class OutputKind(
        val compilerOutputKind: CompilerOutputKind,
        val binaryClass: Class<out KotlinNativeBinary>,
        private val developmentBinaryPriority: Int,
        val runtimeUsageName: String? = null,
        val linkUsageName: String? = null,
        val publishable: Boolean = true
) {
    EXECUTABLE(
        CompilerOutputKind.PROGRAM,
        KotlinNativeExecutableImpl::class.java,
        0,
        Usage.NATIVE_RUNTIME,
        null
    ),
    KLIBRARY(
        CompilerOutputKind.LIBRARY,
        KotlinNativeLibraryImpl::class.java,
        1,
        null,
        KotlinUsages.KOTLIN_API
    ),
    FRAMEWORK(
        CompilerOutputKind.FRAMEWORK,
        KotlinNativeFrameworkImpl::class.java,
        2,
        null,
        KotlinNativeUsage.FRAMEWORK,
        false
    ) {
        override fun availableFor(target: KonanTarget) =
            target.family.isAppleFamily
    },
    DYNAMIC(
        CompilerOutputKind.DYNAMIC,
        KotlinNativeDynamicImpl::class.java,
        3,
        Usage.NATIVE_RUNTIME,
        Usage.NATIVE_LINK,
        false
    ) {
        override fun availableFor(target: KonanTarget): Boolean = target != KonanTarget.WASM32
    },
    STATIC(
        CompilerOutputKind.STATIC,
        KotlinNativeStaticImpl::class.java,
        4,
        Usage.NATIVE_RUNTIME,
        Usage.NATIVE_LINK,
        false
    ) {
        override fun availableFor(target: KonanTarget): Boolean = target != KonanTarget.WASM32
    };

    open fun availableFor(target: KonanTarget) = true

    companion object {
        internal fun Collection<OutputKind>.getDevelopmentKind() = minBy { it.developmentBinaryPriority }
    }
}
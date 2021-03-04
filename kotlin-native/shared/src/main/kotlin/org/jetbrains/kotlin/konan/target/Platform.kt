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

package org.jetbrains.kotlin.konan.target

import org.jetbrains.kotlin.konan.util.DependencyProcessor

class Platform(val configurables: Configurables) 
    : Configurables by configurables {

    val clang by lazy {
        ClangArgs(configurables)
    }
    val linker by lazy {
        linker(configurables)
    }
}

class PlatformManager(private val distribution: Distribution, experimental: Boolean = false) :
        HostManager(distribution, experimental) {

    constructor(konanHome: String, experimental: Boolean = false): this(Distribution(konanHome), experimental)

    private val loaders = enabled.map {
        it to loadConfigurables(it, distribution.properties, DependencyProcessor.defaultDependenciesRoot.absolutePath)
    }.toMap()

    private val platforms = loaders.map {
        it.key to Platform(it.value)
    }.toMap()

    fun platform(target: KonanTarget) = platforms.getValue(target)
    val hostPlatform = platforms.getValue(host)

    fun loader(target: KonanTarget) = loaders.getValue(target)
}


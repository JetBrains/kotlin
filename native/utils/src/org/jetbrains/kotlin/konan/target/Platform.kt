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

class Platform(val configurables: Configurables) : Configurables by configurables {

    val clang: ClangArgs.Native by lazy {
        ClangArgs.Native(configurables)
    }

    val clangForJni: ClangArgs.Jni by lazy {
        ClangArgs.Jni(configurables)
    }

    val linker: LinkerFlags by lazy {
        linker(configurables)
    }
}

class PlatformManager private constructor(private val serialized: Serialized) :
    HostManager(), java.io.Serializable {

    // TODO(KT-66500): elevate to an error after the bootstrap
    @Suppress("UNUSED_PARAMETER")
    @Deprecated("Kept temporary, should be removed after the bootstrap")
    constructor(konanHome: String, experimental: Boolean = false, konanDataDir: String? = null) : this(Distribution(konanHome, konanDataDir = konanDataDir))

    // TODO(KT-66500): elevate to an error after the bootstrap
    @Suppress("UNUSED_PARAMETER")
    @Deprecated("Kept temporary, should be removed after the bootstrap")
    constructor(distribution: Distribution, experimental: Boolean = false) : this(Serialized(distribution))

    constructor(konanHome: String, konanDataDir: String? = null) : this(Distribution(konanHome, konanDataDir = konanDataDir))
    constructor(distribution: Distribution) : this(Serialized(distribution))

    private val distribution by serialized::distribution

    private val loaders = enabled.map {
        it to loadConfigurables(it, distribution.properties, distribution.dependenciesDir)
    }.toMap()

    private val platforms = loaders.map {
        it.key to Platform(it.value)
    }.toMap()

    fun platform(target: KonanTarget) = platforms.getValue(target)
    val hostPlatform = platforms.getValue(host)

    fun loader(target: KonanTarget) = loaders.getValue(target)

    private fun writeReplace(): Any = serialized

    /**
     * This class inherits Serializable to put it into a `org.gradle.api.provider.Property`, which is necessary in kotlin.git build.
     * It is not necessary to maintain the stable and predictably changing `serialVersionUUID` for this class (read below why).
     *
     * # Why serialVersionUUID doesn't matter
     * Gradle uses Serializable for Gradle Configuration Cache. Whenever a buildscript classpath changes, Gradle entirely discards that
     * cache and re-builds it from scratch. So, whenever any changes in [PlatformManager.Serialized]-class happen, the cache will be
     * rebuild from scratch.
     * So, there cases where we try to deserialize a binary representation of [PlatformManager.Serialized] with a class with a newer
     * version should be impossible.
     */
    private data class Serialized(
        val distribution: Distribution,
    ) : java.io.Serializable {
        private fun readResolve(): Any = PlatformManager(this)
    }
}

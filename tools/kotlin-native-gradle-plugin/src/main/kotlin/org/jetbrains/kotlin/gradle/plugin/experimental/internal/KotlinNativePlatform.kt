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

import org.gradle.api.model.ObjectFactory
import org.gradle.internal.os.OperatingSystem
import org.gradle.nativeplatform.OperatingSystemFamily
import org.gradle.nativeplatform.platform.NativePlatform
import org.gradle.nativeplatform.platform.internal.*
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.visibleName

interface KotlinNativePlatform: NativePlatform {
    val target: KonanTarget
}

fun KonanTarget.getGradleOS(): OperatingSystemInternal = family.visibleName.let {
    DefaultOperatingSystem(it, OperatingSystem.forName(it))
}

fun KonanTarget.getGradleOSFamily(objectFactory: ObjectFactory): OperatingSystemFamily {
    return objectFactory.named(OperatingSystemFamily::class.java, family.visibleName)
}

fun KonanTarget.getGradleCPU(): ArchitectureInternal = architecture.visibleName.let {
    Architectures.forInput(it)
}

class DefaultKotlinNativePlatform(name: String, override val target: KonanTarget):
        DefaultNativePlatform(name, target.getGradleOS(), target.getGradleCPU()),
        KotlinNativePlatform
{
    constructor(target: KonanTarget): this(target.visibleName, target)

    // TODO: Extend ImmutableDefaultNativePlatform and get rid of these methods after switch to Gradle 4.8
    private fun notImplemented(): Nothing = throw NotImplementedError("Not Implemented in Kotlin/Native plugin")
    override fun operatingSystem(name: String?) = notImplemented()
    override fun withArchitecture(architecture: ArchitectureInternal?) = notImplemented()
    override fun architecture(name: String?) = notImplemented()
}
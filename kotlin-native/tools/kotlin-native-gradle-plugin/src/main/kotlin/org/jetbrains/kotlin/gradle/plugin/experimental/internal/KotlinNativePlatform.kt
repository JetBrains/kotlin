/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.experimental.internal

import org.gradle.api.Project
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.internal.os.OperatingSystem
import org.gradle.language.cpp.internal.NativeVariantIdentity
import org.gradle.nativeplatform.MachineArchitecture
import org.gradle.nativeplatform.OperatingSystemFamily
import org.gradle.nativeplatform.TargetMachine
import org.gradle.nativeplatform.TargetMachineFactory
import org.gradle.nativeplatform.platform.NativePlatform
import org.gradle.nativeplatform.platform.internal.*
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.visibleName

interface KotlinNativePlatform : NativePlatform {
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

fun KonanTarget.toTargetMachine(objectFactory: ObjectFactory): TargetMachine = object : TargetMachine {
    override fun getOperatingSystemFamily(): OperatingSystemFamily =
            getGradleOSFamily(objectFactory)

    override fun getArchitecture(): MachineArchitecture =
            objectFactory.named(MachineArchitecture::class.java, this@toTargetMachine.architecture.visibleName)
}

class DefaultKotlinNativePlatform(name: String, override val target: KonanTarget) :
        DefaultNativePlatform(name, target.getGradleOS(), target.getGradleCPU()),
        KotlinNativePlatform {
    constructor(target: KonanTarget) : this(target.visibleName, target)

    // TODO: Extend ImmutableDefaultNativePlatform and get rid of these methods after switch to Gradle 4.8
    private fun notImplemented(): Nothing = throw NotImplementedError("Not Implemented in Kotlin/Native plugin")
    override fun operatingSystem(name: String?) = notImplemented()
    override fun withArchitecture(architecture: ArchitectureInternal?) = notImplemented()
    override fun architecture(name: String?) = notImplemented()
}

// NativeVariantIdentity constructor was changed in Gradle 5.1
// So we have to use reflection to create instance of this class in earlier versions.
internal fun compatibleVariantIdentity(
        project: Project,
        name: String,
        baseName: Provider<String>,
        group: Provider<String>,
        version: Provider<String>,
        debuggable: Boolean,
        optimized: Boolean,
        target: KonanTarget,
        linkUsage: UsageContext?,
        runtimeUsage: UsageContext?
): NativeVariantIdentity =
        NativeVariantIdentity::class.java.getConstructor(
                String::class.java,
                Provider::class.java,
                Provider::class.java,
                Provider::class.java,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
                OperatingSystemFamily::class.java,
                UsageContext::class.java,
                UsageContext::class.java
        ).newInstance(
                name,
                baseName,
                group,
                version,
                debuggable,
                optimized,
                target.getGradleOSFamily(project.objects),
                linkUsage,
                runtimeUsage
        )


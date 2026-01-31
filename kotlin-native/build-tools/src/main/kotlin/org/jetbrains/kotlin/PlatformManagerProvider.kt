/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.newInstance
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.PlatformManager
import org.jetbrains.kotlin.nativeDistribution.NativeDistribution
import org.jetbrains.kotlin.nativeDistribution.asNativeDistribution
import org.jetbrains.kotlin.nativeDistribution.asProperties
import org.jetbrains.kotlin.nativeDistribution.llvmDistributionSource
import org.jetbrains.kotlin.nativeDistribution.nativeProtoDistribution
import javax.inject.Inject

/**
 * Convenience class for tasks that need [PlatformManager].
 *
 * These tasks can keep this as a `@Nested` field to have automatic dependency tracking.
 *
 * @see platformManagerProvider
 */
open class PlatformManagerProvider @Inject constructor(
        objectFactory: ObjectFactory,
        providerFactory: ProviderFactory,
        project: Project,
) {
    @get:Internal("only konan.properties and its override matter")
    val distributionRoot = objectFactory.directoryProperty().convention(project.nativeProtoDistribution.root)

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    @Suppress("unused") // used only by Gradle machinery via reflection.
    protected val konanProperties = project.nativeProtoDistribution.konanProperties

    @get:Input
    val konanPropertiesOverride: Provider<Map<String, String>> = project.provider {
        val jdkHome = project.extensions.getByType(JavaToolchainService::class.java).launcherFor {
            languageVersion.set(JavaLanguageVersion.of(11))
        }.get().metadata.installationPath.asFile.absolutePath
        project.llvmDistributionSource.asProperties +
                mapOf("konan.jdk.home" to jdkHome)
    }

    /**
     * [PlatformManager] may depend on the current host, so the current host must be an input
     *
     * One example is `llvm`: we do not guarantee that llvm version is the same between Linux and macOS.
     * There could be other problems as well.
     */
    @get:Input
    @Suppress("UNUSED") // used by Gradle via reflection
    protected val currentHost = HostManager.host

    @get:Input
    @get:Optional
    protected val konanDataDir = providerFactory.gradleProperty("konan.data.dir")

    @get:Internal("dependencies are: konanProperties and konanDataDir")
    val platformManager: Provider<PlatformManager> = distributionRoot.asNativeDistribution().zip(konanPropertiesOverride) { dist: NativeDistribution, overrides: Map<String, String> ->
        PlatformManager(Distribution(
                konanHome = dist.root.asFile.absolutePath,
                onlyDefaultProfiles = true,
                propertyOverrides = overrides,
                konanDataDir = konanDataDir.orNull,
        ))
    }
}

fun ObjectFactory.platformManagerProvider(project: Project) = newInstance<PlatformManagerProvider>(project)

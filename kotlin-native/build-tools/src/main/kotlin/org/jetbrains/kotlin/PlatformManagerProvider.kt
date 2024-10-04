/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.kotlin.dsl.newInstance
import org.jetbrains.kotlin.konan.target.PlatformManager
import org.jetbrains.kotlin.konan.target.buildDistribution
import org.jetbrains.kotlin.nativeDistribution.nativeDistributionProperty
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
    @get:Internal("only konan.properties file matters")
    val distribution = objectFactory.nativeDistributionProperty().convention(project.nativeProtoDistribution)

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    @Suppress("unused") // used only by Gradle machinery via reflection.
    protected val konanProperties = project.nativeProtoDistribution.konanProperties

    @get:Input
    @get:Optional
    protected val konanDataDir = providerFactory.gradleProperty("konan.data.dir")

    @get:Internal("dependencies are: konanProperties and konanDataDir")
    val platformManager = distribution.map {
        PlatformManager(buildDistribution(it.root.asFile.absolutePath, konanDataDir.orNull))
    }
}

fun ObjectFactory.platformManagerProvider(project: Project) = newInstance<PlatformManagerProvider>(project)
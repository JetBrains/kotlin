/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.swc

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.targets.js.AbstractSettings
import org.jetbrains.kotlin.gradle.targets.js.nodejs.JsPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator
import org.jetbrains.kotlin.gradle.utils.property

open class SwcExtension(
    @Transient val rootProject: Project,
    private val swcSpec: SwcEnvSpec,
) : AbstractSettings<SwcEnv>() {
    private val gradleHome = rootProject.gradle.gradleUserHomeDir.also {
        rootProject.logger.kotlinInfo("Storing cached files in $it")
    }

    override val installationDirectory: DirectoryProperty = rootProject.objects.directoryProperty()
        .fileValue(gradleHome.resolve("swc"))

    // value not convention because this property can be nullable to not add repository
    override val downloadBaseUrlProperty: org.gradle.api.provider.Property<String> = rootProject.objects.property<String>()
        .value("https://github.com/swc-project/swc/releases/download")

    override val versionProperty: org.gradle.api.provider.Property<String> = rootProject.objects.property<String>()
        .convention("1.15.3")

    override val downloadProperty: org.gradle.api.provider.Property<Boolean> = rootProject.objects.property<Boolean>()
        .convention(true)

    override val commandProperty: org.gradle.api.provider.Property<String> = rootProject.objects.property<String>()
        .convention("compile")

    val setupTaskProvider: TaskProvider<SwcSetupTask>
        get() = rootProject.tasks.withType(SwcSetupTask::class.java)
            .named(JsPlatformDisambiguator.extensionName(SwcSetupTask.BASE_NAME))

    internal val platform: org.gradle.api.provider.Property<SwcPlatform> = rootProject.objects.property<SwcPlatform>()

    override fun finalizeConfiguration(): SwcEnv {
        return swcSpec.env.get()
    }

    companion object : HasPlatformDisambiguator by JsPlatformDisambiguator {
        val EXTENSION_NAME: String
            get() = extensionName("swc")
    }
}

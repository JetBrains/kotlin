/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.swc

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.targets.js.EnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.JsPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator
import org.jetbrains.kotlin.gradle.utils.getFile

/**
 * Specification for executing Swc, a transpilation tool for JS files.
 */
internal abstract class SwcEnvSpec : EnvSpec<SwcEnv>() {
    /**
     * Specify Swc platform information, with name and architecture.
     */
    internal abstract val platform: Property<SwcPlatform>

    final override val env: Provider<SwcEnv> = produceEnv()

    override val executable: Provider<String> = env.map { it.executable }

    final override fun produceEnv(): Provider<SwcEnv> {
        return version.map { versionValue ->
            val requiredVersionName = "swc-version_$versionValue"
            val targetPath = installationDirectory.getFile().resolve(requiredVersionName)
            val platform = platform.get()
            val classifier = platform.classifier
            val downloadValue = download.get()

            fun getExecutable(customCommand: String): String {
                return if (downloadValue)
                    targetPath.resolve("swc-$versionValue-$classifier").absolutePath
                else
                    customCommand
            }

            SwcEnv(
                download = downloadValue,
                downloadBaseUrl = downloadBaseUrl.orNull,
                allowInsecureProtocol = allowInsecureProtocol.get(),
                ivyDependency = "com.github.swc-project:swc:$versionValue:$classifier@",
                executable = getExecutable(command.get()),
                dir = targetPath
            )
        }
    }

    val Project.swcSetupTaskProvider: TaskProvider<out SwcSetupTask>
        get() = project.tasks.withType(SwcSetupTask::class.java)
            .named(JsPlatformDisambiguator.extensionName(SwcSetupTask.BASE_NAME))

    companion object : HasPlatformDisambiguator by JsPlatformDisambiguator {
        val EXTENSION_NAME: String
            get() = extensionName("swcSpec")
    }
}

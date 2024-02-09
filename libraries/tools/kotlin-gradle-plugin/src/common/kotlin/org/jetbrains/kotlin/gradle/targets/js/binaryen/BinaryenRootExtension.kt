/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.binaryen

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.targets.js.AbstractSettings
import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore

open class BinaryenRootExtension(
    @Transient val rootProject: Project
) : AbstractSettings<BinaryenEnv>() {
    init {
        check(rootProject.rootProject == rootProject)
    }

    private val gradleHome = rootProject.gradle.gradleUserHomeDir.also {
        rootProject.logger.kotlinInfo("Storing cached files in $it")
    }

    override var installationDir by Property(gradleHome.resolve("binaryen"))
    override var downloadBaseUrl: String? by Property("https://github.com/WebAssembly/binaryen/releases/download")
    override var version: String by Property("116")
    override var download: Boolean by Property(true)
    override var command: String by Property("wasm-opt")

    val setupTaskProvider: TaskProvider<BinaryenSetupTask>
        get() = rootProject.tasks.withType(BinaryenSetupTask::class.java).named(BinaryenSetupTask.NAME)

    override fun finalizeConfiguration(): BinaryenEnv {
        val platform = BinaryenPlatform.platform
        val requiredVersionName = "binaryen-version_$version"
        val cleanableStore = CleanableStore[installationDir.absolutePath]
        val targetPath = cleanableStore[requiredVersionName].use()
        val isWindows = BinaryenPlatform.name == BinaryenPlatform.WIN

        fun getExecutable(command: String, customCommand: String, windowsExtension: String): String {
            val finalCommand = if (isWindows && customCommand == command) "$command.$windowsExtension" else customCommand
            return if (download)
                targetPath
                    .resolve("bin")
                    .resolve(finalCommand)
                    .absolutePath
            else
                finalCommand
        }

        return BinaryenEnv(
            download = download,
            downloadBaseUrl = downloadBaseUrl,
            ivyDependency = "com.github.webassembly:binaryen:$version:$platform@tar.gz",
            executable = getExecutable("wasm-opt", command, "exe"),
            dir = targetPath,
            cleanableStore = cleanableStore,
            isWindows = isWindows,
        )
    }

    companion object {
        const val EXTENSION_NAME: String = "kotlinBinaryen"
    }
}

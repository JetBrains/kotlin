/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.binaryen

import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.internal.ConfigurationPhaseAware
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore
import java.io.Serializable
import java.net.URL

open class BinaryenRootExtension(@Transient val rootProject: Project) : ConfigurationPhaseAware<BinaryenEnv>(), Serializable {
    init {
        check(rootProject.rootProject == rootProject)
    }

    private val gradleHome = rootProject.gradle.gradleUserHomeDir.also {
        rootProject.logger.kotlinInfo("Storing cached files in $it")
    }

    var installationPath by Property(gradleHome.resolve("binaryen"))
    var downloadBaseUrl by Property("https://github.com/WebAssembly/binaryen/releases/download/")
    var version by Property("114")

    val setupTaskProvider: TaskProvider<out Copy>
        get() = rootProject.tasks.withType(Copy::class.java).named(BinaryenRootPlugin.INSTALL_TASK_NAME)

    override fun finalizeConfiguration(): BinaryenEnv {
        val requiredVersionName = "binaryen-version_$version-${BinaryenPlatform.platform}"
        val requiredZipName = "$requiredVersionName.tar.gz"
        val cleanableStore = CleanableStore[installationPath.absolutePath]
        val targetPath = cleanableStore[requiredVersionName].use()
        val isWindows = BinaryenPlatform.name == BinaryenPlatform.WIN

        return BinaryenEnv(
            cleanableStore = cleanableStore,
            zipPath = cleanableStore[requiredZipName].use(),
            targetPath = targetPath,
            executablePath = targetPath
                .resolve("binaryen-version_$version")
                .resolve("bin")
                .resolve(if (isWindows) "wasm-opt.exe" else "wasm-opt"),
            isWindows = isWindows,
            downloadUrl = URL("${downloadBaseUrl.trimEnd('/')}/version_$version/$requiredZipName"),
        )
    }

    companion object {
        const val EXTENSION_NAME: String = "kotlinBinaryen"
    }
}

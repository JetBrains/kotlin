/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.yarn

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.targets.js.EnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.Platform
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnEnv
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnResolution
import org.jetbrains.kotlin.gradle.utils.getFile

/**
 * Spec for Yarn - package manager to install NPM dependencies
 */
abstract class BaseYarnRootEnvSpec internal constructor() : EnvSpec<YarnEnv>() {

    /**
     * Specify a platform information with name and architecture
     */
    internal abstract val platform: Property<Platform>

    /**
     * Specify whether to not run install without custom package scripts.
     * It is useful for security
     *
     * Default: true
     */
    abstract val ignoreScripts: Property<Boolean>

    /**
     * Specify a behaviour if yarn.lock file was changed
     *
     * Default: FAIL
     */
    abstract val yarnLockMismatchReport: Property<YarnLockMismatchReport>

    /**
     * Specify whether to fail a build if new yarn.lock file was generated during the build
     *
     * Default: false
     */
    abstract val reportNewYarnLock: Property<Boolean>

    /**
     * Specify whether to replace already existing yarn.lock file with newly generated yarn.lock file
     *
     * Default: false
     */
    abstract val yarnLockAutoReplace: Property<Boolean>

    /**
     * Specify replacements of versions of installed NPM dependencies
     *
     * Details: https://classic.yarnpkg.com/lang/en/docs/selective-version-resolutions/
     */
    abstract val resolutions: ListProperty<YarnResolution>

    final override val env: Provider<YarnEnv> = produceEnv()

    override val executable: Provider<String> = env.map { it.executable }

    final override fun produceEnv(): Provider<YarnEnv> {
        return platform.map { platformValue ->
            val isWindows = platformValue.isWindows()

            val home = installationDirectory.getFile().resolve("yarn-v${version.get()}")

            val downloadValue = download.get()
            fun getExecutable(
                command: String,
                customCommand: String,
                windowsExtension: String,
            ): String {
                val finalCommand =
                    if (isWindows && customCommand == command) "$command.$windowsExtension" else customCommand
                return if (downloadValue)
                    home
                        .resolve("bin/yarn.js").absolutePath
                else
                    finalCommand
            }

            YarnEnv(
                download = downloadValue,
                downloadBaseUrl = downloadBaseUrl.orNull,
                allowInsecureProtocol = allowInsecureProtocol.get(),
                dir = home,
                executable = getExecutable("yarn", command.get(), "cmd"),
                ivyDependency = "com.yarnpkg:yarn:${version.get()}@tar.gz",
                ignoreScripts = ignoreScripts.get(),
                yarnLockMismatchReport = yarnLockMismatchReport.get(),
                reportNewYarnLock = reportNewYarnLock.get(),
                yarnLockAutoReplace = yarnLockAutoReplace.get(),
                yarnResolutions = resolutions.get()
            )
        }
    }

}
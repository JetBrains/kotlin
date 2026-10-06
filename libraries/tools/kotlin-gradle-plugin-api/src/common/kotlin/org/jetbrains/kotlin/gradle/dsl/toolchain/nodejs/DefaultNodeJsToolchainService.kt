/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.ExperimentalNodeJsToolchainDsl

/**
 * The default [NodeJsToolchainService] used by the Kotlin Gradle Plugin.
 *
 * Downloads the requested Node.js distribution and installs it into a shared, machine-wide directory,
 * so that a distribution is downloaded once and then reused by all builds on the machine.
 */
@ExperimentalNodeJsToolchainDsl
interface DefaultNodeJsToolchainService : NodeJsToolchainService<DefaultNodeJsToolchainService.Parameters> {

    /**
     * Parameters of [DefaultNodeJsToolchainService], controlling where Node.js distributions are
     * downloaded from and where they are installed.
     */
    abstract class Parameters : NodeJsToolchainService.Parameters {

        /**
         * The directory containing all Node.js installations.
         *
         * Defaults to `<user.home>/.kotlin/toolchains/nodejs`.
         */
        abstract val installationDir: DirectoryProperty

        /**
         * The base URL the distributions are downloaded from.
         *
         * Defaults to the official Node.js distribution, `https://nodejs.org/dist`.
         */
        abstract val downloadBaseUrl: Property<String>

        /**
         * Whether the build is running in offline mode. It is set with Gradle --offline option.
         *
         * When `true`, an already installed distribution is reused, and a missing one fails the build
         * instead of being downloaded.
         */
        abstract val offline: Property<Boolean>
    }
}

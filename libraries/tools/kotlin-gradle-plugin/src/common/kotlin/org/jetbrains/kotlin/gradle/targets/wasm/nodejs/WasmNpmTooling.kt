/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.nodejs

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.targets.js.NpmPackageVersion
import org.jetbrains.kotlin.gradle.targets.web.nodejs.NpmToolingEnv
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.toHexString
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Represents NPM tooling dependencies. This class works on a specified
 * installation directory and a list of declared dependencies to create an environment-specific tooling configuration.
 *
 * @property installationDir The directory where the NPM dependencies are installed or managed.
 * @property allDeps The list of NPM package versions to be managed, consisting of their names and corresponding versions.
 */
abstract class WasmNpmTooling internal constructor() {

    internal abstract val defaultInstallationDir: DirectoryProperty

    abstract val installationDir: DirectoryProperty

    internal abstract val allDeps: ListProperty<NpmPackageVersion>

    fun produceEnv(): Provider<NpmToolingEnv> {
        return allDeps.map { allDepsValue ->
            val md = MessageDigest.getInstance("MD5")
            allDepsValue.forEach { (name, version) ->
                md.update(name.toByteArray(StandardCharsets.UTF_8))
                md.update(version.toByteArray(StandardCharsets.UTF_8))
            }

            val hashVersion = md.digest().toHexString()

            val isInstallationDirPresent = installationDir.isPresent

            val nodeDir: File = if (isInstallationDirPresent) {
                installationDir.getFile()
            } else {
                defaultInstallationDir.map { it.dir(hashVersion) }.getFile()
            }

            NpmToolingEnv(
                version = hashVersion,
                dir = nodeDir,
                explicitDir = isInstallationDirPresent,
            )
        }
    }
}
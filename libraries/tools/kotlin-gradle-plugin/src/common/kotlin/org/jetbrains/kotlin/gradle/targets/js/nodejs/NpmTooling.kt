/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.targets.js.NpmPackageVersion
import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore
import org.jetbrains.kotlin.gradle.utils.toHexString
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Represents NPM tooling dependencies. This class works on a specified
 * installation directory and a list of declared dependencies to create an environment-specific tooling configuration.
 *
 * @property installationDir The directory where the NPM dependencies are installed or managed.
 * @property allDeps The list of NPM package versions to be managed, consisting of their names and corresponding versions.
 */
open class NpmTooling(
    val installationDir: Provider<Directory>,
    val allDeps: List<NpmPackageVersion>,
) {

    fun produceEnv(): Provider<NpmToolingEnv> {
        return installationDir.map { installationDirectory ->
            val md = MessageDigest.getInstance("MD5")
            allDeps.forEach { (name, version) ->
                md.update(name.toByteArray(StandardCharsets.UTF_8))
                md.update(version.toByteArray(StandardCharsets.UTF_8))
            }

            val hashVersion = md.digest().toHexString()

            val cleanableStore = CleanableStore[installationDirectory.asFile.absolutePath]

            val nodeDir = cleanableStore[hashVersion].use()

            NpmToolingEnv(
                cleanableStore = cleanableStore,
                version = hashVersion,
                dir = nodeDir,
            )
        }
    }
}

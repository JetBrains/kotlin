/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalExternalToolchainDsl::class)

package org.jetbrains.kotlin.gradle.toolchains

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalExternalToolchainDsl
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.utils.userKotlinPersistentDir
import java.io.File

/**
 * The names of the Gradle properties controlling the toolchain of a single tool.
 */
internal class ExternalToolchainGradleProperties(
    private val mode: String,
    private val installationDir: String,
    private val downloadUrl: String,
) {
    fun mode(project: Project): ExternalToolchainMode =
        project.kotlinPropertiesProvider.property(mode).orNull
            ?.let { ExternalToolchainMode.valueOf(it.uppercase()) }
            ?: ExternalToolchainMode.DISABLE

    /**
     * The directory all installations are shared through, `<user.home>/.kotlin/<installationDirName>` by default.
     *
     * The base directory can be customized with the `kotlin.user.home` Gradle property, and defaults to `~/.kotlin`.
     */
    fun installationDir(project: Project, installationDirName: String): File =
        project.kotlinPropertiesProvider.property(installationDir).orNull?.let(::File)
            ?: project.userKotlinPersistentDir.resolve(installationDirName)

    fun downloadBaseUrl(project: Project, defaultDownloadBaseUrl: String): String =
        project.kotlinPropertiesProvider.property(downloadUrl).getOrElse(defaultDownloadBaseUrl)
}

// this function duplicates logic from `PropertiesProvider.kotlinUserHomeDir`, but it is not possible
// to use PropertiesProvider from settings plugin, because PropertiesProvider requires gradle Project instance.
internal fun defaultExternalToolchainInstallationDir(installationDirName: String): File =
    File(System.getProperty("user.home")).resolve(".kotlin").resolve(installationDirName)

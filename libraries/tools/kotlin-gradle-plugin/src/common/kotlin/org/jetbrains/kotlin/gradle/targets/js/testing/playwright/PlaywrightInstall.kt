/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.playwright

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin.Companion.kotlinNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectModules
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependenciesTask
import org.jetbrains.kotlin.gradle.targets.js.npm.npmToolingDir
import org.jetbrains.kotlin.gradle.targets.web.nodejs.nodeJsRoot
import org.jetbrains.kotlin.gradle.utils.directoryProperty
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import javax.inject.Inject

/**
 * Installs Playwright browsers (Chromium) required for browser-based JS tests.
 *
 * Set the PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1 environment variable to skip the download,
 * or set PLAYWRIGHT_BROWSERS_PATH to reuse an existing installation.
 */
internal abstract class PlaywrightInstall @Inject constructor(
    @Internal
    @Transient
    override val compilation: KotlinJsIrCompilation,
    objects: ObjectFactory,
    private val execOperations: ExecOperations,
    private val providers: ProviderFactory,
) : RequiresNpmDependenciesTask, DefaultTask() {

    @Transient
    private val nodeJs = project.kotlinNodeJsEnvSpec

    @get:Input
    internal val nodeExecutable: Property<String> = objects.property(nodeJs.executable)

    @get:Internal
    override val requiredNpmDependencies: Set<RequiredKotlinJsDependency> = setOf(
        compilation.nodeJsRoot.versions.playwright
    )

    @get:Internal
    internal val npmToolingEnvDir: DirectoryProperty = objects.directoryProperty(compilation.npmToolingDir)

    @get:Internal
    internal val browsers = objects.listProperty(String::class.java).convention(emptyList())

    @get:OutputDirectory
    internal val outputDir: DirectoryProperty = objects.directoryProperty().fileProvider(
        providers
            .gradleProperty("kotlin.gradle.playwright.browsers.path")
            .orElse(providers.environmentVariable("PLAYWRIGHT_BROWSERS_PATH"))
            .map { File(it) }
            .orElse(defaultPlaywrightBrowserDir)
    )

    private val defaultPlaywrightBrowserDir: Provider<File>
        get() {
            val userHome = providers.systemProperty("user.home")

            val defaultPath = when {
                HostManager.hostIsMingw -> providers
                    .environmentVariable("USERPROFILE")
                    .orElse(userHome)
                    .map { File(it).resolve("AppData/Local/ms-playwright") }

                HostManager.hostIsMac -> userHome.map { File(it).resolve("Library/Caches/ms-playwright") }
                HostManager.hostIsLinux -> userHome.map { File(it).resolve(".cache/ms-playwright") }
                else -> throw IllegalStateException("Unsupported OS")
            }
            return defaultPath
        }

    @TaskAction
    fun installBrowsers() {
        val modules = NpmProjectModules(npmToolingEnvDir.getFile())
        val playwrightCli = modules.require("playwright/cli.js")
        val args = listOf(playwrightCli, "install") + browsers.get()

        execOperations.exec { spec ->
            spec.executable(nodeExecutable.get())
            spec.args(args)
        }
    }
}

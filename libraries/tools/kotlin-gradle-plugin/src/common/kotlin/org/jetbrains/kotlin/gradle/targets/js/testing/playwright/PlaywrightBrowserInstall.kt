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
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.targets.js.NpmPackageVersion
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.npmToolingDir
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin.Companion.kotlinNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectModules
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependenciesTask
import org.jetbrains.kotlin.gradle.targets.native.internal.KotlinInterprocessDirectoryLock
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault(because = "Playwright cli manages caches on its own")
internal abstract class PlaywrightBrowserInstall @Inject constructor(
    @Internal
    @Transient
    override val compilation: KotlinJsIrCompilation,
    objects: ObjectFactory,
    private val execOperations: ExecOperations,
    private val providers: ProviderFactory,
) : RequiresNpmDependenciesTask, DefaultTask() {

    @get:Input
    internal val nodeExecutable: Property<String> = objects.property(project.kotlinNodeJsEnvSpec.executable)

    @get:Input
    internal val browsers = objects.listProperty(String::class.java).convention(emptyList())

    init {
        onlyIf { browsers.get().isNotEmpty() }
    }

    @get:Internal
    override val requiredNpmDependencies: Set<RequiredKotlinJsDependency>
        get() = if (browsers.get().isNotEmpty()) {
            setOf(
                NpmPackageVersion("playwright", PLAYWRIGHT_VERSION)
            )
        } else emptySet()


    @get:Internal
    internal val npmToolingEnvDir: DirectoryProperty = objects.directoryProperty().convention(compilation.npmToolingDir())

    // this is intentional to prevent gradle warnings about tasks writing to the same location
    // FIXME: KT-87599 Design host-wide toolchain management
    @get:Internal
    internal val outputDir: DirectoryProperty = objects.directoryProperty().fileProvider(
        PropertiesProvider(project).playwrightBrowsersPath
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

        val lock = KotlinInterprocessDirectoryLock(outputDir.getFile())

        lock.withLock {
            execOperations.exec { spec ->
                spec.executable(nodeExecutable.get())
                spec.args(args)
                spec.environment("PLAYWRIGHT_BROWSERS_PATH", outputDir.get().asFile.absolutePath)
            }
        }
    }
}

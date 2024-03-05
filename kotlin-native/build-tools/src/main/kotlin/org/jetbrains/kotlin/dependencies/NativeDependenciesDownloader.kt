/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.dependencies

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.konan.properties.KonanPropertiesLoader
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.PlatformManager
import org.jetbrains.kotlin.konan.util.DependencyProcessor

/**
 * Downloader of native dependencies.
 *
 * Serves as a Gradle task wrapper around [DependencyProcessor].
 */
@UntrackedTask(because = "Output is large and work avoidance is performed in DependencyProcessor anyway")
abstract class NativeDependenciesDownloader : DefaultTask() {
    /**
     * Target for which to download dependency.
     */
    @get:Internal
    abstract val target: Property<KonanTarget>

    /**
     * Root directory where to place all dependencies.
     */
    @get:Internal
    abstract val dependenciesDirectory: DirectoryProperty

    /**
     * Dependency repository.
     */
    @get:Internal
    abstract val repositoryURL: Property<String>

    private val platformManager = project.extensions.getByType<PlatformManager>()

    @TaskAction
    fun downloadAndExtract() {
        val loader = platformManager.loader(target.get())
        check(loader is KonanPropertiesLoader)
        val dependencyProcessor =
                DependencyProcessor(
                        dependenciesDirectory.asFile.get(),
                        loader,
                        repositoryURL.get(),
                        keepUnstable = false) { url, currentBytes, totalBytes ->
                    // TODO: Consider using logger.
                    print("\nDownloading dependency for ${target.get()}: $url (${currentBytes}/${totalBytes}). ")
                }
        dependencyProcessor.showInfo = logger.isEnabled(LogLevel.INFO)
        dependencyProcessor.run()
    }
}

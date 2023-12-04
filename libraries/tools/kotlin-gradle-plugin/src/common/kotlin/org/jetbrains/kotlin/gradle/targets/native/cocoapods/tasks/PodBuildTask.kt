/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("LeakingThis", "PackageDirectoryMismatch") // All tasks should be inherited only by Gradle, Old package for compatibility

package org.jetbrains.kotlin.gradle.targets.native.tasks

import org.gradle.api.file.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension.*
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension.CocoapodsDependency.PodLocation.*
import org.jetbrains.kotlin.gradle.plugin.cocoapods.cocoapodsBuildDirs
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.runCommand
import org.jetbrains.kotlin.konan.target.Family
import javax.inject.Inject

/**
 * The task compiles external cocoa pods sources.
 */
@DisableCachingByDefault
abstract class PodBuildTask @Inject constructor(
    providerFactory: ProviderFactory,
    projectLayout: ProjectLayout,
    objectFactory: ObjectFactory,
) : CocoapodsTask() {

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    abstract val buildSettingsFile: RegularFileProperty

    @get:Nested
    internal abstract val pod: Property<CocoapodsDependency>

    @get:Input
    internal abstract val sdk: Property<String>

    @get:Input
    internal abstract val family: Property<Family>

    private val synthetic = projectLayout.cocoapodsBuildDirs.synthetic(family)

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    @get:InputDirectory
    internal val srcDir: Provider<Directory> = pod.flatMap { pod ->
        val podLocation = pod.source
        if (podLocation is Path) {
            projectLayout.dir(providerFactory.provider { podLocation.dir })
        } else {
            synthetic.map { it.dir("Pods/${pod.schemeName}") }
        }
    }

    @Suppress("unused") // declares an output
    @get:OutputFiles
    internal val buildResult: FileCollection = objectFactory.fileTree()
        .from(synthetic.map { it.dir("build") })
        .matching {
            it.include("**/${pod.get().schemeName}.*/")
            it.include("**/${pod.get().schemeName}/")
        }

    @get:Internal
    internal abstract val podsXcodeProjDir: DirectoryProperty

    @TaskAction
    fun buildDependencies() {
        val podBuildSettings = PodBuildSettingsProperties.readSettingsFromFile(buildSettingsFile.getFile())

        val podsXcodeProjDir = podsXcodeProjDir.get()

        val podXcodeBuildCommand = listOf(
            "xcodebuild",
            "-project", podsXcodeProjDir.asFile.name,
            "-scheme", pod.get().schemeName,
            "-sdk", sdk.get(),
            "-configuration", podBuildSettings.configuration
        )

        runCommand(podXcodeBuildCommand, logger) {
            directory(podsXcodeProjDir.asFile.parentFile)
            environment() // workaround for https://github.com/gradle/gradle/issues/27346
        }
    }
}

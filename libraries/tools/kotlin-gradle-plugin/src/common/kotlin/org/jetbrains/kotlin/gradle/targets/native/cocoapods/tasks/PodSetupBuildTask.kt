/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("LeakingThis", "PackageDirectoryMismatch") // All tasks should be inherited only by Gradle, Old package for compatibility

package org.jetbrains.kotlin.gradle.targets.native.tasks

import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension.CocoapodsDependency
import org.jetbrains.kotlin.gradle.plugin.cocoapods.cocoapodsBuildDirs
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.runCommand
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault
abstract class PodSetupBuildTask @Inject constructor(projectLayout: ProjectLayout) : CocoapodsTask() {

    @get:Input
    abstract val frameworkName: Property<String>

    @get:Input
    internal abstract val sdk: Property<String>

    @get:Nested
    abstract val pod: Property<CocoapodsDependency>

    @get:Internal
    internal abstract val podsXcodeProjDir: Property<File>

    @get:OutputFile
    val buildSettingsFile: Provider<RegularFile> = projectLayout.cocoapodsBuildDirs.buildSettings(pod, sdk)

    @TaskAction
    fun setupBuild() {
        val podsXcodeProjDir = podsXcodeProjDir.get()

        val buildSettingsReceivingCommand = listOf(
            "xcodebuild", "-showBuildSettings",
            "-project", podsXcodeProjDir.name,
            "-scheme", pod.get().schemeName,
            "-sdk", sdk.get()
        )

        val outputText = runCommand(buildSettingsReceivingCommand, logger) { directory(podsXcodeProjDir.parentFile) }

        val buildSettingsProperties = PodBuildSettingsProperties.readSettingsFromReader(outputText.reader())
        buildSettingsFile.getFile().let { bsf ->
            buildSettingsProperties.writeSettings(bsf)
        }
    }
}


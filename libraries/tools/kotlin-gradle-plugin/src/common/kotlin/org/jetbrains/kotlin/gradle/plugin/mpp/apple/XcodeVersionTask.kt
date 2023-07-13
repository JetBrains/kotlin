/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.onlyIfCompat
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.Xcode

@DisableCachingByDefault
internal abstract class XcodeVersionTask : DefaultTask() {

    companion object {
        fun locateOrRegister(project: Project): Provider<XcodeVersionTask> {
            return project.rootProject.locateOrRegisterTask("xcodeVersion") { task ->
                if (!HostManager.hostIsMac) {
                    task.onlyIfCompat("Task can be run only on MacOS") { false }
                    return@locateOrRegisterTask
                }

                // xcode-select stores a symlink to a developer dir of currently selected Xcode in "/var/db/xcode_select_link"
                task.xcodeSelectLink.from("/var/db/xcode_select_link/usr/bin/xcodebuild")

                // DEVELOPER_DIR may override currently selected Xcode (it's respected by xcode-select and xcrun)
                task.xcodeDeveloperDir.convention(project.providers.environmentVariable("DEVELOPER_DIR"))

                task.outputFile.convention(project.layout.buildDirectory.file("xcode-version.txt"))
            }
        }
    }

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputFiles // @InputFiles instead of @InputFile because it allows non-existing files
    abstract val xcodeSelectLink: ConfigurableFileCollection

    @get:Input
    @get:Optional
    abstract val xcodeDeveloperDir: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun execute() {
        outputFile.getFile().writeText(Xcode.findCurrent().version.toString())
    }
}

internal val Provider<XcodeVersionTask>.version: Provider<RegularFile>
    get() = flatMap { it.outputFile }
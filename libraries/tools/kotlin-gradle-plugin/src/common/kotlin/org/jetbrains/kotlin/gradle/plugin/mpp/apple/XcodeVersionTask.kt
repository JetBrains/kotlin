/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.utils.getAsFile
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.Xcode
import java.io.File
import javax.inject.Inject

@Suppress("LeakingThis") // should be inherited only by Gradle
internal abstract class XcodeVersionTask @Inject constructor(projectLayout: ProjectLayout) : DefaultTask() {

    @Suppress("unused") // marks an input
    @get:InputFiles // @InputFiles instead of @InputFile because it allows non-existing files
    protected val xcodeSelectLink: FileCollection = projectLayout.files(File("/var/db/xcode_select_link/usr/bin/xcodebuild"))

    @get:OutputFile
    val outputFile: Provider<RegularFile> = projectLayout.buildDirectory.file("xcode-version.txt")

    init {
        onlyIf {
            HostManager.hostIsMac
        }
    }

    @TaskAction
    fun execute() {
        outputFile.getAsFile().writeText(Xcode.findCurrent().version.toString())
    }
}
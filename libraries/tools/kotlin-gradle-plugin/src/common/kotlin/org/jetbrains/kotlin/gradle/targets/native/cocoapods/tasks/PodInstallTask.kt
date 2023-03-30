/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("LeakingThis", "PackageDirectoryMismatch") // All tasks should be inherited only by Gradle, Old package for compatibility

package org.jetbrains.kotlin.gradle.targets.native.tasks

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension.CocoapodsDependency
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension.SpecRepos
import org.jetbrains.kotlin.gradle.plugin.cocoapods.cocoapodsBuildDirs
import org.jetbrains.kotlin.gradle.targets.native.cocoapods.MissingCocoapodsMessage
import org.jetbrains.kotlin.gradle.targets.native.cocoapods.MissingSpecReposMessage
import java.io.File

abstract class PodInstallTask : AbstractPodInstallTask() {

    @get:Optional
    @get:InputFile
    abstract val podspec: Property<File?>

    @get:Input
    abstract val frameworkName: Property<String>

    @get:Nested
    abstract val specRepos: Property<SpecRepos>

    @get:Nested
    abstract val pods: ListProperty<CocoapodsDependency>

    @get:InputDirectory
    abstract val dummyFramework: Property<File>

    private val framework = project.provider { project.cocoapodsBuildDirs.framework.resolve("${frameworkName.get()}.framework") }
    private val tmpFramework = dummyFramework.map { dummy -> dummy.parentFile.resolve("tmp.framework").also { it.deleteOnExit() } }

    override fun doPodInstall() {
        // We always need to execute 'pod install' with the dummy framework because the one left from a previous build
        // may have a wrong linkage type. So we temporarily swap them, run 'pod install' and then swap them back
        framework.rename(tmpFramework)
        dummyFramework.rename(framework)
        super.doPodInstall()
        framework.rename(dummyFramework)
        tmpFramework.rename(framework)
    }

    private fun Provider<File>.rename(dest: Provider<File>) = get().rename(dest.get())

    private fun File.rename(dest: File) {
        if (!exists()) {
            mkdirs()
        }

        check(renameTo(dest)) { "Can't rename '${this}' to '${dest}'" }
    }

    override fun handleError(retCode: Int, error: String, process: Process): String? {
        val specReposMessages = MissingSpecReposMessage(specRepos.get()).missingMessage
        val cocoapodsMessages = pods.get().map { MissingCocoapodsMessage(it).missingMessage }

        return listOfNotNull(
            "'pod install' command failed with code $retCode.",
            "Error message:",
            error.lines().filter { it.isNotBlank() }.joinToString("\n"),
            """
            |        Please, check that podfile contains following lines in header:
            |        $specReposMessages
            |
            """.trimMargin(),
            """
            |        Please, check that each target depended on ${frameworkName.get()} contains following dependencies:
            |        ${cocoapodsMessages.joinToString("\n")}
            |        
            """.trimMargin()

        ).joinToString("\n")
    }
}

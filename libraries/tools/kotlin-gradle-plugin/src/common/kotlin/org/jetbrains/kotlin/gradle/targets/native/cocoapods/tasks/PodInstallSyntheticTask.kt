/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("LeakingThis", "PackageDirectoryMismatch") // All tasks should be inherited only by Gradle, Old package for compatibility

package org.jetbrains.kotlin.gradle.targets.native.tasks

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.cocoapods.platformLiteral
import org.jetbrains.kotlin.gradle.utils.RunProcessResult
import org.jetbrains.kotlin.konan.target.Family
import java.io.File

@DisableCachingByDefault
abstract class PodInstallSyntheticTask : AbstractPodInstallTask() {

    @get:Input
    abstract val family: Property<Family>

    @get:Input
    abstract val podName: Property<String>

    @get:OutputDirectory
    internal val syntheticXcodeProject: Provider<File> = workingDir.map { it.resolve("synthetic.xcodeproj") }

    override fun doPodInstall() {
        val projResource = "/cocoapods/project.pbxproj"
        val projDestination = syntheticXcodeProject.get().resolve("project.pbxproj")

        syntheticXcodeProject.get().mkdirs()
        projDestination.outputStream().use { file ->
            javaClass.getResourceAsStream(projResource)!!.use { resource ->
                resource.copyTo(file)
            }
        }

        super.doPodInstall()
    }

    override fun handleError(result: RunProcessResult): String? {
        var message = """
            |'pod install' command on the synthetic project failed with return code: ${result.retCode}
            |
            |        Error: ${result.stdErr.lines().filter { it.contains("[!]") }.joinToString("\n")}
            |       
        """.trimMargin()

        if (
            result.stdErr.contains("deployment target") ||
            result.stdErr.contains("no platform was specified") ||
            result.stdErr.contains(Regex("The platform of the target .+ is not compatible with `${podName.get()}"))
        ) {
            message += """
                |
                |        Possible reason: ${family.get().platformLiteral} deployment target is not configured
                |        Configure deployment_target for ALL targets as follows:
                |        cocoapods {
                |           ...
                |           ${family.get().platformLiteral}.deploymentTarget = "..."
                |           ...
                |        }
                |       
            """.trimMargin()
            return message
        } else if (
            result.stdErr.contains("Unable to add a source with url") ||
            result.stdErr.contains("Couldn't determine repo name for URL") ||
            result.stdErr.contains("Unable to find a specification")
        ) {
            message += """
                |
                |        Possible reason: spec repos are not configured correctly.
                |        Ensure that spec repos are correctly configured for all private pod dependencies:
                |        cocoapods {
                |           specRepos {
                |               url("<private spec repo url>")
                |           }
                |        }
                |       
            """.trimMargin()
            return message
        } else {
            return null
        }
    }
}

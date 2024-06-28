/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("LeakingThis", "PackageDirectoryMismatch") // All tasks should be inherited only by Gradle, Old package for compatibility

package org.jetbrains.kotlin.gradle.targets.native.tasks

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension.CocoapodsDependency
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension.SpecRepos
import org.jetbrains.kotlin.gradle.targets.native.cocoapods.MissingCocoapodsMessage
import org.jetbrains.kotlin.gradle.targets.native.cocoapods.MissingSpecReposMessage
import org.jetbrains.kotlin.gradle.utils.RunProcessResult
import java.io.File

@DisableCachingByDefault
abstract class PodInstallTask : AbstractPodInstallTask() {

    @Deprecated("Use PodspecTask#outputFile")
    @get:Internal
    abstract val podspec: Property<File?>

    @get:Input
    abstract val frameworkName: Property<String>

    @get:Input
    abstract val useStaticFramework: Property<Boolean>

    @get:Nested
    abstract val specRepos: Property<SpecRepos>

    @get:Nested
    abstract val pods: ListProperty<CocoapodsDependency>

    override fun handleError(result: RunProcessResult): String? {
        val specReposMessages = MissingSpecReposMessage(specRepos.get()).missingMessage
        val cocoapodsMessages = pods.get().map { MissingCocoapodsMessage(it).missingMessage }

        return listOfNotNull(
            "'pod install' command failed with code ${result.retCode}.",
            "Error message:",
            result.stdErr.lines().filter { it.isNotBlank() }.joinToString("\n"),
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

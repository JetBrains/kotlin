/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.utils.appendLine
import java.io.File

@DisableCachingByDefault(because = "No outputs to cache")
internal abstract class CheckCocoaPodsHasNoSwiftPMDependencies : DefaultTask() {
    @get:Input
    abstract val directSwiftPMDependencies: SetProperty<SwiftPMDependency>

    @get:Input
    abstract val transitiveSwiftPMDependencies: Property<TransitiveSwiftPMDependencies>

    @get:Optional
    @get:Input
    abstract val workspacePath: Property<String>

    @get:Input
    abstract val projectPath: Property<File>

    @get:Input
    abstract val gradleProjectPath: Property<String>

    @get:Input
    abstract val rootProjectDir: Property<File>

    // Stub output file for UTD
    @get:OutputFile
    protected val stubOutput = project.layout.buildDirectory.file("kotlin/swiftImportCocoaPodsCheck")

    @TaskAction
    fun action() {
        val directSwiftPMDependencies = directSwiftPMDependencies.get()
        val transitiveSwiftPMDependencies = transitiveSwiftPMDependencies.get().metadataByDependencyIdentifier
        if (directSwiftPMDependencies.isNotEmpty() || transitiveSwiftPMDependencies.isNotEmpty()) {
            val xcodeProjectPath = workspacePath.orNull?.let {
                File(it).listFiles().firstOrNull {
                    it.name.endsWith(".xcodeproj")
                }
            } ?: "/path/to/iosApp.xcodeproj"
            val gradlewPath = searchForGradlew(projectPath.get())
            val message = buildString {
                appendLine("You are using CocoaPods integration with SwiftPM dependencies. Please follow the migration guide https://kotl.in/cocoapods-to-swiftpm-migration")
                appendLine("and run the following command to switch your Xcode project to the SwiftPM integration:")
                appendLine("${PROJECT_PATH_ENV}='${xcodeProjectPath}' ${IntegrateEmbedAndSignIntoXcodeProject.Companion.GRADLE_PROJECT_PATH_ENV}='${gradleProjectPath.get()}' '${gradlewPath}' -p '${rootProjectDir.get()}' '${gradleProjectPath.get()}:${IntegrateEmbedAndSignIntoXcodeProject.TASK_NAME}' '${gradleProjectPath.get()}:${IntegrateLinkagePackageIntoXcodeProject.TASK_NAME}'")
                if (directSwiftPMDependencies.isNotEmpty()) {
                    appendLine("Direct SwiftPM dependencies: ${directSwiftPMDependencies.joinToString(", ") { it.packageName }}")
                }
                if (transitiveSwiftPMDependencies.isNotEmpty()) {
                    appendLine("Transitive SwiftPM dependencies: ${transitiveSwiftPMDependencies.entries.joinToString("\n") { "${it.key}: ${it.value.dependencies.map { it.packageName }}" }}")
                }
            }

            message.lineSequence().forEach {
                logger.quiet("error: ${it}")
            }
            error(message)
        }
    }
}
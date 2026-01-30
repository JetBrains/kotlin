/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("LeakingThis", "PackageDirectoryMismatch") // All tasks should be inherited only by Gradle, Old package for compatibility

package org.jetbrains.kotlin.gradle.targets.native.tasks

import org.gradle.api.file.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension.CocoapodsDependency
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension.CocoapodsDependency.PodLocation.Path
import org.jetbrains.kotlin.gradle.plugin.cocoapods.cocoapodsBuildDirs
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleSdk
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.applePlatform
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.genericPlatformDestination
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.property
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
    internal abstract val appleTarget: Property<AppleTarget>

    @get:Input
    internal abstract val family: Property<Family>

    @get:Input
    val xcodeBuildSettings: MapProperty<String, String> = objectFactory.mapProperty(String::class.java, String::class.java)

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

    @get:Optional
    @get:Input
    val targetDeviceIdentifier: Property<String> = objectFactory.property<String>().convention(
        providerFactory.environmentVariable("TARGET_DEVICE_IDENTIFIER")
    )

    @Suppress("unused") // declares an output
    @get:OutputFiles
    internal val buildResult: FileCollection = objectFactory.fileTree()
        .from(synthetic.map { it.dir("build") })
        .matching {
            it.include("**/${pod.get().schemeName}.*/")
            it.include("**/${pod.get().schemeName}/")
            it.exclude("XCBuildData/**")
        }

    @get:Internal
    internal abstract val podsXcodeProjDir: DirectoryProperty

    @TaskAction
    fun buildDependencies() {
        val podBuildSettings = PodBuildSettingsProperties.readSettingsFromFile(buildSettingsFile.getFile())

        val podsXcodeProjDir = podsXcodeProjDir.get()

        val baseCommand = listOf(
            "xcodebuild",
            "-project", podsXcodeProjDir.asFile.name,
            "-scheme", pod.get().schemeName,
            "-destination", destination(),
            "-configuration", podBuildSettings.configuration,
        )

        // Add build settings directly as KEY=VALUE pairs
        val buildSettingsArgs = xcodeBuildSettings.getOrElse(emptyMap()).map { (key, value) ->
            "$key=$value"
        }

        // Combine all arguments
        val podXcodeBuildCommand = baseCommand + buildSettingsArgs

        logger.info("Running xcodebuild command: ${podXcodeBuildCommand.joinToString(" ")}")

        // Run the xcodebuild command
        runCommand(podXcodeBuildCommand, logger, errorHandler = { result ->
            val output = result.stdErr.ifBlank { result.stdOut }

            // Detect missing iOS/watchOS/tvOS platform
            if (output.contains("is not installed") && output.contains("platform")) {
                val platform = appleTarget.get().applePlatform
                """
                    |Xcode does not have the required $platform platform installed.
                    |
                    |To install the missing platform, run one of these commands:
                    |    $ xcodebuild -downloadPlatform iOS
                    |    $ xcodebuild -downloadPlatform watchOS
                    |    $ xcodebuild -downloadPlatform tvOS
                    |
                    |Or open Xcode > Settings > Platforms and install the required platform.
                    |
                    |For more information: https://developer.apple.com/documentation/xcode/installing-additional-simulator-runtimes
                    |
                    |Original error:
                    |$output
                """.trimMargin()
            } else {
                null // Fall back to default error handling
            }
        }) {
            directory(podsXcodeProjDir.asFile.parentFile)
            environment().apply { // workaround for https://github.com/gradle/gradle/issues/27346
                keys.filter {
                    // KT-80641 EXECUTABLE_DEBUG_DYLIB_PATH problem
                    AppleSdk.xcodeEnvironmentDebugDylibVars.contains(it)
                }.forEach {
                    remove(it)
                }
            }
        }
    }

    private fun destination() = targetDeviceIdentifier.map { "id=$it" }.getOrElse(appleTarget.get().genericPlatformDestination)
}

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.cocoapods.asValidFrameworkName
import org.jetbrains.kotlin.gradle.plugin.diagnostics.UsesKotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.tasks.FrameworkDescriptor
import org.jetbrains.kotlin.gradle.utils.existsCompat
import org.jetbrains.kotlin.gradle.utils.onlyIfCompat
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import java.io.Serializable
import javax.inject.Inject

@DisableCachingByDefault
abstract class XCFrameworkTask
@Inject
internal constructor(
    private val execOperations: ExecOperations,
    projectLayout: ProjectLayout,
) : DefaultTask(), UsesKotlinToolingDiagnostics {
    init {
        onlyIfCompat("XCFramework may only be produced on macOS") { HostManager.hostIsMac }
    }

    /**
     * A base name for the XCFramework.
     */
    @Input
    var baseName: Provider<String> = project.provider { project.name }

    /**
     * A build type of the XCFramework.
     */
    @Input
    var buildType: NativeBuildType = NativeBuildType.RELEASE

    /**
     * A parent directory for the XCFramework.
     */
    @Internal
    var outputDir: File = projectLayout.buildDirectory.asFile.get().resolve("XCFrameworks")

    @get:IgnoreEmptyDirectories
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:SkipWhenEmpty
    val inputFrameworkFiles: Collection<File>
        get() = (xcframeworkSlices.get().map { it.file }).filter { it.existsCompat() }

    @get:Internal
    internal val xcFrameworkName: Provider<String>
        get() = baseName.map { it.asValidFrameworkName() }

    internal data class XCFrameworkSlice(
        val file: File,
        val isStatic: Boolean,
    ) : Serializable

    @get:Input
    internal abstract val xcframeworkSlices: ListProperty<XCFrameworkSlice>

    @get:Optional
    @get:Input
    internal abstract val frameworksConfigurationError: Property<String>

    @get:OutputDirectory
    protected val outputXCFrameworkFile: File
        get() = outputDir.resolve(buildType.getName()).resolve("${xcFrameworkName.get()}.xcframework")

    /**
     * Adds the specified frameworks in this XCFramework.
     */
    fun from(vararg frameworks: Framework) {
        dependsOn(frameworks.map { it.linkTaskProvider })
        fromFrameworkDescriptors(frameworks.map { FrameworkDescriptor(it) })
    }

    fun fromFrameworkDescriptors(vararg frameworks: FrameworkDescriptor) = fromFrameworkDescriptors(frameworks.toList())

    fun fromFrameworkDescriptors(frameworks: Iterable<FrameworkDescriptor>) {
        frameworks.forEach { framework ->
            require(framework.target.family.isAppleFamily) {
                "XCFramework supports Apple frameworks only"
            }
            xcframeworkSlices.add(
                project.provider {
                    XCFrameworkSlice(
                        framework.file,
                        framework.isStatic,
                    )
                }
            )
        }
    }

    @TaskAction
    fun createXCFramework() {
        val configurationError = frameworksConfigurationError.orNull
        if (configurationError != null) {
            error(configurationError)
        }

        execOperations.exec {
            it.commandLine(
                prepareOutputAndCreateXcodebuildCommand()
            )
        }
    }

    internal fun prepareOutputAndCreateXcodebuildCommand(
        fileExists: (File) -> (Boolean) = { it.exists() },
    ): List<String> {
        val output = outputXCFrameworkFile
        if (output.exists()) output.deleteRecursively()

        val cmdArgs = mutableListOf("xcodebuild", "-create-xcframework")
        xcframeworkSlices.get().forEach { slice ->
            cmdArgs.add("-framework")
            cmdArgs.add(slice.file.path)
            if (!slice.isStatic) {
                val dsymFile = File(slice.file.path + ".dSYM")
                if (fileExists(dsymFile)) {
                    cmdArgs.add("-debug-symbols")
                    cmdArgs.add(dsymFile.path)
                }
            }
        }
        cmdArgs.add("-output")
        cmdArgs.add(output.path)
        return cmdArgs
    }
}
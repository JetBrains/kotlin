/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.cocoapods.asValidFrameworkName
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

internal enum class AppleTarget(
    val targetName: String,
    val targets: List<KonanTarget>
) {
    MACOS_DEVICE("macos", listOf(KonanTarget.MACOS_X64, KonanTarget.MACOS_ARM64)),
    IPHONE_DEVICE("ios", listOf(KonanTarget.IOS_ARM32, KonanTarget.IOS_ARM64)),
    IPHONE_SIMULATOR("iosSimulator", listOf(KonanTarget.IOS_X64, KonanTarget.IOS_SIMULATOR_ARM64)),
    WATCHOS_DEVICE("watchos", listOf(KonanTarget.WATCHOS_ARM32, KonanTarget.WATCHOS_ARM64)),
    WATCHOS_SIMULATOR("watchosSimulator", listOf(KonanTarget.WATCHOS_X64, KonanTarget.WATCHOS_SIMULATOR_ARM64)),
    TVOS_DEVICE("tvos", listOf(KonanTarget.TVOS_ARM64)),
    TVOS_SIMULATOR("tvosSimulator", listOf(KonanTarget.TVOS_X64, KonanTarget.TVOS_SIMULATOR_ARM64))
}

internal class XCFrameworkTaskHolder(
    val buildType: NativeBuildType,
    val task: TaskProvider<XCFrameworkTask>,
    val fatTasks: Map<AppleTarget, TaskProvider<FatFrameworkTask>>
) {
    companion object {
        fun create(project: Project, xcFrameworkName: String, buildType: NativeBuildType): XCFrameworkTaskHolder {
            require(xcFrameworkName.isNotBlank())

            val parentTask = project.parentAssembleXCFrameworkTask(xcFrameworkName)
            val task = project.registerAssembleXCFrameworkTask(xcFrameworkName, buildType)
            parentTask.dependsOn(task)

            val fatTasks = AppleTarget.values().associate { fatTarget ->
                val fatTask = project.registerAssembleFatForXCFrameworkTask(xcFrameworkName, buildType, fatTarget)
                task.dependsOn(fatTask)
                fatTarget to fatTask
            }

            return XCFrameworkTaskHolder(buildType, task, fatTasks)
        }
    }
}

class XCFrameworkConfig {
    private val taskHolders: List<XCFrameworkTaskHolder>

    constructor(project: Project, xcFrameworkName: String) {
        taskHolders = NativeBuildType.values().map { buildType ->
            XCFrameworkTaskHolder.create(project, xcFrameworkName, buildType)
        }
    }

    constructor(project: Project) : this(project, project.name)

    /**
     * Adds the specified frameworks in this XCFramework.
     */
    fun add(framework: Framework) {
        taskHolders.forEach { holder ->
            if (framework.buildType == holder.buildType) {
                holder.task.configure { task -> task.from(framework) }
                AppleTarget.values()
                    .firstOrNull { it.targets.contains(framework.konanTarget) }
                    ?.also { appleTarget ->
                        holder.fatTasks[appleTarget]?.configure { fatTask ->
                            fatTask.from(framework)
                        }
                    }
            }
        }
    }
}

fun Project.XCFramework(xcFrameworkName: String = name) = XCFrameworkConfig(this, xcFrameworkName)

private fun Project.eraseIfDefault(xcFrameworkName: String) =
    if (name == xcFrameworkName) "" else xcFrameworkName

private fun Project.parentAssembleXCFrameworkTask(xcFrameworkName: String): TaskProvider<Task> =
    locateOrRegisterTask(lowerCamelCaseName("assemble", eraseIfDefault(xcFrameworkName), "XCFramework")) {
        it.group = "build"
        it.description = "Assemble all types of registered '$xcFrameworkName' XCFramework"
    }

private fun Project.registerAssembleXCFrameworkTask(
    xcFrameworkName: String,
    buildType: NativeBuildType
): TaskProvider<XCFrameworkTask> {
    val taskName = lowerCamelCaseName(
        "assemble",
        eraseIfDefault(xcFrameworkName),
        buildType.getName(),
        "XCFramework"
    )
    return registerTask(taskName) { task ->
        task.baseName = provider { xcFrameworkName }
        task.buildType = buildType
    }
}

//see: https://developer.apple.com/forums/thread/666335
private fun Project.registerAssembleFatForXCFrameworkTask(
    xcFrameworkName: String,
    buildType: NativeBuildType,
    appleTarget: AppleTarget
): TaskProvider<FatFrameworkTask> {
    val taskName = lowerCamelCaseName(
        "assemble",
        xcFrameworkName,
        buildType.getName(),
        appleTarget.targetName,
        "FatFrameworkForXCFramework"
    )

    return registerTask(taskName) { task ->
        task.destinationDir = XCFrameworkTask.fatFrameworkDir(project, xcFrameworkName, buildType, appleTarget)
        task.baseName = xcFrameworkName.asValidFrameworkName()
        task.onlyIf {
            task.frameworks.size > 1
        }
    }
}

abstract class XCFrameworkTask : DefaultTask() {
    /**
     * A base name for the XCFramework.
     */
    @Input
    var baseName: Provider<String> = project.provider { project.name }

    @get:Internal
    internal val xcFrameworkName: Provider<String>
        get() = baseName.map { it.asValidFrameworkName() }

    /**
     * A build type of the XCFramework.
     */
    @Input
    var buildType: NativeBuildType = NativeBuildType.RELEASE

    /**
     * A collection of frameworks used ot build the XCFramework.
     */
    private val allFrameworks: MutableSet<Framework> = mutableSetOf()

    @get:Internal  // We take it into account as an input in the inputFrameworkFiles property.
    val frameworks: List<Framework>
        get() = allFrameworks.filter { it.buildType == buildType }

    @get:IgnoreEmptyDirectories
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:SkipWhenEmpty
    protected val inputFrameworkFiles: Iterable<FileTree>
        get() = frameworks.map { project.fileTree(it.outputFile) }

    /**
     * A parent directory for the XCFramework.
     */
    @get:Internal  // We take it into account as an output in the outputXCFrameworkFile property.
    var outputDir: File = project.buildDir.resolve("XCFrameworks")

    /**
     * A parent directory for the fat frameworks.
     */
    @get:Internal  // We take it into account as an input in the buildType and baseName properties.
    protected val fatFrameworksDir: File
        get() = fatFrameworkDir(project, xcFrameworkName.get(), buildType)

    @get:OutputDirectory
    protected val outputXCFrameworkFile: File
        get() = outputDir.resolve(buildType.getName()).resolve("${xcFrameworkName.get()}.xcframework")

    /**
     * Adds the specified frameworks in this XCFramework.
     */
    fun from(vararg frameworks: Framework) {
        frameworks.forEach { framework ->
            require(framework.konanTarget.family.isAppleFamily) {
                "XCFramework supports Apple frameworks only"
            }
            allFrameworks.add(framework)
            dependsOn(framework.linkTask)
        }
    }

    @TaskAction
    fun assemble() {
        val frameworksForXCFramework = AppleTarget.values().mapNotNull { appleTarget ->
            val group = frameworks.filter { it.konanTarget in appleTarget.targets }
            when {
                group.size == 1 -> {
                    XCFrameworkFile(group.first().outputFile, group.first().isStatic)
                }
                group.size > 1 -> {
                    XCFrameworkFile(
                        fatFrameworksDir.resolve(appleTarget.targetName).resolve("${xcFrameworkName.get()}.framework"),
                        group.all { it.isStatic }
                    )
                }
                else -> null
            }
        }
        createXCFramework(frameworksForXCFramework, outputXCFrameworkFile, buildType)
    }

    private data class XCFrameworkFile (val file: File, val isStatic: Boolean)

    private fun createXCFramework(frameworkFiles: List<XCFrameworkFile>, output: File, buildType: NativeBuildType) {
        if (output.exists()) output.deleteRecursively()

        val cmdArgs = mutableListOf("xcodebuild", "-create-xcframework")
        frameworkFiles.forEach { frameworkFile ->
            cmdArgs.add("-framework")
            cmdArgs.add(frameworkFile.file.path)
            if (!frameworkFile.isStatic) {
                val dsymFile = File(frameworkFile.file.path + ".dSYM")
                if (dsymFile.exists()) {
                    cmdArgs.add("-debug-symbols")
                    cmdArgs.add(dsymFile.path)
                }
            }
        }
        cmdArgs.add("-output")
        cmdArgs.add(output.path)
        project.exec { it.commandLine(cmdArgs) }
    }

    internal companion object {
        fun fatFrameworkDir(
            project: Project,
            xcFrameworkName: String,
            buildType: NativeBuildType,
            appleTarget: AppleTarget? = null
        ) = project.buildDir
            .resolve("fat-framework")
            .resolve(buildType.getName())
            .resolve(xcFrameworkName)
            .resolveIfNotNull(appleTarget?.targetName)

        private fun File.resolveIfNotNull(relative: String?): File = if (relative == null) this else this.resolve(relative)
    }
}
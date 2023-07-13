/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.cocoapods.asValidFrameworkName
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.UsesKotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import javax.inject.Inject

@Suppress("unused") // used through .values() call
internal enum class AppleTarget(
    val targetName: String,
    val targets: List<KonanTarget>
) {
    MACOS_DEVICE("macos", listOf(KonanTarget.MACOS_X64, KonanTarget.MACOS_ARM64)),
    IPHONE_DEVICE("ios", listOf(KonanTarget.IOS_ARM32, KonanTarget.IOS_ARM64)),
    IPHONE_SIMULATOR("iosSimulator", listOf(KonanTarget.IOS_X64, KonanTarget.IOS_SIMULATOR_ARM64)),
    WATCHOS_DEVICE("watchos", listOf(KonanTarget.WATCHOS_ARM32, KonanTarget.WATCHOS_ARM64, KonanTarget.WATCHOS_DEVICE_ARM64)),
    WATCHOS_SIMULATOR("watchosSimulator", listOf(KonanTarget.WATCHOS_X86, KonanTarget.WATCHOS_X64, KonanTarget.WATCHOS_SIMULATOR_ARM64)),
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
            val task = project.registerAssembleXCFrameworkTask(xcFrameworkName, buildType)

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

    constructor(project: Project, xcFrameworkName: String, buildTypes: Set<NativeBuildType>) {
        val parentTask = project.parentAssembleXCFrameworkTask(xcFrameworkName)
        taskHolders = buildTypes.map { buildType ->
            XCFrameworkTaskHolder.create(project, xcFrameworkName, buildType).also {
                parentTask.dependsOn(it.task)
            }
        }
    }

    constructor(project: Project) : this(project, project.name)
    constructor(project: Project, xcFrameworkName: String) : this(project, xcFrameworkName, NativeBuildType.values().toSet())

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
                            fatTask.baseName = framework.baseName //all frameworks should have same names
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
        xcFrameworkName,
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
        buildType.getName(),
        appleTarget.targetName,
        "FatFrameworkFor",
        xcFrameworkName,
        "XCFramework"
    )

    return registerTask(taskName) { task ->
        task.destinationDir = XCFrameworkTask.fatFrameworkDir(project, xcFrameworkName, buildType, appleTarget)
        task.onlyIf {
            task.frameworks.size > 1
        }
    }
}

@DisableCachingByDefault
abstract class XCFrameworkTask
@Inject
internal constructor(
    private val execOperations: ExecOperations,
    private val projectLayout: ProjectLayout,
) : DefaultTask(), UsesKotlinToolingDiagnostics {
    init {
        onlyIf { HostManager.hostIsMac }
    }

    private val projectBuildDir: File get() = projectLayout.buildDirectory.asFile.get()

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

    private val groupedFrameworkFiles: MutableMap<AppleTarget, MutableList<FrameworkDescriptor>> = mutableMapOf()

    @get:IgnoreEmptyDirectories
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:SkipWhenEmpty
    val inputFrameworkFiles: Collection<File>
        get() = groupedFrameworkFiles.values.flatten().map { it.file }.filter { it.exists() }

    /**
     * A parent directory for the XCFramework.
     */
    @get:Internal  // We take it into account as an output in the outputXCFrameworkFile property.
    var outputDir: File = projectBuildDir.resolve("XCFrameworks")

    /**
     * A parent directory for the fat frameworks.
     */
    @get:Internal  // We take it into account as an input in the buildType and baseName properties.
    protected val fatFrameworksDir: File
        get() = fatFrameworkDir(projectBuildDir, xcFrameworkName.get(), buildType)

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
            dependsOn(framework.linkTask)
        }
        fromFrameworkDescriptors(frameworks.map { FrameworkDescriptor(it) })
    }

    fun fromFrameworkDescriptors(vararg frameworks: FrameworkDescriptor) = fromFrameworkDescriptors(frameworks.toList())

    fun fromFrameworkDescriptors(frameworks: Iterable<FrameworkDescriptor>) {
        val frameworkName = groupedFrameworkFiles.values.flatten().firstOrNull()?.name

        frameworks.forEach { framework ->
            if (frameworkName != null && framework.name != frameworkName) {
                error(
                    "All inner frameworks in XCFramework '${baseName.get()}' should have same names. " +
                            "But there are two with '$frameworkName' and '${framework.name}' names"
                )
            }
            val group = AppleTarget.values().first { it.targets.contains(framework.target) }
            groupedFrameworkFiles.getOrPut(group, { mutableListOf() }).add(framework)
        }
    }

    @TaskAction
    fun assemble() {
        val frameworks = groupedFrameworkFiles.values.flatten()
        val xcfName = xcFrameworkName.get()
        if (frameworks.isNotEmpty()) {
            val rawXcfName = baseName.get()
            val name = frameworks.first().name
            if (frameworks.any { it.name != name }) {
                error("All inner frameworks in XCFramework '$rawXcfName' should have same names!" +
                              frameworks.joinToString("\n") { it.file.path })
            }
            if (name != xcfName) {
                toolingDiagnosticsCollector.get().report(this, KotlinToolingDiagnostics.XCFrameworkDifferentInnerFrameworksName(
                    xcFramework = rawXcfName,
                    innerFrameworks = name,
                ))
            }
        }

        val frameworksForXCFramework = groupedFrameworkFiles.entries.mapNotNull { (group, files) ->
            when {
                files.size == 1 -> files.first()
                files.size > 1 -> FrameworkDescriptor(
                    fatFrameworksDir.resolve(group.targetName).resolve("$xcfName.framework"),
                    files.all { it.isStatic },
                    group.targets.first() //will be not used
                )
                else -> null
            }
        }
        createXCFramework(frameworksForXCFramework, outputXCFrameworkFile, buildType)
    }

    private fun createXCFramework(frameworkFiles: List<FrameworkDescriptor>, output: File, buildType: NativeBuildType) {
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
        execOperations.exec { it.commandLine(cmdArgs) }
    }

    internal companion object {
        fun fatFrameworkDir(
            project: Project,
            xcFrameworkName: String,
            buildType: NativeBuildType,
            appleTarget: AppleTarget? = null
        ) = fatFrameworkDir(project.buildDir, xcFrameworkName, buildType, appleTarget)

        fun fatFrameworkDir(
            buildDir: File,
            xcFrameworkName: String,
            buildType: NativeBuildType,
            appleTarget: AppleTarget? = null
        ) = buildDir
            .resolve(xcFrameworkName.asValidFrameworkName() + "XCFrameworkTemp")
            .resolve("fatframework")
            .resolve(buildType.getName())
            .resolveIfNotNull(appleTarget?.targetName)


        private fun File.resolveIfNotNull(relative: String?): File = if (relative == null) this else this.resolve(relative)
    }
}
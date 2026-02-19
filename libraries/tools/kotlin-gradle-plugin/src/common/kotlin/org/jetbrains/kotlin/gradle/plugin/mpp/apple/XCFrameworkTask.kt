/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinGradlePluginPublicDsl
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.UsesKotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.utils.existsCompat
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import java.io.Serializable
import javax.inject.Inject

@Suppress("unused") // used through .values() call
internal enum class AppleTarget(
    val targetName: String,
    val targets: List<KonanTarget>,
) : Serializable {
    MACOS_DEVICE("macos", listOf(KonanTarget.MACOS_X64, KonanTarget.MACOS_ARM64)),
    IPHONE_DEVICE("ios", listOf(KonanTarget.IOS_ARM64)),
    IPHONE_SIMULATOR("iosSimulator", listOf(KonanTarget.IOS_X64, KonanTarget.IOS_SIMULATOR_ARM64)),
    WATCHOS_DEVICE("watchos", listOf(KonanTarget.WATCHOS_ARM32, KonanTarget.WATCHOS_ARM64, KonanTarget.WATCHOS_DEVICE_ARM64)),
    WATCHOS_SIMULATOR("watchosSimulator", listOf(KonanTarget.WATCHOS_X64, KonanTarget.WATCHOS_SIMULATOR_ARM64)),
    TVOS_DEVICE("tvos", listOf(KonanTarget.TVOS_ARM64)),
    TVOS_SIMULATOR("tvosSimulator", listOf(KonanTarget.TVOS_X64, KonanTarget.TVOS_SIMULATOR_ARM64))
}

internal class XCFrameworkTaskHolder(
    val buildType: NativeBuildType,
    val task: TaskProvider<XCFrameworkTask>,
    val fatTasks: Map<AppleTarget, TaskProvider<FatFrameworkTask>>,
) {
    companion object {
        fun create(project: Project, xcFrameworkName: String, buildType: NativeBuildType): XCFrameworkTaskHolder {
            require(xcFrameworkName.isNotBlank())
            val task = project.registerAssembleXCFrameworkTask(xcFrameworkName, buildType)

            val fatTasks = AppleTarget.values().associateWith { fatTarget ->
                project.registerAssembleFatForXCFrameworkTask(
                    xcFrameworkName,
                    buildType,
                    fatTarget
                ).also { fatTask ->
                    task.configure { task ->
                        task.inputs.files(fatTask.map { it.fatFramework })
                    }
                }
            }

            return XCFrameworkTaskHolder(buildType, task, fatTasks)
        }
    }
}

@KotlinGradlePluginPublicDsl
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

    @Suppress("unused")
    constructor(project: Project) : this(project, project.name)
    constructor(project: Project, xcFrameworkName: String) : this(project, xcFrameworkName, NativeBuildType.values().toSet())

    /**
     * Adds the specified frameworks in this XCFramework.
     */
    fun add(framework: Framework) {
        taskHolders.forEach { holder ->
            if (framework.buildType == holder.buildType) {
                holder.task.configure { task ->
                    task.from(framework)
                }

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

internal data class FrameworkSlice(
    val descriptor: FrameworkDescriptor,
    val resources: File?,
) : Serializable

@KotlinGradlePluginPublicDsl
@Suppress("FunctionName")
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
    buildType: NativeBuildType,
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
    appleTarget: AppleTarget,
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
        task.destinationDirProperty.set(XCFrameworkTask.fatFrameworkDir(project, xcFrameworkName, buildType, appleTarget))
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
    private val fileOperations: FileSystemOperations,
    private val objectsFactory: ObjectFactory,
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
        get() = baseName.map { it.asValidFrameworkName }

    /**
     * A build type of the XCFramework.
     */
    @Input
    var buildType: NativeBuildType = NativeBuildType.RELEASE

    private val groupedFrameworkFiles: MutableMap<AppleTarget, MutableList<FrameworkDescriptor>> = mutableMapOf()

    @get:Internal
    internal val groupedResourcesFiles: MapProperty<AppleTarget, ConfigurableFileCollection> = objectsFactory.mapProperty(
        AppleTarget::class.java,
        ConfigurableFileCollection::class.java
    )

    @get:IgnoreEmptyDirectories
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:SkipWhenEmpty
    val inputFrameworkFiles: Collection<File>
        get() = groupedFrameworkFiles.values.flatten().map { it.file }.filter {
            it.existsCompat()
        }

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
        get() = fatFrameworkDir(projectLayout.buildDirectory, xcFrameworkName.get(), buildType).getFile()

    @get:OutputDirectory
    internal val outputXCFrameworkFile: File
        get() = outputDir.resolve(buildType.getName()).resolve("${xcFrameworkName.get()}.xcframework")

    /**
     * Adds the specified frameworks in this XCFramework.
     */
    fun from(vararg frameworks: Framework) {
        frameworks.forEach { framework ->
            processFramework(framework)
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
            groupedFrameworkFiles.getOrPut(group) { mutableListOf() }.add(framework)
        }
    }

    /**
     * Adds the specified resource directory to the grouped resources for the given Kotlin/Native target.
     *
     * Resources are categorized based on their corresponding Apple platform (macOS, iOS, tvOS, etc.).
     * This method maps the provided KonanTarget to its associated Apple platform category and adds
     * the resource directory to that category's collection.
     *
     * @param resources a provider supplying the directory to be added as a resource. The directory
     *                  will be registered as an input file for the task.
     * @param target the Kotlin/Native target (KonanTarget) that specifies which Apple platform
     *              category these resources should be included in (e.g., iOS device, iOS simulator).
     */
    fun addTargetResources(resources: Provider<File>, target: KonanTarget) {
        inputs.files(resources)
        val group = AppleTarget.values().first { it.targets.contains(target) }

        groupedResourcesFiles.get().getOrElse(group) {
            objectsFactory.fileCollection()
        }.from(resources).also { files ->
            groupedResourcesFiles.put(group, files)
        }
    }

    @TaskAction
    fun assemble() {
        val xcfName = xcFrameworkName.get()

        val frameworksForXCFramework = xcframeworkSlices(
            frameworkName = singleFrameworkName(xcfName)
        )

        createXCFramework(
            frameworksForXCFramework,
            outputXCFrameworkFile
        )
    }

    internal fun singleFrameworkName(xcfName: String): String {
        val frameworks = groupedFrameworkFiles.values.flatten()
        if (frameworks.isEmpty()) error("XCFramework $xcfName is empty")

        val rawXcfName = baseName.get()
        val name = frameworks.first().name
        if (frameworks.any { it.name != name }) {
            error(
                "All inner frameworks in XCFramework '$rawXcfName' should have same names!" +
                        frameworks.joinToString("\n") { it.file.path })
        }
        if (name != xcfName) {
            toolingDiagnosticsCollector.get().report(
                this, KotlinToolingDiagnostics.XCFrameworkDifferentInnerFrameworksName(
                    xcFramework = rawXcfName,
                    innerFrameworks = name,
                )
            )
        }
        return name
    }

    internal fun xcframeworkSlices(frameworkName: String) = groupedFrameworkFiles.entries.mapNotNull { (group, files) ->
        when {
            files.size == 1 -> FrameworkSlice(
                files.first(),
                xcframeworkResources(group).orNull
            )
            files.size > 1 -> FrameworkSlice(
                FrameworkDescriptor(
                    fatTargetDir(group).resolve("${frameworkName}.framework"),
                    files.all { it.isStatic },
                    group.targets.first() //will be not used
                ),
                xcframeworkResources(group).orNull
            )
            else -> null
        }
    }

    internal fun xcframeworkResources(group: AppleTarget): Provider<File> = groupedResourcesFiles.getting(group).map { fileCollection ->
        val files = fileCollection.files
        if (files.size > 1) {
            fatTargetDir(group).resolve(lowerCamelCaseName(group.targetName, "resources")).also { fatDir ->
                combineFrameworksResources(files, fatDir)
            }
        } else files.first()
    }

    private fun fatTargetDir(appleTarget: AppleTarget) = fatFrameworksDir.resolve(appleTarget.targetName)

    private fun combineFrameworksResources(resources: Iterable<File>, output: File) {
        fileOperations.sync {
            it.from(resources)
            it.into(output)
            /**
             * Exclude duplicates to prevent copying the same file multiple times.
             * It's required because we are combining frameworks to a single fat-framework.
             * In case of a resource collision, only the first file will be copied and this is a known limitation.
             */
            it.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
    }

    internal fun xcodebuildArguments(
        frameworkFiles: List<FrameworkSlice>,
        output: File,
        mergeWithResources: (FrameworkSlice) -> File = { it.descriptor.file },
        fileExists: (File) -> Boolean = { it.exists() },
    ): List<String> {
        val cmdArgs = mutableListOf("xcodebuild", "-create-xcframework")
        frameworkFiles.forEach { frameworkFile ->
            cmdArgs.add("-framework")
            cmdArgs.add(mergeWithResources(frameworkFile).path)
            if (!frameworkFile.descriptor.isStatic) {
                val dsymFile = File(frameworkFile.descriptor.file.path + ".dSYM")
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

    private fun processFramework(framework: Framework) {
        require(framework.konanTarget.family.isAppleFamily) {
            "XCFramework supports Apple frameworks only"
        }

        inputs.files(framework.linkTaskProvider.map { it.outputFile.get() })
    }

    private fun createXCFramework(frameworkFiles: List<FrameworkSlice>, output: File) {
        if (output.exists()) output.deleteRecursively()

        val cmdArgs = xcodebuildArguments(frameworkFiles, output, mergeWithResources = { slice ->
            if (slice.resources != null) {
                val frameworkTempDir = temporaryDir
                    .resolve(buildType.getName())
                    .resolve(slice.descriptor.target.name)
                    .resolve(slice.descriptor.file.name)

                frameworkTempDir.also { dir ->
                    fileOperations.sync {
                        it.from(slice.descriptor.file)
                        it.from(slice.resources)
                        it.into(dir)
                    }
                }
            } else {
                slice.descriptor.file
            }
        })
        execOperations.exec { it.commandLine(cmdArgs) }
    }

    internal companion object {
        fun fatFrameworkDir(
            project: Project,
            xcFrameworkName: String,
            buildType: NativeBuildType,
            appleTarget: AppleTarget? = null,
        ): Provider<Directory> = fatFrameworkDir(project.layout.buildDirectory, xcFrameworkName, buildType, appleTarget)

        fun fatFrameworkDir(
            buildDir: DirectoryProperty,
            xcFrameworkName: String,
            buildType: NativeBuildType,
            appleTarget: AppleTarget? = null,
        ): Provider<Directory> = buildDir.map {
            it.dir(xcFrameworkName.asValidFrameworkName + "XCFrameworkTemp")
                .dir("fatframework")
                .dir(buildType.getName())
                .dirIfNotNull(appleTarget?.targetName)
        }

        private fun Directory.dirIfNotNull(relative: String?): Directory = if (relative == null) this else this.dir(relative)
    }
}

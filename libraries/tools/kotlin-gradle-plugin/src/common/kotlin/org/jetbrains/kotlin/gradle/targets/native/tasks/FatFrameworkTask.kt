/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.cocoapods.asValidFrameworkName
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeOutputKind
import org.jetbrains.kotlin.gradle.utils.appendLine
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.KonanTarget.*
import org.jetbrains.kotlin.konan.util.visibleName
import java.io.ByteArrayOutputStream
import java.io.File

class FrameworkDsymLayout(val rootDir: File) {
    init {
        require(rootDir.name.endsWith(".framework.dSYM"))
    }

    private val frameworkName = rootDir.name.removeSuffix(".framework.dSYM")

    val binaryDir = rootDir.resolve("Contents/Resources/DWARF")
    val binary = binaryDir.resolve(frameworkName)
    val infoPlist = rootDir.resolve("Contents/Info.plist")

    fun mkdirs() {
        binaryDir.mkdirs()
    }

    fun exists() = rootDir.exists()
}

class FrameworkLayout(val rootDir: File) {
    init {
        require(rootDir.extension == "framework")
    }

    private val frameworkName = rootDir.nameWithoutExtension

    val headerDir = rootDir.resolve("Headers")
    val modulesDir = rootDir.resolve("Modules")

    val binary = rootDir.resolve(frameworkName)
    val header = headerDir.resolve("$frameworkName.h")
    val moduleFile = modulesDir.resolve("module.modulemap")
    val infoPlist = rootDir.resolve("Info.plist")

    val dSYM = FrameworkDsymLayout(rootDir.parentFile.resolve("$frameworkName.framework.dSYM"))

    fun mkdirs() {
        rootDir.mkdirs()
        headerDir.mkdir()
        modulesDir.mkdir()
    }

    fun exists() = rootDir.exists()
}

class FrameworkDescriptor(
    val file: File,
    val isStatic: Boolean,
    val target: KonanTarget
) {
    constructor(framework: Framework) : this(
        framework.outputFile,
        framework.isStatic,
        framework.konanTarget
    )

    init {
        require(NativeOutputKind.FRAMEWORK.availableFor(target))
    }

    val name = file.nameWithoutExtension
    val files = FrameworkLayout(file)
}

/**
 * Task running lipo to create a fat framework from several simple frameworks. It also merges headers, plists and module files.
 */
open class FatFrameworkTask : DefaultTask() {
    private val archToFramework: MutableMap<Architecture, FrameworkDescriptor> = mutableMapOf()

    //region DSL properties.
    /**
     * A collection of frameworks used ot build the fat framework.
     */
    @get:Internal  // We take it into account as an input in the inputFrameworkFiles property.
    val frameworks: Collection<FrameworkDescriptor>
        get() = archToFramework.values

    /**
     * A base name for the fat framework.
     */
    @Input
    var baseName: String = project.name

    /**
     * A parent directory for the fat framework.
     */
    @OutputDirectory
    var destinationDir: File = project.buildDir.resolve("fat-framework")

    @get:Internal
    val fatFrameworkName: String
        get() = baseName.asValidFrameworkName()

    @get:Internal
    val fatFramework: File
        get() = destinationDir.resolve(fatFrameworkName + ".framework")

    private val fatFrameworkLayout: FrameworkLayout
        get() = FrameworkLayout(fatFramework)

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:IgnoreEmptyDirectories
    @get:InputFiles
    @get:SkipWhenEmpty
    protected val inputFrameworkFiles: Iterable<FileTree>
        get() = frameworks.map { project.fileTree(it.file) }

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:IgnoreEmptyDirectories
    @get:InputFiles
    protected val inputDsymFiles: Iterable<FileTree>
        get() = frameworks.mapNotNull { framework ->
            framework.files.dSYM.takeIf {
                it.exists()
            }?.rootDir?.let {
                project.fileTree(it)
            }
        }

    // region DSL methods.
    /**
     * Adds the specified frameworks in this fat framework.
     */
    fun from(vararg frameworks: Framework) = from(frameworks.toList())

    /**
     * Adds the specified frameworks in this fat framework.
     */
    fun from(frameworks: Iterable<Framework>) {
        fromFrameworkDescriptors(frameworks.map { FrameworkDescriptor(it) })
        frameworks.forEach { dependsOn(it.linkTask) }
    }

    /**
     * Adds the specified frameworks in this fat framework.
     */
    fun fromFrameworkDescriptors(frameworks: Iterable<FrameworkDescriptor>) {
        frameworks.forEach { framework ->
            val arch = framework.target.architecture
            val family = framework.target.family
            val fatFrameworkFamily = getFatFrameworkFamily()
            require(fatFrameworkFamily == null || family == fatFrameworkFamily) {
                "Cannot add a binary with platform family '${family.visibleName}' to the fat framework:\n" +
                        "A fat framework must include binaries with the same platform family " +
                        "while this framework already includes binaries with family '${fatFrameworkFamily!!.visibleName}'"
            }

            require(!archToFramework.containsKey(arch)) {
                val alreadyAdded = archToFramework.getValue(arch)
                "This fat framework already has a binary for architecture `${arch.name.toLowerCase()}` " +
                        "(${alreadyAdded.name} for target `${alreadyAdded.target.name}`)"
            }

            require(archToFramework.all { it.value.isStatic == framework.isStatic }) {
                fun staticName(isStatic: Boolean) = if (isStatic) "static" else "dynamic"

                buildString {
                    append("Cannot create a fat framework from:\n")
                    archToFramework.forEach { append("${it.value.name} - ${it.key.name.toLowerCase()} - ${staticName(it.value.isStatic)}\n") }
                    append("${framework.name} - ${arch.name.toLowerCase()} - ${staticName(framework.isStatic)}\n")
                    append("All input frameworks must be either static or dynamic")
                }
            }

            archToFramework[arch] = framework
        }
    }
    // endregion.

    private fun getFatFrameworkFamily(): Family? {
        assert(archToFramework.values.distinctBy { it.target.family }.size <= 1)
        return archToFramework.values.firstOrNull()?.target?.family
    }

    private val Architecture.clangMacro: String
        get() = when (this) {
            Architecture.X86 -> "__i386__"
            Architecture.X64 -> "__x86_64__"
            Architecture.ARM32 -> "__arm__"
            Architecture.ARM64 -> "__aarch64__"
            else -> error("Fat frameworks are not supported for architecture `$name`")
        }

    private val FrameworkDescriptor.plistPlatform: String
        get() = when (target) {
            IOS_ARM32, IOS_ARM64, IOS_X64, IOS_SIMULATOR_ARM64 -> "iPhoneOS"
            TVOS_ARM64, TVOS_X64, TVOS_SIMULATOR_ARM64 -> "AppleTVOS"
            WATCHOS_ARM32, WATCHOS_ARM64, WATCHOS_X86, WATCHOS_X64, WATCHOS_SIMULATOR_ARM64 -> "WatchOS"
            else -> error("Fat frameworks are not supported for platform `${target.visibleName}`")
        }

    // Runs the PlistBuddy utility with the given commands to configure the given plist file.
    private fun processPlist(plist: File, commands: PlistBuddyRunner.() -> Unit) =
        PlistBuddyRunner(plist).apply {
            commands()
        }.run()


    private inner class PlistBuddyRunner(val plist: File) {

        val commands = mutableListOf<String>()
        var ignoreExitValue = false

        fun run() = project.exec { exec ->
            exec.executable = "/usr/libexec/PlistBuddy"
            commands.forEach {
                exec.args("-c", it)
            }
            exec.args(plist.absolutePath)
            exec.isIgnoreExitValue = ignoreExitValue
            // Hide process output.
            val dummyStream = ByteArrayOutputStream()
            exec.standardOutput = dummyStream
            exec.errorOutput = dummyStream
        }

        fun add(entry: String, value: String) = commands.add("Add \"$entry\" string \"$value\"")
        fun set(entry: String, value: String) = commands.add("Set \"$entry\" \"$value\"")
        fun delete(entry: String) = commands.add("Delete \"$entry\"")
    }

    private fun runLipo(inputFiles: Collection<File>, outputFile: File) =
        project.exec { exec ->
            exec.executable = "/usr/bin/lipo"
            exec.args = listOf(
                "-create",
                *inputFiles.map { it.absolutePath }.toTypedArray(),
                "-output", outputFile.absolutePath
            )
        }

    private fun runInstallNameTool(file: File, frameworkName: String) {
        project.exec { exec ->
            exec.executable = "install_name_tool"
            exec.args = listOf(
                "-id",
                "@rpath/${frameworkName}.framework/${frameworkName}",
                file.absolutePath
            )
        }
    }

    private fun mergeBinaries(outputFile: File) {

        runLipo(archToFramework.values.map { it.files.binary }, outputFile)

        if (archToFramework.values.any { !it.isStatic && it.name != fatFrameworkName }) {
            runInstallNameTool(outputFile, fatFrameworkName)
        }
    }

    private fun mergeHeaders(outputFile: File) = outputFile.writer().use { writer ->

        val headerContents = archToFramework.mapValues { (_, framework) ->
            framework.files.header.readText()
        }

        if (headerContents.values.distinct().size == 1) {
            // If all headers have the same declarations, just write content of one of them.
            writer.write(headerContents.values.first())
        } else {
            // If header contents differ, surround each of them with #ifdefs.
            headerContents.toList().forEachIndexed { i, (arch, content) ->
                val macro = arch.clangMacro
                if (i == 0) {
                    writer.appendLine("#if defined($macro)\n")
                } else {
                    writer.appendLine("#elif defined($macro)\n")
                }
                writer.appendLine(content)
            }
            writer.appendLine(
                """
                #else
                #error Unsupported platform
                #endif
                """.trimIndent()
            )
        }
    }

    private fun createModuleFile(outputFile: File, frameworkName: String) {
        outputFile.writeText("""
            framework module $frameworkName {
                umbrella header "$frameworkName.h"

                export *
                module * { export * }
            }
        """.trimIndent())
    }

    private fun mergePlists(outputFile: File, frameworkName: String) {
        // The list of frameworks isn't empty because Gradle skips the task in this case
        // (corresponding task input is annotated with @SkipWhenEmpty).
        assert(frameworks.isNotEmpty())

        // Use Info.plist of one of the frameworks to get basic data.
        val baseInfo = frameworks.first().files.infoPlist
        project.copy {
            it.from(baseInfo)
            it.into(outputFile.parentFile)
        }

        // Remove required device capabilities (if they exist).
        processPlist(outputFile) {
            // Hack: the plist may have no such entry so we need to ignore the exit code of PlistBuddy.
            // TODO: Handle this in a better way.
            ignoreExitValue = true
            delete(":UIRequiredDeviceCapabilities")
        }

        // TODO: What should we do with bundle id?
        processPlist(outputFile) {
            // Set framework name in the plist file.
            set(":CFBundleExecutable", frameworkName)
            set(":CFBundleName", frameworkName)

            // Remove all platform-specific sections.
            delete(":CFBundleSupportedPlatforms:0")

            // Add supported platforms.
            frameworks
                .map { it.plistPlatform }
                .distinct()
                .forEachIndexed { index, platform ->
                    add(":CFBundleSupportedPlatforms:$index", platform)
                }
        }
    }

    private fun mergeDSYM() {
        val dsymInputs = archToFramework.mapValues { (_, framework) ->
            framework.files.dSYM
        }.filterValues {
            it.exists()
        }

        if (dsymInputs.isEmpty()) {
            return
        }

        val fatDsym = fatFrameworkLayout.dSYM
        fatDsym.mkdirs()

        // Merge dSYM binary.
        runLipo(dsymInputs.values.map { it.binary }, fatDsym.binary)

        // Copy dSYM's Info.plist.
        // It doesn't contain target-specific info or framework names except the bundle id so there is no need to edit it.
        // TODO: handle bundle id.
        project.copy {
            it.from(dsymInputs.values.first().infoPlist)
            it.into(fatDsym.infoPlist.parentFile)
        }
    }

    @TaskAction
    protected fun createFatFramework() {
        val outFramework = fatFrameworkLayout

        outFramework.mkdirs()
        mergeBinaries(outFramework.binary)
        mergeHeaders(outFramework.header)
        createModuleFile(outFramework.moduleFile, fatFrameworkName)
        mergePlists(outFramework.infoPlist, fatFrameworkName)
        mergeDSYM()
    }

    companion object {
        private val supportedTargets = listOf(
            IOS_ARM32, IOS_ARM64, IOS_X64,
            WATCHOS_ARM32, WATCHOS_ARM64, WATCHOS_X86, WATCHOS_X64,
            TVOS_ARM64, TVOS_X64
        )

        fun isSupportedTarget(target: KotlinNativeTarget): Boolean {
            return target.konanTarget in supportedTargets
        }
    }
}

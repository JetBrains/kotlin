/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.cocoapods.asValidFrameworkName
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.ByteArrayOutputStream
import java.io.File

private val Framework.files: IosFrameworkFiles
    get() = IosFrameworkFiles(outputFile)

private class IosDsymFiles(val rootDir: File) {

    val frameworkName = rootDir.name.removeSuffix(".framework.dSYM")

    val binaryDir = rootDir.resolve("Contents/Resources/DWARF")
    val binary = binaryDir.resolve(frameworkName)
    val infoPlist = rootDir.resolve("Contents/Info.plist")

    fun mkdirs() {
        binaryDir.mkdirs()
    }

    fun exists(): Boolean = rootDir.exists()
}

private class IosFrameworkFiles(val rootDir: File) {

    constructor(parentDir: File, frameworkName: String):
            this(parentDir.resolve("$frameworkName.framework"))

    val frameworkName: String = rootDir.nameWithoutExtension

    val headerDir = rootDir.resolve("Headers")
    val modulesDir = rootDir.resolve("Modules")

    val binary = rootDir.resolve(frameworkName)
    val header = headerDir.resolve("$frameworkName.h")
    val moduleFile = modulesDir.resolve("module.modulemap")
    val infoPlist = rootDir.resolve("Info.plist")

    val dSYM: IosDsymFiles = IosDsymFiles(rootDir.parentFile.resolve("$frameworkName.framework.dSYM"))

    fun mkdirs() {
        rootDir.mkdirs()
        headerDir.mkdir()
        modulesDir.mkdir()
    }
}

/**
 * Task running lipo to create a fat framework from several simple frameworks. It also merges headers, plists and module files.
 */
open class FatFrameworkTask: DefaultTask() {

    //region DSL properties.
    /**
     * A collection of frameworks used ot build the fat framework.
     */
    @get:Internal  // We take it into account as an input in the inputFrameworkFiles property.
    val frameworks: Collection<Framework>
        get() = archToFramework.values

    /**
     * A base name for the fat framework.
     */
    @Input
    var baseName: String = project.name

    /**
     * A parent directory for the fat framework..
     */
    @OutputDirectory
    var destinationDir: File = project.buildDir.resolve("fat-framework")

    @get:Internal // We take it into account as an input in the destinationDir property.
    val fatFrameworkDir: File
        get() = fatFramework.rootDir

    @get:Internal  // We take it into account as an input in the destinationDir property.
    val fatDsymDir: File
        get() = fatDsym.rootDir
    // endregion.

    private val archToFramework: MutableMap<Architecture, Framework> = mutableMapOf()

    private val fatFrameworkName: String
        get() = baseName.asValidFrameworkName()

    private val fatFramework: IosFrameworkFiles
        get() = IosFrameworkFiles(destinationDir, fatFrameworkName)

    private val fatDsym: IosDsymFiles
        get() = fatFramework.dSYM

    @get:InputFiles
    @get:SkipWhenEmpty
    protected val inputFrameworkFiles: Iterable<FileTree>
        get() = frameworks.map { project.fileTree(it.outputFile) }

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
        frameworks.forEach {
            val konanTarget = it.target.konanTarget
            val arch = konanTarget.architecture
            require(konanTarget.family == Family.IOS) {
                "Cannot add a framework with target '${konanTarget.visibleName}' to the fat framework: " +
                        "fat frameworks are available only for iOS binaries."
            }
            require(!archToFramework.containsKey(arch)) {
                val alreadyAdded = archToFramework.getValue(arch)
                "This fat framework already has a binary for architecture `${arch.name.toLowerCase()}` " +
                        "(${alreadyAdded.name} for target `${alreadyAdded.target.name}`)"
            }
            archToFramework[arch] = it
            dependsOn(it.linkTask)
            // Framework generating task may stop with NO-SOURCE result. We should track it.
        }
    }
    // endregion.

    private val Architecture.lipoArg: String
        get() = when(this) {
            Architecture.X64 -> "x86_64"
            Architecture.ARM32 -> "armv7"
            Architecture.ARM64 -> "arm64"
            else -> error("Fat frameworks are not supported for architecture `$name`")
        }

    private val Architecture.clangMacro: String
        get() = when(this) {
            Architecture.X64 -> "__x86_64__"
            Architecture.ARM32 -> "__arm__"
            Architecture.ARM64 -> "__aarch64__"
            else -> error("Fat frameworks are not supported for architecture `$name`")
        }

    private val Framework.plistPlatform: String
        get() = when(target.konanTarget) {
            KonanTarget.IOS_ARM32, KonanTarget.IOS_ARM64 -> "iPhoneOS"
            KonanTarget.IOS_X64 -> "iPhoneSimulator"
            else -> error("Fat frameworks are not supported for target `${target.konanTarget.visibleName}`")
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

    private fun runLipo(inputFiles: Map<Architecture, File>, outputFile: File) =
        project.exec {
            it.executable = "/usr/bin/lipo"
            it.args = mutableListOf("-create").apply {
                inputFiles.forEach { (arch, binary) ->
                    addAll(listOf("-arch", arch.lipoArg, binary.absolutePath))
                }
                addArg("-output", outputFile.absolutePath)
            }
        }

    private fun mergeBinaries(outputFile: File) =
        runLipo(archToFramework.mapValues { (_, framework) -> framework.files.binary }, outputFile)

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
                    writer.appendln("#if defined($macro)\n")
                } else {
                    writer.appendln("#elif defined($macro)\n")
                }
                writer.appendln(content)
            }
            writer.appendln(
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

        fatDsym.mkdirs()

        // Merge dSYM binary.
        runLipo(dsymInputs.mapValues { (_, dsym) -> dsym.binary }, fatDsym.binary)

        // Copy dSYM's Info.plist.
        // It doesn't contain target-specific info or framework names except the bundle id so there is no need to edit it.
        // TODO: handle bundle id.
        project.copy {
            it.from(dsymInputs.values.first().infoPlist)
            it.into(fatDsym.infoPlist)
        }
    }

    @TaskAction
    protected fun createFatFramework() {
        fatFramework.mkdirs()

        val frameworkName = fatFramework.frameworkName
        mergeBinaries(fatFramework.binary)
        mergeHeaders(fatFramework.header)
        createModuleFile(fatFramework.moduleFile, frameworkName)
        mergePlists(fatFramework.infoPlist, frameworkName)
        mergeDSYM()
    }
}

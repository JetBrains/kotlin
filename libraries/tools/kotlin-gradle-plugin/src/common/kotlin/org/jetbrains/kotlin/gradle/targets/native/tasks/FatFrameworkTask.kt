/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.cocoapods.asValidFrameworkName
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeOutputKind
import org.jetbrains.kotlin.gradle.utils.appendLine
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.KonanTarget.*
import org.jetbrains.kotlin.konan.util.visibleName
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import javax.inject.Inject

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

class FrameworkLayout(
    val rootDir: File,
    val isMacosFramework: Boolean
) {
    init {
        require(rootDir.extension == "framework")
    }

    private val frameworkName = rootDir.nameWithoutExtension
    private val macosVersionsDir = rootDir.resolve("Versions")
    private val macosADir = macosVersionsDir.resolve("A")
    private val macosResourcesDir = macosADir.resolve("Resources")
    private val contentDir = if (isMacosFramework) macosADir else rootDir

    val headerDir = contentDir.resolve("Headers")
    val modulesDir = contentDir.resolve("Modules")

    val binary = contentDir.resolve(frameworkName)
    val header = headerDir.resolve("$frameworkName.h")
    val moduleFile = modulesDir.resolve("module.modulemap")
    val infoPlist = (if (isMacosFramework) macosResourcesDir else rootDir).resolve("Info.plist")

    val dSYM = FrameworkDsymLayout(rootDir.parentFile.resolve("$frameworkName.framework.dSYM"))

    fun mkdirs() {
        rootDir.mkdirs()
        headerDir.mkdirs()
        modulesDir.mkdirs()
        if (isMacosFramework) {
            macosResourcesDir.mkdirs()

            val currentVersion = macosVersionsDir.resolve("Current")
            Files.createSymbolicLink(currentVersion.toPath(), macosADir.relativeTo(macosVersionsDir).toPath())

            val root = rootDir.toPath()
            val current = currentVersion.relativeTo(rootDir).toPath()
            Files.createSymbolicLink(root.resolve("Headers"), current.resolve("Headers"))
            Files.createSymbolicLink(root.resolve("Modules"), current.resolve("Modules"))
            Files.createSymbolicLink(root.resolve("Resources"), current.resolve("Resources"))
            Files.createSymbolicLink(root.resolve(frameworkName), current.resolve(frameworkName))
        }
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
    val files = FrameworkLayout(file, target.family == Family.OSX)
}

/**
 * Task running lipo to create a fat framework from several simple frameworks. It also merges headers, plists and module files.
 */
@DisableCachingByDefault
open class FatFrameworkTask
@Inject
internal constructor(
    private val execOperations: ExecOperations,
    private val fileOperations: FileSystemOperations,
    private val objectFactory: ObjectFactory,
    projectLayout: ProjectLayout,
) : DefaultTask() {
    init {
        onlyIf { HostManager.hostIsMac }
    }

    private val archToFramework: MutableMap<AppleArchitecture, FrameworkDescriptor> = mutableMapOf()

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

    @get:Internal
    internal val defaultDestinationDir: Provider<Directory> = projectLayout.buildDirectory.dir("fat-framework")

    @OutputDirectory
    val destinationDirProperty: DirectoryProperty = objectFactory
        .directoryProperty()
        .convention(defaultDestinationDir)

    /**
     * A parent directory for the fat framework.
     */
    @get:Internal
    @Deprecated("please use destinationDirProperty", replaceWith = ReplaceWith("destinationDirProperty"))
    var destinationDir: File
        get() = destinationDirProperty.get().asFile
        set(value) = destinationDirProperty.set(value)

    @get:Internal
    val fatFrameworkName: String
        get() = baseName.asValidFrameworkName()

    @get:Internal
    val fatFramework: File
        get() = destinationDirProperty.file(fatFrameworkName + ".framework").getFile()

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:IgnoreEmptyDirectories
    @get:InputFiles
    @get:SkipWhenEmpty
    protected val inputFrameworkFiles: Iterable<FileTree>
        get() = frameworks.map { objectFactory.fileTree().from(it.file) }

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:IgnoreEmptyDirectories
    @get:InputFiles
    protected val inputDsymFiles: Iterable<FileTree>
        get() = frameworks.mapNotNull { framework ->
            framework.files.dSYM.takeIf {
                it.exists()
            }?.rootDir?.let {
                objectFactory.fileTree().from(it)
            }
        }

    private enum class AppleArchitecture(val clangMacro: String) {
        X64("__x86_64__"),
        X86("__i386__"),
        ARM32("__arm__"),
        // We need to distinguish between variants of aarch64, because there are two WatchOS ARM64 targets that we support
        // watchOsArm64 that compiles to arm64_32 architecture
        // watchOsDeviceArm64 that compiles to arm64 architecture
        // https://github.com/apple/llvm-project/blob/6698c7d5889280d336f4aa8bf665d6e3c0c13ea0/clang/lib/Basic/Targets/AArch64.cpp#L1041
        ARM64_32("__ARM64_ARCH_8_32__"),
        ARM64("__ARM64_ARCH_8__"),
    }

    private val KonanTarget.appleArchitecture: AppleArchitecture get() =
        when (architecture) {
            Architecture.X64 -> AppleArchitecture.X64
            Architecture.X86 -> AppleArchitecture.X86
            Architecture.ARM64 -> if (this == WATCHOS_ARM64) AppleArchitecture.ARM64_32 else AppleArchitecture.ARM64
            Architecture.ARM32 -> AppleArchitecture.ARM32
            Architecture.MIPS32,
            Architecture.MIPSEL32,
            Architecture.WASM32 -> error("Fat frameworks are not supported for target `$name`")
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
            val arch = framework.target.appleArchitecture
            val family = framework.target.family
            val fatFrameworkFamily = getFatFrameworkFamily()
            require(fatFrameworkFamily == null || family == fatFrameworkFamily) {
                "Cannot add a binary with platform family '${family.visibleName}' to the fat framework:\n" +
                        "A fat framework must include binaries with the same platform family " +
                        "while this framework already includes binaries with family '${fatFrameworkFamily!!.visibleName}'"
            }

            require(!archToFramework.containsKey(arch)) {
                val alreadyAdded = archToFramework.getValue(arch)
                "This fat framework already has a binary for architecture `${arch.name.toLowerCaseAsciiOnly()}` " +
                        "(${alreadyAdded.name} for target `${alreadyAdded.target.name}`)"
            }

            require(archToFramework.all { it.value.isStatic == framework.isStatic }) {
                fun staticName(isStatic: Boolean) = if (isStatic) "static" else "dynamic"

                buildString {
                    append("Cannot create a fat framework from:\n")
                    archToFramework.forEach {
                        append("${it.value.name} - ${it.key.name.toLowerCaseAsciiOnly()} - ${staticName(it.value.isStatic)}\n")
                    }
                    append("${framework.name} - ${arch.name.toLowerCaseAsciiOnly()} - ${staticName(framework.isStatic)}\n")
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

    private val FrameworkDescriptor.plistPlatform: String
        get() = when (target) {
            // remove `is ...` after Gradle Configuration Cache deserialization for Objects of a Sealed Class is fixed
            // https://github.com/gradle/gradle/issues/22347
            is MACOS_X64, is MACOS_ARM64 -> "MacOSX"
            is IOS_ARM32, is IOS_ARM64, is IOS_X64, is IOS_SIMULATOR_ARM64 -> "iPhoneOS"
            is TVOS_ARM64, is TVOS_X64, is TVOS_SIMULATOR_ARM64 -> "AppleTVOS"
            is WATCHOS_ARM32, is WATCHOS_ARM64, is WATCHOS_X86,
            is WATCHOS_X64, is WATCHOS_SIMULATOR_ARM64, is WATCHOS_DEVICE_ARM64 -> "WatchOS"
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

        fun run() = execOperations.exec { exec ->
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
        execOperations.exec { exec ->
            exec.executable = "/usr/bin/lipo"
            exec.args = listOf(
                "-create",
                *inputFiles.map { it.absolutePath }.toTypedArray(),
                "-output", outputFile.absolutePath
            )
        }

    private fun runInstallNameTool(file: File, frameworkName: String) {
        execOperations.exec { exec ->
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
        fileOperations.copy {
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

    private fun mergeDSYM(fatDsym: FrameworkDsymLayout) {
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
        runLipo(dsymInputs.values.map { it.binary }, fatDsym.binary)

        // Copy dSYM's Info.plist.
        // It doesn't contain target-specific info or framework names except the bundle id so there is no need to edit it.
        // TODO: handle bundle id.
        fileOperations.copy {
            it.from(dsymInputs.values.first().infoPlist)
            it.into(fatDsym.infoPlist.parentFile)
        }
    }

    @TaskAction
    protected fun createFatFramework() {
        val outFramework = FrameworkLayout(fatFramework, getFatFrameworkFamily() == Family.OSX)
        if (outFramework.exists()) outFramework.rootDir.deleteRecursively()

        outFramework.mkdirs()
        mergeBinaries(outFramework.binary)
        mergeHeaders(outFramework.header)
        createModuleFile(outFramework.moduleFile, fatFrameworkName)
        mergePlists(outFramework.infoPlist, fatFrameworkName)
        mergeDSYM(outFramework.dSYM)
    }

    companion object {
        private val supportedTargets = listOf(
            IOS_ARM32, IOS_ARM64, IOS_X64,
            WATCHOS_ARM32, WATCHOS_ARM64, WATCHOS_X86, WATCHOS_X64, WATCHOS_DEVICE_ARM64,
            TVOS_ARM64, TVOS_X64,
            MACOS_X64, MACOS_ARM64
        )

        fun isSupportedTarget(target: KotlinNativeTarget): Boolean {
            return target.konanTarget in supportedTargets
        }
    }
}

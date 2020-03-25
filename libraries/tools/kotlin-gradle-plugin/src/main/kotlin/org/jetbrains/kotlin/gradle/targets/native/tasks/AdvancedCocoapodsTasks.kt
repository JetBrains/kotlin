/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension
import org.jetbrains.kotlin.gradle.plugin.cocoapods.cocoapodsBuildDirs
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.native.tasks.PodBuildTask.Companion.toValidSDK
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*

abstract class AdvancedCocoapodsTask : DefaultTask() {
    @get:Nested
    lateinit var cocoapodsExtension: CocoapodsExtension

    @TaskAction
    fun commonInvoke() {
        podfileCheck(cocoapodsExtension)
        specificInvoke()
    }

    abstract fun specificInvoke()

    private fun podfileCheck(cocoapodsExtension: CocoapodsExtension) {
        check(cocoapodsExtension.podfile != null) {
            "Execution of task '$name' requires the path to the Podfile. Specify it in the file ${project.buildFile.absolutePath}."
        }
    }

}

/**
 * The task takes the path to the Podfile and calls `pod install`
 * to obtain sources or artifacts for the declared dependencies.
 * This task is a part of CocoaPods integration infrastructure.
 */
open class PodInstallTask : AdvancedCocoapodsTask() {

    @get:InputFile
    @get:Optional
    internal val podfileProvider: Provider<File?> = project.provider { cocoapodsExtension.podfile }

    @get:OutputDirectory
    @get:Optional
    internal val podsXcodeProjDirProvider: Provider<File?> = project.provider {
        cocoapodsExtension.podfile
            ?.parentFile
            ?.resolve("Pods")
            ?.resolve("Pods.xcodeproj")
    }

    override fun specificInvoke() {
        val podfileDir = podfileProvider.get()!!.parentFile
        val podInstallProcess = ProcessBuilder("pod", "install").apply {
            directory(podfileDir)
            inheritIO()
        }.start()
        val podInstallRetCode = podInstallProcess.waitFor()
        check(podInstallRetCode == 0) { "Unable to run 'pod install', return code $podInstallRetCode" }

        val podsXcprojFile = podsXcodeProjDirProvider.get()
        check(podsXcprojFile != null && podsXcprojFile.exists() && podsXcprojFile.isDirectory) {
            "The directory 'Pods/Pods.xcodeproj' was not created as a result of the `pod install` call."
        }
    }
}

open class PodSetupBuildTask : AdvancedCocoapodsTask() {
    @get:InputDirectory
    @get:Optional
    internal lateinit var podsXcodeProjDirProvider: Provider<File?>

    @Internal
    lateinit var kotlinNativeTarget: KotlinNativeTarget

    @get:OutputFile
    @get:Optional
    internal val buildSettingsFileProvider: Provider<File?> =
        project.provider {
            project
                .cocoapodsBuildDirs
                .root
                .resolve("buildSettings")
                .resolve(kotlinNativeTarget.toBuildSettingsFileName)
        }

    override fun specificInvoke() {
        val podsXcodeProjDir = podsXcodeProjDirProvider.get()

        val buildSettingsReceivingCommand = listOf(
            "xcodebuild", "-showBuildSettings",
            "-project", podsXcodeProjDir!!.name,
            "-scheme", cocoapodsExtension.frameworkName,
            "-sdk", kotlinNativeTarget.toValidSDK
        )

        val buildSettingsProcess = ProcessBuilder(buildSettingsReceivingCommand)
            .apply {
                directory(podsXcodeProjDir!!.parentFile)
            }.start()

        val buildSettingsRetCode = buildSettingsProcess.waitFor()
        check(buildSettingsRetCode == 0) {
            "Unable to run '${buildSettingsReceivingCommand.joinToString(" ")}' return code $buildSettingsRetCode"
        }


        val stdOut = buildSettingsProcess.inputStream

        val buildSettingsProperties = PodBuildSettingsProperties.readSettingsFromStream(stdOut)
        buildSettingsProperties.writeSettings(buildSettingsFileProvider.get()!!)
    }

    companion object {
        private val KotlinNativeTarget.toBuildSettingsFileName: String
            get() = "build-settings-$disambiguationClassifier.properties"
    }
}

/**
 * The task compiles external cocoa pods sources.
 */
open class PodBuildTask : AdvancedCocoapodsTask() {
    @get:InputDirectory
    @get:Optional
    internal lateinit var podsXcodeProjDirProvider: Provider<File?>

    @get:InputFile
    @get:Optional
    internal lateinit var buildSettingsFileProvider: Provider<File?>

    @Internal
    lateinit var kotlinNativeTarget: KotlinNativeTarget

    @get:OutputFile
    @get:Optional
    internal val buildDirHashFileProvider: Provider<File?> =
        project.provider {
            project
                .cocoapodsBuildDirs
                .root
                .resolve("buildDirHashSums")
                .resolve(kotlinNativeTarget.toBuildDirHashSumFileName)
        }

    override fun specificInvoke() {
        val podBuildSettings = PodBuildSettingsProperties.readSettingsFromStream(
            FileInputStream(buildSettingsFileProvider.get())
        )

        val podsXcodeProjDir = podsXcodeProjDirProvider.get()

        cocoapodsExtension.pods.all {

            val podXcodeBuildCommand = listOf(
                "xcodebuild",
                "-project", podsXcodeProjDir!!.name,
                "-scheme", it.moduleName,
                "-sdk", kotlinNativeTarget.toValidSDK,
                "-configuration", podBuildSettings.configuration
            )

            val podBuildProcess = ProcessBuilder(podXcodeBuildCommand)
                .apply {
                    directory(podsXcodeProjDir!!.parentFile)
                    inheritIO()
                }.start()

            val podBuildRetCode = podBuildProcess.waitFor()
            check(podBuildRetCode == 0) {
                "Unable to run '${podXcodeBuildCommand.joinToString(" ")}' return code $podBuildRetCode"
            }
        }
        val buildDirHashSumFile = buildDirHashFileProvider.get()!!
        buildDirHashSumFile!!.parentFile.mkdirs()
        buildDirHashSumFile!!.createNewFile()
        check(buildDirHashSumFile!!.exists()) {
            "Unable to create file ${buildDirHashSumFile.toRelativeString(project.projectDir)}!"
        }

        buildDirHashSumFile.writeText(getFileChecksumStr(project.file(podBuildSettings.buildDir)))
    }

    companion object {

        private fun getFileChecksumStr(file: File): String {
            val digest = MessageDigest.getInstance("SHA-1")
            file.walkTopDown().forEach {
                val byteArray = ByteArray(1024)
                var bytesCount = 0
                if (it.isFile) {
                    val inputStream = it.inputStream()
                    while (inputStream.read(byteArray).also { bt -> bytesCount = bt } != -1) {
                        digest.update(byteArray, 0, bytesCount)
                    }
                }
            }
            val buildDirHashSumStr = BigInteger(1, digest.digest()).toString(16).padStart(32, '0')
            return buildDirHashSumStr
        }

        internal val KotlinNativeTarget.toBuildDirHashSumFileName: String
            get() = "build-dir-hash-$disambiguationClassifier.txt"

        internal val KotlinNativeTarget.toValidSDK: String
            get() {
                return when (konanTarget) {
                    KonanTarget.IOS_X64 -> "iphonesimulator"
                    KonanTarget.IOS_ARM32, KonanTarget.IOS_ARM64 -> "iphoneos"
                    KonanTarget.WATCHOS_X86, KonanTarget.WATCHOS_X64 -> "watchsimulator"
                    KonanTarget.WATCHOS_ARM32, KonanTarget.WATCHOS_ARM64 -> "watchos"
                    KonanTarget.TVOS_X64 -> "appletvsimulator"
                    KonanTarget.TVOS_ARM64 -> "appletvos"
                    KonanTarget.MACOS_X64 -> "macos"
                    else -> throw Error("Bad target ${konanTarget.name}")
                }
            }
    }
}

internal data class PodBuildSettingsProperties(
    internal val buildDir: String,
    internal val configuration: String,
    internal val cflags: String? = null,
    internal val headerPaths: String? = null,
    internal val frameworkPaths: String? = null
) {

    fun writeSettings(buildSettingsFile: File) {
        buildSettingsFile.parentFile.mkdirs()
        buildSettingsFile.createNewFile()

        check(buildSettingsFile.exists()) { "Unable to create file ${buildSettingsFile.path}!" }

        with(Properties()) {
            setProperty(BUILD_DIR, buildDir)
            setProperty(CONFIGURATION, configuration)
            cflags?.let { setProperty(OTHER_CFLAGS, it) }
            headerPaths?.let { setProperty(HEADER_SEARCH_PATHS, it) }
            frameworkPaths?.let { setProperty(FRAMEWORK_SEARCH_PATHS, it) }
            buildSettingsFile.outputStream().use {
                store(it, null)
            }
        }
    }

    companion object {
        const val BUILD_DIR: String = "BUILD_DIR"
        const val CONFIGURATION: String = "CONFIGURATION"
        const val OTHER_CFLAGS: String = "OTHER_CFLAGS"
        const val HEADER_SEARCH_PATHS: String = "HEADER_SEARCH_PATHS"
        const val FRAMEWORK_SEARCH_PATHS: String = "FRAMEWORK_SEARCH_PATHS"

        fun readSettingsFromStream(inputStream: InputStream): PodBuildSettingsProperties {
            with(Properties()) {
                load(inputStream)
                return PodBuildSettingsProperties(
                    getProperty(BUILD_DIR),
                    getProperty(CONFIGURATION),
                    getProperty(OTHER_CFLAGS),
                    getProperty(HEADER_SEARCH_PATHS),
                    getProperty(FRAMEWORK_SEARCH_PATHS)
                )
            }
        }
    }
}

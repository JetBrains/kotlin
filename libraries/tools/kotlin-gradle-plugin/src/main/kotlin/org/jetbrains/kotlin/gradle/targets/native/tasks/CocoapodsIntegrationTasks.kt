/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension
import org.jetbrains.kotlin.gradle.plugin.cocoapods.asValidFrameworkName
import org.jetbrains.kotlin.gradle.plugin.cocoapods.cocoapodsBuildDirs
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.native.tasks.PodBuildSettings.Companion.fromBuildSettingsFile
import org.jetbrains.kotlin.gradle.targets.native.tasks.PodBuildTask.Companion.toValidSDK
import org.jetbrains.kotlin.gradle.utils.newProperty
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest

/**
 * The task generates a podspec file which used by [podInstall] task.
 * This task is a part of CocoaPods integration infrastructure.
 */
open class PodGenerateSpecTask : DefaultTask() {

    private val specName = project.name.asValidFrameworkName()

    @get:Nested
    internal lateinit var cocoapodsExtension: CocoapodsExtension

    @get:OutputFile
    internal val outputFile: Property<File> = project.newProperty { project.buildDir.resolve("$specName.podspec") }


    @TaskAction
    fun generate() {
        val frameworkDir = project.cocoapodsBuildDirs.framework.relativeTo(outputFile.get().parentFile).path
        val dependencies = cocoapodsExtension.pods.map { pod ->
            val versionSuffix = if (pod.version != null) ", '${pod.version}'" else ""
            "|    spec.dependency '${pod.name}'$versionSuffix"
        }.joinToString(separator = "\n")

        outputFile.get().writeText(
            """
            |Pod::Spec.new do |spec|
            |    spec.name                     = '$specName'
            |    spec.version                  = '${cocoapodsExtension.version}'
            |    spec.homepage                 = '${cocoapodsExtension.homepage.orEmpty()}'
            |    spec.source                   = { :git => "Not Published", :tag => "Cocoapods/#{spec.name}/#{spec.version}" }
            |    spec.authors                  = '${cocoapodsExtension.authors.orEmpty()}'
            |    spec.license                  = '${cocoapodsExtension.license.orEmpty()}'
            |    spec.summary                  = '${cocoapodsExtension.summary.orEmpty()}'
            |
            |    spec.static_framework         = true
            |    spec.vendored_frameworks      = "$frameworkDir/${cocoapodsExtension.frameworkName}.framework"
            |    spec.libraries                = "c++"
            |    spec.module_name              = "#{spec.name}_umbrella"
            |
            $dependencies
            |
            |end
        """.trimMargin()
        )

        logger.quiet(
            """
            Generated a podspec file at: ${outputFile.get().absolutePath}.
            To include it in your Xcode project, add the following dependency snippet in your Podfile:

                pod '$specName', :path => '${outputFile.get().parentFile.absolutePath}'

            """.trimIndent()
        )
    }
}

/**
 * The task takes the path to the Podfile and call the `pod install`
 * to obtain sources or artifacts for the declared dependencies.
 * This task is a part of CocoaPods integration infrastructure.
 */
open class PodInstallTask : DefaultTask() {
    @get:Nested
    internal lateinit var cocoapodsExtension: CocoapodsExtension

    @get:Input
    internal lateinit var podspecFileProvider: Provider<File>

    @get:OutputDirectory
    internal val podsDirectoryProvider: Provider<File> = project.provider {
        project.projectDir
            .resolve(cocoapodsExtension.xcodeproj)
            .parentFile
            .resolve("Pods")
    }

    @get:OutputDirectory
    internal val xcWorkspaceDirProvider: Provider<File> = project.provider {
        project.projectDir
            .resolve(cocoapodsExtension.xcodeproj.replace("xcodeproj", "xcworkspace"))
    }


    @TaskAction
    fun invoke() {
        val podfileDir = project.projectDir.resolve(cocoapodsExtension.podfile).parentFile
        val podInstallProcess = ProcessBuilder("pod", "install").apply { directory(podfileDir) }.start()
        val podInstallRetCode = podInstallProcess.waitFor()
        if (podInstallRetCode != 0) throw GradleException("Unable to run 'pod install', return code $podInstallRetCode")
        val podsDirectory = podfileDir.resolve("Pods")
        check(podsDirectory.exists() && podsDirectory.isDirectory) {
            "The directory '${podsDirectory.absolutePath}' was not created as a result of the `pod install` call."
        }
        val podsXcprojFile = podsDirectory.resolve("Pods.xcodeproj")
        check(podsXcprojFile.exists() && podsXcprojFile.isDirectory) {
            "The directory '${podsXcprojFile.absolutePath}' was not created as a result of the `pod install` call."
        }
    }
}

open class PodSetupBuildTask : DefaultTask() {
    @get:Nested
    internal lateinit var cocoapodsExtension: CocoapodsExtension

    @get:InputDirectory
    internal val xcWorkspaceDirProvider: Provider<File> = project.provider {
        project.projectDir
            .resolve(cocoapodsExtension.xcodeproj.replace("xcodeproj", "xcworkspace"))
    }

    @Internal
    lateinit var kotlinNativeTarget: KotlinNativeTarget

    @get:OutputFile
    internal val buildSettingsFileProvider: Provider<File> =
        project.provider { project.buildDir.resolve(kotlinNativeTarget.toBuildSettingsFileName) }

    @TaskAction
    fun invoke() {
        val xcWorkspaceDir = xcWorkspaceDirProvider.get().parentFile

        with(xcWorkspaceDir) {
            if (!exists() || !isDirectory || listFiles().orEmpty().isEmpty()) {
                throw GradleException("Failed to create $name!")
            }
        }

        val buildSettingsProcess = ProcessBuilder(
            "xcodebuild", "-showBuildSettings",
            "-workspace", xcWorkspaceDirProvider.get().name,
            "-scheme", project.name.asValidFrameworkName(),
            "-configuration", DEFAULT_CONFIGURATION,
            "-sdk", kotlinNativeTarget.toValidSDK
        ).apply {
            directory(xcWorkspaceDir)
        }.start()

        val buildSettingsRetCode = buildSettingsProcess.waitFor()
        if (buildSettingsRetCode != 0) throw GradleException(
            "Unable to run 'xcodebuild -showBuildSettings " +
                    "-workspace ${xcWorkspaceDirProvider.get().name} " +
                    "-scheme ${project.name.asValidFrameworkName()} " +
                    "-configuration $DEFAULT_CONFIGURATION " +
                    "-sdk ${kotlinNativeTarget.toValidSDK}' " +
                    "return code $buildSettingsRetCode"
        )

        val stdOut = buildSettingsProcess.inputStream.bufferedReader().use { it.readText() }

        val buildParameters = stdOut.lines()
            .asSequence()
            .filter { it.matches("^(.+) = (.+)$".toRegex()) }
            .map { val (k, v) = it.split(" = "); k to v }
            .map { it.first.trim() to it.second.trim() }
            .toMap()

        PodBuildSettings.fromBuildSettingsMap(buildParameters).settingsToFile(buildSettingsFileProvider.get())
    }

    companion object {
        private const val BUILD_SETTINGS_FILE_PREFIX: String = "build-settings"
        private const val DEFAULT_CONFIGURATION: String = "Release"

        internal val KotlinNativeTarget.toBuildSettingsFileName: String
            get() = "${BUILD_SETTINGS_FILE_PREFIX}-$disambiguationClassifier.txt"

    }
}

/**
 * The task compile pod sources and cinterop there artifacts.
 */
open class PodBuildTask : DefaultTask() {
    @get:Nested
    internal lateinit var cocoapodsExtension: CocoapodsExtension

    @get:InputDirectory
    internal val xcWorkspaceDirProvider: Provider<File> = project.provider {
        project.projectDir
            .resolve(cocoapodsExtension.xcodeproj.replace("xcodeproj", "xcworkspace"))
    }

    @get:InputFile
    internal lateinit var buildSettingsFileProvider: Provider<File>

    @Internal
    lateinit var kotlinNativeTarget: KotlinNativeTarget

    @get:OutputFile
    internal val buildDirHashFileProvider: Provider<File> =
        project.provider { project.buildDir.resolve(kotlinNativeTarget.toBuildDirHashSumFileName) }

    @TaskAction
    fun invoke() {
        val xcWorkspaceDir = xcWorkspaceDirProvider.get().parentFile

        val podBuildSettings = fromBuildSettingsFile(buildSettingsFileProvider.get())

        val podBuildProcess = ProcessBuilder(
            "xcodebuild", "-workspace", xcWorkspaceDirProvider.get().name,
            "-scheme", project.name.asValidFrameworkName(),
            "-configuration", podBuildSettings.configuration,
            "-sdk", kotlinNativeTarget.toValidSDK
        ).apply {
            directory(xcWorkspaceDir)
            inheritIO()
        }.start()

        val podBuildRetCode = podBuildProcess.waitFor()
        if (podBuildRetCode != 0) throw GradleException(
            "Unable to run 'xcodebuild " +
                    "-workspace ${xcWorkspaceDirProvider.get().name} " +
                    "-scheme ${project.name.asValidFrameworkName()} " +
                    "-configuration ${podBuildSettings.configuration} " +
                    "-sdk ${kotlinNativeTarget.toValidSDK}' " +
                    "return code $podBuildRetCode"
        )
        buildDirHashFileProvider.get().writeText(getFileChecksumStr(project.file(podBuildSettings.buildDir)))
    }

    companion object {
        private const val BUILD_DIR_HASH_SUM_FILE_PREFIX: String = "build-dir-hash"

        private fun getFileChecksumStr(file: File): String {
            val digest = MessageDigest.getInstance("SHA-1")
            file.walkTopDown().forEach { file ->
                val byteArray = ByteArray(1024)
                var bytesCount = 0
                if (file.isFile) {
                    val inputStream = file.inputStream()
                    while (inputStream.read(byteArray).also({ bytesCount = it }) != -1) {
                        digest.update(byteArray, 0, bytesCount)
                    }
                }
            }
            val buildDirHashSumStr = BigInteger(1, digest.digest()).toString(16).padStart(32, '0')
            return buildDirHashSumStr
        }

        internal val KotlinNativeTarget.toBuildDirHashSumFileName: String
            get() = "$BUILD_DIR_HASH_SUM_FILE_PREFIX-$disambiguationClassifier.txt"

        internal val KotlinNativeTarget.toValidSDK: String
            get() {
                return when (konanTarget) {
                    KonanTarget.IOS_X64 -> "iphonesimulator"
                    KonanTarget.IOS_ARM32, KonanTarget.IOS_ARM64 -> "iphoneos"
                    KonanTarget.WATCHOS_X86, KonanTarget.WATCHOS_X64 -> "watchsimulator"
                    KonanTarget.WATCHOS_ARM32, KonanTarget.WATCHOS_ARM64 -> "watchos"
                    KonanTarget.TVOS_X64 -> "appletvsimulator"
                    KonanTarget.TVOS_ARM64 -> "appletvos"
                    KonanTarget.MACOS_X64 -> "macosx10.15"
                    else -> throw Error("Bad target ${konanTarget.name}")
                }
            }
    }
}

internal data class PodBuildSettings(
    internal val target: String,
    internal val buildDir: String,
    internal val configuration: String,
    internal val cflags: String? = null,
    internal val headerPaths: String? = null,
    internal val frameworkPaths: String? = null
) {
    fun settingsToFile(buildSettingsFile: File) {
        buildSettingsFile.writeText(
            """
                $PLATFORM_NAME = $target
                $BUILD_DIR = $buildDir
                $CONFIGURATION = $configuration
                ${cflags?.let { "$OTHER_CFLAGS = $cflags" } ?: ""}
                ${headerPaths?.run { "$HEADER_SEARCH_PATHS = $headerPaths" } ?: ""}
                ${frameworkPaths?.run { "$FRAMEWORK_SEARCH_PATHS = $frameworkPaths" } ?: ""}
                """.trimIndent()
        )
    }


    companion object {
        const val BUILD_DIR: String = "BUILD_DIR"
        const val PLATFORM_NAME: String = "PLATFORM_NAME"
        const val CONFIGURATION: String = "CONFIGURATION"
        const val OTHER_CFLAGS: String = "OTHER_CFLAGS"
        const val HEADER_SEARCH_PATHS: String = "HEADER_SEARCH_PATHS"
        const val FRAMEWORK_SEARCH_PATHS: String = "FRAMEWORK_SEARCH_PATHS"


        fun fromBuildSettingsFile(buildSettingsFile: File): PodBuildSettings {
            val buildSettingsMap = buildSettingsFile.readLines()
                .asSequence()
                .filter { it.matches("^(.+) = (.+)$".toRegex()) }
                .map { val (k, v) = it.split(" = "); k to v }
                .map { it.first.trim() to it.second.trim() }
                .toMap()
            return PodBuildSettings(
                buildSettingsMap[PLATFORM_NAME]!!,
                buildSettingsMap[BUILD_DIR]!!,
                buildSettingsMap[CONFIGURATION]!!,
                buildSettingsMap[OTHER_CFLAGS],
                buildSettingsMap[HEADER_SEARCH_PATHS],
                buildSettingsMap[FRAMEWORK_SEARCH_PATHS]
            )
        }

        fun fromBuildSettingsMap(buildSettingsMap: Map<String, String>): PodBuildSettings {
            return PodBuildSettings(
                buildSettingsMap[PLATFORM_NAME]!!,
                buildSettingsMap[BUILD_DIR]!!,
                buildSettingsMap[CONFIGURATION]!!,
                buildSettingsMap[OTHER_CFLAGS],
                buildSettingsMap[HEADER_SEARCH_PATHS],
                buildSettingsMap[FRAMEWORK_SEARCH_PATHS]
            )
        }
    }
}


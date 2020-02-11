/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension
import org.jetbrains.kotlin.gradle.plugin.cocoapods.asValidFrameworkName
import org.jetbrains.kotlin.gradle.plugin.cocoapods.cocoapodsBuildDirs
import org.jetbrains.kotlin.gradle.utils.newProperty
import java.io.File

/**
 * The task generates a podspec file which used by [podInstall] task.
 * This task is a part of CocoaPods integration infrastructure.
 */
open class PodGenerateSpecTask : DefaultTask() {

    private val specName = project.name.asValidFrameworkName()

    @get:Nested
    internal lateinit var settings: CocoapodsExtension

    @get:OutputFile
    internal val outputFile: Property<File> = project.newProperty { project.buildDir.resolve("$specName.podspec") }


    @TaskAction
    fun generate() {
        val frameworkDir = project.cocoapodsBuildDirs.framework.relativeTo(outputFile.get().parentFile).path
        val dependencies = settings.pods.map { pod ->
            val versionSuffix = if (pod.version != null) ", '${pod.version}'" else ""
            "|    spec.dependency '${pod.name}'$versionSuffix"
        }.joinToString(separator = "\n")

        outputFile.get().writeText(
            """
            |Pod::Spec.new do |spec|
            |    spec.name                     = '$specName'
            |    spec.version                  = '${settings.version}'
            |    spec.homepage                 = '${settings.homepage.orEmpty()}'
            |    spec.source                   = { :git => "Not Published", :tag => "Cocoapods/#{spec.name}/#{spec.version}" }
            |    spec.authors                  = '${settings.authors.orEmpty()}'
            |    spec.license                  = '${settings.license.orEmpty()}'
            |    spec.summary                  = '${settings.summary.orEmpty()}'
            |
            |    spec.static_framework         = true
            |    spec.vendored_frameworks      = "$frameworkDir/${settings.frameworkName}.framework"
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
    internal lateinit var settings: CocoapodsExtension

    @get:OutputDirectory
    internal val podsXcprojFileProvider: Provider<File> = project.provider {
        project.projectDir
            .resolve(settings.xcodeproj)
            .parentFile
            .resolve("Pods")
            .resolve("Pods.xcodeproj")
    }

    @get:OutputDirectory
    internal val xcWorkspaceFileProvider: Provider<File> = project.provider {
        project.projectDir
            .resolve(settings.xcodeproj.replace("xcodeproj", "xcworkspace"))
    }


    @TaskAction
    fun invoke() {
        val podfileDir = project.projectDir.resolve(settings.podfile).parentFile
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

open class PodBuildSettingsTask : DefaultTask() {
    @get:Nested
    internal lateinit var settings: CocoapodsExtension

    @get:Nested
    internal lateinit var buildSettings: PodBuildSettings

    @get:InputDirectory
    internal lateinit var xcWorkspaceDirProvider: Provider<File>

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
            "-scheme", project.name.asValidFrameworkName()
        ).apply {
            directory(xcWorkspaceDir)
        }.start()

        val buildSettingsRetCode = buildSettingsProcess.waitFor()
        if (buildSettingsRetCode != 0) throw GradleException(
            "Unable to run 'xcodebuild -showBuildSettings " +
                    "-workspace ${xcWorkspaceDirProvider.get().name} " +
                    "-scheme ${project.name.asValidFrameworkName()} " +
                    "return code $buildSettingsRetCode"
        )

        val stdOut = buildSettingsProcess.inputStream.bufferedReader().use { it.readText() }

        val buildParameters = stdOut.lines()
            .asSequence()
            .filter { it.matches("^(.+) = (.+)$".toRegex()) }
            .map { val (k, v) = it.split(" = "); k to v }
            .map { it.first.trim() to it.second.trim() }
            .toMap()

        with(buildSettings) {
            target = buildParameters[PLATFORM_NAME]
            configuration = buildParameters[CONFIGURATION]
            cflags = buildParameters[OTHER_CFLAGS]
            headerPaths = buildParameters[HEADER_SEARCH_PATHS]
            frameworkPaths = buildParameters[FRAMEWORK_SEARCH_PATHS]
        }
    }

    companion object {
        const val PODS_SRC_DIR: String = "PODS_TARGET_SRCROOT"
        const val PLATFORM_NAME: String = "PLATFORM_NAME"
        const val CONFIGURATION: String = "CONFIGURATION"
        const val OTHER_CFLAGS: String = "OTHER_CFLAGS"
        const val HEADER_SEARCH_PATHS: String = "HEADER_SEARCH_PATHS"
        const val FRAMEWORK_SEARCH_PATHS: String = "FRAMEWORK_SEARCH_PATHS"

    }

}


/**
 * The task compile pod sources and cinterop there artifacts.
 */
open class PodBuildDependenciesTask : DefaultTask() {
    @get:InputDirectory
    internal lateinit var xcWorkspaceDirProvider: Provider<File>

    @get:Nested
    internal lateinit var buildSettings: PodBuildSettings

    @TaskAction
    fun invoke() {

        val xcWorkspaceDir = xcWorkspaceDirProvider.get().parentFile

        //TODO replace hardcode configuration and sdk to extracting from build settings
        val podBuildProcess = ProcessBuilder(
            "xcodebuild", "-workspace", xcWorkspaceDirProvider.get().name,
            "-scheme", project.name.asValidFrameworkName(),
            "-configuration", buildSettings.configuration,
            "-sdk", buildSettings.target
        ).apply {
            directory(xcWorkspaceDir)
            inheritIO()
        }.start()

        val podBuildRetCode = podBuildProcess.waitFor()
        if (podBuildRetCode != 0) throw GradleException(
            "Unable to run 'xcodebuild " +
                    "-workspace ${xcWorkspaceDirProvider.get().name} " +
                    "-scheme ${project.name.asValidFrameworkName()}' " +
                    "-configuration ${buildSettings.configuration}" +
                    "-sdk ${buildSettings.target}' " +
                    "return code $podBuildRetCode"
        )
    }

}

/**
 * The task compile pod sources and prepares environment variables
 * for the following calls of the cinterop task on pod.
 */
open class PodImportTask : DefaultTask() {
    @get:Nested
    internal lateinit var settings: CocoapodsExtension


}


internal data class PodBuildSettings(private val project: Project) {

    /**
     * Configure kotlin target for the future `cinterop -target` call argument.
     */
    @Optional
    @Input
    var target: String? = null

    /**
     * Configure DEBUG or RELEASE for the name of the framework
     * produced by cinterop call.
     */
    @Optional
    @Input
    var configuration: String? = null

    /**
     * Configure extra compiler options passed to the cinterop call
     * with a key "-compiler-option".
     */
    @Optional
    @Input
    var cflags: String? = null

    /**
     * Configure extra compiler options passed to the cinterop call
     * with a key "-compiler-option -I".
     */
    @Optional
    @Input
    var headerPaths: String? = null

    /**
     * Configure extra compiler options passed to the cinterop call
     * with a key "-compiler-option -F".
     */
    @Optional
    @Input
    var frameworkPaths: String? = null
}

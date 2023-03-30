/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.wrapper.Wrapper
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension.CocoapodsDependency
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension.PodspecPlatformSettings
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.COCOAPODS_EXTENSION_NAME
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.GENERATE_WRAPPER_PROPERTY
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.SYNC_TASK_NAME
import org.jetbrains.kotlin.gradle.plugin.cocoapods.cocoapodsBuildDirs
import java.io.File

/**
 * The task generates a podspec file which allows a user to
 * integrate a Kotlin/Native framework into a CocoaPods project.
 */
open class PodspecTask : DefaultTask() {

    @get:Input
    internal val specName = project.objects.property(String::class.java)

    @get:Internal
    internal val outputDir = project.objects.property(File::class.java)

    @get:OutputFile
    val outputFile: File
        get() = outputDir.get().resolve("${specName.get()}.podspec")

    @get:Input
    internal lateinit var needPodspec: Provider<Boolean>

    @get:Nested
    val pods = project.objects.listProperty(CocoapodsDependency::class.java)

    @get:Input
    internal val version = project.objects.property(String::class.java)

    @get:Input
    internal val publishing = project.objects.property(Boolean::class.java)

    @get:Input
    @get:Optional
    internal val source = project.objects.property(String::class.java)

    @get:Input
    @get:Optional
    internal val homepage = project.objects.property(String::class.java)

    @get:Input
    @get:Optional
    internal val license = project.objects.property(String::class.java)

    @get:Input
    @get:Optional
    internal val authors = project.objects.property(String::class.java)

    @get:Input
    @get:Optional
    internal val summary = project.objects.property(String::class.java)

    @get:Input
    @get:Optional
    internal val extraSpecAttributes = project.objects.mapProperty(String::class.java, String::class.java)

    @get:Input
    internal lateinit var frameworkName: Provider<String>

    @get:Nested
    internal lateinit var ios: Provider<PodspecPlatformSettings>

    @get:Nested
    internal lateinit var osx: Provider<PodspecPlatformSettings>

    @get:Nested
    internal lateinit var tvos: Provider<PodspecPlatformSettings>

    @get:Nested
    internal lateinit var watchos: Provider<PodspecPlatformSettings>

    init {
        onlyIf { needPodspec.get() }
    }

    @TaskAction
    fun generate() {

        check(version.get() != Project.DEFAULT_VERSION) {
            """
                Cocoapods Integration requires pod version to be specified.
                Please specify pod version by adding 'version = "<version>"' to the cocoapods block.
                Alternatively, specify the version for the entire project explicitly. 
                Pod version format has to conform podspec syntax requirements: https://guides.cocoapods.org/syntax/podspec.html#version 
            """.trimIndent()
        }

        val gradleWrapper = (project.rootProject.tasks.getByName("wrapper") as? Wrapper)?.scriptFile
        require(gradleWrapper != null && gradleWrapper.exists()) {
            """
            The Gradle wrapper is required to run the build from Xcode.

            Please run the same command with `-P$GENERATE_WRAPPER_PROPERTY=true` or run the `:wrapper` task to generate the wrapper manually.

            See details about the wrapper at https://docs.gradle.org/current/userguide/gradle_wrapper.html
            """.trimIndent()
        }

        val deploymentTargets = run {
            listOf(ios, osx, tvos, watchos).map { it.get() }.filter { it.deploymentTarget != null }.joinToString("\n") {
                if (extraSpecAttributes.get().containsKey("${it.name}.deployment_target")) "" else "|    spec.${it.name}.deployment_target = '${it.deploymentTarget}'"
            }
        }

        val dependencies = pods.get().map { pod ->
            val versionSuffix = if (pod.version != null) ", '${pod.version}'" else ""
            "|    spec.dependency '${pod.name}'$versionSuffix"
        }.joinToString(separator = "\n")

        val frameworkDir = project.cocoapodsBuildDirs.framework.relativeTo(outputFile.parentFile)
        val vendoredFramework = if (publishing.get()) "${frameworkName.get()}.xcframework" else frameworkDir.resolve("${frameworkName.get()}.framework").invariantSeparatorsPath
        val vendoredFrameworks = if (extraSpecAttributes.get().containsKey("vendored_frameworks")) "" else "|    spec.vendored_frameworks      = '$vendoredFramework'"

        val libraries = if (extraSpecAttributes.get().containsKey("libraries")) "" else "|    spec.libraries                = 'c++'"

        val xcConfig = if (publishing.get() || extraSpecAttributes.get().containsKey("pod_target_xcconfig")) "" else
            """ |
                |    spec.pod_target_xcconfig = {
                |        'KOTLIN_PROJECT_PATH' => '${if (project.depth != 0) project.path else ""}',
                |        'PRODUCT_MODULE_NAME' => '${frameworkName.get()}',
                |    }
            """.trimMargin()

        val gradleCommand = "\$REPO_ROOT/${gradleWrapper.relativeTo(project.projectDir).invariantSeparatorsPath}"
        val scriptPhase = if (publishing.get() || extraSpecAttributes.get().containsKey("script_phases")) "" else
            """ |
                |    spec.script_phases = [
                |        {
                |            :name => 'Build ${specName.get()}',
                |            :execution_position => :before_compile,
                |            :shell_path => '/bin/sh',
                |            :script => <<-SCRIPT
                |                if [ "YES" = "${'$'}OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED" ]; then
                |                  echo "Skipping Gradle build task invocation due to OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED environment variable set to \"YES\""
                |                  exit 0
                |                fi
                |                set -ev
                |                REPO_ROOT="${'$'}PODS_TARGET_SRCROOT"
                |                "$gradleCommand" -p "${'$'}REPO_ROOT" ${'$'}KOTLIN_PROJECT_PATH:$SYNC_TASK_NAME \
                |                    -P${KotlinCocoapodsPlugin.PLATFORM_PROPERTY}=${'$'}PLATFORM_NAME \
                |                    -P${KotlinCocoapodsPlugin.ARCHS_PROPERTY}="${'$'}ARCHS" \
                |                    -P${KotlinCocoapodsPlugin.CONFIGURATION_PROPERTY}="${'$'}CONFIGURATION"
                |            SCRIPT
                |        }
                |    ]
        """.trimMargin()

        val customSpec = extraSpecAttributes.get().map { "|    spec.${it.key} = ${it.value}" }.joinToString("\n")

        with(outputFile) {
            writeText(
                """
                |Pod::Spec.new do |spec|
                |    spec.name                     = '${specName.get()}'
                |    spec.version                  = '${version.get()}'
                |    spec.homepage                 = ${homepage.getOrEmpty().surroundWithSingleQuotesIfNeeded()}
                |    spec.source                   = ${source.getOrElse("{ :http=> ''}")}
                |    spec.authors                  = ${authors.getOrEmpty().surroundWithSingleQuotesIfNeeded()}
                |    spec.license                  = ${license.getOrEmpty().surroundWithSingleQuotesIfNeeded()}
                |    spec.summary                  = '${summary.getOrEmpty()}'
                $vendoredFrameworks
                $libraries
                $deploymentTargets
                $dependencies
                $xcConfig
                $scriptPhase
                $customSpec
                |end
        """.trimMargin()
            )

            if (hasPodfileOwnOrParent(project) && publishing.get().not()) {
                logger.quiet(
                    """
                    Generated a podspec file at: ${absolutePath}.
                    To include it in your Xcode project, check that the following dependency snippet exists in your Podfile:

                    pod '${specName.get()}', :path => '${parentFile.absolutePath}'

            """.trimIndent()
                )
            }

        }
    }

    private fun Provider<String>.getOrEmpty(): String = getOrElse("")

    private fun String.surroundWithSingleQuotesIfNeeded(): String =
        if (startsWith("{") || startsWith("<<-") || startsWith("'")) this else "'$this'"

    companion object {
        private val KotlinMultiplatformExtension?.cocoapodsExtensionOrNull: CocoapodsExtension?
            get() = (this as? ExtensionAware)?.extensions?.findByName(COCOAPODS_EXTENSION_NAME) as? CocoapodsExtension

        private fun hasPodfileOwnOrParent(project: Project): Boolean =
            if (project.rootProject == project) project.multiplatformExtensionOrNull?.cocoapodsExtensionOrNull?.podfile != null
            else project.multiplatformExtensionOrNull?.cocoapodsExtensionOrNull?.podfile != null
                    || (project.parent?.let { hasPodfileOwnOrParent(it) } ?: false)

    }
}

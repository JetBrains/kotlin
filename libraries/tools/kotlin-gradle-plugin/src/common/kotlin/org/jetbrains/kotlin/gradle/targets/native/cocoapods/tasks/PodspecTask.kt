/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch", "LeakingThis") // Old package for compatibility
package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension.CocoapodsDependency
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension.PodspecPlatformSettings
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.DUMMY_FRAMEWORK_TASK_NAME
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.GENERATE_WRAPPER_PROPERTY
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.POD_INSTALL_TASK_NAME
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.SYNC_TASK_NAME
import org.jetbrains.kotlin.gradle.plugin.cocoapods.cocoapodsBuildDirs
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File
import javax.inject.Inject

/**
 * The task generates a podspec file which allows a user to
 * integrate a Kotlin/Native framework into a CocoaPods project.
 */
@DisableCachingByDefault
abstract class PodspecTask @Inject constructor(private val projectLayout: ProjectLayout) : DefaultTask() {

    @get:Input
    internal abstract val specName: Property<String>

    @get:Internal
    internal abstract val outputDir: Property<File>

    @get:OutputFile
    val outputFile: File
        get() = outputDir.get().resolve("${specName.get()}.podspec")

    @get:Input
    internal abstract val needPodspec: Property<Boolean>

    @get:Nested
    abstract val pods: ListProperty<CocoapodsDependency>

    @get:Input
    internal abstract val version: Property<String>

    @get:Input
    internal abstract val publishing: Property<Boolean>

    @get:Input
    @get:Optional
    internal abstract val source: Property<String>

    @get:Input
    @get:Optional
    internal abstract val homepage: Property<String>

    @get:Input
    @get:Optional
    internal abstract val license: Property<String>

    @get:Input
    @get:Optional
    internal abstract val authors: Property<String>

    @get:Input
    @get:Optional
    internal abstract val summary: Property<String>

    @get:Input
    @get:Optional
    internal abstract val extraSpecAttributes: MapProperty<String, String>

    @get:Input
    internal abstract val frameworkName: Property<String>

    @get:Nested
    internal abstract val ios: Property<PodspecPlatformSettings>

    @get:Nested
    internal abstract val osx: Property<PodspecPlatformSettings>

    @get:Nested
    internal abstract val tvos: Property<PodspecPlatformSettings>

    @get:Nested
    internal abstract val watchos: Property<PodspecPlatformSettings>

    @get:Input
    @get:Optional
    internal abstract val gradleWrapperPath: Property<String?>

    @get:Input
    internal abstract val projectPath: Property<String>

    @get:Input
    internal abstract val hasPodfile: Property<Boolean>

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

        val deploymentTargets = run {
            listOf(ios, osx, tvos, watchos).map { it.get() }.filter { it.deploymentTarget != null }.joinToString("\n") {
                if (extraSpecAttributes.get()
                        .containsKey("${it.name}.deployment_target")
                ) "" else "|    spec.${it.name}.deployment_target    = '${it.deploymentTarget}'"
            }
        }

        val dependencies = pods.get().joinToString(separator = "\n") { pod ->
            val versionSuffix = if (pod.version != null) ", '${pod.version}'" else ""
            "|    spec.dependency '${pod.name}'$versionSuffix"
        }

        val frameworkDir = projectLayout.cocoapodsBuildDirs.framework.getFile().relativeTo(outputFile.parentFile)
        val vendoredFramework = if (publishing.get()) "${frameworkName.get()}.xcframework" else frameworkDir.resolve("${frameworkName.get()}.framework").invariantSeparatorsPath
        val vendoredFrameworksOverridden = extraSpecAttributes.get().containsKey("vendored_frameworks")
        val vendoredFrameworks = if (vendoredFrameworksOverridden) "" else "|    spec.vendored_frameworks      = '$vendoredFramework'"

        val vendoredFrameworkExistenceCheck = if (vendoredFrameworksOverridden || publishing.get()) "" else
            """ |
                |    if !Dir.exist?('$vendoredFramework') || Dir.empty?('$vendoredFramework')
                |        raise "
                |
                |        Kotlin framework '${frameworkName.get()}' doesn't exist yet, so a proper Xcode project can't be generated.
                |        'pod install' should be executed after running ':$DUMMY_FRAMEWORK_TASK_NAME' Gradle task:
                |
                |            ./gradlew ${projectPath.get()}:$DUMMY_FRAMEWORK_TASK_NAME
                |
                |        Alternatively, proper pod installation is performed during Gradle sync in the IDE (if Podfile location is set)"
                |    end
            """.trimMargin()

        val libraries = if (extraSpecAttributes.get().containsKey("libraries")) "" else "|    spec.libraries                = 'c++'"

        val xcConfig = if (publishing.get() || extraSpecAttributes.get().containsKey("xcconfig")) "" else
            """ |
                |    spec.xcconfig = {
                |        'ENABLE_USER_SCRIPT_SANDBOXING' => 'NO',
                |    }
            """.trimMargin()

        val podXcConfig = if (publishing.get() || extraSpecAttributes.get().containsKey("pod_target_xcconfig")) "" else
            """ |
                |    spec.pod_target_xcconfig = {
                |        'KOTLIN_PROJECT_PATH' => '${projectPath.get()}',
                |        'PRODUCT_MODULE_NAME' => '${frameworkName.get()}',
                |    }
            """.trimMargin()

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
                |                "${gradleCommand()}" -p "${'$'}REPO_ROOT" ${'$'}KOTLIN_PROJECT_PATH:$SYNC_TASK_NAME \
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
                $vendoredFrameworkExistenceCheck
                $xcConfig
                $podXcConfig
                $scriptPhase
                $customSpec
                |end
        """.trimMargin()
            )

            if (hasPodfile.get() && !publishing.get()) {
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

    private fun gradleCommand(): String {
        val gradleWrapperPath: String? = gradleWrapperPath.get()
        val gradleWrapper = gradleWrapperPath?.let(::File)
        require(gradleWrapper != null && gradleWrapper.exists()) {
            """
            The Gradle wrapper is required to run the build from Xcode.

            Please run the same command with `-P$GENERATE_WRAPPER_PROPERTY=true` or run the `:wrapper` task to generate the wrapper manually.

            See details about the wrapper at https://docs.gradle.org/current/userguide/gradle_wrapper.html
            """.trimIndent()
        }

        return "\$REPO_ROOT/${gradleWrapper.relativeTo(projectLayout.projectDirectory.asFile).invariantSeparatorsPath}"
    }

    private fun Provider<String>.getOrEmpty(): String = getOrElse("")

    private fun String.surroundWithSingleQuotesIfNeeded(): String =
        if (startsWith("{") || startsWith("<<-") || startsWith("'")) this else "'$this'"

}

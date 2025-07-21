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
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.SYNC_TASK_NAME
import org.jetbrains.kotlin.gradle.plugin.cocoapods.cocoapodsBuildDirs
import org.jetbrains.kotlin.gradle.utils.buildStringBlock
import org.jetbrains.kotlin.gradle.utils.connectedLines
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
    internal abstract val gradleWrapperFile: Property<File?>

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

        val publishing = publishing.get()
        val specName = specName.get()
        val frameworkName = frameworkName.get()
        val extraSpecAttributes = extraSpecAttributes.get()

        val frameworkDir = projectLayout.cocoapodsBuildDirs.framework.getFile().relativeTo(outputFile.parentFile)
        val vendoredFramework =
            if (publishing) {
                "${frameworkName}.xcframework"
            } else {
                frameworkDir.resolve("${frameworkName}.framework").invariantSeparatorsPath
            }
        val vendoredFrameworksOverridden = "vendored_frameworks" in extraSpecAttributes


        val podspec = buildStringBlock {
            block("Pod::Spec.new do |spec|", "end") {
                line("spec.name                     = '${specName}'")
                line("spec.version                  = '${version.get()}'")
                line("spec.homepage                 = ${homepage.getOrEmpty().surroundWithSingleQuotesIfNeeded()}")
                line("spec.source                   = ${source.getOrElse("{ :http=> ''}")}")
                line("spec.authors                  = ${authors.getOrEmpty().surroundWithSingleQuotesIfNeeded()}")
                line("spec.license                  = ${license.getOrEmpty().surroundWithSingleQuotesIfNeeded()}")
                line("spec.summary                  = '${summary.getOrEmpty()}'")
                if (!vendoredFrameworksOverridden) {
                    line("spec.vendored_frameworks      = '$vendoredFramework'")
                }
                if ("libraries" !in extraSpecAttributes) {
                    line("spec.libraries                = 'c++'")
                }

                listOf(ios, osx, tvos, watchos)
                    .map { it.get() }
                    .filter { it.deploymentTarget != null }
                    .forEach {
                        if ("${it.name}.deployment_target" !in extraSpecAttributes) {
                            line("spec.${it.name}.deployment_target    = '${it.deploymentTarget}'")
                        }
                    }

                pods.get().forEach { pod ->
                    val versionSuffix = if (pod.version != null) ", '${pod.version}'" else ""
                    line("spec.dependency '${pod.name}'$versionSuffix")
                }

                if (!(vendoredFrameworksOverridden || publishing)) {
                    block("if !Dir.exist?('$vendoredFramework') || Dir.empty?('$vendoredFramework')", "end") {
                        line("raise \"")
                        line("Kotlin framework '${frameworkName}' doesn't exist yet, so a proper Xcode project can't be generated.")
                        line("'pod install' should be executed after running ':$DUMMY_FRAMEWORK_TASK_NAME' Gradle task:")
                        line("    ./gradlew ${projectPath.get()}:$DUMMY_FRAMEWORK_TASK_NAME")
                        line("Alternatively, proper pod installation is performed during Gradle sync in the IDE (if Podfile location is set)\"")
                    }
                }

                if (!(publishing || extraSpecAttributes.containsKey("xcconfig"))) {
                    block("spec.xcconfig = {", "}") {
                        line("'ENABLE_USER_SCRIPT_SANDBOXING' => 'NO',")
                    }
                }

                if (!(publishing || extraSpecAttributes.containsKey("pod_target_xcconfig"))) {
                    block("spec.pod_target_xcconfig = {", "}") {
                        line("'KOTLIN_PROJECT_PATH' => '${projectPath.get()}',")
                        line("'PRODUCT_MODULE_NAME' => '${frameworkName}',")
                    }
                }
                if (!(publishing || extraSpecAttributes.containsKey("script_phases"))) {
                    block("spec.script_phases = [", "]") {
                        block("{", "}") {
                            line(":name => 'Build ${specName}',")
                            line(":execution_position => :before_compile,")
                            line(":shell_path => '/bin/sh',")
                            block(":script => <<-SCRIPT", "SCRIPT") {
                                block("if [ \"YES\" = \"\$OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED\" ]; then", "fi") {
                                    line("echo \"Skipping Gradle build task invocation due to OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED environment variable set to \\\"YES\\\"\"")
                                    line("exit 0")
                                }
                                line("set -ev")
                                line("REPO_ROOT=\"\$PODS_TARGET_SRCROOT\"")
                                connectedLines(" \\") {
                                    line(""""${gradleCommand()}" -p "${'$'}REPO_ROOT" ${'$'}KOTLIN_PROJECT_PATH:$SYNC_TASK_NAME""")
                                    line("-P${KotlinCocoapodsPlugin.PLATFORM_PROPERTY}=\$PLATFORM_NAME")
                                    line("-P${KotlinCocoapodsPlugin.ARCHS_PROPERTY}=\"\$ARCHS\"")
                                    line("-P${KotlinCocoapodsPlugin.CONFIGURATION_PROPERTY}=\"\$CONFIGURATION\"")
                                }
                            }
                        }
                    }
                }

                extraSpecAttributes.forEach { (key, value) ->
                    line("spec.$key = $value")
                }
            }
        }

        outputFile.writeText(podspec)

        if (hasPodfile.get() && !publishing) {
            logger.quiet(
                """
                Generated a podspec file at: ${outputFile.absolutePath}.
                To include it in your Xcode project, check that the following dependency snippet exists in your Podfile:

                pod '${specName}', :path => '${outputFile.parentFile.absolutePath}'
                """.trimIndent()
            )
        }
    }

    private fun gradleCommand(): String {
        val gradleWrapper = gradleWrapperFile.orNull
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

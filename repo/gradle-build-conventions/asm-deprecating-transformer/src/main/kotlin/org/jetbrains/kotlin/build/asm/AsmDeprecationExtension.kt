/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.asm

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Attribute
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import java.io.File
import javax.inject.Inject

/**
 * Allows configuring deprecation of classes in artifacts included in fat jars.
 * Context: https://youtrack.jetbrains.com/issue/KT-70251
 */
abstract class AsmDeprecationExtension @Inject constructor(
    private val dependencies: DependencyHandler,
    private val tasks: TaskContainer,
) {
    private var deprecationSpec: DeprecationSpec? = null
    private val deprecationReportTasks = mutableMapOf<String, TaskProvider<DeprecationReportTask>>()

    /**
     * @param pattern Transforms class FQN-like pattern to file names pattern
     * Examples:
     * * `org.example.**` -> `org/example/**/*.class`
     * * `org.example.Class` -> `org/example/Class.class`
     */
    fun deprecateClassesByPattern(
        inputConfigurations: Iterable<Configuration>,
        pattern: String,
        deprecationMessage: String,
        exclusions: List<String> = emptyList(),
    ) {
        check(deprecationSpec == null) { "Class deprecation can only be configured once per project" }
        deprecationSpec = DeprecationSpec(pattern, exclusions)

        dependencies.artifactTypes.maybeCreate(ArtifactTypeDefinition.JAR_TYPE).attributes
            .attribute(ASM_DEPRECATED_ATTRIBUTE, false)

        dependencies.registerTransform(DeprecatingArtifactTransform::class.java) {
            from.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JAR_TYPE)
            from.attribute(ASM_DEPRECATED_ATTRIBUTE, false)
            to.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JAR_TYPE)
            to.attribute(ASM_DEPRECATED_ATTRIBUTE, true)
            parameters {
                this.pattern.set(pattern)
                this.deprecationMessage.set(deprecationMessage)
                this.exclusions.set(exclusions)
            }
        }
        inputConfigurations.forEach { configuration ->
            configuration.attributes.attribute(ASM_DEPRECATED_ATTRIBUTE, true)
        }
    }

    fun TaskContainer.registerDumpDeprecationsTask(shadowJarTaskName: String, suffix: String): TaskProvider<Copy> {
        val reportTask = deprecationReportTask(shadowJarTaskName)
        return register<Copy>("dumpDeprecationsFor${suffix}") {
            from(reportTask.flatMap { it.outputFile })
            into(project.layout.projectDirectory.dir(BUILD_DIRECTORY))
            rename {
                "$suffix$PATH_SUFFIX"
            }
        }
    }

    fun TaskContainer.registerCheckDeprecationsTask(
        shadowJarTaskName: String,
        suffix: String,
        expectedFileDoesNotExistMessage: String,
        checkFailureMessage: String,
    ): TaskProvider<DefaultTask> {
        val reportTask = deprecationReportTask(shadowJarTaskName)
        return register<DefaultTask>("checkDeprecationsFor${suffix}") {
            val actualDeprecations = reportTask.flatMap { it.outputFile }
            val expectedDeprecations = project.layout.projectDirectory.file("$BUILD_DIRECTORY/$suffix$PATH_SUFFIX")
            inputs.file(actualDeprecations)
            // `files` to check it manually and give an actionable failure message.
            // otherwise Gradle will complain that the input file does not exist
            inputs.files(expectedDeprecations)
            doFirst {
                val expectedFile = expectedDeprecations.asFile
                if (!expectedFile.exists()) {
                    throw GradleException(expectedFileDoesNotExistMessage)
                }
                val actualFile = actualDeprecations.get().asFile
                val diff = getDiff(expectedFile, actualFile)
                if (diff != null) {
                    throw GradleException("$checkFailureMessage\nDifference:\n$diff")
                }
            }
        }
    }

    private fun deprecationReportTask(jarTaskName: String) =
        deprecationReportTasks.getOrPut(jarTaskName) {
            tasks.register<DeprecationReportTask>("generate${jarTaskName.replaceFirstChar { it.uppercase() }}Deprecations") {
                val spec = checkNotNull(deprecationSpec) { "Configure class deprecation before registering report tasks" }
                val jarTask = tasks.named<Jar>(jarTaskName)
                inputJar.set(jarTask.flatMap { it.archiveFile })
                pattern.set(spec.pattern)
                exclusions.set(spec.exclusions)
                outputFile.fileProvider(jarTask.map {
                    val jar = it.archiveFile.get().asFile
                    jar.parentFile.resolve("${jar.name}$PATH_SUFFIX")
                })
            }
        }

    private data class DeprecationSpec(val pattern: String, val exclusions: List<String>)

    companion object {
        private val ASM_DEPRECATED_ATTRIBUTE = Attribute.of("org.jetbrains.kotlin.build.asm-deprecated", Boolean::class.javaObjectType)
        private const val PATH_SUFFIX = ".deprecations"
        private const val BUILD_DIRECTORY = "asm-deprecation"

        private fun getDiff(expectedFile: File, actualFile: File): String? {
            val expectedLines = expectedFile.readText().lines()
            val actualLines = actualFile.readText().lines()

            if (expectedLines == actualLines)
                return null

            val patch = DiffUtils.diff(expectedLines, actualLines)
            val diff =
                UnifiedDiffUtils.generateUnifiedDiff(expectedFile.absolutePath, actualFile.absolutePath, expectedLines, patch, 3)
            return diff.joinToString("\n")
        }
    }
}

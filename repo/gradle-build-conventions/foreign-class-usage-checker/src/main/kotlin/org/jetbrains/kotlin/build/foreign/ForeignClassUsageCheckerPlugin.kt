/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.foreign

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

@Suppress("unused")
class ForeignClassUsageCheckerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.withPlugin("java") {
            val javaExtension = project.extensions.getByType<JavaPluginExtension>()
            val classesDirsProvider = javaExtension.sourceSets
                .named(SourceSet.MAIN_SOURCE_SET_NAME)
                .map { it.output.classesDirs }

            project.tasks.withType<CheckForeignClassUsageTask>().configureEach {
                classesDirs.from(classesDirsProvider)
            }

            project.tasks.named("check").configure {
                dependsOn(project.tasks.withType<CheckForeignClassUsageTask>())
            }
        }
    }
}

abstract class CheckForeignClassUsageTask : DefaultTask() {
    init {
        group = "verification"
        description = "Check that only known foreign classes are used in the public API"
    }

    /**
     * Directories with compiled class files to scan for foreign class usage.
     *
     * This property accepts one or more directories containing `.class` files that will be analyzed
     * to detect usage of external (foreign) classes in their public API surface.
     *
     * By default, this property is configured to point to the output directories of the project's
     * [SourceSet.MAIN_SOURCE_SET_NAME] source set when the `java` plugin is applied.
     */
    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val classesDirs: ConfigurableFileCollection

    /**
     * Set of fully qualified names of annotations that mark declarations as non-public API.
     *
     * Each annotation should be specified using the fully qualified name in the form of `foo.bar.Baz$Inner`
     * (using `$` for nested classes). Declarations annotated with any of these annotations are excluded
     * from foreign class usage reporting, as they are not considered part of the public API surface.
     *
     * The annotation semantics work similarly to the binary-compatibility-validator plugin:
     * - If a class is annotated, all nested callable declarations (methods, properties) are treated as non-public.
     * - Nested classes must be explicitly annotated to be excluded; they don't inherit the non-public status from their outer class.
     *
     * Defaults to an empty set.
     */
    @get:Input
    @get:Optional
    @get:Option(option = "non-public-markers", description = "Annotations that mark declarations as non-public API")
    abstract val nonPublicMarkers: SetProperty<String>

    /**
     * Set of package prefixes whose classes should be excluded from the foreign usage report.
     *
     * Each package should be specified in the form of `foo.bar` (using `.` as separator).
     * When a package is listed, all classes from that package and all its subpackages are excluded from foreign class usage reporting.
     *
     * This property is useful for filtering out ubiquitous platform classes that are not meaningful to track in the foreign API report,
     * such as classes from the Java SDK or the Kotlin standard library.
     *
     * Defaults to: `["java", "kotlin", "org.jetbrains.annotations"]`.
     */
    @get:Input
    @get:Optional
    @get:Option(option = "ignored-packages", description = "Package prefixes to exclude from foreign usage report")
    abstract val ignoredPackages: SetProperty<String>

    /**
     * The output file where the foreign class usage report is written.
     *
     * The output format is a list of foreign class names (one per line) that are referenced in the public API of the analyzed classes.
     * The list uses internal JVM class names with `/` as package separator (e.g., `foo/bar/Baz$Inner`).
     *
     * When [collectUsages] is enabled, each foreign class name is followed by lines showing all locations where that class is used.
     *
     * If the file doesn't exist, it will be created with the current usage. If the file exists but differs from the current usage,
     * the task will fail and update the file.
     */
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    /**
     * Whether to include detailed usage information for each foreign class in the [outputFile].
     *
     * When enabled, the output file will contain not only the list of foreign classes, but also the specific locations
     * (classes and members) where each foreign class is referenced. Each usage is listed on a separate indented line below the foreign
     * class name.
     *
     * This option is primarily useful for debugging and understanding the exact API surface that depends on foreign classes.
     *
     * Defaults to `false`.
     */
    @get:Input
    @get:Optional
    @get:Option(option = "collect-usages", description = "Include detailed usage information in the report")
    abstract val collectUsages: Property<Boolean>

    init {
        nonPublicMarkers.convention(setOf())
        ignoredPackages.convention(setOf("java", "kotlin", "org.jetbrains.annotations"))
        collectUsages.convention(false)
    }

    @TaskAction
    @Suppress("unused")
    fun execute() {
        val collectUsages = collectUsages.get()
        val processor = ForeignClassUsageProcessor(nonPublicMarkers.get(), collectUsages)

        for (classesDir in classesDirs.files) {
            classesDir.walkTopDown().filter { it.isFile && it.extension == "class" }.forEach { classFile ->
                processor.process(classFile)
            }
        }

        val ignoredClassNamePrefixes = ignoredPackages.get()
            .map { it.replace('.', '/') + "/" }

        val filteredClassNames = ArrayList<String>()

        for (className in processor.foreignClassNames.sorted()) {
            if (ignoredClassNamePrefixes.any { className.startsWith(it) }) {
                continue
            }

            if (filteredClassNames.isNotEmpty()) {
                val lastDescriptor = filteredClassNames.last()
                if (className.startsWith("$lastDescriptor$")) {
                    // We aren't interested in nested classes as long as the outer class is already in the list
                    continue
                }
            }

            filteredClassNames.add(className)
        }

        val actualText = buildString {
            for ((index, className) in filteredClassNames.withIndex()) {
                if (index > 0) {
                    appendLine()
                }
                append(className)
                if (collectUsages) {
                    appendLine()
                    processor.usages(className).forEach { appendLine("    $it") }
                }
            }
        }

        val expectedFile = outputFile.get().asFile
        if (!expectedFile.exists()) {
            expectedFile.writeText(actualText)
            throw GradleException("Expected file did not exist and has been created. Please review and commit the changes")
        }

        val expectedText = expectedFile.readText()

        val actualLines = actualText.lines()
        val expectedLines = expectedText.lines()

        if (actualLines != expectedLines) {
            expectedFile.writeText(actualText)
            throw GradleException("Expected file has been modified. Please review and commit the changes")
        }
    }
}
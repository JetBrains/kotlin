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
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.collections.iterator

@Suppress("unused")
class ForeignClassUsageCheckerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.withPlugin("java") {
            val javaExtension = project.extensions.getByType<JavaPluginExtension>()
            val classesDirsProvider = javaExtension.sourceSets
                .named(SourceSet.MAIN_SOURCE_SET_NAME)
                .map { it.output.classesDirs }

            project.tasks.withType<CheckForeignClassUsageTask>().configureEach {
                classes.from(classesDirsProvider)
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
     * Directories or archives with compiled class files to scan for foreign class usage.
     *
     * This property accepts one or more directories containing `.class` files, or JAR files with them,
     * that will be analyzed to detect usage of external (foreign) classes in their public API surface.
     *
     * By default, this property is configured to point to the output directories of the project's
     * [SourceSet.MAIN_SOURCE_SET_NAME] source set when the `java` plugin is applied.
     */
    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val classes: ConfigurableFileCollection

    /**
     * Dependencies of [classes] in the same format: directories containing `.class` files, or JAR files with them.
     *
     * If the [classpath] property is set, the task verifies that all foreign API classes are present in it.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val classpath: ConfigurableFileCollection

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
     *
     * If the [outputFile] isn't set, the task will not generate an output file.
     * This is useful for checks of the foreign API against the provided [classpath].
     */
    @get:OutputFile
    @get:Optional
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
        ignoredPackages.convention(setOf("java", "javax", "kotlin", "org.jetbrains.annotations"))
        collectUsages.convention(false)
    }

    @TaskAction
    @Suppress("unused")
    fun execute() {
        val processor = ForeignClassUsageProcessor(nonPublicMarkers.get(), collectUsages.get())

        for (classesFile in classes.files) {
            classesFile.processClassFiles { classEntry ->
                classEntry.withInputStream(processor::process)
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

        checkAgainstClasspath(filteredClassNames)
        checkAgainstDump(filteredClassNames, processor)
    }

    private fun checkAgainstClasspath(classNames: List<String>) {
        val classpathFiles = classpath.files
        if (classpathFiles.isEmpty()) {
            return
        }

        val classpathClasses = HashSet<String>()
        for (classpathFile in classpathFiles) {
            classpathFile.processClassFiles { classEntry ->
                classpathClasses.add(classEntry.className)
            }
        }

        val missingClassNames = classNames.toSet() - classpathClasses

        if (missingClassNames.isNotEmpty()) {
            val missingClassNamesText = missingClassNames.joinToString(System.lineSeparator(), prefix = System.lineSeparator())
            throw GradleException("The following class names are missing in the classpath:$missingClassNamesText")
        }
    }

    private fun checkAgainstDump(classNames: List<String>, processor: ForeignClassUsageProcessor) {
        val expectedFile = outputFile.getOrNull()?.asFile

        if (expectedFile == null) {
            if (classNames.isEmpty()) {
                throw GradleException("Expected file isn't set, and no foreign API is used")
            }
            return
        }

        val actualText = buildString {
            for ((index, className) in classNames.withIndex()) {
                if (index > 0) {
                    appendLine()
                }
                append(className)
                if (collectUsages.get()) {
                    appendLine()
                    processor.usages(className).forEach { appendLine("    $it") }
                }
            }
        }

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

private fun File.processClassFiles(processor: (ClassEntry) -> Unit) {
    if (isFile && extension == "jar") {
        ZipFile(this).use { zipFile ->
            for (zipEntry in zipFile.entries()) {
                if (zipEntry.name.endsWith(".class")) {
                    val className = zipEntry.name.removeSuffix(".class")
                    processor(ClassEntry.FromZip(zipFile, zipEntry, className))
                }
            }
        }
    } else {
        // Process individual '.class' files and directories with them
        walkTopDown()
            .filter { someFile -> someFile.isFile && someFile.extension == "class" }
            .forEach { classFile ->
                val className = classFile.toRelativeString(this).removeSuffix(".class")
                processor(ClassEntry.LocalFile(classFile, className))
            }
    }
}

private sealed class ClassEntry(val className: String) {
    abstract fun withInputStream(block: (InputStream) -> Unit)

    class FromZip(private val zipFile: ZipFile, private val zipEntry: ZipEntry, className: String) : ClassEntry(className) {
        override fun withInputStream(block: (InputStream) -> Unit) {
            zipFile.getInputStream(zipEntry).use(block)
        }
    }

    class LocalFile(private val file: File, className: String) : ClassEntry(className) {
        override fun withInputStream(block: (InputStream) -> Unit) {
            file.inputStream().buffered().use(block)
        }
    }
}
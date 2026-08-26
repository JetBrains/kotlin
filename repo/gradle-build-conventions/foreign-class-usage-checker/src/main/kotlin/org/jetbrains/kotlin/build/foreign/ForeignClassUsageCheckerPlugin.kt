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
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
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

            project.tasks.withType<ForeignClassUsageTask>().configureEach {
                classes.from(classesDirsProvider)
            }
        }
    }
}

/**
 * Registers the pair of tasks comparing the foreign classes used in this project's public API against a committed
 * dump: `checkForeignClassUsage[nameSuffix]` reports a difference, `updateForeignClassUsage[nameSuffix]` rewrites the
 * dump. Both fail on a mismatch; only the second one touches the source tree.
 *
 * [configure] is applied to both, so a dump and its markers are declared once.
 *
 * The dump files are registered here rather than annotated on [ForeignClassUsageTask], because the same property is
 * an input of the verifying task and an output of the rewriting one. The pair is deliberately left unordered: asking
 * one build to both rewrite a dump and verify it is contradictory, and Gradle's implicit dependency validation says
 * so. Ordering them would instead make the verification pass on whatever the rewrite just produced.
 */
fun Project.registerForeignClassUsageTasks(
    nameSuffix: String = "",
    configure: ForeignClassUsageTask.() -> Unit,
) {
    val updateTask = tasks.register<ForeignClassUsageTask>("updateForeignClassUsage$nameSuffix") {
        description = "Rewrites the dump of the foreign classes used in the public API"
        overwriteDump.set(true)
        configure()

        outputs.file(outputFile).withPropertyName("dump").optional(true)
        outputs.file(missingClasspathEntriesOutputFile).withPropertyName("missingClasspathEntries").optional(true)
    }

    val checkTask = tasks.register<ForeignClassUsageTask>("checkForeignClassUsage$nameSuffix") {
        description = "Checks the dump of the foreign classes used in the public API against the sources"
        overwriteDump.set(false)
        updateTaskName.set("updateForeignClassUsage$nameSuffix")
        configure()

        // Declared as collections rather than single files: a dump that does not exist yet is a normal state for a
        // module that has just started tracking its API, and 'inputs.file' rejects the task before it can say so.
        inputs.files(outputFile)
            .withPropertyName("dump")
            .withPathSensitivity(PathSensitivity.RELATIVE)
            .optional(true)
        inputs.files(missingClasspathEntriesOutputFile)
            .withPropertyName("missingClasspathEntries")
            .withPathSensitivity(PathSensitivity.RELATIVE)
            .optional(true)

        reportFile.set(layout.buildDirectory.file("foreign-class-usage/$name.txt"))

        // Opted in per task rather than through '@CacheableTask' on the type: the rewriting half writes into the
        // source tree, and restoring that from a cache is not something one ever wants.
        outputs.cacheIf { true }
    }

    tasks.named("check").configure { dependsOn(checkTask) }

    // The foreign classes a public API leaks are part of the project's API surface.
    tasks.named("checkApiSurface").configure { dependsOn(checkTask) }
    tasks.named("updateApiSurface").configure { dependsOn(updateTask) }
}

abstract class ForeignClassUsageTask : DefaultTask() {
    init {
        group = "verification"
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
     *
     * Normalized as a classpath, which is what every caller passes. The task only reads class names, from the entries
     * of a JAR or from paths relative to a directory, and classpath normalization keeps both while dropping where the
     * entry itself happens to live. It also brings in the `runtimeClasspath` normalization rules of the build, so
     * that a stamp such as `META-INF/compiler.version` stops invalidating a result it cannot change.
     */
    @get:Classpath
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
     * When [overwriteDump] is set, a file that is missing or differs from the current usage is written and the task
     * fails, so that the change gets reviewed and committed. Otherwise the task only reports the difference.
     *
     * If the [outputFile] isn't set, the task will not compare against a dump.
     * This is useful for checks of the foreign API against the provided [classpath].
     *
     * The file is registered as an input or an output of the task depending on [overwriteDump], which annotations
     * cannot express, so it is left out of the task's property metadata here. Use [registerForeignClassUsageTasks]
     * to declare the pair of tasks that share a dump.
     */
    @get:Internal
    abstract val outputFile: RegularFileProperty

    /**
     * The output file for the classpath check.
     *
     * This file is only accessed if the [classpath] property is not empty.
     * The file format is the same as in the [outputFile].
     *
     * If the [outputFile] isn't set while there are missing classes in the classpath, the task will fail with an explanatory comment.
     * This should be the desired behavior – no API is ever expected to have broken classpath references.
     * However, achieving that at once might be non-trivial for existing "dirty" artifacts. In that case, the
     * [missingClasspathEntriesOutputFile] can at least ensure that there won't be additional API dependency breakages.
     */
    @get:Internal
    abstract val missingClasspathEntriesOutputFile: RegularFileProperty

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

    /**
     * Whether a dump that is missing or out of date is rewritten from the current usage.
     *
     * The task fails either way — a rewritten dump still has to be reviewed and committed — but only a task that
     * rewrites may touch the source tree. Leave it off for the verifying half of a task pair, so that the check can
     * run on CI and in `check`.
     *
     * Defaults to `false`.
     */
    @get:Input
    abstract val overwriteDump: Property<Boolean>

    /**
     * File the computed foreign class usage is written to.
     *
     * A verifying task produces nothing of its own, and Gradle neither caches nor considers up to date a task with
     * no declared outputs. This gives it one, in the build directory, holding the very text it compared against the
     * dump — useful on its own when a failure needs to be inspected.
     *
     * Left unset for a rewriting task, whose output is the dump itself.
     */
    @get:OutputFile
    @get:Optional
    abstract val reportFile: RegularFileProperty

    /**
     * Name of the task that rewrites this dump.
     *
     * A verifying task names it in its failure, so that the fix can be run without looking it up. Unset when the
     * task has no counterpart, which is the case for one registered outside [registerForeignClassUsageTasks].
     */
    @get:Input
    @get:Optional
    abstract val updateTaskName: Property<String>

    init {
        nonPublicMarkers.convention(setOf())
        ignoredPackages.convention(setOf("java", "javax", "kotlin", "org.jetbrains.annotations"))
        collectUsages.convention(false)
        overwriteDump.convention(false)
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

        // Written before the comparisons rather than after them: a failure is exactly when one wants to see what
        // was computed, and a report left over from an earlier run would say something else.
        reportFile.orNull?.asFile?.let { file ->
            file.parentFile.mkdirs()
            file.writeText(renderClassNames(filteredClassNames, processor))
        }

        checkAgainstClasspath(filteredClassNames, processor)
        checkAgainstDump(filteredClassNames, processor)
    }

    private fun checkAgainstClasspath(classNames: List<String>, processor: ForeignClassUsageProcessor) {
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

        val expectedFile = missingClasspathEntriesOutputFile.getOrNull()?.asFile

        if (missingClassNames.isEmpty() && expectedFile == null) {
            return
        }

        if (expectedFile != null) {
            val actualText = renderClassNames(missingClassNames.toList(), processor)
            assertEqualsToFile(expectedFile, actualText)
        } else {
            val missingClassNamesText = buildString {
                append(System.lineSeparator())
                append(renderClassNames(missingClassNames.toList(), processor))
            }

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

        val actualText = renderClassNames(classNames, processor)
        assertEqualsToFile(expectedFile, actualText)
    }

    private fun assertEqualsToFile(expectedFile: File, actualText: String) {
        val overwrite = overwriteDump.get()

        if (!expectedFile.exists()) {
            if (!overwrite) {
                throw GradleException("Expected file '${expectedFile.name}' does not exist. ${updateHint("create")}")
            }

            expectedFile.writeText(actualText)
            throw GradleException("Expected file did not exist and has been created. Please review and commit the changes")
        }

        val actualLines = actualText.lines()
        val expectedLines = expectedFile.readText().lines()

        if (actualLines == expectedLines) {
            return
        }

        if (!overwrite) {
            throw GradleException(
                "Expected file '${expectedFile.name}' does not match the current foreign class usage." +
                        renderDifference(expectedLines, actualLines) +
                        System.lineSeparator() +
                        System.lineSeparator() +
                        updateHint("rewrite")
            )
        }

        expectedFile.writeText(actualText)
        throw GradleException("Expected file has been modified. Please review and commit the changes")
    }

    /**
     * Tells the reader which task rewrites the dump, naming it when it is known.
     */
    private fun updateHint(action: String): String {
        val taskName = updateTaskName.orNull ?: return "Run the corresponding 'update' task to $action it"
        return "Run '$taskName' to $action it"
    }

    /**
     * Renders the entries [actualLines] adds to and removes from [expectedLines].
     *
     * Both are sorted lists of class names, so listing the two sets is more readable than a positional diff.
     */
    private fun renderDifference(expectedLines: List<String>, actualLines: List<String>): String {
        val added = actualLines - expectedLines.toSet()
        val removed = expectedLines - actualLines.toSet()

        return buildString {
            for ((title, lines) in listOf("No longer expected" to removed, "Newly used" to added)) {
                if (lines.isEmpty()) continue

                append(System.lineSeparator())
                append(System.lineSeparator())
                append(title)
                append(':')
                for (line in lines.filter { it.isNotBlank() }) {
                    append(System.lineSeparator())
                    append("    ")
                    append(line)
                }
            }
        }
    }

    private fun renderClassNames(classNames: List<String>, processor: ForeignClassUsageProcessor): String {
        val text = buildString {
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

        return text.replace("\n", System.lineSeparator())
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

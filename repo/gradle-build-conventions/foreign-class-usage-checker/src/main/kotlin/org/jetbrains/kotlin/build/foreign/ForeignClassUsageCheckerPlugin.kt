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
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
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
                dependsOn(project.tasks.named("classes"))
            }

            project.tasks.named("check").configure {
                for (checkForeignClassUsageTask in project.tasks.withType<CheckForeignClassUsageTask>()) {
                    dependsOn(checkForeignClassUsageTask)
                }
            }
        }
    }
}

abstract class CheckForeignClassUsageTask : DefaultTask() {
    init {
        group = "verification"
        description = "Check that only known foreign classes are used in the public API"
    }

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val classesDirs: ConfigurableFileCollection

    @get:Input
    abstract val nonPublicMarkers: SetProperty<String>

    @get:Input
    abstract val ignoredPackages: SetProperty<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Input
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
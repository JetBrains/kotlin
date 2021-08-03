/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleJavaTargetExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.utils.isGradleVersionAtLeast
import org.jetbrains.kotlin.gradle.utils.newProperty
import java.io.File

internal open class InspectClassesForMultiModuleIC : DefaultTask() {
    @get:Input
    internal val archivePath = project.newProperty<String>()

    @get:Input
    internal val archiveName = project.newProperty<String>()

    @get:Input
    lateinit var sourceSetName: String

    @Suppress("MemberVisibilityCanBePrivate")
    @get:OutputFile
    internal val classesListFile: File by lazy {
        (project.kotlinExtension as KotlinSingleJavaTargetExtension).target.defaultArtifactClassesListFile.get()
    }

    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputFiles
    internal val sourceSetOutputClassesDir by lazy {
        project.convention.findPlugin(JavaPluginConvention::class.java)?.sourceSets?.findByName(sourceSetName)?.output?.classesDirs
    }

    @get:Internal
    internal val fileTrees
        get() = sourceSetOutputClassesDir?.map {
            objects.fileTree().from(it).include("**/*.class")
        }

    @get:Internal
    internal val objects = project.objects

    @Suppress("MemberVisibilityCanBePrivate")
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:IgnoreEmptyDirectories
    @get:InputFiles
    internal val classFiles: FileCollection
        get() {
            if (sourceSetOutputClassesDir != null) {
                return objects.fileCollection().from(fileTrees)
            }
            return objects.fileCollection()
        }

    @TaskAction
    fun run() {
        classesListFile.parentFile.mkdirs()
        val text = classFiles.map { it.absolutePath }.sorted().joinToString(File.pathSeparator)
        classesListFile.writeText(text)
    }

    private fun sanitizeFileName(candidate: String): String =
        candidate.filter { it.isLetterOrDigit() }
}
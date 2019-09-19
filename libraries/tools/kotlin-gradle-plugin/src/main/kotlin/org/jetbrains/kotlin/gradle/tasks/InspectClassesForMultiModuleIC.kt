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
import org.jetbrains.kotlin.gradle.utils.archivePathCompatible
import java.io.File

internal open class InspectClassesForMultiModuleIC : DefaultTask() {
    @get:Input
    internal val archivePath = project.objects.property(String::class.java)

    @get:Input
    internal val archiveName = project.objects.property(String::class.java)

    @get:Input
    lateinit var sourceSetName: String

    @Suppress("MemberVisibilityCanBePrivate")
    @get:OutputFile
    internal val classesListFile: File
        get() = (project.kotlinExtension as KotlinSingleJavaTargetExtension).target.defaultArtifactClassesListFile

    @Suppress("MemberVisibilityCanBePrivate")
    @get:InputFiles
    internal val classFiles: FileCollection
        get() {
            val convention = project.convention.findPlugin(JavaPluginConvention::class.java)
            val sourceSet = convention?.sourceSets?.findByName(sourceSetName) ?: return project.files()

            val fileTrees = sourceSet.output.classesDirs.map { project.fileTree(it).include("**/*.class") }
            return project.files(fileTrees)
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
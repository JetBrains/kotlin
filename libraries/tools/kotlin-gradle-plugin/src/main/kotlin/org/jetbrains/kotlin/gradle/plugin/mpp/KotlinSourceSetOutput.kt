/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.SourceSetOutput
import java.io.File
import java.util.concurrent.Callable

private class MutableReference<T>(var item: T)

class KotlinSourceSetOutput constructor(
    private val project: Project,
    private var resourcesDir: Any
) : SourceSetOutput, ConfigurableFileCollection by project.files() {

    private val classesDirs: FileCollection = project.files()
    private val otherDirs: ConfigurableFileCollection = project.files()

    init {
        this.from(classesDirs)
        this.from(otherDirs)
    }

    override fun setResourcesDir(dirNotation: Any) {
        resourcesDir = dirNotation
    }

    override fun setResourcesDir(dir: File): Unit = setResourcesDir(dir as Any)

    override fun getDirs(): FileCollection = this

    override fun isLegacyLayout(): Boolean = false

    override fun getResourcesDir(): File = project.file(resourcesDir)

    override fun setClassesDir(file: File?) = @Suppress("DEPRECATION") setClassesDir(file as Any)

    override fun setClassesDir(fileNotation: Any?) = throw UnsupportedOperationException("Setting classesDir is not supported, use classesDirs.")

    override fun getClassesDir(): File = classesDirs.singleFile

    override fun getClassesDirs(): FileCollection = classesDirs

    override fun dir(args: Map<String, Any>, dir: Any?) {
        val dirFileCollection = project.files(dir)
        args["builtBy"]?.let { dirFileCollection.builtBy(it) }
        otherDirs.from(dirFileCollection)
    }

    override fun dir(dirNotation: Any?): Unit = dir(emptyMap(), dirNotation)
}
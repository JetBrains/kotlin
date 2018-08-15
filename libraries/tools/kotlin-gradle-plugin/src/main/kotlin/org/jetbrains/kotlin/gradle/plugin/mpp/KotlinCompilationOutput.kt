/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.FileCollectionInternal
import org.gradle.api.internal.file.FileCollectionVisitor
import org.gradle.api.internal.file.FileSystemSubset
import org.gradle.api.tasks.SourceSetOutput
import java.io.File
import java.util.concurrent.Callable

class KotlinCompilationOutput constructor(
    private val project: Project,
    private var resourcesDir: Any,
    private var backingFileCollection: ConfigurableFileCollection = project.files() // must be a parameter since it is used in delegation
) : SourceSetOutput, ConfigurableFileCollection by backingFileCollection, FileCollectionInternal {

    /* TODO report Gradle bug: cannot implement SourceSetOutput without also implementing FileCollectionInternal, there's a cast to the latter
     inside the Gradle logic for resolving file collections */
    override fun registerWatchPoints(p0: FileSystemSubset.Builder) = (backingFileCollection as FileCollectionInternal).registerWatchPoints(p0)
    override fun visitRootElements(p0: FileCollectionVisitor) = (backingFileCollection as FileCollectionInternal).visitRootElements(p0)

    private val classesDirs: FileCollection = project.files()
    private val otherDirs: ConfigurableFileCollection = project.files()

    init {
        this.from(classesDirs)
        this.from(Callable { resourcesDir })
    }

    override fun setResourcesDir(dirNotation: Any) {
        resourcesDir = dirNotation
    }

    override fun setResourcesDir(dir: File): Unit = setResourcesDir(dir as Any)

    override fun getDirs(): FileCollection = otherDirs

    override fun isLegacyLayout(): Boolean = false

    override fun getResourcesDir(): File = project.file(resourcesDir)

    @Suppress("OverridingDeprecatedMember")
    override fun setClassesDir(file: File?) = @Suppress("deprecation") setClassesDir(file as Any)

    @Suppress("OverridingDeprecatedMember")
    override fun setClassesDir(fileNotation: Any?) = throw UnsupportedOperationException("Setting classesDir is not supported, use classesDirs.")

    @Suppress("OverridingDeprecatedMember")
    override fun getClassesDir(): File = classesDirs.singleFile

    override fun getClassesDirs(): FileCollection = classesDirs

    override fun dir(args: Map<String, Any>, dir: Any?) {
        val dirFileCollection = project.files(dir)
        args["builtBy"]?.let { dirFileCollection.builtBy(it) }
        otherDirs.from(dirFileCollection)
    }

    override fun dir(dirNotation: Any?): Unit = dir(emptyMap(), dirNotation)
}
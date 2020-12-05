/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import com.google.gson.GsonBuilder
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency
import org.jetbrains.kotlin.gradle.internal.ProcessedFilesCache
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import java.io.File

/**
 * Cache for storing already created [GradleNodeModule]s
 */
internal abstract class AbstractNodeModulesCache(val nodeJs: NodeJsRootExtension) : AutoCloseable {
    companion object {
        const val STATE_FILE_NAME = ".visited"
    }

    val project: Project get() = nodeJs.rootProject
    internal val dir = nodeJs.nodeModulesGradleCacheDir
    private val cache = ProcessedFilesCache(project, dir, STATE_FILE_NAME, "9")

    @Synchronized
    fun get(
        name: String,
        version: String,
        file: File
    ): GradleNodeModule? = cache.getOrCompute(file) {
        buildImportedPackage(name, version, file)
    }?.let {
        GradleNodeModule(it)
    }

    abstract fun buildImportedPackage(
        name: String,
        version: String,
        file: File
    ): File?

    @Synchronized
    override fun close() {
        cache.close()
    }
}

fun makeNodeModule(
    container: File,
    packageJson: PackageJson,
    files: (File) -> Unit
): File {
    val dir = importedPackageDir(container, packageJson.name, packageJson.version)

    if (dir.exists()) dir.deleteRecursively()

    check(dir.mkdirs()) {
        "Cannot create directory: $dir"
    }

    val gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create()

    files(dir)

    dir.resolve("package.json").writer().use {
        gson.toJson(packageJson, it)
    }

    return dir
}

fun importedPackageDir(container: File, name: String, version: String): File =
    container.resolve(name).resolve(version)

fun GradleNodeModule(dir: File) = GradleNodeModule(dir.parentFile.name, dir.name, dir)
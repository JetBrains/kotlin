/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.jetbrains.kotlin.gradle.targets.js.JS
import org.jetbrains.kotlin.gradle.targets.js.JS_MAP
import org.jetbrains.kotlin.gradle.targets.js.META_JS
import org.jetbrains.kotlin.gradle.targets.js.ir.KLIB_TYPE
import java.io.File

/**
 * Creates fake NodeJS module directory from given gradle [dependency].
 */
internal class GradleNodeModuleBuilder(
    val project: Project,
    val fs: FileSystemOperations,
    val archiveOperations: Any?,
    val moduleName: String,
    val moduleVersion: String,
    val srcFiles: Collection<File>,
    val cacheDir: File
) {
    private var srcPackageJsonFile: File? = null
    private val files = mutableListOf<File>()

    fun visitArtifacts() {
        srcFiles.forEach { srcFile ->
            when {
                isKotlinJsRuntimeFile(srcFile) -> files.add(srcFile)
                srcFile.isCompatibleArchive -> {
                    val archiveFiles = if (archiveOperations != null) {
                        (archiveOperations as ArchiveOperations).zipTree(srcFile)
                    } else {
                        project.zipTree(srcFile)
                    }
                    archiveFiles.forEach { innerFile ->
                        when {
                            innerFile.name == NpmProject.PACKAGE_JSON -> srcPackageJsonFile = innerFile
                            isKotlinJsRuntimeFile(innerFile) -> files.add(innerFile)
                        }
                    }
                }
            }
        }
    }

    fun rebuild(): File? {
        if (files.isEmpty() && srcPackageJsonFile == null) return null

        val packageJson = fromSrcPackageJson(srcPackageJsonFile)?.apply {
            // Gson set nulls reflectively no matter on default values and non-null types
            @Suppress("USELESS_ELVIS")
            version = version ?: moduleVersion
        } ?: PackageJson(moduleName, moduleVersion)

        val metaFiles = files.filter { it.name.endsWith(".$META_JS") }
        if (metaFiles.size == 1) {
            val metaFile = metaFiles.single()
            val name = metaFile.name.removeSuffix(".$META_JS")
            packageJson.name = name
            packageJson.main = "${name}.js"
        }

        // yarn requires semver
        packageJson.version = fixSemver(packageJson.version)

        val actualFiles = files.filterNot { it.name.endsWith(".$META_JS") }

        return makeNodeModule(cacheDir, packageJson) { nodeModule ->
            fs.copy { copy ->
                copy.from(actualFiles)
                copy.into(nodeModule)
            }
        }
    }
}

internal val File.isCompatibleArchive
    get() = isFile
            && (extension == "jar"
            || extension == "zip"
            || extension == KLIB_TYPE)

private fun isKotlinJsRuntimeFile(file: File): Boolean {
    if (!file.isFile) return false
    val name = file.name
    return name.endsWith(".$JS")
            || name.endsWith(".$JS_MAP")
}
/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import com.google.gson.GsonBuilder
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.FileTree
import org.jetbrains.kotlin.gradle.targets.js.ir.KLIB_TYPE
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

/**
 * Creates fake NodeJS module directory from given Gradle `dependency`.
 */
internal class GradleNodeModuleBuilder(
    val fs: FileSystemOperations,
    val archiveOperations: ArchiveOperations,
    val moduleName: String,
    val moduleVersion: String,
    val srcFiles: Collection<File>,
    val cacheDir: File,
) {
    private var srcPackageJsonFile: Path? = null
    private val files = mutableListOf<Path>()
    private val fileTrees: MutableList<FileTree> = mutableListOf()

    fun visitArtifacts() {
        srcFiles.forEach { srcFile ->
            val srcPath = srcFile.toPath()
            when {
                isKotlinJsRuntimeFile(srcPath) -> files.add(srcPath)
                srcFile.name == NpmProject.PACKAGE_JSON -> {
                    srcPackageJsonFile = srcPath
                }
                srcFile.isCompatibleArchive -> {
                    archiveOperations.zipTree(srcFile).forEach { innerFile ->
                        val innerPath = innerFile.toPath()
                        when {
                            innerFile.name == NpmProject.PACKAGE_JSON -> srcPackageJsonFile = innerPath
                            isKotlinJsRuntimeFile(innerPath) -> files.add(innerPath)
                        }
                    }

                    fileTrees.add(
                        archiveOperations.zipTree(srcFile)
                            .matching {
                                it.include {
                                    isKotlinJsRuntimeFile(it.file.toPath())
                                }
                            }
                    )
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

        val metaJsExt = ".meta.js"

        val metaFiles = files.filter { it.fileName.toString().endsWith(metaJsExt) }
        if (metaFiles.size == 1) {
            val metaFile = metaFiles.single()
            val name = metaFile.fileName.toString().removeSuffix(metaJsExt)
            packageJson.name = name
            packageJson.main = "${name}.js"
        }

        packageJson.devDependencies.clear()

        // npm requires semver
        packageJson.version = fixSemver(packageJson.version)

        return createNodeModule(cacheDir.toPath(), packageJson) { nodeModule ->
            fs.copy { copy ->
                copy.from(fileTrees)
                copy.into(nodeModule.toFile())
            }
        }.toFile()
    }
}

internal val File.isCompatibleArchive
    get() = toPath().isCompatibleArchive

internal val Path.isCompatibleArchive
    get() = this.isRegularFile()
            && (fileName.toString().substringAfterLast('.', "") == "jar"
            || fileName.toString().substringAfterLast('.', "") == "zip"
            || fileName.toString().substringAfterLast('.', "") == KLIB_TYPE)

private fun isKotlinJsRuntimeFile(file: Path): Boolean {
    if (!file.isRegularFile()) return false
    val name = file.fileName.toString()
    return name.endsWith(".js")
            || name.endsWith(".mjs")
            || name.endsWith(".wasm")
            || name.endsWith(".js.map")
            || name.endsWith(".html")
}

private fun createNodeModule(
    container: Path,
    packageJson: PackageJson,
    files: (Path) -> Unit,
): Path {
    /** imported package directory */
    val dir = container.resolve(packageJson.name).resolve(packageJson.version)

    if (dir.exists()) dir.toFile().deleteRecursively()

    checkNotNull(dir.createDirectories()) {
        "Cannot create directory: $dir"
    }

    val gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create()

    files(dir)

    dir.resolve("package.json").bufferedWriter().use {
        gson.toJson(packageJson, it)
    }

    return dir
}

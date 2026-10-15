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
import kotlin.io.path.name

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
    private var srcPackageJsonFile: File? = null
    private val files = mutableListOf<File>()
    private val fileTrees: MutableList<FileTree> = mutableListOf()

    fun visitArtifacts() {
        srcFiles.forEach { srcFile ->
            when {
                isKotlinJsRuntimeFile(srcFile) -> files.add(srcFile)
                srcFile.name == NpmProject.PACKAGE_JSON -> {
                    srcPackageJsonFile = srcFile
                }
                srcFile.isCompatibleArchive -> {
                    archiveOperations.zipTree(srcFile).forEach { innerFile ->
                        when {
                            innerFile.name == NpmProject.PACKAGE_JSON -> srcPackageJsonFile = innerFile
                            isKotlinJsRuntimeFile(innerFile) -> files.add(innerFile)
                        }
                    }

                    fileTrees.add(
                        archiveOperations.zipTree(srcFile)
                            .matching {
                                it.include {
                                    isKotlinJsRuntimeFile(it.file)
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

        val metaFiles = files.filter { it.name.endsWith(metaJsExt) }
        if (metaFiles.size == 1) {
            val metaFile = metaFiles.single()
            val name = metaFile.name.removeSuffix(metaJsExt)
            packageJson.name = name
            packageJson.main = "${name}.js"
        }

        packageJson.devDependencies.clear()

        // npm requires semver
        packageJson.version = fixSemver(packageJson.version)

        return createNodeModule(cacheDir, packageJson) { nodeModule ->
            fs.copy { copy ->
                copy.from(fileTrees)
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
    return name.endsWith(".js")
            || name.endsWith(".mjs")
            || name.endsWith(".wasm")
            || name.endsWith(".js.map")
            || name.endsWith(".html")
}

private fun createNodeModule(
    container: File,
    packageJson: PackageJson,
    files: (File) -> Unit,
): File {
    /** imported package directory */
    val dir = importedPackageDirWithinCache(container, packageJson.name, packageJson.version)

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

/**
 * Returns the directory in [container] that a package with [packageName] and [packageVersion] is imported into:
 * `<name>/<version>`, or `<scope>/<name>/<version>` for a scoped name.
 *
 * Protects against path traversal.
 */
internal fun importedPackageDirWithinCache(container: File, packageName: String, packageVersion: String): File {
    // A scoped npm name, `@scope/name`, is the only packageName that may contain a `/`: it gets one directory per part.
    // Every other name, including `@scope` without a `/`, must resolve to a single directory.
    return if (packageName.startsWith("@") && packageName.contains("/")) {
        val scope = packageName.substringBefore("/")
        val name = packageName.substringAfter("/")
        container.resolveChild(scope).resolveChild(name).resolveChild(packageVersion)
    } else {
        container.resolveChild(packageName).resolveChild(packageVersion)
    }
}

/**
 * Resolves [segment] as a direct child of this path.
 *
 * Protects against path traversal.
 */
internal fun File.resolveChild(segment: String): File {
    require(segment.isNotBlank()) { "Path segment must not be blank" }

    val base = this.toPath().toAbsolutePath().normalize()
    val child = base.resolve(segment).normalize()

    require(child.parent == base) { "Path segment '$segment' did not resolve to a child directory of $this" }
    require(child.fileName.name == segment) { "Path segment '$segment' is not a plain name" }

    return child.toFile()
}

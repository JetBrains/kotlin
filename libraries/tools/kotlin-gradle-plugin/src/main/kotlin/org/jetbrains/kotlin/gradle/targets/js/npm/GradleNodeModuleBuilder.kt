/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import java.io.File

/**
 * Creates fake NodeJS module directory from given gradle [dependency].
 */
internal class GradleNodeModuleBuilder(
    val project: Project,
    val dependency: ResolvedDependency,
    val artifacts: Set<ResolvedArtifact>,
    val cache: GradleNodeModulesCache
) {
    var srcPackageJsonFile: File? = null
    val files = mutableListOf<File>()

    fun visitArtifacts() {
        artifacts.forEach { artifact ->
            val srcFile = artifact.file
            when {
                isKotlinJsRuntimeFile(srcFile) -> files.add(srcFile)
                srcFile.isZip -> project.zipTree(srcFile).forEach { innerFile ->
                    when {
                        innerFile.name == NpmProject.PACKAGE_JSON -> srcPackageJsonFile = innerFile
                        isKotlinJsRuntimeFile(innerFile) -> files.add(innerFile)
                    }
                }
            }
        }
    }

    fun rebuild(): PackageJson? {
        if (files.isEmpty()) return null

        val packageJson = srcPackageJsonFile?.reader()?.use {
            Gson().fromJson(it, PackageJson::class.java)
        } ?: PackageJson(dependency.moduleName, dependency.moduleVersion)

        val jsFiles = files.filter { it.name.endsWith(".js") }
        if (jsFiles.size == 1) {
            val jsFile = jsFiles.single()
            packageJson.name = jsFile.nameWithoutExtension
            packageJson.main = jsFile.name
        }

        // yarn requires semver
        packageJson.version = fixSemver(packageJson.version)

        return makeNodeModule(cache.dir, packageJson) { nodeModule ->
            project.copy { copy ->
                copy.from(files)
                copy.into(nodeModule)
            }
        }
    }
}

private val File.isZip
    get() = isFile && (name.endsWith(".jar") || name.endsWith(".zip"))

private fun isKotlinJsRuntimeFile(file: File): Boolean {
    if (!file.isFile) return false
    val name = file.name
    return (name.endsWith(".js") && !name.endsWith(".meta.js"))
            || name.endsWith(".js.map")
}

fun makeNodeModule(
    container: File,
    packageJson: PackageJson,
    files: (File) -> Unit
): PackageJson {
    val dir = importedPackageDir(container, packageJson.name, packageJson.version)

    if (dir.exists()) dir.deleteRecursively()

    check(dir.mkdirs()) {
        "Cannot create directory: $dir"
    }

    val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    files(dir)

    dir.resolve("package.json").writer().use {
        gson.toJson(packageJson, it)
    }

    return packageJson
}

fun importedPackageDir(container: File, name: String, version: String): File =
    container.resolve(name).resolve(version)
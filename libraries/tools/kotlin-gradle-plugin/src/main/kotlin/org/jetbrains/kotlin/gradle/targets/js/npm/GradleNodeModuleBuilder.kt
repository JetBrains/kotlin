/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency
import org.jetbrains.kotlin.gradle.targets.js.ir.KLIB_TYPE
import java.io.File

/**
 * Creates fake NodeJS module directory from given gradle [dependency].
 */
internal class GradleNodeModuleBuilder(
    val project: Project,
    val dependency: ResolvedDependency,
    val srcFiles: Collection<File>,
    val cache: GradleNodeModulesCache
) {
    var srcPackageJsonFile: File? = null
    val files = mutableListOf<File>()

    fun visitArtifacts() {
        srcFiles.forEach { srcFile ->
            when {
                isKotlinJsRuntimeFile(srcFile) -> files.add(srcFile)
                srcFile.isCompatibleArchive -> project.zipTree(srcFile).forEach { innerFile ->
                    when {
                        innerFile.name == NpmProject.PACKAGE_JSON -> srcPackageJsonFile = innerFile
                        isKotlinJsRuntimeFile(innerFile) -> files.add(innerFile)
                    }
                }
            }
        }
    }

    fun rebuild(): File? {
        if (files.isEmpty()) return null

        val packageJson = fromSrcPackageJson(srcPackageJsonFile)
            ?: PackageJson(dependency.moduleName, dependency.moduleVersion)

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

private val File.isCompatibleArchive
    get() = isFile
            && (extension == "jar"
            || extension == "zip"
            || extension == KLIB_TYPE)

private fun isKotlinJsRuntimeFile(file: File): Boolean {
    if (!file.isFile) return false
    val name = file.name
    return (name.endsWith(".js") && !name.endsWith(".meta.js"))
            || name.endsWith(".js.map")
}
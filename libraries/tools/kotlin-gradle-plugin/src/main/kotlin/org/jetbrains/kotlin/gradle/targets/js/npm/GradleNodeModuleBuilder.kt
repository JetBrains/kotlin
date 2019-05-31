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

internal fun fixSemver(version: String): String {
    var i = 0
    var number = 0
    var major = "1"
    var minor = "0"
    var patch = "0"
    val rest = StringBuilder()
    val digits = StringBuilder()

    fun setComponent() {
        val digitsFiltered = digits.toString().trimStart { it == '0' }.takeIf { it.isNotEmpty() } ?: "0"
        when (number) {
            0 -> major = digitsFiltered
            1 -> minor = digitsFiltered
            2 -> patch = digitsFiltered
            else -> error(number)
        }
    }

    while (i < version.length) {
        val c = version[i++]
        if (c.isDigit()) digits.append(c)
        else if (c == '-' || c == '+') {
            // examples:
            // 1.2.3-RC1-1234,
            // 1.2-RC1-1234
            // 1.2-beta.11+sha.0x
            setComponent()
            number = 3
            rest.append(c)
            break
        } else if (c == '.') {
            rest.append(c)
            setComponent()
            digits.setLength(0)
            number++
            if (number > 2) break
        } else rest.append(c)
    }

    if (number <= 2) setComponent()

    rest.append(version.substring(i))

    val restFiltered = rest.filter {
        it in '0'..'9' ||
                it in 'A'..'Z' ||
                it in 'a'..'z' ||
                it == '.' ||
                it == '-' ||
                it == '+'
    }
    val restComponents = restFiltered.split('+', limit = 2)

    val preRelease = restComponents.getOrNull(0)
        ?.foldDelimiters()
        ?.trim { it == '-' || it == '.' }
        ?.takeIf { it.isNotEmpty() }

    val build = restComponents.getOrNull(1)
        ?.filter { it != '+' }
        ?.trim { it == '-' || it == '.' }
        ?.takeIf { it.isNotEmpty() }

    return "$major.$minor.$patch" +
            (if (preRelease != null) "-$preRelease" else "") +
            (if (build != null) "+$build" else "")
}

private fun String.foldDelimiters(): String {
    val result = StringBuilder(length)
    var endsWithDelimiter = false
    for (i in 0 until length) {
        val c = this[i]
        if (c == '+' || c == '-' || c == '.') {
            if (!endsWithDelimiter) {
                result.append(c)
                endsWithDelimiter = true
            }
        } else {
            endsWithDelimiter = false
            result.append(c)
        }
    }
    return result.toString()
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
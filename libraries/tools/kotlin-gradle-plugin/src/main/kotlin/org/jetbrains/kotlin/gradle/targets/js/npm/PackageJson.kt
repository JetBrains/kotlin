/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import java.io.File

// Gson set nulls reflectively no matter on default values and non-null types
class PackageJson(
    var name: String,
    var version: String
) {
    val empty: Boolean
        get() = main == null &&
                private == null &&
                workspaces == null &&
                dependencies.isEmpty() &&
                devDependencies.isEmpty()

    val scopedName: ScopedName
        get() = scopedName(name)

    var private: Boolean? = null

    var main: String? = null

    var workspaces: Collection<String>? = null

    var resolutions: Map<String, String>? = null

    var types: String? = null

    @Suppress("USELESS_ELVIS")
    val devDependencies = mutableMapOf<String, String>()
        get() = field ?: mutableMapOf()

    @Suppress("USELESS_ELVIS")
    val dependencies = mutableMapOf<String, String>()
        get() = field ?: mutableMapOf()

    @Suppress("USELESS_ELVIS")
    val peerDependencies = mutableMapOf<String, String>()
        get() = field ?: mutableMapOf()

    @Suppress("USELESS_ELVIS")
    val optionalDependencies = mutableMapOf<String, String>()
        get() = field ?: mutableMapOf()

    @Suppress("USELESS_ELVIS")
    val bundledDependencies = mutableListOf<String>()
        get() = field ?: mutableListOf()

    companion object {
        fun scopedName(name: String): ScopedName = if (name.contains("/")) ScopedName(
            scope = name.substringBeforeLast("/").removePrefix("@"),
            name = name.substringAfterLast("/")
        ) else ScopedName(scope = null, name = name)

        operator fun invoke(scope: String, name: String, version: String) =
            PackageJson(ScopedName(scope, name).toString(), version)
    }

    data class ScopedName(val scope: String?, val name: String) {
        override fun toString() = if (scope == null) name else "@$scope/$name"
    }

    fun saveTo(packageJsonFile: File) {
        val gson = GsonBuilder()
            .setPrettyPrinting()
            .create()

        packageJsonFile.ensureParentDirsCreated()
        packageJsonFile.writer().use {
            gson.toJson(this, it)
        }
    }
}

fun fromSrcPackageJson(packageJson: File?): PackageJson? =
    packageJson?.reader()?.use {
        Gson().fromJson(it, PackageJson::class.java)
    }

fun packageJson(
    npmProject: NpmProject,
    npmDependencies: Collection<NpmDependency>
): PackageJson {
    val compilation = npmProject.compilation

    val packageJson = PackageJson(
        npmProject.name,
        fixSemver(compilation.target.project.version.toString())
    )

    packageJson.main = npmProject.main

    val dependencies = mutableMapOf<String, String>()

    npmDependencies.forEach {
        val module = it.key
        dependencies[it.key] = chooseVersion(dependencies[module], it.version)
    }

    npmDependencies.forEach {
        val dependency = dependencies.getValue(it.key)
        when (it.scope) {
            NpmDependency.Scope.NORMAL -> packageJson.dependencies[it.key] = dependency
            NpmDependency.Scope.DEV -> packageJson.devDependencies[it.key] = dependency
            NpmDependency.Scope.OPTIONAL -> packageJson.optionalDependencies[it.key] = dependency
            NpmDependency.Scope.PEER -> packageJson.peerDependencies[it.key] = dependency
        }
    }

    compilation.packageJsonHandlers.forEach {
        it(packageJson)
    }

    return packageJson
}

// TODO: real versions conflict resolution
private fun chooseVersion(oldVersion: String?, newVersion: String): String {
    // https://yarnpkg.com/lang/en/docs/dependency-versions/#toc-x-ranges
    if (oldVersion == "*") {
        return newVersion
    }

    return oldVersion ?: newVersion
}
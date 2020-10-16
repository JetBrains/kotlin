/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import java.io.File

// Gson set nulls reflectively no matter on default values and non-null types
class PackageJson(
    var name: String,
    var version: String
) {
    internal val customFields = mutableMapOf<String, Any?>()

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

    fun customField(pair: Pair<String, Any>) {
        customFields[pair.first] = pair.second
    }

    fun customField(key: String, value: Any) {
        customFields[key] = value
    }

    fun customField(key: String, value: Number) {
        customFields[key] = value
    }

    fun customField(key: String, value: Boolean) {
        customFields[key] = value
    }

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
            .disableHtmlEscaping()
            .addSerializationExclusionStrategy(
                object : ExclusionStrategy {
                    override fun shouldSkipField(f: FieldAttributes?): Boolean =
                        f?.name == this@PackageJson::customFields.name

                    override fun shouldSkipClass(clazz: Class<*>?): Boolean =
                        false
                }
            )
            .create()

        packageJsonFile.ensureParentDirsCreated()
        val jsonTree = gson.toJsonTree(this)
        customFields
            .forEach { (key, value) ->
                val valueElement = gson.toJsonTree(value)
                jsonTree.asJsonObject.add(key, valueElement)
            }
        packageJsonFile.writer().use {
            gson.toJson(jsonTree, it)
        }
    }
}

fun fromSrcPackageJson(packageJson: File?): PackageJson? =
    packageJson?.reader()?.use {
        Gson().fromJson(it, PackageJson::class.java)
    }

internal fun packageJson(
    name: String,
    version: String,
    main: String,
    npmDependencies: Collection<NpmDependencyDeclaration>
): PackageJson {

    val packageJson = PackageJson(
        name,
        fixSemver(version)
    )

    packageJson.main = main

    val dependencies = mutableMapOf<String, String>()

    npmDependencies.forEach {
        val module = it.name
        dependencies[module] = chooseVersion(module, dependencies[module], it.version)
    }

    npmDependencies.forEach {
        val dependency = dependencies.getValue(it.name)
        when (it.scope) {
            NpmDependency.Scope.NORMAL -> packageJson.dependencies[it.name] = dependency
            NpmDependency.Scope.DEV -> packageJson.devDependencies[it.name] = dependency
            NpmDependency.Scope.OPTIONAL -> packageJson.optionalDependencies[it.name] = dependency
            NpmDependency.Scope.PEER -> packageJson.peerDependencies[it.name] = dependency
        }
    }

//    compilation.packageJsonHandlers.forEach {
//        it(packageJson)
//    }

    return packageJson
}

private fun chooseVersion(
    module: String,
    oldVersion: String?,
    newVersion: String
): String {
    if (oldVersion == null) {
        return newVersion
    }

    return (includedRange(oldVersion) intersect includedRange(newVersion))?.toString()
        ?: throw GradleException(
            """
                There is already declared version of '$module' with version '$oldVersion' which does not intersects with another declared version '${newVersion}'
            """.trimIndent()
        )
}

internal const val fakePackageJsonValue = "FAKE"
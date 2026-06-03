/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import org.jetbrains.kotlin.gradle.internal.json.anyToJsonElement
import java.io.File
import java.io.Serializable

// Gson set nulls reflectively no matter on default values and non-null types
class PackageJson(
    var name: String,
    var version: String
) : Serializable {
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

    var overrides: Map<String, String>? = null

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

    fun customField(pair: Pair<String, Any?>) {
        customFields[pair.first] = pair.second
    }

    fun customField(key: String, value: Any?) {
        customFields[key] = value
    }

    fun customField(key: String, value: Number) {
        customFields[key] = value
    }

    fun customField(key: String, value: Boolean) {
        customFields[key] = value
    }

    companion object {
        internal val prettyJson = Json { prettyPrint = true }

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
        packageJsonFile.ensureParentDirsCreated()
        val jsonTree = toJsonElement()
        val newText = prettyJson.encodeToString(jsonTree)
        val previousText = if (packageJsonFile.exists()) packageJsonFile.readText() else null
        if (newText != previousText) {
            packageJsonFile.writeText(newText)
        }
    }

    private fun toJsonElement(): JsonObject = buildJsonObject {
        put("name", JsonPrimitive(name))
        put("version", JsonPrimitive(version))
        if (private != null) put("private", JsonPrimitive(private!!))
        if (main != null) put("main", JsonPrimitive(main!!))
        if (types != null) put("types", JsonPrimitive(types!!))
        if (workspaces != null) put("workspaces", buildJsonArray {
            workspaces!!.forEach { add(JsonPrimitive(it)) }
        })
        if (overrides != null) put("overrides", buildJsonObject {
            overrides!!.forEach { (k, v) -> put(k, JsonPrimitive(v)) }
        })
        if (dependencies.isNotEmpty()) put("dependencies", buildJsonObject {
            dependencies.forEach { (k, v) -> put(k, JsonPrimitive(v)) }
        })
        if (devDependencies.isNotEmpty()) put("devDependencies", buildJsonObject {
            devDependencies.forEach { (k, v) -> put(k, JsonPrimitive(v)) }
        })
        if (peerDependencies.isNotEmpty()) put("peerDependencies", buildJsonObject {
            peerDependencies.forEach { (k, v) -> put(k, JsonPrimitive(v)) }
        })
        if (optionalDependencies.isNotEmpty()) put("optionalDependencies", buildJsonObject {
            optionalDependencies.forEach { (k, v) -> put(k, JsonPrimitive(v)) }
        })
        if (bundledDependencies.isNotEmpty()) put("bundledDependencies", buildJsonArray {
            bundledDependencies.forEach { add(JsonPrimitive(it)) }
        })
        // Inline customFields, honouring user-supplied null values
        customFields.forEach { (k, v) -> put(k, anyToJsonElement(v)) }
    }
}

fun fromSrcPackageJson(packageJson: File?): PackageJson? = packageJson?.let { parsePackageJson(it.readText()) }

private fun parsePackageJson(text: String): PackageJson? {
    return try {
        val obj = Json.parseToJsonElement(text).jsonObject
        val name = obj["name"]?.jsonPrimitive?.content ?: return null
        val version = obj["version"]?.jsonPrimitive?.content ?: return null
        PackageJson(name, version).also { pkg ->
            pkg.private = obj["private"]?.jsonPrimitive?.content?.toBoolean()
            pkg.main = obj["main"]?.jsonPrimitive?.content
            pkg.types = obj["types"]?.jsonPrimitive?.content
            pkg.workspaces = obj["workspaces"]?.let { el ->
                (el as? JsonArray)?.map { it.jsonPrimitive.content }
            }
            obj["dependencies"]?.jsonObject?.forEach { (k, v) -> pkg.dependencies[k] = v.jsonPrimitive.content }
            obj["devDependencies"]?.jsonObject?.forEach { (k, v) -> pkg.devDependencies[k] = v.jsonPrimitive.content }
            obj["peerDependencies"]?.jsonObject?.forEach { (k, v) -> pkg.peerDependencies[k] = v.jsonPrimitive.content }
            obj["optionalDependencies"]?.jsonObject?.forEach { (k, v) -> pkg.optionalDependencies[k] = v.jsonPrimitive.content }
        }
    } catch (_: Exception) {
        null
    }
}

internal fun packageJson(
    name: String,
    version: String,
    main: String,
    types: String? = null,
    npmDependencies: Collection<NpmDependencyDeclaration>,
    packageJsonHandlers: List<Action<PackageJson>>
): PackageJson {

    val packageJson = PackageJson(
        name,
        fixSemver(version)
    )

    packageJson.main = main
    packageJson.types = types

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

    packageJsonHandlers.forEach {
        it.execute(packageJson)
    }

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

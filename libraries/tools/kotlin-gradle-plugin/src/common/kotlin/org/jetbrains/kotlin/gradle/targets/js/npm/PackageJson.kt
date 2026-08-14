/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.jetbrains.kotlin.gradle.internal.json.KgpJson
import org.jetbrains.kotlin.gradle.internal.json.anyToJsonElement
import java.io.File
import java.io.Serializable
import kotlin.io.path.createDirectories

// The elvis guards below look useless to the compiler, but this type is java.io.Serializable: Java deserialization
// bypasses constructors and initializers, so a non-null property can still arrive as null.
class PackageJson(
    var name: String,
    var version: String,
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
        packageJsonFile.toPath().parent.createDirectories()

        val jsonTree = toJsonElement()
        val previous = if (packageJsonFile.exists()) {
            packageJsonFile.reader().use { Json.parseToJsonElement(it.readText()) }
        } else {
            null
        }

        if (jsonTree != previous) {
            packageJsonFile.writeText(KgpJson.prettyPrintedTwoSpaceIndent.encodeToString(JsonObject.serializer(), jsonTree))
        }
    }

    /**
     * Reproduces what Gson's reflective serializer plus `PackageJsonTypeAdapter` used to emit, so that the file
     * npm and yarn consume — and which is compared against the previous run to decide whether to rewrite it —
     * keeps its exact shape:
     *  - keys follow the property declaration order;
     *  - `null` declared properties are dropped, while the collection ones are always written, even when empty;
     *  - [customFields] is inlined at the end and keeps user-supplied `null` values.
     */
    private fun toJsonElement(): JsonObject = buildJsonObject {
        put("name", JsonPrimitive(name))
        put("version", JsonPrimitive(version))
        private?.let { put("private", JsonPrimitive(it)) }
        main?.let { put("main", JsonPrimitive(it)) }
        workspaces?.let { workspaces ->
            put("workspaces", buildJsonArray { workspaces.forEach { add(JsonPrimitive(it)) } })
        }
        overrides?.let { overrides ->
            put("overrides", buildJsonObject { overrides.forEach { (k, v) -> put(k, JsonPrimitive(v)) } })
        }
        types?.let { put("types", JsonPrimitive(it)) }
        put("devDependencies", buildJsonObject { devDependencies.forEach { (k, v) -> put(k, JsonPrimitive(v)) } })
        put("dependencies", buildJsonObject { dependencies.forEach { (k, v) -> put(k, JsonPrimitive(v)) } })
        put("peerDependencies", buildJsonObject { peerDependencies.forEach { (k, v) -> put(k, JsonPrimitive(v)) } })
        put(
            "optionalDependencies",
            buildJsonObject { optionalDependencies.forEach { (k, v) -> put(k, JsonPrimitive(v)) } }
        )
        put("bundledDependencies", buildJsonArray { bundledDependencies.forEach { add(JsonPrimitive(it)) } })
        customFields.forEach { (k, v) -> put(k, anyToJsonElement(v)) }
    }
}

fun fromSrcPackageJson(packageJson: File?): PackageJson? = packageJson?.let { parsePackageJson(it.readText()) }

/**
 * A `package.json` this plugin did not write may carry a byte order mark or use the relaxations npm tolerates, both
 * of which Gson's reader accepted by default. Keep parsing lenient so such files do not start failing the build.
 */
private val lenientJson = Json { isLenient = true }

internal fun parsePackageJsonObject(file: File): JsonObject =
    lenientJson.parseToJsonElement(file.readText().removePrefix("﻿")).jsonObject

/**
 * `JsonNull` is itself a `JsonPrimitive`, so reading `.content` off it would silently produce the string `"null"`.
 */
private val JsonElement.stringOrNull: String?
    get() = (this as? JsonPrimitive)?.contentOrNull

private fun parsePackageJson(text: String): PackageJson? {
    return try {
        val obj = lenientJson.parseToJsonElement(text.removePrefix("﻿")).jsonObject
        val name = obj["name"]?.stringOrNull ?: return null
        // a missing "version" is normal for a directory dependency; callers substitute the Gradle module version
        val version = obj["version"]?.stringOrNull ?: ""
        PackageJson(name, version).also { pkg ->
            pkg.private = obj["private"]?.stringOrNull?.toBoolean()
            pkg.main = obj["main"]?.stringOrNull
            pkg.types = obj["types"]?.stringOrNull
            pkg.workspaces = (obj["workspaces"] as? JsonArray)?.mapNotNull { it.stringOrNull }
            pkg.overrides = (obj["overrides"] as? JsonObject)?.mapNotNull { (k, v) -> v.stringOrNull?.let { k to it } }?.toMap()
            (obj["bundledDependencies"] as? JsonArray)?.mapNotNullTo(pkg.bundledDependencies) { it.stringOrNull }
            for (scope in listOf("dependencies", "devDependencies", "peerDependencies", "optionalDependencies")) {
                val target = when (scope) {
                    "dependencies" -> pkg.dependencies
                    "devDependencies" -> pkg.devDependencies
                    "peerDependencies" -> pkg.peerDependencies
                    else -> pkg.optionalDependencies
                }
                (obj[scope] as? JsonObject)?.forEach { (k, v) -> v.stringOrNull?.let { target[k] = it } }
            }
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
    packageJsonHandlers: List<Action<PackageJson>>,
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
    newVersion: String,
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

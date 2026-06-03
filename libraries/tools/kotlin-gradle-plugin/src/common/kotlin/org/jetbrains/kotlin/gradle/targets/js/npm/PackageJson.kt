/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.internal.json.KgpJson
import org.jetbrains.kotlin.gradle.internal.json.anyToJsonElement
import java.io.File
import java.io.Serializable
import kotlin.io.path.createDirectories

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

    val devDependencies = mutableMapOf<String, String>()

    val dependencies = mutableMapOf<String, String>()

    val peerDependencies = mutableMapOf<String, String>()

    val optionalDependencies = mutableMapOf<String, String>()

    val bundledDependencies = mutableListOf<String>()

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
        // an interrupted build can leave an empty or truncated file behind: rewrite whatever cannot be read
        // instead of failing every build from now on
        val previous = if (packageJsonFile.exists()) {
            runCatching { parsePackageJsonObject(packageJsonFile) }.getOrNull()
        } else {
            null
        }

        if (jsonTree != previous) {
            packageJsonFile.writeText(KgpJson.prettyPrintedTwoSpaceIndent.encodeToString(JsonObject.serializer(), jsonTree))
        }
    }

    /** [customFields] go last and override a declared key with the same name. */
    private fun toJsonElement(): JsonObject {
        val file = PackageJsonFile(
            name = name,
            version = version,
            private = private,
            main = main,
            workspaces = workspaces?.toList(),
            overrides = overrides,
            types = types,
            devDependencies = devDependencies,
            dependencies = dependencies,
            peerDependencies = peerDependencies,
            optionalDependencies = optionalDependencies,
            bundledDependencies = bundledDependencies,
        )
        val declared = KgpJson.default.encodeToJsonElement(PackageJsonFile.serializer(), file).jsonObject
        return JsonObject(declared + customFields.mapValues { (_, value) -> anyToJsonElement(value) })
    }
}

fun fromSrcPackageJson(packageJson: File?): PackageJson? =
    packageJson?.let {
        try {
            parsePackageJson(it)
        } catch (e: Exception) {
            logger.warn("Cannot parse '$it'. Ignoring it.", e)
            null
        }
    }

/** Strips a byte order mark, as Node does. */
private fun File.readJsonText(): String = readText().removePrefix("\uFEFF")

internal fun parsePackageJsonObject(file: File): JsonObject =
    KgpJson.lenient.parseToJsonElement(file.readJsonText()).jsonObject

private val logger = Logging.getLogger(PackageJson::class.java)

internal fun readPackageJsonFile(file: File): PackageJsonFile =
    KgpJson.lenient.decodeFromString(PackageJsonFile.serializer(), file.readJsonText())

/**
 * Throws on malformed JSON. A missing "name" falls back to [defaultName]; without one the file is ignored.
 * A missing "version" reads as empty, callers substitute the Gradle module version.
 */
internal fun parsePackageJson(file: File, defaultName: String? = null): PackageJson? {
    val json = readPackageJsonFile(file)
    val name = json.name ?: defaultName ?: run {
        logger.warn("Cannot read '$file': it declares no \"name\". Ignoring it.")
        return null
    }
    return PackageJson(name, json.version ?: "").also { pkg ->
        pkg.private = json.private
        pkg.main = json.main
        pkg.types = json.types
        pkg.workspaces = json.workspaces
        pkg.overrides = json.overrides
        json.bundledDependencies?.let { pkg.bundledDependencies.addAll(it) }
        json.dependencies?.let { pkg.dependencies.putAll(it) }
        json.devDependencies?.let { pkg.devDependencies.putAll(it) }
        json.peerDependencies?.let { pkg.peerDependencies.putAll(it) }
        json.optionalDependencies?.let { pkg.optionalDependencies.putAll(it) }
    }
}

/**
 * The declared part of `package.json`; property order is key order. npm allows shapes this class doesn't model
 * (e.g. "workspaces" as an object), so collections drop what they can't read instead of failing.
 */
@kotlinx.serialization.Serializable
internal class PackageJsonFile(
    val name: String? = null,
    val version: String? = null,
    val private: Boolean? = null,
    val main: String? = null,
    @kotlinx.serialization.Serializable(with = StringListSerializer::class)
    val workspaces: List<String>? = null,
    @kotlinx.serialization.Serializable(with = StringMapSerializer::class)
    val overrides: Map<String, String>? = null,
    val types: String? = null,
    @kotlinx.serialization.Serializable(with = StringMapSerializer::class)
    val devDependencies: Map<String, String>? = null,
    @kotlinx.serialization.Serializable(with = StringMapSerializer::class)
    val dependencies: Map<String, String>? = null,
    @kotlinx.serialization.Serializable(with = StringMapSerializer::class)
    val peerDependencies: Map<String, String>? = null,
    @kotlinx.serialization.Serializable(with = StringMapSerializer::class)
    val optionalDependencies: Map<String, String>? = null,
    @kotlinx.serialization.Serializable(with = StringListSerializer::class)
    val bundledDependencies: List<String>? = null,
)

/** Keeps the primitive elements of an array; any other shape reads as `null`. */
@OptIn(ExperimentalSerializationApi::class)
private object StringListSerializer : KSerializer<List<String>?> {
    private val delegate = ListSerializer(String.serializer())
    override val descriptor: SerialDescriptor = delegate.descriptor.nullable

    override fun serialize(encoder: Encoder, value: List<String>?) = encoder.encodeNullableSerializableValue(delegate, value)

    override fun deserialize(decoder: Decoder): List<String>? =
        ((decoder as JsonDecoder).decodeJsonElement() as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
}

/** Keeps the entries of an object with primitive values; any other shape reads as `null`. */
@OptIn(ExperimentalSerializationApi::class)
private object StringMapSerializer : KSerializer<Map<String, String>?> {
    private val delegate = MapSerializer(String.serializer(), String.serializer())
    override val descriptor: SerialDescriptor = delegate.descriptor.nullable

    override fun serialize(encoder: Encoder, value: Map<String, String>?) = encoder.encodeNullableSerializableValue(delegate, value)

    override fun deserialize(decoder: Decoder): Map<String, String>? =
        ((decoder as JsonDecoder).decodeJsonElement() as? JsonObject)
            ?.mapNotNull { (key, value) -> (value as? JsonPrimitive)?.contentOrNull?.let { key to it } }
            ?.toMap()
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

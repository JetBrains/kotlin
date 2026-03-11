@file:OptIn(ExperimentalSerializationApi::class)

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.getExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMDependency.Remote.Repository
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMDependency.Remote.Version
import org.jetbrains.kotlin.gradle.utils.normalizedAbsoluteFile
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.Serializable
import javax.inject.Inject

internal fun Project.locateOrRegisterSwiftPMDependenciesExtension(): SwiftPMImportExtension {
    val existingExtension = kotlinExtension.extensions.findByName(SwiftPMImportExtension.EXTENSION_NAME)
    if (existingExtension != null) {
        return existingExtension as SwiftPMImportExtension
    }
    kotlinExtension.extensions.create(
        SwiftPMImportExtension.EXTENSION_NAME,
        SwiftPMImportExtension::class.java
    )
    return kotlinExtension.getExtension<SwiftPMImportExtension>(
        SwiftPMImportExtension.EXTENSION_NAME
    )!!
}

abstract class SwiftPMImportExtension @Inject constructor(
    objects: ObjectFactory,
) {
    val iosMinimumDeploymentTarget: Property<String> = objects.property(String::class.java)
    val macosMinimumDeploymentTarget: Property<String> = objects.property(String::class.java)
    val watchosMinimumDeploymentTarget: Property<String> = objects.property(String::class.java)
    val tvosMinimumDeploymentTarget: Property<String> = objects.property(String::class.java)

    val discoverClangModulesImplicitly: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(true)

    abstract val xcodeProjectPathForKmpIJPlugin: RegularFileProperty

    internal abstract val swiftPMDependencies: DomainObjectSet<SwiftPMDependency>

    fun url(value: String): Repository.Url = Repository.Url(value)
    fun id(value: String): Repository.Id = Repository.Id(value)

    fun from(version: String): Version = Version.From(version)
    fun exact(version: String): Version = Version.Exact(version)
    fun revision(version: String): Version = Version.Revision(version)
    fun branch(version: String): Version = Version.Branch(version)
    fun range(from: String, through: String): Version = Version.Range(from, through)

    fun product(
        name: String,
        platforms: Set<SwiftPMDependency.Platform>? = null,
        importedClangModules: Set<String> = if (platforms != null) setOf(name) else emptySet(),
    ): SwiftPMDependency.Product = SwiftPMDependency.Product(
        name,
        importedClangModules,
        platforms
    )
    fun iOS(): SwiftPMDependency.Platform = SwiftPMDependency.Platform.iOS
    fun macOS(): SwiftPMDependency.Platform = SwiftPMDependency.Platform.macOS
    fun watchOS(): SwiftPMDependency.Platform = SwiftPMDependency.Platform.watchOS
    fun tvOS(): SwiftPMDependency.Platform = SwiftPMDependency.Platform.tvOS

    @ExperimentalKotlinGradlePluginApi
    fun swiftPackage(
        url: String,
        version: String,
        products: List<String>,
        packageName: String = inferPackageName(url),
        importedClangModules: List<String> = products,
        traits: Set<String> = setOf(),
    ) {
        swiftPMDependencies.add(
            SwiftPMDependency.Remote(
                repository = Repository.Url(url),
                version = Version.From(version),
                packageName = packageName,
                products = products.map { SwiftPMDependency.Product(it) },
                cinteropClangModules = importedClangModules.map {
                    SwiftPMDependency.CinteropClangModule(it)
                },
                traits = traits
            )
        )
    }

    @ExperimentalKotlinGradlePluginApi
    fun swiftPackage(
        url: Repository.Url,
        version: Version,
        products: List<SwiftPMDependency.Product>,
        packageName: String = inferPackageName(url.value),
        importedClangModules: List<String> = products.filter {
            it.platformConstraints == null && it.cinteropClangModules.isEmpty()
        }.map { it.name },
        traits: Set<String> = setOf(),
    ) {
        swiftPMDependencies.add(
            SwiftPMDependency.Remote(
                repository = url,
                version = version,
                packageName = packageName,
                products = products,
                cinteropClangModules = importedClangModules.map {
                    SwiftPMDependency.CinteropClangModule(it)
                } + products.flatMap { product ->
                    product.cinteropClangModules.map {
                        SwiftPMDependency.CinteropClangModule(it, product.platformConstraints)
                    }
                },
                traits = traits,
            )
        )
    }

    @ExperimentalKotlinGradlePluginApi
    fun swiftPackage(
        repository: Repository,
        version: Version,
        products: List<SwiftPMDependency.Product>,
        packageName: String,
        importedClangModules: List<String> = products.filter {
            it.platformConstraints == null && it.cinteropClangModules.isEmpty()
        }.map { it.name },
        traits: Set<String> = setOf(),
    ) {
        swiftPMDependencies.add(
            SwiftPMDependency.Remote(
                repository = repository,
                version = version,
                packageName = packageName,
                products = products,
                cinteropClangModules = importedClangModules.map {
                    SwiftPMDependency.CinteropClangModule(it)
                } + products.flatMap { product ->
                    product.cinteropClangModules.map {
                        SwiftPMDependency.CinteropClangModule(it, product.platformConstraints)
                    }
                },
                traits = traits,
            )
        )
    }

    /**
     * Adds a local SwiftPM package dependency using a Gradle [Directory].
     *
     * @param directory The directory containing the SwiftPM package (must contain Package.swift)
     * @param products List of SwiftPM product names to import
     * @param packageName Optional package name (inferred from directory name if not specified)
     * @param importedClangModules List of modules to import (defaults to products list)
     * @param traits SwiftPM traits to enable
     *
     * Example:
     * ```
     * localPackage(
     *     directory = layout.projectDirectory.dir("../MySwiftPackage"),
     *     products = listOf("MySwiftPackage"),
     * )
     * ```
     */
    @ExperimentalKotlinGradlePluginApi
    fun localSwiftPackage(
        directory: Directory,
        products: List<String>,
        packageName: String = inferLocalPackageName(directory),
        importedClangModules: List<String> = products,
        traits: Set<String> = setOf(),
    ) {
        val absolutePath = directory.asFile.normalizedAbsoluteFile()

        swiftPMDependencies.add(
            SwiftPMDependency.Local(
                absolutePath = absolutePath,
                packageName = packageName,
                products = products.map { SwiftPMDependency.Product(it) },
                cinteropClangModules = importedClangModules.map {
                    SwiftPMDependency.CinteropClangModule(it)
                },
                traits = traits,
            )
        )
    }

    /**
     * Adds a local SwiftPM package dependency using a Gradle [Directory] with advanced product configuration.
     *
     * @param directory The directory containing the SwiftPM package (must contain Package.swift)
     * @param products List of SwiftPM products with platform constraints
     * @param packageName Optional package name (inferred from directory name if not specified)
     * @param importedClangModules List of modules to import
     * @param traits SwiftPM traits to enable
     */
    @ExperimentalKotlinGradlePluginApi
    // Otherwise this overload clashes in jvm signature
    @JvmName("localPackageWithProducts")
    fun localSwiftPackage(
        directory: Directory,
        products: List<SwiftPMDependency.Product>,
        packageName: String = inferLocalPackageName(directory),
        importedClangModules: List<String> = products.filter {
            it.platformConstraints == null && it.cinteropClangModules.isEmpty()
        }.map { it.name },
        traits: Set<String> = setOf(),
    ) {
        val absolutePath = directory.asFile.normalizedAbsoluteFile()

        swiftPMDependencies.add(
            SwiftPMDependency.Local(
                absolutePath = absolutePath,
                packageName = packageName,
                products = products,
                cinteropClangModules = importedClangModules.map {
                    SwiftPMDependency.CinteropClangModule(it)
                } + products.flatMap { product ->
                    product.cinteropClangModules.map {
                        SwiftPMDependency.CinteropClangModule(it, product.platformConstraints)
                    }
                },
                traits = traits,
            )
        )
    }

    // FIXME: KT-84695 Check and test if this is actually correct
    private fun inferPackageName(url: String) = url.split("/").last().split(".git").first()

    private fun inferLocalPackageName(directory: Directory): String {
        return directory.asFile.toPath().toAbsolutePath().normalize().fileName?.toString()
            ?: directory.asFile.name
    }

    companion object {
        const val EXTENSION_NAME = "swiftPMDependencies"
    }
}

@kotlinx.serialization.Serializable
sealed class SwiftPMDependency : Serializable {
    internal abstract val packageName: String
    internal abstract val products: List<Product>
    internal abstract val cinteropClangModules: List<CinteropClangModule>
    internal abstract val traits: Set<String>

    @kotlinx.serialization.Serializable
    data class Product internal constructor(
        internal val name: String,
        // this is not actually used for translation
        internal val cinteropClangModules: Set<String> = setOf(),
        internal val platformConstraints: Set<Platform>? = null,
    ) : Serializable

    @kotlinx.serialization.Serializable
    data class CinteropClangModule internal constructor(
        internal val name: String,
        internal val platformConstraints: Set<Platform>? = null,
    ) : Serializable

    @kotlinx.serialization.Serializable
    enum class Platform {
        iOS,
        macOS,
        tvOS,
        watchOS;

        val swiftEnumName: String get() = when (this) {
                iOS -> "iOS"
                macOS -> "macOS"
                tvOS -> "tvOS"
                watchOS -> "watchOS"
            }
    }

    @kotlinx.serialization.Serializable
    data class Remote internal constructor(
        internal val repository: Repository,
        internal val version: Version,
        override val products: List<Product>,
        override val cinteropClangModules: List<CinteropClangModule>,
        override val packageName: String,
        override val traits: Set<String>,
    ) : SwiftPMDependency() {
        @kotlinx.serialization.Serializable
        sealed class Version : Serializable {
            @kotlinx.serialization.Serializable
            data class Exact internal constructor(internal val value: String) : Version()

            @kotlinx.serialization.Serializable
            data class From internal constructor(internal val value: String) : Version()

            @kotlinx.serialization.Serializable
            data class Range internal constructor(internal val from: String, val through: String) : Version()

            @kotlinx.serialization.Serializable
            data class Branch internal constructor(internal val value: String) : Version()

            @kotlinx.serialization.Serializable
            data class Revision internal constructor(internal val value: String) : Version()
        }

        @kotlinx.serialization.Serializable
        sealed class Repository : Serializable {
            @kotlinx.serialization.Serializable
            data class Id internal constructor(internal val value: String) : Repository()

            @kotlinx.serialization.Serializable
            data class Url internal constructor(internal val value: String) : Repository()
        }

        companion object {
        }
    }

    /**
     * A local SwiftPM package dependency.
     *
     * @property absolutePath Absolute path to the SwiftPM package directory
     */
    @kotlinx.serialization.Serializable
    data class Local internal constructor(
        @kotlinx.serialization.Serializable(with = LocalFileSerializer::class)
        internal val absolutePath: File,
        override val products: List<Product>,
        override val cinteropClangModules: List<CinteropClangModule>,
        override val packageName: String,
        override val traits: Set<String>,
    ) : SwiftPMDependency() {

        companion object {
        }

        internal class LocalFileSerializer : KSerializer<File> {
            override val descriptor: SerialDescriptor get() = JsonElement.serializer().descriptor
            override fun serialize(encoder: Encoder, value: File) = encoder.encodeString(value.path)
            override fun deserialize(decoder: Decoder): File = File(decoder.decodeString())
        }
    }
}

// This is the structure that we serialize into
@kotlinx.serialization.Serializable
internal data class SwiftPMImportMetadata(
    val iosDeploymentVersion: String?,
    val macosDeploymentVersion: String?,
    val watchosDeploymentVersion: String?,
    val tvosDeploymentVersion: String?,
    @Suppress("unused")
    val isModulesDiscoveryEnabled: Boolean,
    val dependencies: Set<SwiftPMDependency>
) : Serializable

private val swiftPMMetadataJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

internal fun deserializeSwiftPMImportMetadata(inputStream: InputStream) =
    swiftPMMetadataJson.decodeFromStream<SwiftPMImportMetadata>(inputStream)

internal fun SwiftPMImportMetadata.serializeSwiftPMImportMetadata(outputStream: OutputStream) =
    swiftPMMetadataJson.encodeToStream(this, outputStream)

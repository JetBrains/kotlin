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
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
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
        SwiftPMImportExtension::class.java,
        project,
    )
    return kotlinExtension.getExtension<SwiftPMImportExtension>(
        SwiftPMImportExtension.EXTENSION_NAME
    )!!
}

/**
 * Configures Swift Package Manager dependencies consumed by the Kotlin Apple integration.
 *
 * SwiftPM import is cinterop-based and exposes Objective-C-compatible APIs from SwiftPM packages to Kotlin/Native.
 * Packages must provide importable Objective-C headers directly or through Swift declarations exported with `@objc` or
 * `@objcMembers`. Pure Swift APIs are not imported by this DSL.
 *
 * This extension is available inside the `kotlin {}` block:
 *
 * ```kotlin
 * kotlin {
 *     swiftPMDependencies {
 *         iosMinimumDeploymentTarget.set("16.0")
 *
 *         swiftPackage(
 *             url = "https://github.com/RevenueCat/purchases-ios.git",
 *             version = "5.43.0",
 *             products = listOf("RevenueCat"),
 *         )
 *     }
 * }
 * ```
 *
 * The declared dependencies apply to Apple compilations in the current Gradle project. Use [product] platform
 * constraints to narrow individual products to specific Apple targets when necessary.
 *
 * Use [swiftPackage] to declare remote packages and [localSwiftPackage] to declare packages from the local filesystem.
 */
abstract class SwiftPMImportExtension @Inject constructor(
    objects: ObjectFactory,
    private val project: Project,
) {
    /** Minimum iOS deployment target written to generated SwiftPM manifests. */
    val iosMinimumDeploymentTarget: Property<String> = objects.property(String::class.java)

    /** Minimum macOS deployment target written to generated SwiftPM manifests. */
    val macosMinimumDeploymentTarget: Property<String> = objects.property(String::class.java)

    /** Minimum watchOS deployment target written to generated SwiftPM manifests. */
    val watchosMinimumDeploymentTarget: Property<String> = objects.property(String::class.java)

    /** Minimum tvOS deployment target written to generated SwiftPM manifests. */
    val tvosMinimumDeploymentTarget: Property<String> = objects.property(String::class.java)

    /**
     * Enables implicit discovery of Clang modules from imported SwiftPM packages.
     *
     * When enabled, generated cinterop definition files discover Clang modules automatically and skip non-importable
     * modules. In this mode, explicit module lists configured through [swiftPackage], [localSwiftPackage], or [product]
     * are not used for cinterop module selection. Set this to `false` to rely only on those explicit module
     * declarations.
     *
     * Defaults to `true`.
     */
    val discoverClangModulesImplicitly: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(true)

    /**
     * Shared lock identifier for Package.resolved synchronization across projects.
     *
     * Projects with the same identifier are expected to contribute into one aggregation bucket.
     *
     * 1. Plugin application
     * 2. Build script evaluation
     * 3. After evaluate ...
     */
    private var isProjectEvaluated = false

    init {
        project.afterEvaluate {
            isProjectEvaluated = true
        }
    }

    /**
     * Controls how `Package.resolved` is synchronized for SwiftPM import.
     *
     * By default, all Gradle projects participate in the shared `identifier("default")` scope.
     * Projects with the same [PackageResolvedSynchronization.Identifier] aggregate their SwiftPM dependency graph into a
     * shared lock-file bucket under the root project, while [noSynchronization] keeps resolution isolated per project.
     *
     * Configure this property during project configuration.
     */
    var packageResolvedSynchronization: PackageResolvedSynchronization =
        PackageResolvedSynchronization.Identifier("default")
        set(value) {
            if (isProjectEvaluated) {
                project.reportDiagnostic(
                    KotlinToolingDiagnostics.SwiftPMImportLockFileSync()
                )
            }

            field = value
        }


    // packageResolvedSynchronization = identifier("id")


    internal abstract val xcodeProjectPathForKmpIJPlugin: RegularFileProperty

    internal abstract val swiftPMDependencies: DomainObjectSet<SwiftPMDependency>

    /** Creates a repository descriptor from a package URL. */
    fun url(value: String): Repository.Url = Repository.Url(value)

    /** Creates a repository descriptor from a SwiftPM package identity. */
    fun id(value: String): Repository.Id = Repository.Id(value)

    /** Creates a version requirement from [version] up to, but not including, the next major version. */
    fun from(version: String): Version = Version.From(version)

    /** Creates an exact version requirement for [version]. Prefer range-based requirements when possible. */
    fun exact(version: String): Version = Version.Exact(version)

    /** Creates a version requirement pinned to a Git revision. */
    fun revision(version: String): Version = Version.Revision(version)

    /** Creates a version requirement pinned to a Git branch. */
    fun branch(version: String): Version = Version.Branch(version)

    /** Creates an inclusive version range requirement. */
    fun range(from: String, through: String): Version = Version.Range(from, through)

    /**
     * Declares a SwiftPM product imported from a package.
     *
     * In SwiftPM terms, products are the linkage-level units consumed across package boundaries.
     * Use [platforms] to restrict the product to specific Apple targets and [importedClangModules] to override the
     * Clang modules exposed to cinterop for this product.
     */
    fun product(
        name: String,
        platforms: Set<SwiftPMDependency.Platform>? = null,
        importedClangModules: Set<String> = if (platforms != null) setOf(name) else emptySet(),
    ): SwiftPMDependency.Product = SwiftPMDependency.Product(
        name,
        importedClangModules,
        platforms
    )

    /** Returns the iOS platform constraint for [product]. */
    fun iOS(): SwiftPMDependency.Platform = SwiftPMDependency.Platform.iOS

    /** Returns the macOS platform constraint for [product]. */
    fun macOS(): SwiftPMDependency.Platform = SwiftPMDependency.Platform.macOS

    /** Returns the watchOS platform constraint for [product]. */
    fun watchOS(): SwiftPMDependency.Platform = SwiftPMDependency.Platform.watchOS

    /** Returns the tvOS platform constraint for [product]. */
    fun tvOS(): SwiftPMDependency.Platform = SwiftPMDependency.Platform.tvOS

    /**
     * Adds a remote SwiftPM package dependency using URL and version shorthand.
     *
     * This overload treats [version] as [Version.From] and each entry in [products] as a plain [SwiftPMDependency.Product].
     */
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

    /**
     * Adds a remote SwiftPM package dependency with typed repository, version, and product descriptors.
     *
     * Use [product], [url], [exact], [from], [range], [branch], and [revision] to build the arguments.
     */
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

    /**
     * Adds a remote SwiftPM package dependency with a custom repository descriptor.
     *
     * This overload supports both repository URLs and SwiftPM package identities via [Repository].
     */
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
     * localSwiftPackage(
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
     * @param products List of SwiftPM products with optional platform constraints
     * @param packageName Optional package name (inferred from directory name if not specified)
     * @param importedClangModules List of modules to import (defaults to products without per-platform overrides)
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


    /** Creates a shared lock-file synchronization identifier for projects that should resolve one SwiftPM graph together. */
    fun identifier(value: String): PackageResolvedSynchronization =
        PackageResolvedSynchronization.Identifier(value)

    /** Disables cross-project lock-file synchronization and resolves this project's SwiftPM graph independently. */
    fun noSynchronization(): PackageResolvedSynchronization =
        PackageResolvedSynchronization.None

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

/** Represents a SwiftPM dependency declared through [SwiftPMImportExtension]. */
@kotlinx.serialization.Serializable
sealed class SwiftPMDependency : Serializable {
    internal abstract val packageName: String
    internal abstract val products: List<Product>
    internal abstract val cinteropClangModules: List<CinteropClangModule>
    internal abstract val traits: Set<String>

    /** Describes a SwiftPM product imported from a package. */
    @kotlinx.serialization.Serializable
    data class Product internal constructor(
        internal val name: String,
        // this is not actually used for translation
        internal val cinteropClangModules: Set<String> = setOf(),
        internal val platformConstraints: Set<Platform>? = null,
    ) : Serializable

    /** Describes a Clang module imported into cinterop from a SwiftPM package. */
    @kotlinx.serialization.Serializable
    data class CinteropClangModule internal constructor(
        internal val name: String,
        internal val platformConstraints: Set<Platform>? = null,
    ) : Serializable

    /** Apple platform constraint used by [Product] and [CinteropClangModule]. */
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

    /** A remote SwiftPM package dependency created by [SwiftPMImportExtension.swiftPackage]. */
    @kotlinx.serialization.Serializable
    data class Remote internal constructor(
        internal val repository: Repository,
        internal val version: Version,
        override val products: List<Product>,
        override val cinteropClangModules: List<CinteropClangModule>,
        override val packageName: String,
        override val traits: Set<String>,
    ) : SwiftPMDependency() {
        /** Version requirement used when resolving a remote SwiftPM package. */
        @kotlinx.serialization.Serializable
        sealed class Version : Serializable {
            /** Pins the package to an exact version. Prefer range-based requirements when possible. */
            @kotlinx.serialization.Serializable
            data class Exact internal constructor(internal val value: String) : Version()

            /** Allows versions from the given lower bound up to, but not including, the next major version. */
            @kotlinx.serialization.Serializable
            data class From internal constructor(internal val value: String) : Version()

            /** Allows versions in the inclusive range from [from] to [through]. */
            @kotlinx.serialization.Serializable
            data class Range internal constructor(internal val from: String, val through: String) : Version()

            /** Resolves the package from the given Git branch. */
            @kotlinx.serialization.Serializable
            data class Branch internal constructor(internal val value: String) : Version()

            /** Resolves the package from the given Git revision. */
            @kotlinx.serialization.Serializable
            data class Revision internal constructor(internal val value: String) : Version()
        }

        /** Remote repository descriptor used to locate a SwiftPM package. */
        @kotlinx.serialization.Serializable
        sealed class Repository : Serializable {
            /** SwiftPM package identity. */
            @kotlinx.serialization.Serializable
            data class Id internal constructor(internal val value: String) : Repository()

            /** SwiftPM package repository URL. */
            @kotlinx.serialization.Serializable
            data class Url internal constructor(internal val value: String) : Repository()
        }

        companion object {
        }
    }

    /**
     * A local SwiftPM package dependency created by [SwiftPMImportExtension.localSwiftPackage].
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


/** Controls persisted `Package.resolved` synchronization for SwiftPM import. */
sealed class PackageResolvedSynchronization {
    /** Shares one persisted lock file bucket between all projects with the same identifier. */
    data class Identifier(val identifier: String) : PackageResolvedSynchronization()

    /** Disables persisted lock-file synchronization. */
    object None : PackageResolvedSynchronization()
}

// This is the structure that we serialize into
@kotlinx.serialization.Serializable
internal data class SwiftPMImportMetadata(
    val konanTargets: Set<String>,
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

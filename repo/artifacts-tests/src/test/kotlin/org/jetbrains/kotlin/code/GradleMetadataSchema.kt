package org.jetbrains.kotlin.code

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class GradleMetadata(
    val formatVersion: String,
    val component: Component,
    val createdBy: CreatedBy,
    val variants: List<Variant>,
) {
    fun removeFilesFingerprint() {
        variants.forEach { it.removeFilesFingerprint() }
    }
}

@Serializable
data class Component(
    val url: String? = null,
    val group: String,
    val module: String,
    val version: String,
    val attributes: ComponentAttributes? = null,
)

@Serializable
data class ComponentAttributes(
    @SerialName("org.gradle.status") var orgGradleStatus: String,
)

@Serializable
data class CreatedBy(
    val gradle: Gradle,
)

@Serializable
data class Gradle(
    val version: String,
)

@Serializable
data class Variant(
    val name: String,
    val attributes: VariantAttributes,
    @SerialName("available-at") val availableAt: Component? = null,
    val dependencies: List<Dependency>? = null,
    val dependencyConstraints: List<Dependency>? = null,
    val files: List<File>? = null,
    val capabilities: List<Capability>? = null,
) {
    fun removeFilesFingerprint() {
        files?.forEach { it.removeFingerprint() }
    }
}

@Serializable
data class VariantAttributes(
    @SerialName("org.gradle.category") val orgGradleCategory: String? = null,
    @SerialName("org.gradle.dependency.bundling") val orgGradleDependencyBundling: String? = null,
    @SerialName("org.gradle.docstype") val orgGradleDocstype: String? = null,
    @SerialName("org.gradle.usage") val orgGradleUsage: String? = null,
    @SerialName("org.gradle.jvm.environment") val orgGradleJvmEnvironment: String? = null,
    @SerialName("org.gradle.jvm.version") val orgGradleJvmVersion: Int? = null,
    @SerialName("org.gradle.libraryelements") val orgGradleLibraryelements: String? = null,
    @SerialName("org.gradle.plugin.api-version") val orgGradlePluginApiVersion: String? = null,
    @SerialName("org.jetbrains.kotlin.platform.type") val orgJetbrainsKotlinPlatformType: String? = null,
    @SerialName("org.jetbrains.kotlin.klib.packaging") val orgJetbrainsKotlinKlibPackaging: String? = null,
    @SerialName("org.jetbrains.kotlin.js.compiler") val orgJetbrainsKotlinJsCompiler: String? = null,
    @SerialName("org.jetbrains.kotlin.wasm.target") val orgJetbrainsKotlinWasmTarget: String? = null,
)

@Serializable
data class Dependency(
    val group: String,
    val module: String,
    val version: Version,
    val excludes: List<Exclude>? = null,
    val attributes: DependencyAttributes? = null,
    val endorseStrictVersions: Boolean? = null,
    val requestedCapabilities: List<RequestedCapability>? = null,
) : Comparable<Dependency> {
    override fun compareTo(other: Dependency): Int {
        return compareValuesBy(this, other, { it.group }, { it.module }, { it.version })
    }
}

@Serializable
data class Version(
    val requires: String,
) : Comparable<Version> {
    override fun compareTo(other: Version): Int {
        return compareValuesBy(this, other, { it.requires })
    }
}

@Serializable
data class Exclude(
    val group: String,
    val module: String,
)

@Serializable
data class DependencyAttributes(
    @SerialName("org.gradle.category") val orgGradleCategory: String,
)

@Serializable
data class RequestedCapability(
    val group: String,
    val name: String,
)

@Serializable
data class File(
    val name: String,
    val url: String,
    var size: Int?,
    var sha512: String?,
    var sha256: String?,
    var sha1: String?,
    var md5: String?,
) {
    fun removeFingerprint() {
        size = null
        sha512 = null
        sha256 = null
        sha1 = null
        md5 = null
    }
}

@Serializable
data class Capability(
    val group: String,
    val name: String,
    val version: String,
)

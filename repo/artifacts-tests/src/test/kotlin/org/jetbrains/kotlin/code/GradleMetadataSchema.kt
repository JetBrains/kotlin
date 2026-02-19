package org.jetbrains.kotlin.code

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import org.jetbrains.kotlin.utils.addToStdlib.sequenceOfLazyValues

private const val ORG_JETBRAINS_KOTLIN = "org.jetbrains.kotlin"

fun Sequence<Int>.firstNonZeroOrZero() = firstOrNull { it != 0 } ?: 0

private fun <T : Comparable<T>> compareLists(
    lhs: MutableList<T>,
    rhs: MutableList<T>,
): Int {
    for (i in 0 until minOf(lhs.size, rhs.size)) {
        val cmp = lhs[i].compareTo(rhs[i])
        if (cmp != 0) return cmp
    }
    return lhs.size.compareTo(rhs.size)
}

private fun <T : Comparable<T>> compareNullableLists(
    lhs: MutableList<T>?,
    rhs: MutableList<T>?,
): Int {
    return when {
        lhs == null && rhs == null -> 0
        lhs == null -> -1
        rhs == null -> 1
        else -> compareLists(lhs, rhs)
    }
}


@Serializable
data class GradleMetadata(
    val formatVersion: String,
    val component: Component,
    val createdBy: CreatedBy,
    val variants: MutableList<Variant>,
) : Comparable<GradleMetadata> {
    fun setOrgGradleStatusAttributeToRelease() {
        component.attributes?.orgGradleStatus = "release"
    }

    fun removeFilesFingerprint() {
        variants.forEach { it.removeFilesFingerprint() }
    }

    fun sortListsRecursively() {
        variants.sort()
        variants.forEach { it.sortListsRecursively() }
    }

    fun replaceKotlinVersion(oldVersion: String, newVersion: String) {
        component.replaceKotlinVersion(oldVersion, newVersion)
        variants.forEach { it.replaceKotlinVersion(oldVersion, newVersion) }
    }

    override fun compareTo(other: GradleMetadata): Int {
        return sequenceOf(
            compareValuesBy(this, other, { it.formatVersion }, { it.component }, { it.createdBy }),
            compareLists(this.variants, other.variants)
        ).firstNonZeroOrZero()
    }
}

@Serializable
data class Component(
    var url: String? = null,
    val group: String,
    val module: String,
    var version: String,
    val attributes: ComponentAttributes? = null,
) : Comparable<Component> {
    fun replaceKotlinVersion(oldVersion: String, newVersion: String) {
        if (group == ORG_JETBRAINS_KOTLIN) {
            url = url?.replace(oldVersion, newVersion)
            version = version.replace(oldVersion, newVersion)
        }
    }

    override fun compareTo(other: Component): Int {
        return compareValuesBy(
            this, other,
            { it.url },
            { it.group },
            { it.module },
            { it.version },
            { it.attributes }
        )
    }
}

@Serializable
data class ComponentAttributes(
    @SerialName("org.gradle.status") var orgGradleStatus: String,
) : Comparable<ComponentAttributes> {
    override fun compareTo(other: ComponentAttributes): Int {
        return compareValuesBy(this, other, { it.orgGradleStatus })
    }
}

@Serializable
data class CreatedBy(
    val gradle: Gradle,
) : Comparable<CreatedBy> {
    override fun compareTo(other: CreatedBy): Int {
        return compareValuesBy(this, other, { it.gradle })
    }
}

@Serializable
data class Gradle(
    val version: String,
) : Comparable<Gradle> {
    override fun compareTo(other: Gradle): Int {
        return compareValuesBy(this, other, { it.version })
    }
}

@Serializable
data class Variant(
    val name: String,
    val attributes: VariantAttributes,
    @SerialName("available-at") val availableAt: Component? = null,
    val dependencies: MutableList<Dependency>? = null,
    val dependencyConstraints: MutableList<Dependency>? = null,
    val files: MutableList<File>? = null,
    val capabilities: MutableList<Capability>? = null,
) : Comparable<Variant> {
    fun replaceKotlinVersion(oldVersion: String, newVersion: String) {
        files?.forEach { it.replaceKotlinVersion(oldVersion, newVersion) }
        capabilities?.forEach { it.replaceKotlinVersion(oldVersion, newVersion) }
        dependencies?.forEach { it.replaceKotlinVersion(oldVersion, newVersion) }
        dependencyConstraints?.forEach { it.replaceKotlinVersion(oldVersion, newVersion) }
        availableAt?.replaceKotlinVersion(oldVersion, newVersion)
    }

    fun removeFilesFingerprint() {
        files?.forEach { it.removeFingerprint() }
    }

    fun sortListsRecursively() {
        dependencies?.sort()
        dependencies?.forEach { it.sortListsRecursively() }
        dependencyConstraints?.sort()
        dependencyConstraints?.forEach { it.sortListsRecursively() }
        files?.sort()
        capabilities?.sort()
    }

    override fun compareTo(other: Variant): Int {
        return sequenceOfLazyValues(
            {
                compareValuesBy(
                    this, other,
                    { it.name },
                    { it.attributes },
                    { it.availableAt }
                )
            },
            { compareNullableLists(this.dependencies, other.dependencies) },
            { compareNullableLists(this.dependencyConstraints, other.dependencyConstraints) },
            { compareNullableLists(this.files, other.files) },
            { compareNullableLists(this.capabilities, other.capabilities) },
        ).firstOrNull { it != 0 } ?: 0
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
) : Comparable<VariantAttributes> {
    override fun compareTo(other: VariantAttributes): Int {
        return compareValuesBy(
            this, other,
            { it.orgGradleCategory },
            { it.orgGradleDependencyBundling },
            { it.orgGradleDocstype },
            { it.orgGradleUsage },
            { it.orgGradleJvmEnvironment },
            { it.orgGradleJvmVersion },
            { it.orgGradleLibraryelements },
            { it.orgGradlePluginApiVersion },
            { it.orgJetbrainsKotlinPlatformType },
            { it.orgJetbrainsKotlinKlibPackaging },
            { it.orgJetbrainsKotlinJsCompiler },
            { it.orgJetbrainsKotlinWasmTarget }
        )
    }
}

@Serializable
data class Dependency(
    val group: String,
    val module: String,
    val version: Version,
    val attributes: DependencyAttributes? = null,
    val endorseStrictVersions: Boolean? = null,
    val excludes: MutableList<Exclude>? = null,
    val requestedCapabilities: MutableList<RequestedCapability>? = null,
) : Comparable<Dependency> {
    fun sortListsRecursively() {
        excludes?.sort()
        requestedCapabilities?.sort()
    }

    fun replaceKotlinVersion(oldVersion: String, newVersion: String) {
        if (group == ORG_JETBRAINS_KOTLIN) {
            version.requires = version.requires.replace(oldVersion, newVersion)
        }
    }

    override fun compareTo(other: Dependency): Int {
        return sequenceOfLazyValues(
            {
                compareValuesBy(
                    this, other,
                    { it.group },
                    { it.module },
                    { it.version },
                    { it.attributes },
                    { it.endorseStrictVersions }
                )
            },
            { compareNullableLists(this.excludes, other.excludes) },
            { compareNullableLists(this.requestedCapabilities, other.requestedCapabilities) },
        ).firstNonZeroOrZero()
    }
}

@Serializable
data class Version(
    var requires: String,
) : Comparable<Version> {
    override fun compareTo(other: Version): Int {
        return compareValuesBy(this, other, { it.requires })
    }
}

@Serializable
data class Exclude(
    val group: String,
    val module: String,
) : Comparable<Exclude> {
    override fun compareTo(other: Exclude): Int {
        return compareValuesBy(this, other, { it.group }, { it.module })
    }
}

@Serializable
data class DependencyAttributes(
    @SerialName("org.gradle.category") val orgGradleCategory: String,
) : Comparable<DependencyAttributes> {
    override fun compareTo(other: DependencyAttributes): Int {
        return compareValuesBy(this, other, { it.orgGradleCategory })
    }
}

@Serializable
data class RequestedCapability(
    val group: String,
    val name: String,
) : Comparable<RequestedCapability> {
    override fun compareTo(other: RequestedCapability): Int {
        return compareValuesBy(this, other, { it.group }, { it.name })
    }
}

@Serializable
data class File(
    var name: String,
    var url: String,
    var size: Int?,
    var sha512: String?,
    var sha256: String?,
    var sha1: String?,
    var md5: String?,
) : Comparable<File> {
    fun removeFingerprint() {
        size = null
        sha512 = null
        sha256 = null
        sha1 = null
        md5 = null
    }

    fun replaceKotlinVersion(oldVersion: String, newVersion: String) {
        name = name.replace(oldVersion, newVersion)
        url = url.replace(oldVersion, newVersion)
    }

    override fun compareTo(other: File) = compareValuesBy(
        this, other,
        { it.name },
        { it.url },
        { it.size },
        { it.sha512 },
        { it.sha256 },
        { it.sha1 },
        { it.md5 },
    )
}

@Serializable
data class Capability(
    val group: String,
    val name: String,
    var version: String,
) : Comparable<Capability> {
    override fun compareTo(other: Capability): Int {
        return compareValuesBy(this, other, { it.group }, { it.name }, { it.version })
    }

    fun replaceKotlinVersion(oldVersion: String, newVersion: String) {
        if (group == ORG_JETBRAINS_KOTLIN) {
            version = version.replace(oldVersion, newVersion)
        }
    }
}

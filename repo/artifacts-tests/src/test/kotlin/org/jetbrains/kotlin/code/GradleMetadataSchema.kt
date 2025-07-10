/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

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
    /**
     * Compares two GradleMetadata objects, ignoring file fingerprint details (size, hashes).
     *
     * @param other The other GradleMetadata object to compare with.
     * @return True if the content is equal, ignoring specified fields, false otherwise.
     */
    infix fun equalsWithoutFingerprint(other: GradleMetadata): Boolean { // Renamed
        if (this === other) return true

        if (formatVersion != other.formatVersion) return false
        if (component != other.component) return false
        if (createdBy != other.createdBy) return false

        return this.variants.equalAsSets(
            other.variants,
            equals = { a, b -> a equalsWithoutFingerprint b })
    }
}

@Serializable
data class Component(
    val url: String? = null,
    val group: String,
    val module: String,
    val version: String,
    val attributes: ComponentAttributes? = null,
) : Comparable<Component> {
    override fun compareTo(other: Component): Int {
        return compareValuesBy(this, other, { it.url }, { it.group }, { it.module }, { it.version })
    }
}

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
) : Comparable<Variant> {
    infix fun equalsWithoutFingerprint(other: Variant): Boolean { // Renamed
        if (this === other) return true

        if (name != other.name) return false
        if (attributes != other.attributes) return false
        if (availableAt != other.availableAt) return false

        if (!this.dependencies.equalAsSets(other.dependencies)) return false
        if (!this.dependencyConstraints.equalAsSets(other.dependencyConstraints)) return false

        if (!this.files.equalAsSets(other.files, equals = { a, b -> a equalsWithoutFingerprint b })) {
            return false
        }

        if (!this.capabilities.equalAsSets(other.capabilities)) {
            return false
        }

        return true
    }

    override fun compareTo(other: Variant): Int {
        return compareValuesBy(this, other, { it.name })
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
    val size: Int?,
    val sha512: String?,
    val sha256: String?,
    val sha1: String?,
    val md5: String?,
) : Comparable<File>{
    infix fun equalsWithoutFingerprint(other: File): Boolean {
        if (this === other) return true

        if (name != other.name) return false
        if (url != other.url) return false
        return true
    }

    override fun compareTo(other: File): Int {
        return compareValuesBy(this, other, { it.name })
    }
}

@Serializable
data class Capability(
    val group: String,
    val name: String,
    val version: String,
): Comparable<Capability> {
    override fun compareTo(other: Capability): Int {
        return compareValuesBy(this, other, { it.group }, { it.name }, { it.version })
    }
}

/**
 * Compares two lists as if they were sets, meaning the order of elements does not matter.
 * Elements are compared using the provided [equals] function.
 * The lists are sorted using [sortedWith] before comparison to ensure consistent order.
 *
 * @param other The other list to compare with.
 * @param equals A lambda function to compare two elements of type [T].
 * @return True if the lists contain the same elements, regardless of order, false otherwise.
 */
private fun <T : Comparable<T>> List<T>?.equalAsSets(
    other: List<T>?,
    equals: (T, T) -> Boolean = { a, b -> a == b },
): Boolean {
    if (this === other) return true
    if (this == null || other == null) return false

    if (this.size != other.size) return false

    val sortedThis = this.sorted()
    val sortedOther = other.sorted()

    for (i in sortedThis.indices) {
        if (!equals(sortedThis[i], sortedOther[i])) {
            return false
        }
    }

    return true
}

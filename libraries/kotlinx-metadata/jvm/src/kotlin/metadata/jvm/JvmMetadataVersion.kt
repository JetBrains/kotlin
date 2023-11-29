/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.metadata.jvm

import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion as CompilerMetadataVersion

/**
 * Version of the metadata inside JVM classfile.
 *
 * Starting from Kotlin 1.4, the metadata version is equal to the language version.
 * It consists of major and minor versions. Patch version usually does not affect the metadata format, but it is present for completeness because
 * compiler writes its full version to the `@Metadata` annotation; therefore, the patch version is accounted for in comparisons and equality.
 * All components (major, minor, and patch) should be non-negative.
 *
 * While the metadata version is currently equal to the language version, this may be changed in the future, including adding or removing components.
 * Therefore, it is recommended to rely on the [compareTo] and [equals] method instead of accessing components directly.
 *
 * Note that the metadata version is 1.1 for Kotlin compilers from 1.0 until 1.4, and is 1.0 or less for pre-1.0 compilers.
 * Metadata with versions less than 1.1 is considered incompatible and cannot be read or written.
 *
 * The library can read in strict mode only compatible versions of metadata. For definition of a compatible version, see documentation for [LATEST_STABLE_SUPPORTED] property.
 *
 * @property major Major component of version
 * @property minor Minor component of version
 * @property patch Patch component of version
 */
public class JvmMetadataVersion(public val major: Int, public val minor: Int, public val patch: Int) : Comparable<JvmMetadataVersion> {

    /**
     * Creates a new [JvmMetadataVersion] instance with specified [major] and [minor] versions and a patch version equal to 0.
     */
    public constructor(major: Int, minor: Int) : this(major, minor, 0)

    internal constructor(intArray: IntArray) : this(intArray[0], intArray[1], intArray[2])

    @JvmName("toIntArray")
    internal fun toIntArray(): IntArray = intArrayOf(major, minor, patch)

    init {
        require(major >= 0) { "Major version should be not less than 0" }
        require(minor >= 0) { "Minor version should be not less than 0" }
        require(patch >= 0) { "Patch version should be not less than 0" }
    }

    /**
     * Compares this JvmMetadataVersion object with another JvmMetadataVersion object.
     *
     * Comparison is based on integer values of version parts with [major] being most significant one, then [minor], and finally [patch].
     *
     * @return a negative integer, zero, or a positive integer if this JvmMetadataVersion object is less than, equal to, or greater than [other].
     */
    override fun compareTo(other: JvmMetadataVersion): Int {
        val majors = major.compareTo(other.major)
        if (majors != 0) return majors
        val minors = minor.compareTo(other.minor)
        return if (minors != 0) minors else patch.compareTo(other.patch)
    }

    /**
     * Returns a string representation of the version number.
     * The string representation is in the format "major.minor.patch".
     */
    override fun toString(): String = "$major.$minor.$patch"

    /**
     * Calculates the hash code value for the object based on major, minor, and patch components.
     */
    override fun hashCode(): Int {
        var result = major
        result = 31 * result + minor
        result = 31 * result + patch
        return result
    }

    /**
     * Checks if this JvmMetadataVersion object is equal to [other] JvmMetadataVersion object.
     *
     * Instances of JvmMetadataVersion are equal if they have the same major, minor, and patch components.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as JvmMetadataVersion

        if (major != other.major) return false
        if (minor != other.minor) return false
        if (patch != other.patch) return false

        return true
    }

    /**
     * Companion object to hold pre-defined JvmMetadataVersion instances.
     */
    public companion object {
        /**
         * The latest stable metadata version supported by this version of the library.
         * The library can read in strict mode Kotlin metadata produced by Kotlin compilers from 1.0 up to and including this version + 1 minor.
         *
         * In other words, the metadata version is supported if it is greater or equal than 1.1, and less or equal than [LATEST_STABLE_SUPPORTED] + 1 minor version.
         * Note that the metadata version is 1.1 for Kotlin from 1.0 until 1.4, and is equal to the language version starting from Kotlin 1.4.
         *
         * For example, if the latest supported stable Kotlin version is 1.7.0, kotlinx-metadata-jvm can read in strict mode binaries produced by Kotlin compilers from 1.0
         * to 1.8.* inclusively. In this case, this property will have the value `1.7.0`.
         *
         * @see Metadata.metadataVersion
         * @see KotlinClassMetadata.readStrict
         */
        @JvmField
        public val LATEST_STABLE_SUPPORTED: JvmMetadataVersion = JvmMetadataVersion(CompilerMetadataVersion.INSTANCE.toArray())

        @JvmField
        internal val HIGHEST_ALLOWED_TO_WRITE: JvmMetadataVersion = JvmMetadataVersion(CompilerMetadataVersion.INSTANCE_NEXT.toArray())
    }
}

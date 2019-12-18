/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

/**
 * Represents a version of the Kotlin standard library.
 *
 * [major], [minor] and [patch] are integer components of a version,
 * they must be non-negative and not greater than 255 ([MAX_COMPONENT_VALUE]).
 *
 * @constructor Creates a version from all three components.
 */
@SinceKotlin("1.1")
public class KotlinVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<KotlinVersion> {
    /**
     * Creates a version from [major] and [minor] components, leaving [patch] component zero.
     */
    public constructor(major: Int, minor: Int) : this(major, minor, 0)

    private val version = versionOf(major, minor, patch)

    private fun versionOf(major: Int, minor: Int, patch: Int): Int {
        require(major in 0..MAX_COMPONENT_VALUE && minor in 0..MAX_COMPONENT_VALUE && patch in 0..MAX_COMPONENT_VALUE) {
            "Version components are out of range: $major.$minor.$patch"
        }
        return major.shl(16) + minor.shl(8) + patch
    }

    /**
     * Returns the string representation of this version
     */
    override fun toString(): String = "$major.$minor.$patch"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherVersion = (other as? KotlinVersion) ?: return false
        return this.version == otherVersion.version
    }

    override fun hashCode(): Int = version

    override fun compareTo(other: KotlinVersion): Int = version - other.version

    /**
     * Returns `true` if this version is not less than the version specified
     * with the provided [major] and [minor] components.
     */
    public fun isAtLeast(major: Int, minor: Int): Boolean = // this.version >= versionOf(major, minor, 0)
        this.major > major || (this.major == major &&
                this.minor >= minor)

    /**
     * Returns `true` if this version is not less than the version specified
     * with the provided [major], [minor] and [patch] components.
     */
    public fun isAtLeast(major: Int, minor: Int, patch: Int): Boolean = // this.version >= versionOf(major, minor, patch)
        this.major > major || (this.major == major &&
                (this.minor > minor || this.minor == minor &&
                        this.patch >= patch))

    companion object {
        /**
         * Maximum value a version component can have, a constant value 255.
         */
        // NOTE: Must be placed before CURRENT because its initialization requires this field being initialized in JS
        public const val MAX_COMPONENT_VALUE = 255

        /**
         * Returns the current version of the Kotlin standard library.
         */
        @kotlin.jvm.JvmField
        public val CURRENT: KotlinVersion = KotlinVersion(1, 4, 0) // value is written here automatically during build
    }
}

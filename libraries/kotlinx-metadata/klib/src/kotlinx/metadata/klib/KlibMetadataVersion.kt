/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.klib

/**
 * KLIB-specific metadata version. Supposed to be used mainly with the logic related to kotlinx-metadata-klib library.
 */
class KlibMetadataVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<KlibMetadataVersion> {
    constructor(intArray: IntArray) : this(
        intArray.getOrNull(0) ?: error("Major version is expected"),
        intArray.getOrNull(1) ?: error("Minor version is expected"),
        intArray.getOrNull(2) ?: 0,
    ) {
        require(intArray.size <= 3) { "Metadata version should have no more than 3 components" }
    }

    init {
        require(major >= 0) { "Major version should be not less than 0" }
        require(minor >= 0) { "Minor version should be not less than 0" }
        require(patch >= 0) { "Patch version should be not less than 0" }
    }

    fun toArray(): IntArray = intArrayOf(major, minor, patch)

    override fun compareTo(other: KlibMetadataVersion): Int {
        val majors = major.compareTo(other.major)
        if (majors != 0) return majors
        val minors = minor.compareTo(other.minor)
        return if (minors != 0) minors else patch.compareTo(other.patch)
    }

    override fun toString(): String = "$major.$minor.$patch"

    override fun hashCode(): Int {
        var result = major
        result = 31 * result + minor
        result = 31 * result + patch
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KlibMetadataVersion

        if (major != other.major) return false
        if (minor != other.minor) return false
        if (patch != other.patch) return false

        return true
    }
}
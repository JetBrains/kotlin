/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.gradle.targets.js

import java.math.BigInteger

data class SemVer(
    val major: BigInteger,
    val minor: BigInteger,
    val patch: BigInteger,
    val preRelease: String? = null,
    val build: String? = null
) : Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int {
        val compareMajor = major.compareTo(other.major)
        if (compareMajor != 0) return compareMajor

        val compareMinor = minor.compareTo(other.minor)
        if (compareMinor != 0) return compareMinor

        val comparePatch = patch.compareTo(other.patch)
        if (comparePatch != 0) return comparePatch

        val comparePreRelease = compareValues(preRelease, other.preRelease)
        if (comparePreRelease != 0) return comparePreRelease

        val compareBuild = compareValues(build, other.build)
        if (compareBuild != 0) return compareBuild

        return 0
    }


    override fun toString() = "$major.$minor.$patch" +
            (if (preRelease != null) "-$preRelease" else "") +
            (if (build != null) "+$build" else "")

    companion object {
        fun from(string: String): SemVer {

            val minorStart = string.indexOf('.')
            check(minorStart != -1) { "Bad semver: $string. Minor version missed." }

            val patchStart = string.indexOf('.', minorStart + 1)
            check(patchStart != -1) { "Bad semver: $string. Patch version missed." }

            val preReleaseStart = string.indexOf('-', patchStart + 1)
            val buildStart = string.indexOf('+', if (preReleaseStart == -1) patchStart + 1 else preReleaseStart + 1)
            val preReleaseEnd = when {
                buildStart != -1 -> buildStart
                else -> string.length
            }
            val patchEnd = when {
                preReleaseStart != -1 -> preReleaseStart
                buildStart != -1 -> buildStart
                else -> string.length
            }

            val major = string.substring(0, minorStart)
            val minor = string.substring(minorStart + 1, patchStart)
            val patch = string.substring(patchStart + 1, patchEnd)
            val preRelease = if (preReleaseStart != -1) string.substring(preReleaseStart + 1, preReleaseEnd) else ""
            val build = if (buildStart != -1) string.substring(buildStart + 1) else ""

            check(major.isNotBlank()) { "Bad semver: $string. Major version missed." }
            check(minor.isNotBlank()) { "Bad semver: $string. Minor version missed." }
            check(patch.isNotBlank()) { "Bad semver: $string. Patch version missed." }

            return SemVer(
                BigInteger(major),
                BigInteger(minor),
                BigInteger(patch),
                preRelease.takeIf { it.isNotBlank() },
                build.takeIf { it.isNotBlank() }
            )
        }
    }
}
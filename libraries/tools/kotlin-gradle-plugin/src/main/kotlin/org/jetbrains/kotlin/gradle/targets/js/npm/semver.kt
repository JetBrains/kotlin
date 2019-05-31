/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

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

    fun toDebugString(): String {
        return "SemVer(major=$major, minor=$minor, patch=$patch, preRelease=$preRelease, build=$build)"
    }

    companion object {
        fun from(string: String, loose: Boolean = false): SemVer {
            val fixed = if (loose) fixSemver(string) else string

            val minorStart = fixed.indexOf('.')
            check(minorStart != -1) { "Bad semver: $string. Minor version missed." }

            val patchStart = fixed.indexOf('.', minorStart + 1)
            check(patchStart != -1) { "Bad semver: $string. Patch version missed." }

            val preReleaseStart = fixed.indexOf('-', patchStart + 1)
            val buildStart = fixed.indexOf('+', if (preReleaseStart == -1) patchStart + 1 else preReleaseStart + 1)
            val preReleaseEnd = when {
                buildStart != -1 -> buildStart
                else -> fixed.length
            }
            val patchEnd = when {
                preReleaseStart != -1 -> preReleaseStart
                buildStart != -1 -> buildStart
                else -> fixed.length
            }

            val major = fixed.substring(0, minorStart)
            val minor = fixed.substring(minorStart + 1, patchStart)
            val patch = fixed.substring(patchStart + 1, patchEnd)
            val preRelease = if (preReleaseStart != -1) fixed.substring(preReleaseStart + 1, preReleaseEnd) else ""
            val build = if (buildStart != -1) fixed.substring(buildStart + 1) else ""

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

internal fun fixSemver(version: String): String {
    var i = 0
    var number = 0
    var major = "0"
    var minor = "0"
    var patch = "0"
    val rest = StringBuilder()
    val digits = StringBuilder()

    fun setComponent() {
        val digitsFiltered = digits.toString().trimStart { it == '0' }.takeIf { it.isNotEmpty() } ?: "0"
        when (number) {
            0 -> major = digitsFiltered
            1 -> minor = digitsFiltered
            2 -> patch = digitsFiltered
            else -> error(number)
        }
    }

    while (i < version.length) {
        val c = version[i++]
        if (c.isDigit()) digits.append(c)
        else if (c == '-' || c == '+') {
            // examples:
            // 1.2.3-RC1-1234,
            // 1.2-RC1-1234
            // 1.2-beta.11+sha.0x
            setComponent()
            number = 3
            rest.append(c)
            break
        } else if (c == '.') {
            rest.append(c)
            setComponent()
            digits.setLength(0)
            number++
            if (number > 2) break
        } else rest.append(c)
    }

    if (number <= 2) setComponent()

    rest.append(version.substring(i))

    val restFiltered = rest.filter {
        it in '0'..'9' ||
                it in 'A'..'Z' ||
                it in 'a'..'z' ||
                it == '.' ||
                it == '-' ||
                it == '+'
    }
    val restComponents = restFiltered.split('+', limit = 2)

    val preRelease = restComponents.getOrNull(0)
        ?.foldDelimiters()
        ?.trim { it == '-' || it == '.' }
        ?.takeIf { it.isNotEmpty() }

    val build = restComponents.getOrNull(1)
        ?.filter { it != '+' }
        ?.trim { it == '-' || it == '.' }
        ?.takeIf { it.isNotEmpty() }

    return "$major.$minor.$patch" +
            (if (preRelease != null) "-$preRelease" else "") +
            (if (build != null) "+$build" else "")
}

private fun String.foldDelimiters(): String {
    val result = StringBuilder(length)
    var endsWithDelimiter = false
    for (i in 0 until length) {
        val c = this[i]
        if (c == '+' || c == '-' || c == '.') {
            if (!endsWithDelimiter) {
                result.append(c)
                endsWithDelimiter = true
            }
        } else {
            endsWithDelimiter = false
            result.append(c)
        }
    }
    return result.toString()
}
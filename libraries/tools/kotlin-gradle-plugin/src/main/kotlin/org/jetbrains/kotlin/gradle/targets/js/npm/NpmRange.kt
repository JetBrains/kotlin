/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import com.github.gundy.semver4j.model.Version

data class NpmRange(
    val startVersion: SemVer? = null,
    val startInclusive: Boolean = false,
    val endVersion: SemVer? = null,
    val endInclusive: Boolean = false
)

val NONE_RANGE = NpmRange(
    startVersion = Version.fromString(NONE_VERSION)
        .toSemVer()
)

infix fun NpmRange.union(other: NpmRange): List<NpmRange> {
    if (!isIntersect(other)) return listOf(this, other)

    val startVersion = min(this.startVersion, other.startVersion)

    val endVersion = max(this.endVersion, other.endVersion)

    return NpmRange(
        startVersion = startVersion,
        startInclusive = if (startVersion == other.startVersion) other.startInclusive else this.startInclusive,
        endVersion = endVersion,
        endInclusive = if (startVersion == other.startVersion) other.startInclusive else this.startInclusive
    ).let {
        listOf(it)
    }
}

infix fun NpmRange.intersect(other: NpmRange): NpmRange? {
    if (!isIntersect(other)) return null

    val startVersion = max(this.startVersion, other.startVersion)

    val endVersion = min(this.endVersion, other.endVersion)

    return NpmRange(
        startVersion = startVersion,
        startInclusive = if (startVersion == this.startVersion) this.startInclusive else other.startInclusive,
        endVersion = endVersion,
        endInclusive = if (endVersion == this.endVersion) this.endInclusive else other.endInclusive
    )
}

infix fun NpmRange.isIntersect(other: NpmRange): Boolean {
    if (this.startVersion == null) {
        return other.startVersion == null || this.endVersion == null || other.startVersion < this.endVersion ||
                (other.startInclusive && this.endInclusive && other.startVersion == this.endVersion)
    }

    if (other.startVersion == null) {
        return other.endVersion == null || this.startVersion < other.endVersion ||
                (this.startInclusive && other.endInclusive && this.startVersion == other.endVersion)
    }

    if (this.endVersion == null) {
        return other.endVersion == null || other.endVersion > this.startVersion ||
                (this.startInclusive && other.endInclusive && other.endVersion == this.startVersion)
    }

    if (other.endVersion == null) {
        return this.endVersion > other.startVersion ||
                (other.startInclusive && this.endInclusive && this.endVersion == other.startVersion)
    }

    if (other.startVersion < this.startVersion) {
        return this.startVersion < other.endVersion ||
                (this.startInclusive && other.endInclusive && this.startVersion == other.endVersion)
    }

    if (this.startVersion < other.startVersion) {
        return other.startVersion < this.endVersion ||
                (other.startInclusive && this.endInclusive && other.startVersion == this.endVersion)
    }

    if (this.endVersion < other.endVersion) {
        return this.startVersion < other.endVersion ||
                (this.startInclusive && other.endInclusive && this.startVersion == other.endVersion)
    }

    if (this.startVersion < other.startVersion) {
        return other.startVersion < this.endVersion ||
                (other.startInclusive && this.endInclusive && other.startVersion == this.endVersion)
    }

    return this.startInclusive && other.startInclusive && this.startVersion == other.startVersion ||
            this.startInclusive && other.endInclusive && this.startVersion == other.endVersion ||
            this.endInclusive && other.startInclusive && this.endVersion == other.startVersion ||
            this.endInclusive && other.endInclusive && this.endVersion == other.endVersion
}
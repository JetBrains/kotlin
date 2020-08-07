/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.jetbrains.kotlin.gradle.targets.js.npm.NpmRangeVisitor.Companion.GT
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmRangeVisitor.Companion.GTEQ
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmRangeVisitor.Companion.LT
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmRangeVisitor.Companion.LTEQ
import org.jetbrains.kotlin.gradle.utils.toSetOrEmpty

/**
[startVersion] or [endVersion] equaling null means Infinite on appropriate edge,
In this case appropriate [startInclusive] or [endInclusive] do no matter
 */
data class NpmRange(
    val startVersion: SemVer? = null,
    val startInclusive: Boolean = false,
    val endVersion: SemVer? = null,
    val endInclusive: Boolean = false
) {
    override fun toString(): String =
        "${if (startVersion != null) "${if (startInclusive) GTEQ else GT}$startVersion" else ""} ${if (endVersion != null) "${if (endInclusive) LTEQ else LT}$endVersion" else ""}"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NpmRange

        if (startVersion != other.startVersion) return false
        if (endVersion != other.endVersion) return false
        if (startInclusive != other.startInclusive && startVersion != null && other.startVersion != null) return false
        if (endInclusive != other.endInclusive && endVersion != null && other.endVersion != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = startVersion?.hashCode() ?: 0
        result = 31 * result + (endVersion?.hashCode() ?: 0)
        result = 31 * result + (startInclusive && startVersion != null).hashCode()
        result = 31 * result + (endInclusive && endVersion != null).hashCode()
        return result
    }
}

infix fun NpmRange.union(other: NpmRange): Set<NpmRange> {
    if (!hasIntersection(other)) return setOf(this, other)

    val startVersion = minStart(this, other)

    val endVersion = maxEnd(this, other)

    return NpmRange(
        startVersion = startVersion,
        startInclusive = if (startVersion == other.startVersion) other.startInclusive else this.startInclusive,
        endVersion = endVersion,
        endInclusive = if (endVersion == other.endVersion) other.endInclusive else this.endInclusive
    ).toSetOrEmpty()
}

fun NpmRange.invert(): Set<NpmRange> {
    if (startVersion == null && endVersion == null) return emptySet()

    val result = mutableSetOf<NpmRange>()
    if (startVersion != null || endVersion == null) {
        result.add(
            NpmRange(
                endVersion = startVersion,
                endInclusive = !startInclusive
            )
        )
    }

    if (endVersion != null || startVersion == null) {
        result.add(
            NpmRange(
                startVersion = endVersion,
                startInclusive = !endInclusive
            )
        )
    }

    return result
}

infix fun Set<NpmRange>.intersect(others: Set<NpmRange>): Set<NpmRange> = flatMapTo(mutableSetOf()) { current ->
    others.mapNotNull { other ->
        current intersect other
    }
}

infix fun NpmRange.intersect(other: NpmRange): NpmRange? {
    if (!hasIntersection(other)) return null

    val startVersion = maxStart(this, other)

    val endVersion = minEnd(this, other)

    return NpmRange(
        startVersion = startVersion,
        startInclusive = if (startVersion == this.startVersion) this.startInclusive else other.startInclusive,
        endVersion = endVersion,
        endInclusive = if (endVersion == this.endVersion) this.endInclusive else other.endInclusive
    )
}

infix fun NpmRange.hasIntersection(other: NpmRange): Boolean {
    val maxStart = maxStart(this, other)
    val minEnd = minEnd(this, other)

    return maxStart == null || minEnd == null || maxStart < minEnd ||
            run {
                val startInclusive = if (maxStart == startVersion) this.startInclusive else other.startInclusive
                val endInclusive = if (minEnd == endVersion) this.endInclusive else other.endInclusive
                startInclusive && endInclusive && maxStart == minEnd
            }
}

fun maxStart(a: NpmRange, b: NpmRange): SemVer? =
    when {
        a.startVersion == null -> b.startVersion
        b.startVersion == null -> a.startVersion
        else -> max(a.startVersion, b.startVersion)
    }

fun minStart(a: NpmRange, b: NpmRange): SemVer? =
    when {
        a.startVersion == null || b.startVersion == null -> null
        else -> min(a.startVersion, b.startVersion)
    }

fun maxEnd(a: NpmRange, b: NpmRange): SemVer? =
    when {
        a.endVersion == null || b.endVersion == null -> null
        else -> max(a.endVersion, b.endVersion)
    }

fun minEnd(a: NpmRange, b: NpmRange): SemVer? =
    when {
        a.endVersion == null -> b.endVersion
        b.endVersion == null -> a.endVersion
        else -> min(a.endVersion, b.endVersion)
    }
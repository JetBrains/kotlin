/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import com.github.gundy.hidden.antlr.v4.runtime.ANTLRInputStream
import com.github.gundy.hidden.antlr.v4.runtime.CommonTokenStream
import com.github.gundy.semver4j.generated.grammar.NodeSemverExpressionLexer
import com.github.gundy.semver4j.generated.grammar.NodeSemverExpressionParser
import org.gradle.api.InvalidUserDataException

fun versionToNpmRanges(version: String): Set<NpmRange> {
    val lexer = NodeSemverExpressionLexer(ANTLRInputStream(version))
    val tokens = CommonTokenStream(lexer)
    val parser = NodeSemverExpressionParser(tokens)
    return NpmRangeVisitor()
        .visit(parser.rangeSet())!!
}

fun includedRange(
    includedVersion: String,
    includedWithCaret: Boolean = false
): NpmRange =
    includedRange(
        listOf(includedVersion),
        includedWithCaret
    )

fun includedRange(
    includedVersions: List<String>,
    includedWithCaret: Boolean = false
): NpmRange =
    try {
        includedVersions
            .flatMap { versionToNpmRanges(it) }
            .map { if (includedWithCaret) it.caretizeSingleVersion() else it }
            .reduce { acc: NpmRange, next: NpmRange ->
                val intersection = acc intersect next
                requireNotNull(intersection) {
                    "Included versions have no intersection $includedVersions"
                }
                intersection
            }
    } catch (e: UnsupportedOperationException) {
        throw InvalidUserDataException("No ranges for included versions $includedVersions")
    }

fun buildNpmVersion(
    includedVersions: List<String>,
    excludedVersions: List<String>,
    includedWithCaret: Boolean = false
): String {
    val includedRange: NpmRange = includedRange(includedVersions, includedWithCaret)

    if (excludedVersions.isEmpty()) return includedRange.toString()

    val excludedRanges: Set<NpmRange> = try {
        excludedVersions
            .flatMap { versionToNpmRanges(it) }
            .map { it.invert() }
            .reduce { acc, next -> acc intersect next }
    } catch (e: UnsupportedOperationException) {
        throw InvalidUserDataException("No ranges for excluded versions $excludedVersions")
    }

    return if (excludedRanges.isEmpty()) {
        includedRange.toString()
    } else
        excludedRanges
            .mapNotNull { it intersect includedRange }
            .sortedBy { it.startVersion }
            .joinToString(NpmRangeVisitor.OR)
}

private fun NpmRange.caretizeSingleVersion(): NpmRange {
    if (startVersion?.toVersion() == endVersion?.toVersion() && (startInclusive || endInclusive)) {
        return NpmRange(
            startVersion = startVersion,
            startInclusive = startInclusive,
            endVersion = startVersion
                ?.toVersion()
                ?.incrementMajor()
                ?.toSemVer()
        )
    }

    return this
}
/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import com.github.gundy.semver4j.generated.grammar.NodeSemverExpressionLexer
import com.github.gundy.semver4j.generated.grammar.NodeSemverExpressionParser
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.gradle.api.InvalidUserDataException

fun versionToNpmRanges(version: String): List<NpmRange?> {
    val lexer = NodeSemverExpressionLexer(ANTLRInputStream(version))
    val tokens = CommonTokenStream(lexer)
    val parser = NodeSemverExpressionParser(tokens)
    return NpmVersionInvertedVisitor()
        .visit(parser.rangeSet())!!
}

fun buildNpmVersion(
    includedVersions: List<String>,
    excludedVersions: List<String>
): String {
    val includedRange: NpmRange? = try {
        includedVersions
            .flatMap { versionToNpmRanges(it) }
            .onEach {
                if (it == null) {
                    throw InvalidUserDataException("Problem with parsing of included versions $includedVersions")
                }
            }
            .filterNotNull()
            .reduce { acc: NpmRange, next: NpmRange ->
                val intersection = acc intersect next
                requireNotNull(intersection) {
                    "Included versions have no intersection $includedVersions"
                }
                intersection!!
            }
    } catch (e: UnsupportedOperationException) {
        throw InvalidUserDataException("No ranges for included versions $includedVersions")
    }

    includedRange!!

    val excludedRanges: List<NpmRange> = try {
        excludedVersions
            .flatMap { versionToNpmRanges(it) }
            .mapNotNull { it?.invert() }
            .reduce { acc, next -> acc intersect next }
    } catch (e: UnsupportedOperationException) {
        throw InvalidUserDataException("No ranges for excluded versions $excludedVersions")
    }

    return if (excludedRanges.isEmpty()) {
        includedRange.toString()
    } else
        excludedRanges
            .mapNotNull { it intersect includedRange }
            .joinToString(NpmVersionInvertedVisitor.OR)
}
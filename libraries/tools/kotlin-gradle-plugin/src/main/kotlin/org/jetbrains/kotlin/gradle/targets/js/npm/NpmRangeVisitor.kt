/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import com.github.gundy.semver4j.generated.grammar.NodeSemverExpressionBaseVisitor
import com.github.gundy.semver4j.generated.grammar.NodeSemverExpressionParser
import com.github.gundy.semver4j.model.Version
import org.antlr.v4.runtime.tree.TerminalNode

class NpmRangeVisitor : NodeSemverExpressionBaseVisitor<List<NpmRange?>>() {
    override fun visitEmptyRange(ctx: NodeSemverExpressionParser.EmptyRangeContext): List<NpmRange?> =
        listOf(null)

    override fun visitWildcard(ctx: NodeSemverExpressionParser.WildcardContext): List<NpmRange?> =
        listOf(null)

    override fun visitWildcardRange(ctx: NodeSemverExpressionParser.WildcardRangeContext): List<NpmRange> =
        listOf(eq(ctx.partialWildcardSemver().text))

    override fun visitOperator(ctx: NodeSemverExpressionParser.OperatorContext): List<NpmRange> {
        val eq: TerminalNode? = ctx.unaryOperator().EQ()
        val gt: TerminalNode? = ctx.unaryOperator().GT()
        val lt: TerminalNode? = ctx.unaryOperator().LT()

        val version = ctx.partialWildcardSemver().text

        if (gt == null && lt == null) {
            return listOf(eq(version))
        }

        val eqToken = eq != null

        if (lt != null) {
            return listOf(
                if (eqToken) lteq(version) else lt(version)
            )
        }

        return listOf(
            if (eqToken) gteq(version) else gt(version)
        )
    }

    override fun visitWildcardOperator(ctx: NodeSemverExpressionParser.WildcardOperatorContext): List<NpmRange?> =
        listOf(null)

    override fun visitTildeRange(ctx: NodeSemverExpressionParser.TildeRangeContext): List<NpmRange> {
        val version = Version.fromString(ctx.fullSemver().text)
        val nextVersion = version
            .incrementMinor()

        return listOf(gteq(version))
    }

    override fun visitCaretRange(ctx: NodeSemverExpressionParser.CaretRangeContext): List<NpmRange> {
        val version = Version.fromString(ctx.fullSemver().text)
        val nextVersion = version
            .incrementMajor()

        return lt(version) union gteq(nextVersion)
    }

    override fun visitFullySpecifiedSemver(ctx: NodeSemverExpressionParser.FullySpecifiedSemverContext): List<NpmRange> =
        inverseVersion(ctx.fullSemver().text)

    override fun visitLogicalAndOfSimpleExpressions(ctx: NodeSemverExpressionParser.LogicalAndOfSimpleExpressionsContext): List<NpmRange?> =
        try {
            ctx.simple()
                .flatMap { visit(it) }
                .filterNotNull()
                .reduce { acc: NpmRange?, next: NpmRange ->
                    val npmRange = if (acc == null) null else acc intersect next
                    npmRange
                }
        } catch (e: UnsupportedOperationException) {
            null
        }.let { listOf(it) }

    override fun visitLogicalOrOfMultipleRanges(ctx: NodeSemverExpressionParser.LogicalOrOfMultipleRangesContext): List<NpmRange?> =
        ctx.basicRange()
            .flatMap { visitBasicRange(it) }
            .onEach { if (it == null) return listOf(null) }
            .filterNotNull()
            .fold(listOf<NpmRange>()) { ranges, range ->
                if (ranges.isEmpty()) listOf(range)
                else ranges
                    .flatMap { it union range }

            }

    private fun inverseVersion(version: String): List<NpmRange> =
        inverseVersion(Version.fromString(version))

    private fun inverseVersion(version: Version): List<NpmRange> =
        gt(version) union lt(version)

    private fun lt(version: String): NpmRange =
        lt(Version.fromString(version))

    private fun lt(version: Version): NpmRange =
        NpmRange(
            endVersion = version.toSemVer(),
            endInclusive = false
        )

    private fun lteq(version: String): NpmRange =
        lteq(Version.fromString(version))

    private fun lteq(version: Version): NpmRange =
        NpmRange(
            endVersion = version.toSemVer(),
            endInclusive = true
        )

    private fun gt(version: String): NpmRange =
        gt(Version.fromString(version))

    private fun gt(version: Version): NpmRange =
        NpmRange(
            startVersion = version.toSemVer()
        )

    private fun gteq(version: String): NpmRange =
        gteq(Version.fromString(version))

    private fun gteq(version: Version): NpmRange =
        NpmRange(
            startVersion = version.toSemVer(),
            startInclusive = true
        )

    private fun eq(version: String): NpmRange =
        eq(Version.fromString(version))

    private fun eq(version: Version): NpmRange =
        NpmRange(
            startVersion = version.toSemVer(),
            startInclusive = true,
            endVersion = version.toSemVer(),
            endInclusive = true
        )

    companion object {
        const val EQ = "="
        const val GT = ">"
        const val LT = "<"
        const val GTEQ = ">="
        const val LTEQ = "<="

        const val AND = " "
        const val OR = "||"
    }
}
/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import com.github.gundy.hidden.antlr.v4.runtime.tree.TerminalNode
import com.github.gundy.semver4j.generated.grammar.NodeSemverExpressionBaseVisitor
import com.github.gundy.semver4j.generated.grammar.NodeSemverExpressionParser
import com.github.gundy.semver4j.model.Version
import org.jetbrains.kotlin.gradle.utils.toSetOrEmpty

class NpmRangeVisitor : NodeSemverExpressionBaseVisitor<Set<NpmRange>>() {
    override fun visitEmptyRange(ctx: NodeSemverExpressionParser.EmptyRangeContext): Set<NpmRange> =
        setOf(NpmRange())

    override fun visitWildcard(ctx: NodeSemverExpressionParser.WildcardContext): Set<NpmRange> =
        setOf(NpmRange())

    override fun visitWildcardRange(ctx: NodeSemverExpressionParser.WildcardRangeContext): Set<NpmRange> {
        val partialWildcardSemver = ctx.partialWildcardSemver()
        val versionText = partialWildcardSemver.text
        val version = versionText
            .replace(".x", "")
            .replace(".X", "")
            .replace(".*", "")
            .let { Version.fromString(it) }

        val nextVersion = when {
            partialWildcardSemver.minor == null -> version.incrementMajor()
            partialWildcardSemver.patch == null -> version.incrementMinor()
            else -> return version(version)
        }

        return (gteq(version) intersect lt(nextVersion))
            .toSetOrEmpty()
    }

    override fun visitOperator(ctx: NodeSemverExpressionParser.OperatorContext): Set<NpmRange> {
        val eq: TerminalNode? = ctx.unaryOperator().EQ()
        val gt: TerminalNode? = ctx.unaryOperator().GT()
        val lt: TerminalNode? = ctx.unaryOperator().LT()

        val version = ctx.partialWildcardSemver().text

        if (gt == null && lt == null) {
            return setOf(eq(version))
        }

        val eqToken = eq != null

        if (lt != null) {
            return setOf(
                if (eqToken) lteq(version) else lt(version)
            )
        }

        return setOf(
            if (eqToken) gteq(version) else gt(version)
        )
    }

    override fun visitWildcardOperator(ctx: NodeSemverExpressionParser.WildcardOperatorContext): Set<NpmRange> =
        setOf(NpmRange())

    override fun visitTildeRange(ctx: NodeSemverExpressionParser.TildeRangeContext): Set<NpmRange> {
        val version = Version.fromString(ctx.fullSemver().text)
        val nextVersion = version
            .incrementMinor()

        return (gteq(version) intersect lt(nextVersion))
            .toSetOrEmpty()
    }

    override fun visitCaretRange(ctx: NodeSemverExpressionParser.CaretRangeContext): Set<NpmRange> {
        val version = Version.fromString(ctx.fullSemver().text)
        val nextVersion = version
            .incrementMajor()

        return (gteq(version) intersect lt(nextVersion))
            .toSetOrEmpty()
    }

    override fun visitFullySpecifiedSemver(ctx: NodeSemverExpressionParser.FullySpecifiedSemverContext): Set<NpmRange> =
        visitFullSemver(ctx.fullSemver())

    override fun visitFullSemver(ctx: NodeSemverExpressionParser.FullSemverContext): Set<NpmRange> =
        version(ctx.text)

    override fun visitHyphenatedRangeOfFullySpecifiedVersions(ctx: NodeSemverExpressionParser.HyphenatedRangeOfFullySpecifiedVersionsContext): Set<NpmRange> =
        (gteq(ctx.left.text) intersect lteq(ctx.right.text))
            .toSetOrEmpty()

    override fun visitLogicalAndOfSimpleExpressions(ctx: NodeSemverExpressionParser.LogicalAndOfSimpleExpressionsContext): Set<NpmRange> =
        try {
            ctx.simple()
                .flatMap { visit(it) }
                .reduce { acc: NpmRange?, next: NpmRange ->
                    val npmRange = if (acc == null) null else acc intersect next
                    npmRange
                }
        } catch (e: UnsupportedOperationException) {
            null
        }.toSetOrEmpty()

    override fun visitLogicalOrOfMultipleRanges(ctx: NodeSemverExpressionParser.LogicalOrOfMultipleRangesContext): Set<NpmRange> =
        ctx.basicRange()
            .flatMap { visitBasicRange(it) }
            .fold(setOf()) { ranges, range ->
                if (ranges.isEmpty()) setOf(range)
                else ranges
                    .flatMapTo(mutableSetOf()) { it union range }
            }

    private fun version(version: String): Set<NpmRange> =
        version(Version.fromString(version))

    private fun version(version: Version): Set<NpmRange> =
        (gteq(version) intersect lteq(version)).toSetOrEmpty()

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
        const val OR = " || "
        const val WILDCARD = "*"
    }
}
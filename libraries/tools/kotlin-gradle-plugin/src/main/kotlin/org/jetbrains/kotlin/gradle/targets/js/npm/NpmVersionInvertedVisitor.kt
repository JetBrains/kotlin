/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import com.github.gundy.semver4j.generated.grammar.NodeSemverExpressionBaseVisitor
import com.github.gundy.semver4j.generated.grammar.NodeSemverExpressionParser
import com.github.gundy.semver4j.model.Version
import org.antlr.v4.runtime.tree.TerminalNode

class NpmVersionInvertedVisitor : NodeSemverExpressionBaseVisitor<NpmRange>() {
    override fun visitEmptyRange(ctx: NodeSemverExpressionParser.EmptyRangeContext): NpmRange =
        NONE_RANGE

    override fun visitWildcard(ctx: NodeSemverExpressionParser.WildcardContext): NpmRange =
        NONE_RANGE

    override fun visitWildcardRange(ctx: NodeSemverExpressionParser.WildcardRangeContext): NpmRange =
        inverseVersion(ctx.partialWildcardSemver().text)

    override fun visitOperator(ctx: NodeSemverExpressionParser.OperatorContext): NpmRange {
        val eq: TerminalNode? = ctx.unaryOperator().EQ()
        val gt: TerminalNode? = ctx.unaryOperator().GT()
        val lt: TerminalNode? = ctx.unaryOperator().LT()

        val version = ctx.partialWildcardSemver().text

        if (gt == null && lt == null) {
            return inverseVersion(version)
        }

        val invertedEqToken = if (eq != null) EQ else ""

        if (lt != null) {
            return "$GT$invertedEqToken$version"
        }

        return "$LT$invertedEqToken$version"
    }

    override fun visitWildcardOperator(ctx: NodeSemverExpressionParser.WildcardOperatorContext): NpmRange =
        NONE_RANGE

    override fun visitTildeRange(ctx: NodeSemverExpressionParser.TildeRangeContext): NpmRange {
        val version = Version.fromString(ctx.fullSemver().text)
        val nextVersion = version
            .incrementMinor()

        return "$LT$version $OR $GTEQ$nextVersion"
    }

    override fun visitCaretRange(ctx: NodeSemverExpressionParser.CaretRangeContext): NpmRange {
        val version = Version.fromString(ctx.fullSemver().text)
        val nextVersion = version
            .incrementMajor()

        return "$LT$version $OR $GTEQ$nextVersion"
    }

    override fun visitFullySpecifiedSemver(ctx: NodeSemverExpressionParser.FullySpecifiedSemverContext): NpmRange =
        inverseVersion(ctx.fullSemver().text)

    override fun visitLogicalAndOfSimpleExpressions(ctx: NodeSemverExpressionParser.LogicalAndOfSimpleExpressionsContext): NpmRange =
        ctx.simple()
            .joinToString(OR) { visit(it) }

    override fun visitLogicalOrOfMultipleRanges(ctx: NodeSemverExpressionParser.LogicalOrOfMultipleRangesContext): NpmRange =
        ctx.basicRange()
            .joinToString(AND) { visitBasicRange(it) }

    private fun inverseVersion(version: String): String =
        "$GT$version $OR $LT$version"


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
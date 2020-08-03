/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import com.github.gundy.semver4j.generated.grammar.NodeSemverExpressionLexer
import com.github.gundy.semver4j.generated.grammar.NodeSemverExpressionParser
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmVersionInvertedVisitor.Companion.AND

fun invertNpmVersion(version: String): String {
    val lexer = NodeSemverExpressionLexer(ANTLRInputStream(version))
    val tokens = CommonTokenStream(lexer)
    val parser = NodeSemverExpressionParser(tokens)
    return NpmVersionInvertedVisitor()
        .visit(parser.rangeSet())!!
}

fun buildNpmVersion(
    includedVersions: List<String>,
    excludedVersions: List<String>
): String =
    includedVersions
        .joinToString(AND) { it.trimIndent() } +
            excludedVersions
                .joinToString(prefix = AND, separator = AND) { invertNpmVersion(it.trimIndent()) }
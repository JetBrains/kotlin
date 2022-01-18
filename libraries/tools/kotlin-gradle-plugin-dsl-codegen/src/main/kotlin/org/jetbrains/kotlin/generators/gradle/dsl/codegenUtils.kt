/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.gradle.dsl

internal data class TypeName(val fqName: String, val typeArguments: List<TypeName>)

internal fun typeName(fqName: String, vararg typeArgumentFlatFqNames: String): TypeName {
    require(typeArgumentFlatFqNames.none { "<" in it }) { "generics won't render to short type names properly, use the full constructor" }
    return TypeName(
        fqName,
        typeArgumentFlatFqNames.map {
            TypeName(
                it,
                emptyList()
            )
        })
}

internal fun TypeName.shortName() = fqName.split(".").last()

internal fun TypeName.packageName() = fqName.substringBeforeLast(".")

internal fun TypeName.renderShort(): String =
    shortName() + typeArguments.takeIf { it.isNotEmpty() }?.joinToString(", ", "<", ">") { it.renderShort() }.orEmpty()

internal fun TypeName.renderErased(): String =
    shortName() + typeArguments.takeIf { it.isNotEmpty() }?.joinToString(", ", "<", ">") { "*" }.orEmpty()

internal fun TypeName.collectFqNames(): Set<String> =
    setOf(fqName) + typeArguments.flatMap { it.collectFqNames() }.toSet()

/**
 * @param skipFirstLine if true doesn't indent first line
 */
internal fun String.indented(nSpaces: Int = 4, skipFirstLine: Boolean = false): String {
    val spaces = String(CharArray(nSpaces) { ' ' })

    return lines()
        .withIndex()
        .joinToString(separator = "\n") { (index, line) ->
            if (skipFirstLine && index == 0) return@joinToString line
            if (line.isNotBlank()) "$spaces$line" else line
        }
}

internal val outputSourceRoot get() = System.getProperties()["org.jetbrains.kotlin.generators.gradle.dsl.outputSourceRoot"] as String
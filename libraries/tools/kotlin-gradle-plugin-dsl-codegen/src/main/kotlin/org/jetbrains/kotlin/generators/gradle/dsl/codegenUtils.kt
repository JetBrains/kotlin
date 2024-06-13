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

/**
 * Replaces old content of code region with [name] to [content]
 * Code region is defined as following:
 *
 * ``` kotlin
 * // region [name]
 *   /* ANY CONTENT HERE */
 * // endregion
 * ```
 *
 * NB: This function doesn't expect nested regions!
 * i.e. this is not allowed
 *
 * ```kotlin
 * // region foo
 * // region bar
 *   /* nested regions are not allowed! */
 * // endregion
 * // endregion
 * ```
 */
internal fun String.replaceRegion(regionName: String, content: String): String {
    val lines = lineSequence().iterator()
    return buildString {
        // Insert content before region
        var startOfRegionFound = false
        for (line in lines) {
            appendLine(line)
            if (line.trim() == "// region $regionName") {
                startOfRegionFound = true
                break
            }
        }
        check(startOfRegionFound) { "Region with name $regionName not found" }

        // Skip region content
        var originalEndRegionLine: String? = null
        for (line in lines) {
            if (line.trim() == "// endregion") {
                originalEndRegionLine = line
                break
            }
        }
        checkNotNull(originalEndRegionLine) { "End of region with name $regionName not found" }

        // Insert replacing content
        appendLine(content)
        appendLine(originalEndRegionLine)

        // Insert content after region
        while (lines.hasNext()) {
            val line = lines.next()
            if (lines.hasNext()) {
                appendLine(line)
            } else { // for the list line we don't want to add extra "\n"
                append(line)
            }
        }
    }
}

internal val kotlinGradlePluginSourceRoot get() = System
        .getProperties()["org.jetbrains.kotlin.generators.gradle.dsl.kotlinGradlePluginSourceRoot"] as String

internal val kotlinGradlePluginApiSourceRoot get() = System
    .getProperties()["org.jetbrains.kotlin.generators.gradle.dsl.kotlinGradlePluginApiSourceRoot"] as String
/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCEntryPoint.Kind.*
import org.jetbrains.kotlin.konan.file.File

/**
 * An entry point which matches declarations of a given kind and fully-qualified name pattern.
 *
 * @property kind a kind of entry point.
 * @property pattern a pattern which matches fully-qualified declaration name.
 */
data class ObjCEntryPoint(val kind: Kind, val pattern: Pattern) {
    /** Entry point kind. */
    enum class Kind {
        /** A function. */
        FUNCTION,

        /** A property. */
        PROPERTY,

        /** A callable: function or property. */
        CALLABLE,
    }

    /**
     * A pattern which matches fully-qualified name.
     *
     * @property path Fully-qualified name components preceding the final name component.
     * @property name The last component in a fully-qualified name, which may either be an explicit name or a wildcard.
     */
    data class Pattern(val path: List<String>, val name: Name) {
        /** The last component of a pattern. */
        sealed class Name {
            /** Matches explicit name. */
            data class Explicit(val string: String) : Name()

            /** Matches any name. */
            data object Wildcard : Name()
        }
    }
}

/** Parent kind in a hierarchy of kinds, or null for root. */
val ObjCEntryPoint.Kind.parentOrNull: ObjCEntryPoint.Kind?
    get() =
        when (this) {
            FUNCTION -> CALLABLE
            PROPERTY -> CALLABLE
            CALLABLE -> null
        }

/** Reads a list of entry points from this file. */
fun File.readObjCEntryPointList(): List<ObjCEntryPoint> =
    readStrings()
        .asSequence()
        .map { it.trim() }  // Strip leading / trailing whitespaces
        .filter { !it.startsWith("//") }  // Strip comment lines
        .filter { it.isNotBlank() }  // Remove empty lines
        .map { it.toObjCEntryPoint() }
        .toList()

/** Convert this string to an entry point kind. */
private fun String.toObjCEntryPointKind(): ObjCEntryPoint.Kind =
    when (this) {
        "function" -> FUNCTION
        "property" -> PROPERTY
        "callable" -> CALLABLE
        else -> throw IllegalArgumentException("invalid kind: $this, should be one of: function, property or callable")
    }

/** Convert this string to an entry point pattern. */
private fun String.toObjCEntryPointPattern(): ObjCEntryPoint.Pattern =
    split('.').let { components ->
        ObjCEntryPoint.Pattern(
            components.dropLast(1),
            components.lastOrNull()
                .let { it ?: throw IllegalArgumentException("invalid pattern: \"$this\", should be non-empty") }
                .toObjCEntryPointPatternName())
    }

/** Convert this string to an entry point pattern name. */
private fun String.toObjCEntryPointPatternName(): ObjCEntryPoint.Pattern.Name =
    when (this) {
        "*" -> ObjCEntryPoint.Pattern.Name.Wildcard
        else -> ObjCEntryPoint.Pattern.Name.Explicit(this)
    }

/** Convert this string to an entry point. */
private fun String.toObjCEntryPoint(): ObjCEntryPoint =
    split(' ')
        .also { if (it.size != 2) throw IllegalArgumentException("invalid entry point: \"$this\", should match: \"<kind> <pattern>\"") }
        .let { ObjCEntryPoint(it[0].toObjCEntryPointKind(), it[1].toObjCEntryPointPattern()) }

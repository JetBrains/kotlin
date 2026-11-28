/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation.analyzer.ids

import org.jetbrains.kotlin.documentation.KdElementId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import java.net.URLDecoder

/**
 * Utilities for working with declaration IDs ([generateDeclarationId]): synthetic IDs for documentables that
 * are not declarations (parameters, accessors, modules), and recovery of the package and class names encoded
 * in an ID without re-resolving the symbol.
 */

/**
 * The ID of a [DModule][org.jetbrains.dokka.model.DModule]. A module is not a declaration, so it gets a synthetic
 * ID. The leading `#` cannot occur in any generated declaration ID (package and classifier IDs never contain `#`,
 * and callable IDs always start with their owner), so the module can never collide with a real declaration.
 */
internal val MODULE_DECLARATION_ID = KdElementId("#module")

/**
 * The ID of the [index]th value parameter of the callable with the given [ownerDeclarationId].
 */
internal fun valueParameterDeclarationId(ownerDeclarationId: KdElementId, index: Int): KdElementId =
    KdElementId("${ownerDeclarationId.value}#param[$index]")

/** The ID of the [index]th context parameter of the callable with the given [ownerDeclarationId]. */
internal fun contextParameterDeclarationId(ownerDeclarationId: KdElementId, index: Int): KdElementId =
    KdElementId("${ownerDeclarationId.value}#context-param[$index]")

/** The ID of the extension receiver of the callable with the given [ownerDeclarationId]. */
internal fun receiverDeclarationId(ownerDeclarationId: KdElementId): KdElementId =
    KdElementId("${ownerDeclarationId.value}#receiver")

/** The ID of the [index]th type parameter of the declaration with the given [ownerDeclarationId]. */
internal fun typeParameterDeclarationId(ownerDeclarationId: KdElementId, index: Int): KdElementId =
    KdElementId("${ownerDeclarationId.value}#type-param[$index]")

/**
 * The owner part of a callable ID: everything before the first `#`, i.e. the containing class ID
 * (`package/Class.Nested`) or the package ID for top-level callables. For classifier IDs, which contain no `#`,
 * this is the ID itself.
 */
internal fun ownerDeclarationIdOf(declarationId: KdElementId): KdElementId =
    KdElementId(declarationId.value.substringBefore('#'))

/**
 * The percent-encoded name part of a callable ID (second `#`-separated segment; empty for constructors).
 * Returns `null` if the ID is not a callable ID.
 */
internal fun callableNameOf(declarationId: KdElementId): String? =
    declarationId.value.split('#').getOrNull(1)?.takeIf { declarationId.value.count { it == '#' } >= 2 }

/**
 * Replaces the owner of a callable ID with [newOwner], preserving the name and signature hash, e.g. re-addressing
 * an inherited member under the class that inherits it. Non-callable IDs are returned unchanged.
 */
internal fun reownDeclarationId(declarationId: KdElementId, newOwner: KdElementId): KdElementId {
    val parts = declarationId.value.split('#')
    require(parts.size >= 3) { "Not a callable declaration ID: $declarationId" }
    return KdElementId((listOf(newOwner.value) + parts.drop(1)).joinToString("#"))
}

/** Reverses [escapeName] for a single name. */
internal fun unescapeName(name: String): String = URLDecoder.decode(name, Charsets.UTF_8.name())

/**
 * The package Fq name encoded in a declaration ID (package, classifier, or callable): the percent-encoded
 * segments before the first `/` (for classifiers) or `#`/end (for packages and callables). The root package
 * and the empty package of a callable yield `""`.
 */
internal fun declarationIdToPackageName(declarationId: KdElementId): String {
    val packageId = declarationId.value.substringBefore('/').substringBefore('#')
        .let { if (it.isEmpty()) return "" else it }
    return packageId.split('.').joinToString(".") { unescapeName(it) }
}

/**
 * The nested class names (dot-separated, unescaped) encoded
 * in a classifier ID or in the owner part of a callable ID, or `null` if the ID denotes a package or a
 * top-level callable (no class).
 */
internal fun declarationIdToClassNames(declarationId: KdElementId): String? {
    val relative = declarationId.value.substringBefore('#').substringAfter('/', "")
    if (relative.isEmpty()) return null
    return relative.split('.').joinToString(".") { unescapeName(it) }
}

/**
 * Whether [declarationId] denotes a classifier: a `package/Class.Nested` ID, as opposed to a package,
 * callable, or synthetic ID. See [classIdFromDeclarationId] for decoding the [ClassId] of a classifier ID.
 */
internal fun isClassifierDeclarationId(declarationId: KdElementId): Boolean =
    declarationId.value.contains('/') && !declarationId.value.contains('#')

/**
 * Reconstructs the [ClassId] encoded in a classifier declaration ID; the inverse of the ID generation for
 * classifiers. Only valid for classifier IDs (see [generateDeclarationId]).
 */
internal fun classIdFromDeclarationId(declarationId: KdElementId): ClassId {
    val relativeId = declarationId.value.substringAfter('/', "")
    require(relativeId.isNotEmpty()) { "Not a classifier declaration ID: $declarationId" }
    val packageName = declarationId.value.substringBefore('/').split('.').joinToString(".") { unescapeName(it) }
    val classNames = relativeId.split('.').joinToString(".") { unescapeName(it) }
    return ClassId(FqName(packageName), FqName(classNames), false)
}

// TODO KT Unresolved declaration ID
/** A placeholder ID for a reference that could not be resolved to any declaration. */
internal val UNRESOLVED_DECLARATION_ID = KdElementId("#unresolved")

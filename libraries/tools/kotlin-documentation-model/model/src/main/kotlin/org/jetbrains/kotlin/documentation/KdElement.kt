/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.Serializable

/**
 * A unique, opaque text identifier used to reference a [KdElement] from other elements (types,
 * annotations, [KdExpectActualMapping], etc.) without embedding the element itself.
 *
 * An ID is unique within a single [KdFragment], but the same declaration may have a different ID
 * in a different fragment, see [KdExpectActualMapping].
 */
@Serializable
@JvmInline
value class KdElementId(val value: String)

/**
 * A base type for all elements stored in a [KdBundle]: declarations (classes, properties,
 * functions, etc.), packages, and modules.
 *
 * Polymorphic (de)serialization of [KdElement] uses a `kind` discriminator field, see [KdJson].
 */
@Serializable
sealed class KdElement

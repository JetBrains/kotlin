/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.Serializable

/**
 * Maps the `expect` and `actual` declarations of the same declaration across fragments.
 *
 * An element's [KdElementId] is unique within a fragment, but may differ across fragments for
 * the same declaration (e.g. when an `actual typealias` is used), so IDs alone aren't enough to
 * match expect and actual declarations across fragments.
 */
@Serializable
data class KdExpectActualMapping(
    val expectIds: Map<KdFragmentName, KdElementId>,
    val actualIds: Map<KdFragmentName, KdElementId>,
)

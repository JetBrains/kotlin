/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The name of a [KdFragment], corresponding to a Kotlin Multiplatform source set name, e.g.
 * `commonMain` or `jvmMain`.
 */
typealias KdFragmentName = String

/**
 * A per-source-set view of all declarations accessible in that source set. For example, all
 * declarations from `commonMain` are also present in the `jvmMain` fragment.
 *
 * [KdElementId] is unique within a single fragment, but the same declaration may have a
 * different ID in a different fragment, see [KdExpectActualMapping].
 */
@Serializable
data class KdFragment(
    val name: KdFragmentName,
    val elements: Map<KdElementId, KdElement>,
    /**
     * Elements that are not directly available in this fragment but come from dependencies
     * (e.g. Java libraries, the JDK, or Kotlin libraries without a published KDM artifact).
     * Used only to retrieve additional information (e.g. to render supertypes, overrides, or
     * KDoc links), not to be rendered on their own.
     */
    val embeddedElements: Map<KdElementId, KdElement>,
)

/**
 * A de-duplicated [KdElement]: a declaration identical to the one with the same ID present in
 * [fragmentName], so the full declaration doesn't need to be repeated for every fragment it's
 * accessible from.
 */
@Serializable
@SerialName("refinedElement")
data class KdRefinedElement(
    val fragmentName: KdFragmentName,
) : KdElement()

/**
 * A de-duplicated [KdElement] representing an inherited member that was not explicitly
 * overridden, pointing to the original declaration it was inherited from.
 */
@Serializable
@SerialName("inheritedMember")
data class KdInheritedMember(
    val originalId: KdElementId,
) : KdElement()

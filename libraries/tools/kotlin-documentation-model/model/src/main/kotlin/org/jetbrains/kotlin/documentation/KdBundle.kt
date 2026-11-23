/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.Serializable

const val KDM_SCHEMA_VERSION: String = "2.5.255"

/**
 * The root type of a KDM artifact, representing a single "source-set-tree" (e.g. `main` or
 * `test`). Source-sets from different trees should not be included in the same bundle.
 */
@Serializable
data class KdBundle(
    /** The version of the KDM schema this bundle conforms to (see [KDM_SCHEMA_VERSION]). */
    val schemaVersion: String,
    val fragments: List<KdFragment>,
    val expectActualMappings: List<KdExpectActualMapping>,
)

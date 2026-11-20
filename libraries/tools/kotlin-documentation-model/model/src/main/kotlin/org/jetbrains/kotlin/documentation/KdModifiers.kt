/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.Serializable

/** The visibility of a declaration. */
@Serializable
enum class KdVisibility {
    PUBLIC, PROTECTED, INTERNAL, PRIVATE,

    // Java-specific visibilities.
    PACKAGE_PROTECTED, PACKAGE_PRIVATE,
}

/** The modality of a declaration. */
@Serializable
enum class KdModality {
    FINAL, SEALED, OPEN, ABSTRACT,
}

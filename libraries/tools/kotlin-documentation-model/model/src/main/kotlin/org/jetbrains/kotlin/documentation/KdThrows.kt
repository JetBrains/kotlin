/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.Serializable

/**
 * A checked exception a callable declaration may throw, as declared via a Java `throws` clause,
 * a Kotlin `@Throws` annotation, or a `@throws`/`@exception` documentation tag.
 */
@Serializable
data class KdThrows(
    val classLikeId: KdElementId,
    val documentation: KdDocumentation = KdDocumentation.EMPTY,
)

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A module. Not directly referenceable from the compiler, but present in Javadoc links and
 * documented separately, e.g. via `module-info.java` or a Dokka module documentation file.
 */
@Serializable
@SerialName("module")
data class KdModule(
    val name: String,

    val packages: List<KdElementId>,

    /** Java-only, for now. */
    val annotations: List<KdAnnotation>,
    val documentation: KdDocumentation = KdDocumentation.EMPTY,
) : KdElement()

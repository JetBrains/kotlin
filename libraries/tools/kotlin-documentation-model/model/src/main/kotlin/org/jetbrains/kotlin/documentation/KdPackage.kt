/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A package. Not directly referenceable from the compiler, but present in KDoc/Javadoc links and
 * documented separately, e.g. via `package-info.java` or a Dokka module/package documentation file.
 */
@Serializable
@SerialName("package")
data class KdPackage(
    val name: String,

    val declarations: List<KdElementId>,

    /** Java-only, for now. */
    val annotations: List<KdAnnotation>,
    val documentation: KdDocumentation = KdDocumentation.EMPTY,
) : KdElement()

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.Serializable

/**
 * The parsed documentation (KDoc or Javadoc) of a declaration or parameter.
 *
 * TODO This is a placeholder: the detailed KDoc/Javadoc content model is being redesigned and isn't
 * needed to unblock an analyzer implementation yet.
 */
@Serializable
class KdDocumentation {
    companion object {
        /** The documentation of a declaration without any KDoc/Javadoc comment. */
        val EMPTY: KdDocumentation = KdDocumentation()
    }
}

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.artifacts.component.ComponentIdentifier

internal fun interface ParentSourceSetVisibilityProvider {
    fun getSourceSetsVisibleInParents(identifier: ComponentIdentifier): Set<String>

    object Empty : ParentSourceSetVisibilityProvider {
        override fun getSourceSetsVisibleInParents(identifier: ComponentIdentifier): Set<String> = emptySet()
    }
}

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

internal fun interface ParentSourceSetVisibilityProvider {
    fun getSourceSetsVisibleInParents(identifier: KmpModuleIdentifier): Set<String>

    object Empty : ParentSourceSetVisibilityProvider {
        override fun getSourceSetsVisibleInParents(identifier: KmpModuleIdentifier): Set<String> = emptySet()
    }
}

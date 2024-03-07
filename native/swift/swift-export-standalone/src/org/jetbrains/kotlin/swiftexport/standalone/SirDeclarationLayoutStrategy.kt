/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone

internal sealed interface SirDeclarationLayoutStrategy {
    fun collapsePackagesIntoTopLevelNames(): Boolean

    fun generateEnumsForNamespaces(): Boolean

    fun storeDeclarationsInEnumExtensions(): Boolean

    object Flat : SirDeclarationLayoutStrategy {
        override fun collapsePackagesIntoTopLevelNames(): Boolean =
            true

        override fun generateEnumsForNamespaces(): Boolean =
            false

        override fun storeDeclarationsInEnumExtensions(): Boolean =
            false
    }

    object Enums : SirDeclarationLayoutStrategy {
        override fun collapsePackagesIntoTopLevelNames(): Boolean =
            false

        override fun generateEnumsForNamespaces(): Boolean =
            true

        override fun storeDeclarationsInEnumExtensions(): Boolean =
            false
    }
}
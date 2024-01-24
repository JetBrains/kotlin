/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base

// revisit in scope of https://github.com/Kotlin/dokka/issues/2776
internal object DokkaBaseInternalConfiguration {
    const val SHOULD_DISPLAY_ALL_TYPES_PAGE_SYS_PROP = "dokka.shouldDisplayAllTypesPage"
    const val SHOULD_DISPLAY_SINCE_KOTLIN_SYS_PROP = "dokka.shouldDisplaySinceKotlin"

    var allTypesPageEnabled: Boolean = false
        private set
    var sinceKotlinRenderingEnabled: Boolean = false
        private set

    init {
        reinitialize()
    }

    // should be private, internal is only for usage in tests
    internal fun reinitialize() {
        allTypesPageEnabled = getBooleanProperty(SHOULD_DISPLAY_ALL_TYPES_PAGE_SYS_PROP)
        sinceKotlinRenderingEnabled = getBooleanProperty(SHOULD_DISPLAY_SINCE_KOTLIN_SYS_PROP)
    }

    private fun getBooleanProperty(propertyName: String): Boolean {
        return System.getProperty(propertyName) in setOf("1", "true")
    }
}

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.attributes

import org.gradle.api.Named
import org.gradle.api.attributes.Attribute
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

/**
 * Represents a configuration for specifying the desired variant of Klib dependency.
 *
 * Use [NON_PACKED] when the artifact will undergo introspection, as this avoids unnecessary packaging and unpackaging steps.
 *
 * If a variant with attribute value [NON_PACKED] is not available, a variant with attribute value [PACKED] will be selected.
 *
 * @since 2.1.0
 */
@ExperimentalKotlinGradlePluginApi
interface KlibPackaging : Named {
    /**
     * @suppress
     */
    companion object {
        /**
         * Represents the attribute key for specifying the Klib packaging type.
         */
        @JvmField
        val ATTRIBUTE = Attribute.of("org.jetbrains.kotlin.klib.packaging", KlibPackaging::class.java)

        /**
         * Represents the packed variant of Klib
         */
        @JvmField
        val PACKED = "packed"

        /**
         * Represents the non-packed variant of Klib
         */
        @JvmField
        val NON_PACKED = "non-packed"
    }
}
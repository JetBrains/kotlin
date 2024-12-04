/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.attributes

import org.gradle.api.Named
import org.gradle.api.attributes.Attribute
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

/**
 * Represents a configuration that specifies the desired klib packaging type.
 *
 * Use [NON_PACKED] when the artifact will undergo introspection to avoid unnecessary packaging and unpackaging steps.
 *
 * If a variant with the attribute value [NON_PACKED] isn't available, a variant with the attribute value [PACKED] is selected.
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
         * Represents the attribute key that specifies the klib packaging type.
         */
        @JvmField
        val ATTRIBUTE = Attribute.of("org.jetbrains.kotlin.klib.packaging", KlibPackaging::class.java)

        /**
         * Represents the packed variant of klibs.
         */
        @JvmField
        val PACKED = "packed"

        /**
         * Represents the non-packed variant of klibs.
         */
        @JvmField
        val NON_PACKED = "non-packed"
    }
}
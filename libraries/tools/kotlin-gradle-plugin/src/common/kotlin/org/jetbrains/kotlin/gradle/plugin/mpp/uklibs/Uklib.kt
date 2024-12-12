/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs


internal data class Uklib(
    val module: UklibModule,
    val manifestVersion: String,
) {
    companion object {
        // Use this symbol in diagnostics
        const val UKLIB_NAME = "uklib"

        // This extension has to be stable because we need it for pom resolution
        const val UKLIB_EXTENSION = "uklib"

        /**
         * The packaging must be equal to the extension because when resolving POM only components, Gradle prefers the artifact with the
         * <packaging> POM value over the .jar (but falls back to jar if the artifact doesn't exist)
         */
        const val UKLIB_PACKAGING = UKLIB_EXTENSION

        const val UMANIFEST_FILE_NAME = "umanifest"

        const val FRAGMENTS = "fragments"
        const val FRAGMENT_IDENTIFIER = "identifier"
        const val ATTRIBUTES = "targets"
        const val UMANIFEST_VERSION = "manifestVersion"

        const val CURRENT_UMANIFEST_VERSION = "0.0.1"
        const val MAXIMUM_COMPATIBLE_UMANIFEST_VERSION = CURRENT_UMANIFEST_VERSION
    }

}
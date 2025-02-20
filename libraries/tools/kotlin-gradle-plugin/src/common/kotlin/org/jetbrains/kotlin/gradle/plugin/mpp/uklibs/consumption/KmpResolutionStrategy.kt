/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption

internal enum class KmpResolutionStrategy {
    ResolveUklibsAndResolvePSMLeniently,
    /**
     * FIXME: Introduce two modes of Uklib resolution
     * - Prefer Uklib variants
     * - Prefer PSM variants
     */
    StandardKMPResolution;

    val propertyName: String
        get() = when (this) {
            ResolveUklibsAndResolvePSMLeniently -> "resolveUklibsAndResolvePSMLeniently"
            StandardKMPResolution -> "standardKMPResolution"
        }

    companion object {
        fun fromProperty(name: String): KmpResolutionStrategy? = when (name) {
            ResolveUklibsAndResolvePSMLeniently.propertyName -> ResolveUklibsAndResolvePSMLeniently
            StandardKMPResolution.propertyName -> StandardKMPResolution
            else -> null
        }
    }
}
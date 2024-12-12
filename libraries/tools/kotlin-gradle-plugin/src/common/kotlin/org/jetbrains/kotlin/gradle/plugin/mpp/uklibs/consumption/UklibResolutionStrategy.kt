/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption

internal enum class UklibResolutionStrategy {
    ResolveUklibsInMavenComponents,
    IgnoreUklibs;

    val propertyName: String
        get() = when (this) {
            ResolveUklibsInMavenComponents -> "resolveUklibsInMavenComponents"
            IgnoreUklibs -> "ignoreUklibs"
        }

    companion object {
        fun fromProperty(name: String): UklibResolutionStrategy? = when (name) {
            ResolveUklibsInMavenComponents.propertyName -> ResolveUklibsInMavenComponents
            IgnoreUklibs.propertyName -> IgnoreUklibs
            else -> null
        }
    }
}
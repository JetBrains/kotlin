/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication

internal enum class KmpPublicationStrategy {
    UklibPublicationInASingleComponentWithKMPPublication,
    StandardKMPPublication;

    val propertyName: String
        get() = when (this) {
            UklibPublicationInASingleComponentWithKMPPublication -> "uklibPublicationInASingleComponentWithKMPPublication"
            StandardKMPPublication -> "standardKMPPublication"
        }

    companion object {
        fun fromProperty(name: String): KmpPublicationStrategy? = when (name) {
            UklibPublicationInASingleComponentWithKMPPublication.propertyName -> UklibPublicationInASingleComponentWithKMPPublication
            StandardKMPPublication.propertyName -> StandardKMPPublication
            else -> null
        }
    }
}
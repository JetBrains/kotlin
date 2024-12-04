/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.utils.*

internal interface KotlinAndroidSourceSetNaming {

    /**
     * Returns the name of the corresponding [KotlinSourceSet]
     * This function can be called w/ or w/o a specific [type].
     */
    fun kotlinSourceSetName(
        disambiguationClassifier: String, androidSourceSetName: String, type: AndroidVariantType?
    ): String?


    /**
     * Returns the name of the default KotlinSourceSet for a given Android compilation.
     * Returns `null`, if this naming schema does not know about it. In this case, the
     * 'default' defaultSourceSetName will be constructed by the compilation.
     */
    fun defaultKotlinSourceSetName(
        target: KotlinAndroidTarget,
        @Suppress("TYPEALIAS_EXPANSION_DEPRECATION") variant: DeprecatedAndroidBaseVariant
    ): String? = null
}

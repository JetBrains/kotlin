/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.AndroidCompilationDetails

internal interface KotlinAndroidSourceSetNaming {

    /**
     * Returns non-null name of the corresponding [KotlinSourceSet], if the name of said source set can be
     * determined by the [disambiguationClassifier] and [androidSourceSetName] alone.
     *
     * Returns `null`, if KotlinSourceSet name requires additional [AndroidVariantType] to be determined.
     */
    fun kotlinSourceSetName(
        disambiguationClassifier: String,
        androidSourceSetName: String
    ): String?

    /**
     * Returns the name of the corresponding [KotlinSourceSet], if the name of said source set cannot be determined
     * by the [disambiguationClassifier] and [androidSourceSetName] alone.
     */
    fun kotlinSourceSetName(
        disambiguationClassifier: String,
        androidSourceSetName: String,
        type: AndroidVariantType
    ): String


    /**
     * Returns the name of the default KotlinSourceSet for a given Android compilation.
     * Returns `null`, if this naming schema does not know about it. In this case, the
     * 'default' defaultSourceSetName will be constructed by the compilation.
     */
    fun defaultKotlinSourceSetName(compilation: AndroidCompilationDetails): String? = null

    /**
     * Always capable of creating the [KotlinSourceSet]'s name based upon the disambiguationClassifier and androidSourceSetName alone.
     */
    interface Simple : KotlinAndroidSourceSetNaming {
        override fun kotlinSourceSetName(disambiguationClassifier: String, androidSourceSetName: String): String
        override fun kotlinSourceSetName(disambiguationClassifier: String, androidSourceSetName: String, type: AndroidVariantType): String =
            kotlinSourceSetName(disambiguationClassifier, androidSourceSetName)
    }
}

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android

import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

internal object MultiplatformLayoutV2KotlinAndroidSourceSetNaming : KotlinAndroidSourceSetNaming {
    private val AndroidBaseSourceSetName.kotlinName
        get() = when (this) {
            AndroidBaseSourceSetName.Main -> "main"
            AndroidBaseSourceSetName.Test -> "unitTest"
            AndroidBaseSourceSetName.AndroidTest -> "instrumentedTest"
        }

    override fun kotlinSourceSetName(
        disambiguationClassifier: String,
        androidSourceSetName: String,
    ): String? {
        val androidBaseSourceSetName = AndroidBaseSourceSetName.byName(androidSourceSetName) ?: return null
        return lowerCamelCaseName(disambiguationClassifier, androidBaseSourceSetName.kotlinName)
    }

    override fun kotlinSourceSetName(
        disambiguationClassifier: String,
        androidSourceSetName: String,
        type: AndroidVariantType
    ): String {
        return lowerCamelCaseName(disambiguationClassifier, replaceAndroidBaseSourceSetName(androidSourceSetName, type))
    }

    private fun replaceAndroidBaseSourceSetName(
        androidSourceSetName: String,
        type: AndroidVariantType
    ): String {
        if (type == AndroidVariantType.Main) return androidSourceSetName
        val androidBaseSourceSetName = type.androidBaseSourceSetName ?: return androidSourceSetName
        return lowerCamelCaseName(androidBaseSourceSetName.kotlinName, androidSourceSetName.removePrefix(androidBaseSourceSetName.name))
    }
}

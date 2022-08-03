/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android

import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.plugin.mpp.AndroidCompilationDetails
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

internal object MultiplatformLayoutV2KotlinAndroidSourceSetNaming : KotlinAndroidSourceSetNaming {
    private val logger = Logging.getLogger(this::class.java)

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

    override fun defaultKotlinSourceSetName(compilation: AndroidCompilationDetails): String? {
        val kotlinSourceSetName: String? = run {
            val baseSourceSetName = compilation.androidVariant.type.androidBaseSourceSetName ?: return@run null
            val androidSourceSetName = lowerCamelCaseName(
                baseSourceSetName.takeIf { it != AndroidBaseSourceSetName.Main }?.name,
                compilation.androidVariant.flavorName,
                compilation.androidVariant.buildType.name
            )
            val androidSourceSet = compilation.androidVariant.sourceSets.find { it.name == androidSourceSetName } ?: return@run null
            compilation.project.findKotlinSourceSet(androidSourceSet)?.name
        }

        if (kotlinSourceSetName == null) {
            logger.warn("Can't determine 'defaultKotlinSourceSet' for android compilation: ${compilation.androidVariant.name}")
        }
        return kotlinSourceSetName
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

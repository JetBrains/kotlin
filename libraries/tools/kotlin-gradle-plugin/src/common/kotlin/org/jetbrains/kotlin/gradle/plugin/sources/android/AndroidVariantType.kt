/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android

import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.utils.*

@Suppress("TYPEALIAS_EXPANSION_DEPRECATION")
internal val DeprecatedAndroidBaseVariant.type: AndroidVariantType
    get() = when (this) {
        is DeprecatedAndroidUnitTestVariant -> AndroidVariantType.UnitTest
        is DeprecatedAndroidTestVariant -> AndroidVariantType.InstrumentedTest
        is DeprecatedAndroidApplicationVariant, is DeprecatedAndroidLibraryVariant -> AndroidVariantType.Main
        else -> AndroidVariantType.Unknown
    }

internal val AndroidBaseSourceSetName.variantType: AndroidVariantType
    get() = when (this) {
        AndroidBaseSourceSetName.Main -> AndroidVariantType.Main
        AndroidBaseSourceSetName.Test -> AndroidVariantType.UnitTest
        AndroidBaseSourceSetName.AndroidTest -> AndroidVariantType.InstrumentedTest
    }

// Required for AGP/Built-in Kotlin integration
// ABI preferably should not change
@InternalKotlinGradlePluginApi
enum class AndroidVariantType {
    Main, UnitTest, InstrumentedTest, Unknown;
}

/**
 * Every known type of Android variant has a 'base source set', which
 * participates in all variants of a said type (main, test, androidTest, ...)
 */
internal val AndroidVariantType.androidBaseSourceSetName: AndroidBaseSourceSetName?
    get() = when (this) {
        AndroidVariantType.Main -> AndroidBaseSourceSetName.Main
        AndroidVariantType.UnitTest -> AndroidBaseSourceSetName.Test
        AndroidVariantType.InstrumentedTest -> AndroidBaseSourceSetName.AndroidTest
        AndroidVariantType.Unknown -> null
    }

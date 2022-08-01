/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android

import com.android.build.gradle.api.*

internal val BaseVariant.type: AndroidVariantType
    get() = when (this) {
        is UnitTestVariant -> AndroidVariantType.UnitTest
        is TestVariant -> AndroidVariantType.InstrumentedTest
        is ApplicationVariant, is LibraryVariant -> AndroidVariantType.Main
        else -> AndroidVariantType.Unknown
    }

internal val AndroidBaseSourceSetName.variantType: AndroidVariantType
    get() = when (this) {
        AndroidBaseSourceSetName.Main -> AndroidVariantType.Main
        AndroidBaseSourceSetName.Test -> AndroidVariantType.UnitTest
        AndroidBaseSourceSetName.AndroidTest -> AndroidVariantType.InstrumentedTest
    }

internal enum class AndroidVariantType {
    Main, UnitTest, InstrumentedTest, Unknown;

    /**
     * Every known type of Android variant has a 'base source set', which
     * participates in all variants of sad type (main, test, androidTest, ...)
     */
    val androidBaseSourceSetName: AndroidBaseSourceSetName?
        get() = when (this) {
            Main -> AndroidBaseSourceSetName.Main
            UnitTest -> AndroidBaseSourceSetName.Test
            InstrumentedTest -> AndroidBaseSourceSetName.AndroidTest
            Unknown -> null
        }
}

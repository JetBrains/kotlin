/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android.configurator

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.sources.android.androidSourceSetInfo
import org.jetbrains.kotlin.gradle.plugin.sources.android.type
import org.jetbrains.kotlin.gradle.utils.*

internal object KotlinAndroidSourceSetInfoConfigurator : KotlinAndroidSourceSetConfigurator {
    override fun configureWithVariant(
        target: KotlinAndroidTarget,
        kotlinSourceSet: KotlinSourceSet,
        @Suppress("TYPEALIAS_EXPANSION_DEPRECATION") variant: DeprecatedAndroidBaseVariant
    ) {
        val info = kotlinSourceSet.androidSourceSetInfo.asMutable()
        info.androidVariantType = variant.type
        info.androidVariantNames.add(variant.name)
    }
}

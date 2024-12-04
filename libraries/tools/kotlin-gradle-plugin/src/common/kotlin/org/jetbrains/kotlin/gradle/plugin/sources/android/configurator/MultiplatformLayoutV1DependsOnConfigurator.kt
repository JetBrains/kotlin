/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android.configurator

import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.Companion.COMMON_MAIN_SOURCE_SET_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.Companion.COMMON_TEST_SOURCE_SET_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.utils.*

internal object MultiplatformLayoutV1DependsOnConfigurator : KotlinAndroidSourceSetConfigurator {
    override fun configure(
        target: KotlinAndroidTarget,
        kotlinSourceSet: KotlinSourceSet,
        @Suppress("TYPEALIAS_EXPANSION_DEPRECATION") androidSourceSet: DeprecatedAndroidSourceSet
    ) {
        val commonSourceSetName = when (androidSourceSet.name) {
            "main" -> COMMON_MAIN_SOURCE_SET_NAME
            "test" -> COMMON_TEST_SOURCE_SET_NAME
            "androidTest" -> COMMON_TEST_SOURCE_SET_NAME
            else -> return
        }
        val commonSourceSet = target.project.kotlinExtension.sourceSets.findByName(commonSourceSetName) ?: return
        kotlinSourceSet.dependsOn(commonSourceSet)
    }
}

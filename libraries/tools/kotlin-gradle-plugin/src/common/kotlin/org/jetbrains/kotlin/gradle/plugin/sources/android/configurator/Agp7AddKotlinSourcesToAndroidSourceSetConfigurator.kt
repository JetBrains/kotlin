/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android.configurator

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.utils.*

object Agp7AddKotlinSourcesToAndroidSourceSetConfigurator: KotlinAndroidSourceSetConfigurator {
    override fun configure(
        target: KotlinAndroidTarget,
        kotlinSourceSet: KotlinSourceSet,
        @Suppress("TYPEALIAS_EXPANSION_DEPRECATION") androidSourceSet: DeprecatedAndroidSourceSet
    ) {
        @Suppress("TYPEALIAS_EXPANSION_DEPRECATION")
        val androidKotlinSourceDirectorySet = androidSourceSet.javaClass.getMethod("getKotlin")
            .invoke(androidSourceSet) as DeprecatedAndroidSourceDirectorySet

        androidKotlinSourceDirectorySet.setSrcDirs(listOf(target.project.provider { kotlinSourceSet.kotlin.srcDirs }))
    }
}
/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android.configurator

import com.android.build.gradle.api.AndroidSourceDirectorySet
import com.android.build.gradle.api.AndroidSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget

object Agp7AddKotlinSourcesToAndroidSourceSetConfigurator: KotlinAndroidSourceSetConfigurator {
    override fun configure(target: KotlinAndroidTarget, kotlinSourceSet: KotlinSourceSet, androidSourceSet: AndroidSourceSet) {
        val androidKotlinSourceDirectorySet = androidSourceSet.javaClass.getMethod("getKotlin")
            .invoke(androidSourceSet) as AndroidSourceDirectorySet

        androidKotlinSourceDirectorySet.setSrcDirs(listOf(target.project.provider { kotlinSourceSet.kotlin.srcDirs }))
    }
}
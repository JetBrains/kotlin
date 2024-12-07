/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider

internal val Project.kotlinAndroidSourceSetLayout: KotlinAndroidSourceSetLayout
    get() {
        return if (kotlinExtension is KotlinMultiplatformExtension) {
            when (val version = kotlinPropertiesProvider.mppAndroidSourceSetLayoutVersion) {
                1 -> multiplatformAndroidSourceSetLayoutV1
                null, 2 -> multiplatformAndroidSourceSetLayoutV2
                else -> throw IllegalArgumentException(
                    "Unsupported '${PropertiesProvider.PropertyNames.KOTLIN_MPP_ANDROID_SOURCE_SET_LAYOUT_VERSION}=$version' Supported versions: {1, 2}"
                )
            }
        } else singleTargetAndroidSourceSetLayout
    }

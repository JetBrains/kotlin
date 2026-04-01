/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

internal val Project.kotlinAndroidSourceSetLayout: KotlinAndroidSourceSetLayout
    get() {
        return if (kotlinExtension is KotlinMultiplatformExtension) {
            multiplatformAndroidSourceSetLayoutV2
        } else singleTargetAndroidSourceSetLayout
    }

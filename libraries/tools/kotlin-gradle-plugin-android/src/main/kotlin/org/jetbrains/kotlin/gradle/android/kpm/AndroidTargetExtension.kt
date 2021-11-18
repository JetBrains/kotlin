/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.gradle.android.kpm

import com.android.build.gradle.AppExtension
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinPm20ProjectExtension
import org.jetbrains.kotlin.gradle.utils.project

fun KotlinPm20ProjectExtension.android() {
    project.extensions.getByType<AppExtension>().applicationVariants.all { androidVariant ->
        main { createKotlinAndroidVariant(androidVariant) }
        test { createKotlinAndroidVariant(androidVariant.unitTestVariant ?: return@test) }
        instrumentedTest { createKotlinAndroidVariant(androidVariant.testVariant ?: return@instrumentedTest) }
    }
}

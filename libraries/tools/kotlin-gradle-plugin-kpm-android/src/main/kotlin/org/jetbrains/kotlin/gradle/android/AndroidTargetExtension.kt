/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")
@file:OptIn(ExternalVariantApi::class)

package org.jetbrains.kotlin.gradle.android

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.kotlin.dsl.findByType
import org.jetbrains.kotlin.gradle.kpm.external.ExternalVariantApi
import org.jetbrains.kotlin.gradle.kpm.external.project
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinPm20ProjectExtension

fun KotlinPm20ProjectExtension.android() {
    project.extensions.findByType<AppExtension>()?.applicationVariants?.all { androidVariant ->
        main { createKotlinAndroidVariant(androidVariant) }
        test { createKotlinAndroidVariant(androidVariant.unitTestVariant ?: return@test) }
        instrumentedTest { createKotlinAndroidVariant(androidVariant.testVariant ?: return@instrumentedTest) }
    }

    project.extensions.findByType<LibraryExtension>()?.libraryVariants?.all { androidVariant ->
        main { createKotlinAndroidVariant(androidVariant) }
        test { createKotlinAndroidVariant(androidVariant.unitTestVariant ?: return@test) }
        instrumentedTest { createKotlinAndroidVariant(androidVariant.testVariant ?: return@instrumentedTest) }
    }
}

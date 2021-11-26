/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.android

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget

internal fun Project.findAndroidTarget(): KotlinAndroidTarget? {
    return when (val kotlinExtension = project.kotlinExtension) {
        is KotlinMultiplatformExtension -> kotlinExtension.targets.withType(KotlinAndroidTarget::class.java).singleOrNull()
        is KotlinAndroidProjectExtension -> kotlinExtension.target
        else -> null
    }
}

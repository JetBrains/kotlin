/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import com.android.build.gradle.AppExtension
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.targets.external.KotlinExternalTargetHandle
import org.jetbrains.kotlin.gradle.targets.external.dependsOnCommonMain

internal fun setupAndroidMain(externalTargetHandle: KotlinExternalTargetHandle) {
    val project = externalTargetHandle.project

    // Create 'common' source set across variants
    val androidMain = externalTargetHandle.createCommonAndroidSourceSet("androidMain")
    androidMain.dependsOnCommonMain(project)

    // Create Kotlin Compilation for variants
    project.extensions.getByType<AppExtension>().applicationVariants.all { variant ->
        project.logger.quiet("Variant: ${variant.name} source sets: ${variant.sourceSets}")
        val mainKotlinCompilation = externalTargetHandle.createKotlinCompilation(variant)
        mainKotlinCompilation.defaultSourceSet.dependsOn(androidMain)

        // Associate unitTests with main compilation
        val unitTestKotlinCompilation = externalTargetHandle.getKotlinCompilation(variant.unitTestVariant.name)
        unitTestKotlinCompilation.associateWith(mainKotlinCompilation)
    }
}

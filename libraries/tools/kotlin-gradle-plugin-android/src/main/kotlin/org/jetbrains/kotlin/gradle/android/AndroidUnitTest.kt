/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import com.android.build.gradle.AppExtension
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.targets.external.KotlinExternalTargetHandle
import org.jetbrains.kotlin.gradle.targets.external.dependsOnCommonTest


internal fun setupAndroidUnitTest(externalTargetHandle: KotlinExternalTargetHandle) {
    val project = externalTargetHandle.project

    val androidUnitTest = externalTargetHandle.createCommonAndroidSourceSet("androidUnitTest")
    androidUnitTest.dependsOnCommonTest(project)

    project.extensions.getByType<AppExtension>().unitTestVariants.all { variant ->
        project.logger.quiet("UnitTestVariant: ${variant.name} source sets: ${variant.sourceSets}")
        val kotlinCompilation = externalTargetHandle.createKotlinCompilation(variant)
        kotlinCompilation.defaultSourceSet.dependsOn(androidUnitTest)
    }
}

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.internal

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectAction
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.hierarchy.setupDefaultKotlinHierarchy

internal val MultiplatformSourceSetsSetupAction = KotlinProjectAction {
    with(multiplatformExtension) {
        /* Create 'commonMain' and 'commonTest' SourceSets */
        sourceSets.maybeCreate(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME)
        sourceSets.maybeCreate(KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME)
        project.setupDefaultKotlinHierarchy()
    }
}

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationToRunnableFiles
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget

internal val CreateDefaultCompilationsSideEffect = KotlinTargetSideEffect { target ->
    val main = target.compilations.create(KotlinCompilation.MAIN_COMPILATION_NAME)

    /* Create test compilation by default, except for metadata targets, obviously */
    if (target !is KotlinMetadataTarget) {
        target.compilations.create(KotlinCompilation.TEST_COMPILATION_NAME).apply {
            associateWith(main)

            @Suppress("DEPRECATION")
            if (this is KotlinCompilationToRunnableFiles && this !is KotlinJsIrTarget) {
                // TODO: fix inconsistency? KT-27272
                runtimeDependencyFiles += project.files(output.allOutputs)
            }
        }
    }
}

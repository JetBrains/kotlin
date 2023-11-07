/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.jetbrains.kotlin.gradle.plugin.Kotlin2JvmSourceSetProcessor
import org.jetbrains.kotlin.gradle.plugin.KotlinCommonSourceSetProcessor
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationInfo
import org.jetbrains.kotlin.gradle.plugin.KotlinJsIrSourceSetProcessor
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCommonCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider

internal val KotlinCompilationProcessorSideEffect = KotlinCompilationSideEffect { compilation ->
    val processor = when (compilation) {
        is KotlinCommonCompilation -> KotlinCommonSourceSetProcessor(KotlinCompilationInfo(compilation), KotlinTasksProvider())
        is KotlinJvmCompilation -> Kotlin2JvmSourceSetProcessor(KotlinTasksProvider(), KotlinCompilationInfo(compilation))
        is KotlinJsIrCompilation -> KotlinJsIrSourceSetProcessor(KotlinTasksProvider(), KotlinCompilationInfo(compilation))
        else -> null
    }
    processor?.run()
}

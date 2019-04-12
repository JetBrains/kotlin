 /*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformCommonOptions
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon

class KotlinCommonCompilation(
    target: KotlinTarget,
    name: String
) : AbstractKotlinCompilation<KotlinMultiplatformCommonOptions>(target, name) {
    override val compileKotlinTask: KotlinCompileCommon
        get() = super.compileKotlinTask as KotlinCompileCommon

    private val commonSourceSetName by lazy {
        when (compilationName) {
            // Historically, a metadata target has a main compilation. We keep using it to compile just the commonMain source set:
            KotlinCompilation.MAIN_COMPILATION_NAME -> KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME
            // All other common source sets are compiled by compilations named according to the source sets:
            else -> compilationName
        }
    }

    override val defaultSourceSet: KotlinSourceSet
        get() = target.project.kotlinExtension.sourceSets.getByName(commonSourceSetName)
}
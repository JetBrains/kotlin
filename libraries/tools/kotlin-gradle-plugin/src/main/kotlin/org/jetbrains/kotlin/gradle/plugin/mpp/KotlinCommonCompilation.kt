 /*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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

    // TODO once we properly compile metadata for each source set, the default source sets will likely become just the source sets
    // which are transformed to metadata
    private val commonSourceSetName = when (compilationName) {
        KotlinCompilation.MAIN_COMPILATION_NAME -> KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME
        else -> error("Custom metadata compilations are not supported yet")
    }

    override val defaultSourceSet: KotlinSourceSet
        get() = target.project.kotlinExtension.sourceSets.getByName(commonSourceSetName)
}
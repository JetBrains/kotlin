 /*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformCommonOptions
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon

interface KotlinMetadataCompilation<T : KotlinCommonOptions> : KotlinCompilation<T>

class KotlinCommonCompilation(
    target: KotlinTarget,
    name: String
) : AbstractKotlinCompilation<KotlinMultiplatformCommonOptions>(target, name), KotlinMetadataCompilation<KotlinMultiplatformCommonOptions> {
    override val compileKotlinTask: KotlinCompileCommon
        get() = super.compileKotlinTask as KotlinCompileCommon
}
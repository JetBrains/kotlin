/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.konan.target.HostManager

@Suppress("unused")
/* Receiver acts as scope, or key to that function */
internal fun IdeaKpmProjectModelBuildingContext.IdeaKpmPlatform(variant: GradleKpmVariant): IdeaKpmPlatform {
    when (variant) {
        is GradleKpmJvmVariant -> return IdeaKpmPlatform.jvm(variant.compilationData.kotlinOptions.jvmTarget ?: JvmTarget.DEFAULT.name)
        is GradleKpmNativeVariantInternal -> return IdeaKpmPlatform.native(variant.konanTarget.name)
        is GradleKpmLegacyMappedVariant -> when (val compilation = variant.compilation) {
            is KotlinJvmCompilation -> return IdeaKpmPlatform.jvm(compilation.kotlinOptions.jvmTarget ?: JvmTarget.DEFAULT.name)
            is KotlinJvmAndroidCompilation -> return IdeaKpmPlatform.jvm(compilation.kotlinOptions.jvmTarget ?: JvmTarget.DEFAULT.name)
            is KotlinNativeCompilation -> return IdeaKpmPlatform.native(compilation.konanTarget.name)
            is KotlinJsIrCompilation -> return IdeaKpmPlatform.js(isIr = true)
            is KotlinJsCompilation -> return IdeaKpmPlatform.js(isIr = false)
        }
    }

    /* Fallback calculation based on 'platformType' alone */
    /* This is a last line of defence, not expected to be actually executed */
    assert(false) { "Unable to build 'IdeaKpmPlatform' from variant ${variant.path}" }
    return when (variant.platformType) {
        KotlinPlatformType.common -> throw IllegalArgumentException("Unexpected platformType 'common' for variant ${variant.name}")
        KotlinPlatformType.jvm -> IdeaKpmPlatform.jvm(JvmTarget.DEFAULT.name)
        KotlinPlatformType.androidJvm -> IdeaKpmPlatform.jvm(JvmTarget.DEFAULT.name)
        KotlinPlatformType.js -> IdeaKpmPlatform.js(false)
        KotlinPlatformType.native -> IdeaKpmPlatform.native(HostManager.host.name)
        KotlinPlatformType.wasm -> IdeaKpmPlatform.wasm()
    }
}

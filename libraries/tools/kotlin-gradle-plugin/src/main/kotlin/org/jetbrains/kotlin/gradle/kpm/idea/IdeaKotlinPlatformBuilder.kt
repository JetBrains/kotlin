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
internal fun IdeaKotlinProjectModelBuildingContext.IdeaKotlinPlatform(variant: KotlinGradleVariant): IdeaKotlinPlatform {
    when (variant) {
        is KotlinJvmVariant -> return IdeaKotlinPlatform.jvm(variant.compilationData.kotlinOptions.jvmTarget ?: JvmTarget.DEFAULT.name)
        is KotlinNativeVariantInternal -> return IdeaKotlinPlatform.native(variant.konanTarget.name)
        is LegacyMappedVariant -> when (val compilation = variant.compilation) {
            is KotlinJvmCompilation -> return IdeaKotlinPlatform.jvm(compilation.kotlinOptions.jvmTarget ?: JvmTarget.DEFAULT.name)
            is KotlinJvmAndroidCompilation -> return IdeaKotlinPlatform.jvm(compilation.kotlinOptions.jvmTarget ?: JvmTarget.DEFAULT.name)
            is KotlinNativeCompilation -> return IdeaKotlinPlatform.native(compilation.konanTarget.name)
            is KotlinJsIrCompilation -> return IdeaKotlinPlatform.js(isIr = true)
            is KotlinJsCompilation -> return IdeaKotlinPlatform.js(isIr = false)
        }
    }

    /* Fallback calculation based on 'platformType' alone */
    /* This is a last line of defence, not expected to be actually executed */
    assert(false) { "Unable to build 'IdeaKotlinPlatform' from variant ${variant.path}" }
    return when (variant.platformType) {
        KotlinPlatformType.common -> throw IllegalArgumentException("Unexpected platformType 'common' for variant ${variant.name}")
        KotlinPlatformType.jvm -> IdeaKotlinPlatform.jvm(JvmTarget.DEFAULT.name)
        KotlinPlatformType.androidJvm -> IdeaKotlinPlatform.jvm(JvmTarget.DEFAULT.name)
        KotlinPlatformType.js -> IdeaKotlinPlatform.js(false)
        KotlinPlatformType.native -> IdeaKotlinPlatform.native(HostManager.host.name)
        KotlinPlatformType.wasm -> IdeaKotlinPlatform.wasm()
    }
}

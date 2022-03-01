/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm

import org.gradle.api.Project

internal fun registerDefaultVariantFactories(project: Project) {
    project.kpmModules.configureEach { module ->
        module.fragments.registerFactory(
            KotlinJvmVariant::class.java,
            KotlinJvmVariantFactory(module)
        )

        fun <T : KotlinNativeVariantInternal> registerNativeVariantFactory(
            constructor: KotlinNativeVariantConstructor<T>
        ) = module.fragments.registerFactory(
            constructor.variantClass, KotlinNativeVariantFactory(module, constructor)
        )

        allKpmNativeVariantConstructors.forEach { constructor -> registerNativeVariantFactory(constructor) }
    }
}

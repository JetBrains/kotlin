/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.pm20Extension
import kotlin.reflect.KClass

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

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.pm20Extension

internal fun registerDefaultVariantFactories(project: Project) {
    project.pm20Extension.modules.configureEach { module ->
        module.fragments.registerFactory(
            GradleKpmJvmVariant::class.java,
            GradleKpmJvmVariantFactory(module)
        )

        fun <T : GradleKpmNativeVariantInternal> registerNativeVariantFactory(
            constructor: GradleKpmNativeVariantConstructor<T>
        ) = module.fragments.registerFactory(
            constructor.variantClass, GradleKpmNativeVariantFactory(module, constructor)
        )

        allKpmNativeVariantConstructors.forEach { constructor -> registerNativeVariantFactory(constructor) }
    }
}

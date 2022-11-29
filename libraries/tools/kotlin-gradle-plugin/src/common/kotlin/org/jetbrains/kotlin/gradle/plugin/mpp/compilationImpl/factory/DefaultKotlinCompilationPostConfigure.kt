/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory

import org.jetbrains.kotlin.gradle.plugin.mpp.DecoratedKotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.InternalKotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.sources.internal

internal object DefaultKotlinCompilationPostConfigure : KotlinCompilationImplFactory.PostConfigure {
    override fun configure(compilation: DecoratedKotlinCompilation<*>) {
        /* Ensure proper tracking of compilations in the source sets */
        compilation.allKotlinSourceSets.forAll { sourceSet ->
            sourceSet.internal.compilations.add(compilation)
        }
    }
}
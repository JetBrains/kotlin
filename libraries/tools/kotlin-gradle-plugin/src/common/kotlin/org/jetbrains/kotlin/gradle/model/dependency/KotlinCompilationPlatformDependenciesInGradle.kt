/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model.dependency

import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.build.KotlinCompileDependencyScopeModel
import org.jetbrains.kotlin.build.KotlinDependencyScopeModel
import org.jetbrains.kotlin.build.KotlinPlatformDependenciesModel
import org.jetbrains.kotlin.gradle.model.KotlinPlatformLibrariesInGradle
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

internal sealed class GradleKotlinDependencyScope : KotlinDependencyScopeModel {
    object COMPILE : GradleKotlinDependencyScope(), KotlinCompileDependencyScopeModel
    object RUNTIME : GradleKotlinDependencyScope()
}

internal class KotlinCompilationPlatformDependenciesInGradle(
    val compilation: KotlinCompilation<*>,
    val scope: GradleKotlinDependencyScope,
) : KotlinPlatformDependenciesModel, KotlinPlatformLibrariesInGradle {
    override val files: FileCollection
        get() = when (scope) {
            GradleKotlinDependencyScope.COMPILE -> compilation.compileDependencyFiles
            GradleKotlinDependencyScope.RUNTIME -> compilation.runtimeDependencyFiles
                ?: error("Runtime dependencies are not supported by $compilation")
        }
}
/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model.compilation

import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.build.KotlinPlatformCompilationOutputModel
import org.jetbrains.kotlin.gradle.model.KotlinPlatformLibrariesInGradle
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

internal class KotlinPlatformCompilationOutputInGradle(
    private val kotlinCompilation: KotlinCompilation<*>
) : KotlinPlatformCompilationOutputModel, KotlinPlatformLibrariesInGradle {
    override val files: FileCollection
        get() = kotlinCompilation.output.classesDirs
}
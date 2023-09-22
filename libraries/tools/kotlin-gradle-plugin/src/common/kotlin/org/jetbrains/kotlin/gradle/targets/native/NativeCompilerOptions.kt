/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeCompilerOptionsDefault
import org.jetbrains.kotlin.gradle.plugin.HasCompilerOptions
import org.jetbrains.kotlin.gradle.utils.configureExperimentalTryK2

@Deprecated("This type is not in use anymore")
class NativeCompilerOptions(override val options: KotlinNativeCompilerOptions) : HasCompilerOptions<KotlinNativeCompilerOptions>,
    KotlinNativeCompilerOptions by options {
    constructor(project: Project) : this(
        project.objects
            .newInstance(KotlinNativeCompilerOptionsDefault::class.java)
            .configureExperimentalTryK2(project)
    )
}

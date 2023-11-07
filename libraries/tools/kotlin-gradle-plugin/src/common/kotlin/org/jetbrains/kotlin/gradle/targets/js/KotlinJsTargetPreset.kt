/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch")

// Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation

@Deprecated("The Kotlin/JS legacy target is deprecated and its support completely discontinued", level = DeprecationLevel.WARNING)
abstract class KotlinJsTargetPreset(
    project: Project
) : KotlinOnlyTargetPreset<KotlinOnlyTarget<KotlinJsIrCompilation>, KotlinJsIrCompilation>(
    project
)

@Suppress("DEPRECATION")
@Deprecated("The Kotlin/JS legacy target is deprecated and its support completely discontinued", level = DeprecationLevel.HIDDEN)
abstract class KotlinJsSingleTargetPreset(
    project: Project
) : KotlinJsTargetPreset(
    project
)

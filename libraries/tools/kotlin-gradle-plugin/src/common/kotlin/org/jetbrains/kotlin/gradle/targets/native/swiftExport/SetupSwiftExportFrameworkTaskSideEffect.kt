/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.swiftExport

import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.registerSwiftExportFrameworkTask
import org.jetbrains.kotlin.gradle.targets.KotlinTargetSideEffect

internal val SetupSwiftExportFrameworkTaskSideEffect = KotlinTargetSideEffect<KotlinNativeTarget> { target ->
    if (!target.konanTarget.family.isAppleFamily) return@KotlinTargetSideEffect
    if (!target.project.kotlinPropertiesProvider.swiftExportEnabled) return@KotlinTargetSideEffect
    target.binaries.withType(Framework::class.java).all { framework ->
        target.project.registerSwiftExportFrameworkTask(framework)
    }
}
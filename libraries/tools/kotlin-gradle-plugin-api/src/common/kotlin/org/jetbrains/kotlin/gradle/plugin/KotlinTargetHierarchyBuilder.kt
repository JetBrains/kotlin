/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

@ExperimentalKotlinGradlePluginApi
interface KotlinTargetHierarchyBuilder {
    val target: KotlinTarget
    val compilation: KotlinCompilation<*>

    val isNative: Boolean
    val isApple: Boolean
    val isIos: Boolean
    val isWatchos: Boolean
    val isMacos: Boolean
    val isTvos: Boolean
    val isWindows: Boolean
    val isLinux: Boolean
    val isAndroidNative: Boolean
    val isJvm: Boolean
    val isAndroidJvm: Boolean
    val isJsLegacy: Boolean
    val isJsIr: Boolean
    val isJs: Boolean

    fun common(build: KotlinTargetHierarchyBuilder.() -> Unit) = group("common", build)
    fun group(name: String, build: KotlinTargetHierarchyBuilder.() -> Unit = {})
}

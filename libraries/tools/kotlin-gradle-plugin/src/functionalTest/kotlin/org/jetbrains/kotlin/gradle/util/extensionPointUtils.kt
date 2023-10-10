/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinGradlePluginExtensionPoint
import org.jetbrains.kotlin.gradle.plugin.KotlinGradlePluginExtensionPointInternal
import org.jetbrains.kotlin.tooling.core.UnsafeApi

/**
 * Completely overwrites the currently registered extensions on this [KotlinGradlePluginExtensionPoint] in this project.
 */
@OptIn(UnsafeApi::class)
operator fun <T> KotlinGradlePluginExtensionPoint<T>.set(project: Project, extensions: List<T>) {
    (this as KotlinGradlePluginExtensionPointInternal<T>)
    set(project, extensions)
}

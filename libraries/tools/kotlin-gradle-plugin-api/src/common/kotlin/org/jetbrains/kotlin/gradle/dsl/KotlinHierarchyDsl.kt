/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyTemplate

interface KotlinHierarchyDsl {
    fun applyHierarchyTemplate(template: KotlinHierarchyTemplate)

    @ExperimentalKotlinGradlePluginApi
    fun applyHierarchyTemplate(template: KotlinHierarchyTemplate, extension: KotlinHierarchyBuilder.Root.() -> Unit)

    @ExperimentalKotlinGradlePluginApi
    fun applyHierarchyTemplate(template: KotlinHierarchyBuilder.Root.() -> Unit)
}
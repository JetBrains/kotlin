/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.gradle.dsl

import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyTemplate

@Deprecated("Use 'KotlinHierarchyTemplate's instead.")
class DeprecatedKotlinTargetHierarchyDsl internal constructor(
    private val extension: KotlinMultiplatformExtension,
) : KotlinTargetHierarchyDsl {

    @Deprecated("Replace with 'kotlin.applyHierarchyTemplate'")
    override fun apply(hierarchyDescriptor: KotlinHierarchyTemplate, describeExtension: (KotlinHierarchyBuilder.Root.() -> Unit)?) {
        if (describeExtension != null) extension.applyHierarchyTemplate(hierarchyDescriptor, describeExtension)
        else extension.applyHierarchyTemplate(hierarchyDescriptor)
    }

    @Deprecated("Replace with 'kotlin.applyDefaultHierarchyTemplate'")
    override fun default(describeExtension: (KotlinHierarchyBuilder.Root.() -> Unit)?) {
        if (describeExtension != null) extension.applyDefaultHierarchyTemplate(describeExtension)
        else extension.applyDefaultHierarchyTemplate()
    }

    @Deprecated("Replace with 'kotlin.applyHierarchyTemplate'")
    override fun custom(describe: KotlinHierarchyBuilder.Root.() -> Unit) {
        extension.applyHierarchyTemplate(describe)
    }
}
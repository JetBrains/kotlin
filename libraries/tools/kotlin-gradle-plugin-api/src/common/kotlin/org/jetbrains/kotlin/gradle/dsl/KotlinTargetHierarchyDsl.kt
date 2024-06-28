/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyTemplate

/**
 * @suppress
 */
@Deprecated("Use 'KotlinHierarchyTemplate' instead")
@ExperimentalKotlinGradlePluginApi
interface KotlinTargetHierarchyDsl {
    fun apply(hierarchyDescriptor: KotlinHierarchyTemplate, describeExtension: (KotlinHierarchyBuilder.Root.() -> Unit)? = null)
    fun default(describeExtension: (KotlinHierarchyBuilder.Root.() -> Unit)? = null)
    fun custom(describe: KotlinHierarchyBuilder.Root.() -> Unit)
}

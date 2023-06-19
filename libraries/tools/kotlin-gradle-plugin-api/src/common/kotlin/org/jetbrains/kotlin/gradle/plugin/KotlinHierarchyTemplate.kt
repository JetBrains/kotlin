/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi



sealed interface KotlinHierarchyTemplate {
    companion object Templates
}

/*
EXPERIMENTAL API
 */

@ExperimentalKotlinGradlePluginApi
fun KotlinHierarchyTemplate(
    describe: KotlinHierarchyBuilder.Root.() -> Unit
): KotlinHierarchyTemplate {
    return KotlinHierarchyTemplateImpl(describe)
}

@ExperimentalKotlinGradlePluginApi
fun KotlinHierarchyTemplate.extend(describe: KotlinHierarchyBuilder.Root.() -> Unit): KotlinHierarchyTemplate {
    return KotlinHierarchyTemplate {
        this@extend.internal.layout(this)
        describe()
    }
}

/*
INTERNAL API
 */

@InternalKotlinGradlePluginApi
@OptIn(ExperimentalKotlinGradlePluginApi::class)
fun KotlinHierarchyBuilder.Root.applyHierarchyTemplate(template: KotlinHierarchyTemplate) {
    template.internal.layout(this)
}

internal val KotlinHierarchyTemplate.internal
    get() = when (this) {
        is InternalKotlinHierarchyTemplate -> this
    }

@OptIn(ExperimentalKotlinGradlePluginApi::class)
internal interface InternalKotlinHierarchyTemplate : KotlinHierarchyTemplate {
    fun layout(builder: KotlinHierarchyBuilder.Root)
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
internal class KotlinHierarchyTemplateImpl(
    private val describe: KotlinHierarchyBuilder.Root.() -> Unit
) : InternalKotlinHierarchyTemplate {
    override fun layout(builder: KotlinHierarchyBuilder.Root) {
        describe(builder)
    }
}

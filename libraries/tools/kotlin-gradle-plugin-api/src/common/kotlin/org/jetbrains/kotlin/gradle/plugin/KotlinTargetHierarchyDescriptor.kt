/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

@ExperimentalKotlinGradlePluginApi
interface KotlinTargetHierarchyDescriptor {
    fun describe(builder: KotlinTargetHierarchyBuilder.Root)
    fun extend(describe: KotlinTargetHierarchyBuilder.Root.() -> Unit): KotlinTargetHierarchyDescriptor
}

@ExperimentalKotlinGradlePluginApi
fun KotlinTargetHierarchyDescriptor(
    describe: KotlinTargetHierarchyBuilder.Root.() -> Unit
): KotlinTargetHierarchyDescriptor {
    return KotlinTargetHierarchyDescriptorImpl(describe)
}

@ExperimentalKotlinGradlePluginApi
private class KotlinTargetHierarchyDescriptorImpl(
    private val describe: KotlinTargetHierarchyBuilder.Root.() -> Unit
) : KotlinTargetHierarchyDescriptor {

    override fun extend(describe: KotlinTargetHierarchyBuilder.Root.() -> Unit): KotlinTargetHierarchyDescriptor {
        val sourceDescribe = this.describe
        return KotlinTargetHierarchyDescriptor {
            sourceDescribe()
            describe()
        }
    }

    override fun describe(builder: KotlinTargetHierarchyBuilder.Root) = builder.describe()
}

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.android

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.FinaliseDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetHierarchy
import org.jetbrains.kotlin.gradle.plugin.newKotlinPluginLifecycleAwareProperty

@ExperimentalKotlinGradlePluginApi
interface KotlinAndroidTargetVariantTypeDsl {
    interface TargetHierarchyDsl {
        val module: Property<KotlinTargetHierarchy.ModuleName>
    }

    val targetHierarchy: TargetHierarchyDsl
    fun targetHierarchy(configure: TargetHierarchyDsl.() -> Unit): Unit = targetHierarchy.configure()
    fun targetHierarchy(configure: Action<@UnsafeVariance TargetHierarchyDsl>): Unit = configure.execute(targetHierarchy)
}


internal class KotlinAndroidTargetVariantTypeDslImpl(private val project: Project) : KotlinAndroidTargetVariantTypeDsl {
    internal inner class TargetHierarchyDslImpl : KotlinAndroidTargetVariantTypeDsl.TargetHierarchyDsl {
        override val module: Property<KotlinTargetHierarchy.ModuleName> by project.newKotlinPluginLifecycleAwareProperty(FinaliseDsl)
    }

    override val targetHierarchy: KotlinAndroidTargetVariantTypeDsl.TargetHierarchyDsl = TargetHierarchyDslImpl()
}
/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidVariantHierarchyDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetHierarchy
import org.jetbrains.kotlin.gradle.utils.property

@ExperimentalKotlinGradlePluginApi
interface KotlinAndroidTargetHierarchyDsl {
    val main: KotlinAndroidVariantHierarchyDsl
    val unitTest: KotlinAndroidVariantHierarchyDsl
    val instrumentedTest: KotlinAndroidVariantHierarchyDsl
}

internal class KotlinAndroidTargetHierarchyDslImpl(objects: ObjectFactory) : KotlinAndroidTargetHierarchyDsl {
    override val main: KotlinAndroidVariantHierarchyDsl = KotlinAndroidVariantHierarchyDslImpl(objects)
    override val unitTest: KotlinAndroidVariantHierarchyDsl = KotlinAndroidVariantHierarchyDslImpl(objects)
    override val instrumentedTest: KotlinAndroidVariantHierarchyDsl = KotlinAndroidVariantHierarchyDslImpl(objects)
}

internal class KotlinAndroidVariantHierarchyDslImpl(objects: ObjectFactory) : KotlinAndroidVariantHierarchyDsl {
    override val sourceSetTree: Property<KotlinTargetHierarchy.SourceSetTree> = objects.property()
}
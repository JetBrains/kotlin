/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinTargetHierarchyDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetHierarchy
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetHierarchy.SourceSetTree
import org.jetbrains.kotlin.gradle.utils.property

@ExperimentalKotlinGradlePluginApi
interface KotlinAndroidTargetHierarchyDsl {
    val main: KotlinAndroidVariantHierarchyDsl
    val unitTest: KotlinAndroidVariantHierarchyDsl
    val instrumentedTest: KotlinAndroidVariantHierarchyDsl
}


@ExperimentalKotlinGradlePluginApi
interface KotlinAndroidVariantHierarchyDsl {
    /**
     * Configures under which [SourceSetTree] the currently configured Android Variant shall be placed.
     * e.g.
     *
     * ```kotlin
     * kotlin {
     *     targetHierarchy.android {
     *         instrumentedTest.sourceSetTree.set(SourceSetTree.test)
     *     }
     * }
     * ```
     *
     * Will ensure that all android instrumented tests (androidInstrumentedTest, androidInstrumentedTestDebug, ...)
     * will be placed into the 'test' SourceSet tree (with 'commonTest' as root)
     *
     * See [KotlinTargetHierarchyDsl.android]
     */
    val sourceSetTree: Property<SourceSetTree>
}

internal class KotlinAndroidTargetHierarchyDslImpl(objects: ObjectFactory) : KotlinAndroidTargetHierarchyDsl {
    override val main: KotlinAndroidVariantHierarchyDsl = KotlinAndroidVariantHierarchyDslImpl(objects)
    override val unitTest: KotlinAndroidVariantHierarchyDsl = KotlinAndroidVariantHierarchyDslImpl(objects)
    override val instrumentedTest: KotlinAndroidVariantHierarchyDsl = KotlinAndroidVariantHierarchyDslImpl(objects)
}

internal class KotlinAndroidVariantHierarchyDslImpl(objects: ObjectFactory) : KotlinAndroidVariantHierarchyDsl {
    override val sourceSetTree: Property<SourceSetTree> = objects.property()
}
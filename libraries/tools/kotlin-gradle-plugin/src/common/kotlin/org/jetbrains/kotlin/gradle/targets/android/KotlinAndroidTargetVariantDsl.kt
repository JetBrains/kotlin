/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.utils.property


@ExperimentalKotlinGradlePluginApi
interface KotlinAndroidTargetVariantDsl {
    /**
     * Configures under which [KotlinSourceSetTree] the currently configured Android Variant shall be placed.
     * e.g.
     *
     * ```kotlin
     * kotlin {
     *     androidTarget().instrumentedTest {
     *         sourceSetTree.set(SourceSetTree.test)
     *     }
     * }
     * ```
     *
     * Will ensure that all android instrumented tests (androidInstrumentedTest, androidInstrumentedTestDebug, ...)
     * will be placed into the 'test' SourceSet tree (with 'commonTest' as root)
     */
    val sourceSetTree: Property<KotlinSourceSetTree>
}

internal class KotlinAndroidTargetVariantDslImpl(objects: ObjectFactory) : KotlinAndroidTargetVariantDsl {
    override val sourceSetTree: Property<KotlinSourceSetTree> = objects.property()
}

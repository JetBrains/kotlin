/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetHierarchyBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetHierarchyDescriptor

@ExperimentalKotlinGradlePluginApi
interface KotlinTargetHierarchyDsl {
    fun apply(
        hierarchyDescriptor: KotlinTargetHierarchyDescriptor,
        describeExtension: (KotlinTargetHierarchyBuilder.Root.() -> Unit)? = null
    )

    /**
     * Set's up a 'natural'/'default' hierarchy withing [KotlinTarget]'s in the project.
     *
     * #### Example 1
     *
     * ```kotlin
     * kotlin {
     *     targetHierarchy.default() // <- position of this call is not relevant!
     *
     *     iosX64()
     *     iosArm64()
     *     linuxX64()
     *     linuxArm64()
     * }
     * ```
     *
     * Will create the following SourceSets:
     * `[iosMain, iosTest, appleMain, appleTest, linuxMain, linuxTest, nativeMain, nativeTest]
     *
     *
     * Hierarchy:
     * ```
     *                                                                     common
     *                                                                        |
     *                                                      +-----------------+-------------------+
     *                                                      |                                     |
     *
     *                                                    native                                 ...
     *
     *                                                     |
     *                                                     |
     *                                                     |
     *         +----------------------+--------------------+-----------------------+
     *         |                      |                    |                       |
     *
     *       apple                  linux                mingw              androidNative
     *
     *         |
     *  +-----------+------------+------------+
     *  |           |            |            |
     *
     * macos       ios         tvos        watchos
     * ```
     *
     * #### Example 2: Adding custom groups
     * Let's imagine we would additionally like to share code between linux and apple (unixLike)
     *
     * ```kotlin
     * kotlin {
     *     targetHierarchy.default { target ->
     *         group("native") { // <- we can re-declare already existing groups and connect children to it!
     *             group("unixLike") {
     *                 withLinux()
     *                 withApple()
     *             }
     *         }
     *     }
     * }
     * ```
     *
     * @param describeExtension: Additional groups can  be described to extend the 'default'/'natural' hierarchy:
     * @see KotlinTargetHierarchyDescriptor.extend
     */
    fun default(describeExtension: (KotlinTargetHierarchyBuilder.Root.() -> Unit)? = null)
    fun custom(describe: KotlinTargetHierarchyBuilder.Root.() -> Unit)
}

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetHierarchy.SourceSetTree
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

    /**
     * Allows to create a fully custom hierarchy (no defaults applied)
     * Note: Using the custom hierarchy will also require to set the edges to 'commonMain' and 'commonTest' SourceSets by
     * using the `common` group.
     *
     * ####  Example 1:
     * Sharing code between iOS and a jvmTarget:
     * ```kotlin
     * targetHierarchy.custom {
     *     common {
     *         withJvm()
     *         group("ios") {
     *             withIos()
     *         }
     *     }
     * }
     * ```
     *
     * Will create two [SourceSetTree] using the 'common' and 'ios' groups, applied on the "test" and "main" compilations:
     * When the following targets are specified:
     * - jvm()
     * - iosX64()
     * - iosArm64()
     * ```
     *                    "main"                               "test"
     *                  commonMain                           commonTest
     *                      |                                    |
     *                      |                                    |
     *           +----------+----------+              +----------+----------+
     *           |                     |              |                     |
     *         iosMain               jvmMain        iosTest               jvmTest
     *           |                                    |
     *      +----+-----+                         +----+-----+
     *      |          |                         |          |
     * iosX64Main   iosArm64Main            iosX64Test   iosArm64Test
     * ```
     *
     * #### Example 2: Creating a 'diamond structure'
     * ```kotlin
     * targetHierarchy.custom {
     *     common {
     *         group("ios") {
     *             withIos()
     *         }
     *
     *         group("frontend") {
     *             withJvm()
     *             group("ios") // <- ! We can again reference the 'ios' group
     *         }
     *
     *         group("apple") {
     *             withMacos()
     *             group("ios") // <- ! We can again reference the 'ios' group
     *         }
     *     }
     * }
     * ```
     *
     * In this case, the _group_ "ios" can be created with 'group("ios")' and later referenced with the same construction to build
     * the tree. Applying the descriptor from the example to the following targets:
     * - iosX64()
     * - iosArm64()
     * - macosX64()
     * - jvm()
     *
     * will create the following 'main' SourceSetTree:
     *
     * ```
     *                      commonMain
     *                           |
     *              +------------+----------+
     *              |                       |
     *          frontendMain            appleMain
     *              |                        |
     *    +---------+------------+-----------+----------+
     *    |                      |                      |
     * jvmMain                iosMain               macosX64Main
     *                           |
     *                           |
     *                      +----+----+
     *                      |         |
     *                iosX64Main   iosArm64Main
     * ```
     */
    fun custom(describe: KotlinTargetHierarchyBuilder.Root.() -> Unit)
}
